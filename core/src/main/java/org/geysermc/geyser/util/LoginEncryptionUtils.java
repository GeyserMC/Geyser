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
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.data.auth.AuthPayload;
import org.cloudburstmc.protocol.bedrock.data.auth.AuthType;
import org.cloudburstmc.protocol.bedrock.data.auth.CertificateChainPayload;
import org.cloudburstmc.protocol.bedrock.data.auth.TokenPayload;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.ServerToClientHandshakePacket;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult.IdentityData;
import org.cloudburstmc.protocol.bedrock.util.EducationTokenValidationResult;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.cumulus.response.result.FormResponseResult;
import org.geysermc.cumulus.response.result.ValidFormResponseResult;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.network.netty.BedrockEncryptionControl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.auth.AuthData;
import org.geysermc.geyser.session.auth.BedrockClientData;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;
import org.jose4j.lang.JoseException;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.UUID;
import java.util.function.BiConsumer;

public class LoginEncryptionUtils {
    private static boolean HAS_SENT_ENCRYPTION_MESSAGE = false;

    /**
     * The identity ChainValidationResult derives for a chain whose xid claim is
     * empty: UUID.nameUUIDFromBytes("pocket-auth-1-xuid:" + xid). Education
     * clients have no Xbox account, so every education login degenerates to
     * this constant instead of the client's real identity.
     */
    private static final UUID EMPTY_XID_IDENTITY =
            UUID.nameUUIDFromBytes("pocket-auth-1-xuid:".getBytes(StandardCharsets.UTF_8));

    public static void encryptPlayerConnection(GeyserSession session, LoginPacket loginPacket) {
        encryptConnectionWithCert(session, loginPacket.getAuthPayload(), loginPacket.getClientJwt());
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

            geyser.getLogger().debug(String.format("Is player data signed? %s", result.signed()));

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

            // Education Edition clients use self-signed login chains (no Xbox Live),
            // so result.signed() is always false for them. We must detect edu clients
            // before the Xbox validation check to avoid rejecting them.
            boolean isEducationClient = data.isEducationEdition();

            // Xbox validation: reject unsigned non-edu clients when validation is enabled
            if (!result.signed() && !isEducationClient && session.getGeyser().config().advanced().bedrock().validateBedrockLogin()) {
                session.disconnect(GeyserLocale.getLocaleStringLog("geyser.network.remote.invalid_xbox_account"));
                return;
            }

            // Handle Education Edition: verify the MESS-signed server token echoed in the EduTokenChain JWT
            if (isEducationClient) {
                session.setEducationClient(true);

                String eduTokenChain = data.getEduTokenChain();
                String serverToken;
                try {
                    serverToken = EncryptionUtils.extractServerTokenFromEduTokenChain(eduTokenChain);
                } catch (JoseException e) {
                    serverToken = null;
                }
                EducationTokenValidationResult tokenResult = EncryptionUtils.validateEducationToken(serverToken);

                if (tokenResult.getStatus() == EducationTokenValidationResult.Status.EXPIRED) {
                    // 10-day tokens commonly expire while the client is still running
                    // (e.g. a student leaves their Chromebook logged in for 2+ weeks).
                    // Tell them exactly what to do instead of a generic auth-failure.
                    geyser.getLogger().debug("MESS token expired for " + session.bedrockUsername());
                    session.disconnect("Your Education Edition session has expired.\n\nPlease fully restart Minecraft Education Edition and try again.");
                    return;
                }
                if (tokenResult.getStatus() != EducationTokenValidationResult.Status.VALID) {
                    geyser.getLogger().debug("MESS token verification failed for " + session.bedrockUsername());
                    session.disconnect("Education Edition Connection Failed\n\nYour education token could not be verified.");
                    return;
                }

                session.setEducationTenantId(tokenResult.getTenantId());
                session.setEducationServerToken(serverToken);

                // Swap to the education codec. A swap is structurally unavoidable
                // for protocol versions shared with standard Bedrock (v898): the
                // initial codec is chosen when RequestNetworkSettingsPacket
                // arrives, and at that point only the protocol version is known.
                // The education flag only surfaces when LoginPacket's clientData
                // is parsed, which is where this code runs. At education-only
                // protocol versions (1002) the version gate already picked the
                // education codec and this re-set is a no-op.
                BedrockCodec educationCodec = GameProtocol.getEducationCodec(
                        session.getUpstream().getSession().getCodec().getProtocolVersion());
                if (educationCodec == null) {
                    session.disconnect("Outdated Geyser proxy! This server has not yet been updated to support your Education Edition version.");
                    return;
                }
                session.getUpstream().getSession().setCodec(educationCodec);
            }

            IdentityData extraData = result.identityClaims().extraData;
            // For education clients, use the MESS-verified oid as xuid
            String xuid = isEducationClient && session.getEducationServerToken() != null
                    ? session.getEducationServerToken().split("\\|")[1]
                    : extraData.xuid;
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

            String displayName = extraData.displayName;
            if (isEducationClient && (displayName == null || displayName.isEmpty())) {
                // Education 26.30 logs in with a self-signed chain whose name claims
                // are all empty; the display name only exists in clientData ThirdPartyName.
                displayName = data.getUsername();
            }

            UUID identity = extraData.identity;
            if (isEducationClient && EMPTY_XID_IDENTITY.equals(identity) && data.getSelfSignedId() != null) {
                // Same restructure as the display name above: the client's real
                // identity is no longer in the chain and only survives as clientData
                // SelfSignedId (equal to the token's leguuid claim). The client looks
                // itself up in the player list by this value, and receiving the
                // placeholder instead crashes the 26.30 pause menu.
                identity = data.getSelfSignedId();
            }

            session.setAuthData(new AuthData(displayName, identity, xuid, issuedAt, extraData.minecraftId));

            try {
                boolean enableEncryption = !BedrockEncryptionControl.isEncryptionDisabled(
                        session.getUpstream().getSession().getPeer().getChannel());
                startEncryptionHandshake(session, identityPublicKey, enableEncryption);
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

    private static void startEncryptionHandshake(GeyserSession session, PublicKey key,
                                                 boolean enableEncryption) throws Exception {
        KeyPair serverKeyPair = EncryptionUtils.createKeyPair();
        byte[] token = EncryptionUtils.generateRandomToken();

        String signedToken = session.isEducationClient() ? session.getEducationServerToken() : null;
        String jwt = EncryptionUtils.createHandshakeJwt(serverKeyPair, token, signedToken);

        ServerToClientHandshakePacket packet = new ServerToClientHandshakePacket();
        packet.setJwt(jwt);
        session.sendUpstreamPacketImmediately(packet);

        if (enableEncryption) {
            SecretKey encryptionKey = EncryptionUtils.getSecretKey(serverKeyPair.getPrivate(), key, token);
            session.getUpstream().getSession().enableEncryption(encryptionKey);
        }
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
