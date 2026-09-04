/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.util;

import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.auth.AuthPayload;
import org.cloudburstmc.protocol.bedrock.data.auth.AuthType;
import org.cloudburstmc.protocol.bedrock.data.auth.CertificateChainPayload;
import org.cloudburstmc.protocol.bedrock.data.auth.TokenPayload;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.ServerToClientHandshakePacket;
import org.cloudburstmc.protocol.bedrock.packet.SubClientLoginPacket;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult.IdentityData;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.cumulus.response.result.FormResponseResult;
import org.geysermc.cumulus.response.result.ValidFormResponseResult;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.CodecProcessor;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.auth.AuthData;
import org.geysermc.geyser.session.auth.BedrockClientData;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BiConsumer;

public class LoginEncryptionUtils {
    private static boolean HAS_SENT_ENCRYPTION_MESSAGE = false;

    public static void encryptPlayerConnection(GeyserSession session, LoginPacket loginPacket) {
        encryptConnectionWithCert(session, loginPacket.getAuthPayload(), loginPacket.getClientJwt());
    }

    /**
     * Authenticates a split-screen sub-client from its {@link SubClientLoginPacket} and prepares its
     * session to connect to the Java server.
     *
     * <p>This is deliberately not a variant of {@link #encryptConnectionWithCert}. Codec,
     * compression and encryption all belong to the console's connection as a whole, are already
     * established by the primary session before any guest can join, and
     * {@link org.cloudburstmc.protocol.bedrock.BedrockSession} throws if a sub-client tries to set
     * any of them. So this path validates identity and touches the transport not at all.
     *
     * @return true if the sub-client was authenticated and may proceed to connect
     */
    public static boolean setupSubClientSession(GeyserSession session, SubClientLoginPacket subClientLoginPacket) {
        GeyserImpl geyser = session.getGeyser();
        try {
            AuthPayload authPayload = subClientLoginPacket.getAuthPayload();
            String jwt = subClientLoginPacket.getClientJwt();
            if (authPayload == null || jwt == null) {
                geyser.getLogger().warning("Split-screen: sub-client sent a login with no auth payload; rejecting it.");
                return false;
            }

            ChainValidationResult result = EncryptionUtils.validatePayload(authPayload);
            if (!result.signed() && geyser.config().advanced().bedrock().validateBedrockLogin()) {
                geyser.getLogger().warning("Split-screen: sub-client login chain is unsigned and "
                        + "validate-bedrock-login is on; rejecting it.");
                return false;
            }

            PublicKey identityPublicKey = result.identityClaims().parsedIdentityPublicKey();
            byte[] clientDataPayload = EncryptionUtils.verifyClientData(jwt, identityPublicKey);
            if (clientDataPayload == null) {
                geyser.getLogger().warning("Split-screen: could not verify sub-client client data; rejecting it.");
                return false;
            }

            GeyserSession parent = findPrimarySession(geyser, session);
            if (parent == null) {
                // Should be unreachable: the peer only creates a sub-client session for a connection
                // that already has a primary one.
                geyser.getLogger().warning("Split-screen: sub-client login with no primary session on the "
                        + "same connection; rejecting it.");
                return false;
            }
            if (parent.getAuthData() == null) {
                // getAllSessions() includes sessions still logging in, and resolving a guest's
                // identity reads the host's. Rejecting beats an NPE swallowed by the catch below.
                geyser.getLogger().warning("Split-screen: sub-client login arrived before the host "
                        + "finished logging in; rejecting it.");
                return false;
            }
            session.setParentSession(parent);

            BedrockClientData data = JsonUtils.fromJson(clientDataPayload, BedrockClientData.class);
            data.setOriginalString(jwt);

            // A guest has no options screen of its own, so a console leaves these empty and the
            // player inherits whatever the console is set to.
            BedrockClientData parentData = parent.getClientData();
            if (parentData != null) {
                if (data.getLanguageCode() == null) {
                    data.setLanguageCode(parentData.getLanguageCode());
                }
                if (data.getServerAddress() == null) {
                    data.setServerAddress(parentData.getServerAddress());
                }
            }

            IdentityData extraData = result.identityClaims().extraData;
            AuthData authData = resolveSubClientAuthData(geyser, session, parent, extraData, result);
            session.setAuthData(authData);

            if (authPayload instanceof TokenPayload tokenPayload) {
                session.setToken(tokenPayload.getToken());
            } else if (authPayload instanceof CertificateChainPayload certificateChainPayload) {
                session.setCertChainData(certificateChainPayload.getChain());
            }

            session.setClientData(data);
            return true;
        } catch (Exception ex) {
            geyser.getLogger().error("Failed to set up split-screen sub-client session", ex);
            return false;
        }
    }

    /**
     * Finds the session for the player who signed in first on this console - the one that owns the
     * shared connection.
     */
    private static @Nullable GeyserSession findPrimarySession(GeyserImpl geyser, GeyserSession subClient) {
        BedrockPeer peer = subClient.getUpstream().getSession().getPeer();
        for (GeyserSession other : geyser.getSessionManager().getAllSessions()) {
            if (other == subClient) {
                continue;
            }
            BedrockServerSession otherUpstream = other.getUpstream().getSession();
            if (otherUpstream.getPeer() == peer && !otherUpstream.isSubClient()) {
                return other;
            }
        }
        return null;
    }

    /**
     * Builds a sub-client's identity, filling in whatever the console left out.
     *
     * <p>This is the awkward part of split-screen. A guest signing in on a second controller has no
     * Xbox Live identity of its own, and consoles have long shipped an XUID of {@code ""} for that
     * player - reported on PlayStation and Switch alike, and tracked upstream as
     * <a href="https://bugs.mojang.com/browse/MCPE-71033">MCPE-71033</a>. Geyser keys sessions by
     * XUID, so two such players would collide with each other on the very first check.
     *
     * <p>What gets synthesized, in descending order of how stable it is:
     * <ol>
     *   <li>a real XUID, if the console sent one - nothing is invented;</li>
     *   <li>otherwise one derived from the identity UUID, if that is present;</li>
     *   <li>otherwise one derived from the primary player's XUID and this guest's controller slot,
     *       which is stable across leaving and rejoining because the slot is.</li>
     * </ol>
     *
     * <p>A synthesized XUID is <b>not</b> proof of identity and must not be treated as one. It is
     * safe here only because it is derived from the primary player's already-validated XUID, so it
     * cannot collide across consoles, and because it never leaves this server. On an online-mode or
     * Floodgate setup, where the XUID is what links a Bedrock player to a real account, this is not
     * good enough and the guest should be refused instead - see the note on the class.
     */
    private static AuthData resolveSubClientAuthData(GeyserImpl geyser, GeyserSession session,
                                                     GeyserSession parent, IdentityData extraData,
                                                     ChainValidationResult result) {
        int slot = session.getUpstream().getSubClientId();

        Long rawIssuedAt = (Long) result.rawIdentityClaims().get("iat");
        long issuedAt = rawIssuedAt != null ? rawIssuedAt : -1;

        // Log what the console actually sent before touching any of it. This is the one place that
        // records how a given platform behaves, and the answer differs per platform.
        geyser.getLogger().info(String.format(
                "Split-screen: sub-client joining in slot %d behind %s - xuid=%s, displayName=%s, "
                        + "identity=%s, minecraftId=%s, signed=%s",
                slot, parent.bedrockUsername(),
                describe(extraData.xuid), describe(extraData.displayName),
                extraData.identity, describe(extraData.minecraftId), result.signed()));

        // The identity UUID is only worth deriving from if it actually identifies this guest. A
        // console with nothing to say about a guest may send the nil UUID, or echo the signed-in
        // player's - either of which would make two guests derive the same XUID and collide.
        UUID identity = extraData.identity;
        boolean identityIdentifies = identity != null
                && !identity.equals(NIL_UUID)
                && !identity.equals(parent.getAuthData().uuid());

        String xuid = extraData.xuid;
        if (isBlank(xuid)) {
            String source = identityIdentifies ? "its identity UUID" : "the host's XUID and controller slot";
            xuid = syntheticXuid(identityIdentifies
                    ? "identity:" + identity
                    : "slot:" + parent.xuid() + ':' + slot);
            geyser.getLogger().warning(String.format(
                    "Split-screen: the console sent no XUID for the guest in slot %d - a long-standing "
                            + "console limitation (MCPE-71033). Deriving one from %s. This is fine for an "
                            + "offline-mode server; it is NOT an identity claim and is not suitable for "
                            + "Floodgate or online mode.", slot, source));
        }

        String name = extraData.displayName;
        if (isBlank(name) || name.equals(parent.bedrockUsername())) {
            name = syntheticUsername(parent.bedrockUsername(), slot);
            geyser.getLogger().warning(String.format(
                    "Split-screen: the console sent no usable display name for the guest in slot %d; "
                            + "they will appear as '%s'.", slot, name));
        }

        if (!identityIdentifies) {
            identity = UUID.nameUUIDFromBytes(("GeyserSplitScreen:" + xuid).getBytes(StandardCharsets.UTF_8));
        }

        return new AuthData(name, identity, xuid, issuedAt, extraData.minecraftId);
    }

    private static final UUID NIL_UUID = new UUID(0, 0);

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    private static String describe(@Nullable String value) {
        return value == null ? "<null>" : value.isEmpty() ? "<empty>" : value;
    }

    /**
     * Derives a stable, non-negative pseudo-XUID from {@code seed}. Real XUIDs are decimal, so this
     * produces one too, keeping anything that parses an XUID as a number working. The result uses
     * the full 63-bit range and so runs to 19 digits, well clear of the ~16-digit range Xbox Live
     * actually issues from - a synthesized XUID cannot collide with a real one.
     */
    private static String syntheticXuid(String seed) {
        UUID derived = UUID.nameUUIDFromBytes(("GeyserSplitScreen:" + seed).getBytes(StandardCharsets.UTF_8));
        return Long.toUnsignedString(derived.getMostSignificantBits() >>> 1);
    }

    /**
     * Builds a Java-legal username for a guest the console did not name. Java usernames are capped
     * at 16 characters and limited to {@code [A-Za-z0-9_]}, so the host's name is trimmed to leave
     * room for the suffix.
     */
    private static String syntheticUsername(String parentName, int slot) {
        int player = slot >= 0 ? slot + 1 : 2;
        String base = parentName.replaceAll("[^A-Za-z0-9_]", "").toLowerCase(Locale.ROOT);
        if (base.isEmpty()) {
            base = "player";
        }
        String suffix = "_P" + player;
        if (base.length() + suffix.length() > 16) {
            base = base.substring(0, 16 - suffix.length());
        }
        return base + suffix;
    }

    private static void encryptConnectionWithCert(GeyserSession session, AuthPayload authPayload, String jwt) {
        try {
            GeyserImpl geyser = session.getGeyser();

            // Regardless of auth type, we don't support guest type accounts used for splitscreen
            if (authPayload.getAuthType() == AuthType.GUEST) {
                session.disconnect(GeyserLocale.getLocaleStringLog("geyser.network.remote.invalid_xbox_account"));
                return;
            }

            ChainValidationResult result = EncryptionUtils.validatePayload(authPayload);

            geyser.getLogger().debug("Is player data signed? %s", result.signed());
            if (!result.signed() && session.getGeyser().config().advanced().bedrock().validateBedrockLogin()) {
                session.disconnect(GeyserLocale.getLocaleStringLog("geyser.network.remote.invalid_xbox_account"));
                return;
            }

            // Should always be present, but hey, why not make it safe :D
            Long rawIssuedAt = (Long) result.rawIdentityClaims().get("iat");
            long issuedAt = rawIssuedAt != null ? rawIssuedAt : -1;

            if (authPayload instanceof TokenPayload tokenPayload) {
                session.setToken(tokenPayload.getToken());
            } else if (authPayload instanceof CertificateChainPayload certificateChainPayload) {
                session.setCertChainData(certificateChainPayload.getChain());
            } else {
                GeyserImpl.getInstance().getLogger().warning("Unknown auth payload! Skin uploading will not work");
            }

            PublicKey identityPublicKey = result.identityClaims().parsedIdentityPublicKey();

            byte[] clientDataPayload = EncryptionUtils.verifyClientData(jwt, identityPublicKey);
            if (clientDataPayload == null) {
                throw new IllegalStateException("Client data isn't signed by the given chain data");
            }

            BedrockClientData data = JsonUtils.fromJson(clientDataPayload, BedrockClientData.class);
            data.setOriginalString(jwt);
            session.setClientData(data);

            IdentityData extraData = result.identityClaims().extraData;
            String xuid = extraData.xuid;
            if (geyser.config().advanced().bedrock().useWaterdogpeForwarding()) {
                String waterdogIp = data.getWaterdogIp();
                String waterdogXuid = data.getWaterdogXuid();
                if (waterdogXuid != null && !waterdogXuid.isBlank() && waterdogIp != null && !waterdogIp.isBlank()) {
                    xuid = waterdogXuid;
                    session.getUpstream().setInetAddress(new InetSocketAddress(waterdogIp, 0));
                } else {
                    session.disconnect("Did not receive IP and xuid forwarded from the proxy!");
                    return;
                }
            }
            session.setAuthData(new AuthData(extraData.displayName, extraData.identity, xuid, issuedAt, extraData.minecraftId));

            // Thanks 26.44, we love protocol bumps without protocol version bumps
            CodecProcessor.updateCodec(session.getUpstream(), data.getGameVersion());

            try {
                startEncryptionHandshake(session, identityPublicKey);
            } catch (Throwable e) {
                // An error can be thrown on older Java 8 versions about an invalid key
                if (geyser.config().debugMode()) {
                    e.printStackTrace();
                }

                sendEncryptionFailedMessage(geyser);
            }
        } catch (Exception ex) {
            session.disconnect("disconnectionScreen.internalError.cantConnect");
            throw new RuntimeException("Unable to complete login", ex);
        }
    }

    private static void startEncryptionHandshake(GeyserSession session, PublicKey key) throws Exception {
        KeyPair serverKeyPair = EncryptionUtils.createKeyPair();
        byte[] token = EncryptionUtils.generateRandomToken();

        ServerToClientHandshakePacket packet = new ServerToClientHandshakePacket();
        packet.setJwt(EncryptionUtils.createHandshakeJwt(serverKeyPair, token));
        session.sendUpstreamPacketImmediately(packet);

        SecretKey encryptionKey = EncryptionUtils.getSecretKey(serverKeyPair.getPrivate(), key, token);
        session.getUpstream().getSession().enableEncryption(encryptionKey);
    }

    private static void sendEncryptionFailedMessage(GeyserImpl geyser) {
        if (!HAS_SENT_ENCRYPTION_MESSAGE) {
            geyser.getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.network.encryption.line_1"));
            geyser.getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.network.encryption.line_2", "https://geysermc.org/supported_java"));
            HAS_SENT_ENCRYPTION_MESSAGE = true;
        }
    }

    public static void buildAndShowLoginWindow(GeyserSession session) {
        if (session.isLoggedIn()) {
            // Can happen if a window is cancelled during dimension switch
            return;
        }

        // So the time doesn't accelerate while we're here
        session.resetTimeParameters();

        session.sendForm(
                SimpleForm.builder()
                        .translator(GeyserLocale::getPlayerLocaleString, session.locale())
                        .title("geyser.auth.login.form.notice.title")
                        .content("geyser.auth.login.form.notice.desc")
                        .button("geyser.auth.login.form.notice.btn_login.microsoft")
                        .button("geyser.auth.login.form.notice.btn_disconnect")
                        .closedOrInvalidResultHandler(() -> buildAndShowLoginWindow(session))
                        .validResultHandler((response) -> {
                            if (response.clickedButtonId() == 0) {
                                session.authenticateWithMicrosoftCode();
                                return;
                            }

                            session.disconnect(GeyserLocale.getPlayerLocaleString("geyser.auth.login.form.disconnect", session.locale()));
                        }));
    }

    /**
     * Build a window that explains the user's credentials will be saved to the system.
     */
    public static void buildAndShowConsentWindow(GeyserSession session) {
        String locale = session.locale();

        session.sendForm(
                SimpleForm.builder()
                        .translator(LoginEncryptionUtils::translate, locale)
                        .title("%gui.signIn")
                        .content("""
                                geyser.auth.login.save_token.warning

                                geyser.auth.login.save_token.proceed""")
                        .button("%gui.ok")
                        .button("%gui.decline")
                        .resultHandler(authenticateOrKickHandler(session))
        );
    }

    public static void buildAndShowTokenExpiredWindow(GeyserSession session) {
        String locale = session.locale();

        session.sendForm(
                SimpleForm.builder()
                        .translator(LoginEncryptionUtils::translate, locale)
                        .title("geyser.auth.login.form.expired")
                        .content("""
                                geyser.auth.login.save_token.expired

                                geyser.auth.login.save_token.proceed""")
                        .button("%gui.ok")
                        .resultHandler(authenticateOrKickHandler(session))
        );
    }

    private static BiConsumer<SimpleForm, FormResponseResult<SimpleFormResponse>> authenticateOrKickHandler(GeyserSession session) {
        return (form, genericResult) -> {
            if (genericResult instanceof ValidFormResponseResult<SimpleFormResponse> result &&
                    result.response().clickedButtonId() == 0) {
                session.authenticateWithMicrosoftCode(true);
            } else {
                session.disconnect("%disconnect.quitting");
            }
        };
    }

    /**
     * Shows the code that a user must input into their browser
     */
    public static void buildAndShowMicrosoftCodeWindow(GeyserSession session, MsaDeviceCode msCode) {
        String locale = session.locale();

        StringBuilder message = new StringBuilder("%xbox.signin.website\n")
                .append(ChatColor.AQUA)
                .append("%xbox.signin.url")
                .append(ChatColor.RESET)
                .append("\n%xbox.signin.enterCode\n")
                .append(ChatColor.GREEN)
                .append(msCode.getUserCode());
        int timeout = session.getGeyser().config().pendingAuthenticationTimeout();
        if (timeout != 0) {
            message.append("\n\n")
                    .append(ChatColor.RESET)
                    .append(GeyserLocale.getPlayerLocaleString("geyser.auth.login.timeout", session.locale(), String.valueOf(timeout)));
        }

        session.sendForm(
                ModalForm.builder()
                        .title("%xbox.signin")
                        .content(message.toString())
                        .button1("%gui.done")
                        .button2("%menu.disconnect")
                        .closedOrInvalidResultHandler(() -> buildAndShowLoginWindow(session))
                        .validResultHandler((response) -> {
                            if (response.clickedButtonId() == 1) {
                                session.disconnect(GeyserLocale.getPlayerLocaleString("geyser.auth.login.form.disconnect", locale));
                            }
                        })
        );
    }

    /*
    This checks per line if there is something to be translated, and it skips Bedrock translation keys (%)
     */
    private static String translate(String key, String locale) {
        StringBuilder newValue = new StringBuilder();
        int previousIndex = 0;
        while (previousIndex < key.length()) {
            int nextIndex = key.indexOf('\n', previousIndex);
            int endIndex = nextIndex == -1 ? key.length() : nextIndex;

            // if there is more to this line than just a new line char
            if (endIndex - previousIndex > 1) {
                String substring = key.substring(previousIndex, endIndex);
                if (key.charAt(previousIndex) != '%') {
                    newValue.append(GeyserLocale.getPlayerLocaleString(substring, locale));
                } else {
                    newValue.append(substring);
                }
            }
            newValue.append('\n');

            previousIndex = endIndex + 1;
        }
        return newValue.toString();
    }
}
