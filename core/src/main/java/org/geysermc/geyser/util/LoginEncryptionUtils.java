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
import org.cloudburstmc.protocol.bedrock.data.auth.AuthPayload;
import org.cloudburstmc.protocol.bedrock.data.auth.CertificateChainPayload;
import org.cloudburstmc.protocol.bedrock.data.auth.TokenPayload;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.ServerToClientHandshakePacket;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult.IdentityData;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.cumulus.response.result.FormResponseResult;
import org.geysermc.cumulus.response.result.ValidFormResponseResult;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.EducationAuthManager;
import org.geysermc.geyser.network.EducationChainVerifier;
import org.geysermc.geyser.network.EducationTenancyMode;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.auth.AuthData;
import org.geysermc.geyser.session.auth.BedrockClientData;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.function.BiConsumer;

public class LoginEncryptionUtils {
    private static boolean HAS_SENT_ENCRYPTION_MESSAGE = false;

    public static void encryptPlayerConnection(GeyserSession session, LoginPacket loginPacket) {
        encryptConnectionWithCert(session, loginPacket.getAuthPayload(), loginPacket.getClientJwt());
    }

    private static void encryptConnectionWithCert(GeyserSession session, AuthPayload authPayload, String jwt) {
        try {
            GeyserImpl geyser = session.getGeyser();

            ChainValidationResult result = EncryptionUtils.validatePayload(authPayload);

            geyser.getLogger().debug("Is player data signed? %s", result.signed());

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

            // Handle Education Edition authentication and tenant extraction
            if (isEducationClient) {
                if (geyser.config().education().tenancyMode() == EducationTenancyMode.OFF) {
                    session.disconnect("Education Edition is not enabled on this server.");
                    return;
                }
                if (!handleEducationLogin(session, geyser, data, result, authPayload, jwt)) {
                    return;
                }
            }

            IdentityData extraData = result.identityClaims().extraData;
            String xuid = extraData.xuid;
            if (geyser.config().advanced().bedrock().useWaterdogpeForwarding()) {
                String waterdogIp = data.getWaterdogIp();
                String waterdogXuid = data.getWaterdogXuid();
                if (waterdogXuid != null && !waterdogXuid.isBlank() && waterdogIp != null && !waterdogIp.isBlank()) {
                    xuid = waterdogXuid;
                    InetSocketAddress originalAddress = session.getUpstream().getAddress();
                    InetSocketAddress proxiedAddress = new InetSocketAddress(waterdogIp, originalAddress.getPort());
                    session.getGeyser().getGeyserServer().getProxiedAddresses().put(originalAddress, proxiedAddress);
                    session.getUpstream().setInetAddress(proxiedAddress);
                } else {
                    session.disconnect("Did not receive IP and xuid forwarded from the proxy!");
                    return;
                }
            }

            session.setAuthData(new AuthData(extraData.displayName, extraData.identity, xuid, issuedAt, extraData.minecraftId));

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

    /**
     * Handles Education Edition-specific login logic: sets the education flag,
     * extracts the tenant ID from the EduTokenChain, and verifies the client
     * via MESS nonce verification (official/hybrid) or config trust (standalone).
     *
     * @return true if the login should continue, false if the session was disconnected
     */
    private static boolean handleEducationLogin(GeyserSession session, GeyserImpl geyser,
                                             BedrockClientData data, ChainValidationResult result,
                                             AuthPayload authPayload, String jwt) {
        session.setEducationClient(true);

        // Dump the full JWT chain for education clients (debug mode only)
        if (geyser.config().debugMode()) {
            EducationChainVerifier.dumpEduChain(geyser.getLogger(), authPayload, jwt);
        }

        EducationAuthManager eduAuthMgr = geyser.getEducationAuthManager();

        // Extract the REAL tenant ID from the EduTokenChain JWT payload.
        // Do NOT use data.getTenantId() -- it is always null for edu clients.
        String tenantId = eduAuthMgr.extractTenantIdFromEduTokenChain(data.getEduTokenChain());
        session.setEducationTenantId(tenantId);

        geyser.getLogger().debug("[EduAuth] Education client: tenant=%s, role=%s",
                tenantId != null ? tenantId : "unknown", data.adRoleName());

        // Verify the client based on tenancy mode
        EducationTenancyMode tenancyMode = geyser.config().education().tenancyMode();
        String joinerNonce = data.getEduJoinerToHostNonce();
        boolean hasNonce = joinerNonce != null && !joinerNonce.isEmpty();

        if (tenancyMode == EducationTenancyMode.STANDALONE) {
            // Standalone: no MESS registration, no nonce verification possible
            eduAuthMgr.recordUnverifiedJoin();
            return true;
        }

        if (tenancyMode == EducationTenancyMode.OFFICIAL) {
            // Official: nonce is required
            if (!hasNonce) {
                geyser.getLogger().warning("[EduAuth] Rejected: no nonce (official mode requires server list connection)");
                eduAuthMgr.recordRejectedJoin();
                session.disconnect(
                    "Education Edition Connection Failed\n\n" +
                    "This server requires connection via the Minecraft Education server list.\n" +
                    "Direct IP/URI connections are not allowed in official mode.\n\n" +
                    "Please connect through the in-game server list instead."
                );
                return false;
            }
            if (!eduAuthMgr.verifyNonce(joinerNonce)) {
                geyser.getLogger().warning("[EduAuth] Rejected: nonce not found in MESS joiner queue");
                eduAuthMgr.recordRejectedJoin();
                session.disconnect(
                    "Education Edition Connection Failed\n\n" +
                    "Your connection could not be verified with Microsoft.\n" +
                    "This may happen if the server just restarted.\n\n" +
                    "Please try again in a few seconds."
                );
                return false;
            }
            session.setNonceVerified(true);
            eduAuthMgr.recordVerifiedJoin();
            geyser.getLogger().debug("[EduAuth] Nonce verified for tenant %s", tenantId);
            return true;
        }

        // Hybrid: config-trust tenants are allowed without nonce verification.
        // All other tenants must be verified via nonce (owning tenant via server list).
        if (tenantId != null && eduAuthMgr.isConfigTrustTenant(tenantId)) {
            // Tenant is explicitly listed in server-tokens — allow via config trust
            eduAuthMgr.recordUnverifiedJoin();
            geyser.getLogger().debug("[EduAuth] Config-trust allow for tenant %s (hybrid mode)", tenantId);
            return true;
        }

        // Not a config-trust tenant — require nonce verification
        if (!hasNonce) {
            geyser.getLogger().warning("[EduAuth] Rejected: tenant " +
                    (tenantId != null ? tenantId : "unknown") + " not in server-tokens and no nonce present");
            eduAuthMgr.recordRejectedJoin();
            session.disconnect(
                "Education Edition Connection Failed\n\n" +
                "Your school (tenant: " + (tenantId != null ? tenantId : "unknown") + ") is not configured on this server.\n" +
                "Connect via the Minecraft Education server list, or ask the\n" +
                "server administrator to add your school's tenant token."
            );
            return false;
        }
        if (!eduAuthMgr.verifyNonce(joinerNonce)) {
            geyser.getLogger().warning("[EduAuth] Rejected: nonce not found in MESS joiner queue");
            eduAuthMgr.recordRejectedJoin();
            session.disconnect(
                "Education Edition Connection Failed\n\n" +
                "Your connection could not be verified with Microsoft.\n\n" +
                "Please try again in a few seconds."
            );
            return false;
        }
        session.setNonceVerified(true);
        eduAuthMgr.recordVerifiedJoin();
        geyser.getLogger().debug("[EduAuth] Nonce verified for tenant %s (hybrid mode)", tenantId);
        return true;
    }

    private static void startEncryptionHandshake(GeyserSession session, PublicKey key) throws Exception {
        KeyPair serverKeyPair = EncryptionUtils.createKeyPair();
        byte[] token = EncryptionUtils.generateRandomToken();

        String jwt;
        if (session.isEducationClient()) {
            jwt = buildEducationHandshakeJwt(session, serverKeyPair, token);
            if (jwt == null) {
                return; // Client was disconnected inside buildEducationHandshakeJwt
            }
        } else {
            jwt = EncryptionUtils.createHandshakeJwt(serverKeyPair, token);
        }

        SecretKey encryptionKey = EncryptionUtils.getSecretKey(serverKeyPair.getPrivate(), key, token);

        ServerToClientHandshakePacket packet = new ServerToClientHandshakePacket();
        packet.setJwt(jwt);
        session.sendUpstreamPacketImmediately(packet);
        session.getUpstream().getSession().enableEncryption(encryptionKey);
    }

    /**
     * Builds the education handshake JWT with a signedToken claim.
     * Routes tokens by tenant ID from the pool, falling back to the MESS-registered token.
     *
     * @return the JWT string, or null if no token was available (client is disconnected)
     */
    private static String buildEducationHandshakeJwt(GeyserSession session, KeyPair serverKeyPair, byte[] token) throws Exception {
        GeyserImpl geyser = session.getGeyser();
        EducationAuthManager eduAuth = geyser.getEducationAuthManager();

        String educationToken = eduAuth.getTokenForSession(session);

        if (educationToken == null || educationToken.isEmpty()) {
            String clientTenantId = session.getEducationTenantId();
            String tenantInfo = (clientTenantId != null) ? clientTenantId : "unknown";
            int poolSize = eduAuth.getRegisteredTenantCount();
            geyser.getLogger().warning("[EduTenancy] No server token available for tenant: " + tenantInfo);

            session.disconnect(
                "Education Edition Connection Failed\n\n" +
                "Your school (tenant: " + tenantInfo + ") is not configured on this server.\n\n" +
                "This server has " + poolSize + " tenant(s) registered.\n" +
                "Ask the server administrator to add your school's tenant to the server configuration.\n\n" +
                "If you are the admin, add a server token in edu_standalone.yml,\n" +
                "use /geyser edu token, or register the server in your school's\n" +
                "Minecraft Education admin portal."
            );
            return null;
        }

        // Build the edu handshake JWT with signedToken claim
        JsonWebSignature jws = new JsonWebSignature();
        jws.setAlgorithmHeaderValue("ES384");
        jws.setHeader("x5u", Base64.getEncoder().encodeToString(
            serverKeyPair.getPublic().getEncoded()));
        jws.setKey(serverKeyPair.getPrivate());

        JwtClaims claims = new JwtClaims();
        claims.setClaim("salt", Base64.getEncoder().encodeToString(token));
        claims.setClaim("signedToken", educationToken);
        jws.setPayload(claims.toJson());
        return jws.getCompactSerialization();
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

        // Set DoDaylightCycle to false so the time doesn't accelerate while we're here
        session.setDaylightCycle(false);

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
