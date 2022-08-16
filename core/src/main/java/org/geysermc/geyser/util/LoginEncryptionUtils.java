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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.shaded.json.JSONObject;
import com.nimbusds.jose.shaded.json.JSONValue;
import com.nukkitx.network.util.Preconditions;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.packet.ServerToClientHandshakePacket;
import com.nukkitx.protocol.bedrock.packet.SubClientLoginPacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import io.netty.util.AsciiString;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.cumulus.response.result.FormResponseResult;
import org.geysermc.cumulus.response.result.ValidFormResponseResult;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.configuration.GeyserConfiguration;
import org.geysermc.geyser.configuration.GeyserConfiguration.ISplitscreenUserInfo;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.auth.AuthData;
import org.geysermc.geyser.session.auth.AuthType;
import org.geysermc.geyser.session.auth.BedrockClientData;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public class LoginEncryptionUtils {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static boolean HAS_SENT_ENCRYPTION_MESSAGE = false;

    private static boolean validateChainData(JsonNode data) throws Exception {
        if (data.size() != 3) {
            return false;
        }

        ECPublicKey lastKey = null;
        boolean mojangSigned = false;
        Iterator<JsonNode> iterator = data.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            JWSObject jwt = JWSObject.parse(node.asText());

            // x509 cert is expected in every claim
            URI x5u = jwt.getHeader().getX509CertURL();
            if (x5u == null) {
                return false;
            }

            ECPublicKey expectedKey = EncryptionUtils.generateKey(jwt.getHeader().getX509CertURL().toString());
            // First key is self-signed
            if (lastKey == null) {
                lastKey = expectedKey;
            } else if (!lastKey.equals(expectedKey)) {
                return false;
            }

            if (!EncryptionUtils.verifyJwt(jwt, lastKey)) {
                return false;
            }

            if (mojangSigned) {
                return !iterator.hasNext();
            }

            if (lastKey.equals(EncryptionUtils.getMojangPublicKey())) {
                mojangSigned = true;
            }

            Object payload = JSONValue.parse(jwt.getPayload().toString());
            Preconditions.checkArgument(payload instanceof JSONObject, "Payload is not an object");

            Object identityPublicKey = ((JSONObject) payload).get("identityPublicKey");
            Preconditions.checkArgument(identityPublicKey instanceof String, "identityPublicKey node is missing in chain");
            lastKey = EncryptionUtils.generateKey((String) identityPublicKey);
        }

        return mojangSigned;
    }

    public static void handleEncryption(GeyserImpl geyser, GeyserSession session, LoginPacket loginPacket) {
        handleEncryption(geyser, session, loginPacket.getChainData(), loginPacket.getSkinData(), true);
    }

    public static void handleSubClient(GeyserImpl geyser, GeyserSession session, SubClientLoginPacket loginPacket) {
        handleEncryption(geyser, session, loginPacket.getChainData(), loginPacket.getSkinData(), false);
    }

    private static void handleEncryption(GeyserImpl geyser, GeyserSession session, AsciiString chainData,
                                         AsciiString skinData, boolean isMainClient) {
        JsonNode certChainData;
        try {
            certChainData = getCertChainData(chainData);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot get cert chain data: " + ex.getMessage(), ex);
        }

        boolean debugLogging = geyser.getLogger().isDebug();

        if (isMainClient) {
            boolean validChain;
            try {
                validChain = validateChainData(certChainData);

                if (debugLogging) {
                    geyser.getLogger().debug(String.format("Is player data valid? %s", validChain));
                }
            } catch (Exception ex) {
                // rethrow until we can wrap in custom exceptions
                throw new RuntimeException("Unable to validate chain data: " + ex.getMessage(), ex);
            }

            if (!validChain && !geyser.getConfig().isEnableProxyConnections()) {
                session.disconnect(GeyserLocale.getLocaleStringLog("geyser.network.remote.invalid_xbox_account"));
                return;
            }
        }

        JsonNode certChainPayload = getCertChainPayload(certChainData);

        ECPublicKey identityPublicKey = getIdentityPublicKey(certChainPayload);

        AuthData authData = getAuthData(geyser, certChainPayload);
        if (debugLogging) {
            geyser.getLogger().debug(authData.toString());
        }
        session.setAuthData(authData);

        BedrockClientData bedrockClientData = getClientData(skinData, identityPublicKey);
        if (debugLogging) {
            geyser.getLogger().debug(bedrockClientData.toString());
        }
        session.setClientData(bedrockClientData);

        if (!isMainClient) {
            // Additional encryption isn't necessary for subclients
            return;
        }

        encryptPlayerConnection(geyser, session, identityPublicKey);
    }

    private static BedrockClientData getClientData(AsciiString skinData, ECPublicKey identityPublicKey) {
        BedrockClientData bedrockClientData;
        try {
            String clientData = skinData.toString();
            JWSObject clientJwt = JWSObject.parse(clientData);
            EncryptionUtils.verifyJwt(clientJwt, identityPublicKey);

            bedrockClientData = JSON_MAPPER.convertValue(JSON_MAPPER.readTree(clientJwt.getPayload().toBytes()), BedrockClientData.class);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to set auth data: " + ex.getMessage(), ex);
        }
        return bedrockClientData;
    }

    private static ECPublicKey getIdentityPublicKey(JsonNode payload) {
        if (payload.get("identityPublicKey").getNodeType() != JsonNodeType.STRING) {
            throw new RuntimeException("Identity Public Key was not found!");
        }
        ECPublicKey identityPublicKey;
        try {
            identityPublicKey = EncryptionUtils.generateKey(payload.get("identityPublicKey").textValue());
        } catch (Exception ex) {
            throw new RuntimeException("Unable to set auth data: " + ex.getMessage(), ex);
        }
        return identityPublicKey;
    }

    private static AuthData getAuthData(GeyserImpl geyser, JsonNode payload) {
        if (payload.get("extraData").getNodeType() != JsonNodeType.OBJECT) {
            throw new RuntimeException("AuthData was not found!");
        }

        JsonNode extraData = payload.get("extraData");
        geyser.getLogger().debug(extraData.toString());

        String displayName = extraData.get("displayName").asText();
        UUID uuid = UUID.fromString(extraData.get("identity").asText());
        String xuid = extraData.get("XUID").asText();

        GeyserConfiguration.ISplitscreenConfiguration splitscreenConfig = geyser.getConfig().getSplitscreen();
        if (splitscreenConfig.isEnabled()) {
            boolean showXuidPrompt = true;
            Map<String, ? extends ISplitscreenUserInfo> splitscreenUsers = splitscreenConfig.getUsers();
            if (splitscreenUsers != null) {
                if (xuid.length() == 0) {
                    showXuidPrompt = false;
                    ISplitscreenUserInfo splitscreenUser = splitscreenUsers.get(displayName);
                    if (splitscreenUser != null && splitscreenUser.getXuid() != null) {
                        xuid = splitscreenUser.getXuid();
                        if (splitscreenUser.getBedrockUsername() != null) {
                            displayName = splitscreenUser.getBedrockUsername();
                        }
                    }
                } else {
                    String xuidToMatch = xuid;
                    showXuidPrompt = splitscreenUsers.entrySet().stream().noneMatch(
                            user -> {
                                String mappedXUID = user.getValue().getXuid();
                                return mappedXUID != null && mappedXUID.equals(xuidToMatch);
                            }
                    );
                }
            }

            if (showXuidPrompt && geyser.getConfig().getRemote().getAuthType() == AuthType.FLOODGATE) {
                geyser.getLogger().info(
                        """
                        Add the following user to the Geyser splitscreen config to allow them to play via splitscreen:
                        
                            Profile Username (Change this):
                              bedrock-username: %s
                              xuid: %s
                        """.formatted(displayName, xuid)
                );
            }
        }

        return new AuthData(displayName, uuid, xuid);
    }

    private static JsonNode getCertChainData(AsciiString chainData) {
        JsonNode certData;
        try {
            certData = JSON_MAPPER.readTree(chainData.toByteArray());
        } catch (IOException ex) {
            throw new RuntimeException("Certificate JSON can not be read: " + ex.getMessage(), ex);
        }

        JsonNode certChainData = certData.get("chain");
        if (certChainData.getNodeType() != JsonNodeType.ARRAY) {
            throw new RuntimeException("Certificate data is not valid");
        }
        return certChainData;
    }

    private static JsonNode getCertChainPayload(JsonNode certChainData) {
        JsonNode payload;
        try {
            JWSObject jwt = JWSObject.parse(certChainData.get(certChainData.size() - 1).asText());
            payload = JSON_MAPPER.readTree(jwt.getPayload().toBytes());
        } catch (Exception ex) {
            throw new RuntimeException("Unable to set auth data: " + ex.getMessage(), ex);
        }
        return payload;
    }

    public static void encryptPlayerConnection(GeyserImpl geyser, GeyserSession session, ECPublicKey identityPublicKey) {
        try {
            if (EncryptionUtils.canUseEncryption()) {
                try {
                    LoginEncryptionUtils.startEncryptionHandshake(session, identityPublicKey);
                } catch (Throwable e) {
                    // An error can be thrown on older Java 8 versions about an invalid key
                    if (geyser.getConfig().isDebugMode()) {
                        e.printStackTrace();
                    }

                    sendEncryptionFailedMessage(geyser);
                }
            } else {
                sendEncryptionFailedMessage(geyser);
            }
        } catch (Exception ex) {
            session.disconnect("disconnectionScreen.internalError.cantConnect");
            throw new RuntimeException("Unable to complete login: " + ex.getMessage(), ex);
        }
    }

    private static void startEncryptionHandshake(GeyserSession session, ECPublicKey identityPublicKey) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        KeyPair serverKeyPair = generator.generateKeyPair();

        byte[] token = EncryptionUtils.generateRandomToken();
        SecretKey encryptionKey = EncryptionUtils.getSecretKey(serverKeyPair.getPrivate(), identityPublicKey, token);
        session.getUpstream().getSession().enableEncryption(encryptionKey);

        ServerToClientHandshakePacket packet = new ServerToClientHandshakePacket();
        packet.setJwt(EncryptionUtils.createHandshakeJwt(serverKeyPair, token).serialize());
        session.sendUpstreamPacketImmediately(packet);
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

        GeyserConfiguration config = session.getGeyser().getConfig();
        boolean isPasswordAuthEnabled = config.getRemote().isPasswordAuthentication();

        session.sendForm(
                SimpleForm.builder()
                        .translator(GeyserLocale::getPlayerLocaleString, session.getLocale())
                        .title("geyser.auth.login.form.notice.title")
                        .content("geyser.auth.login.form.notice.desc")
                        .optionalButton("geyser.auth.login.form.notice.btn_login.mojang", isPasswordAuthEnabled)
                        .button("geyser.auth.login.form.notice.btn_login.microsoft")
                        .button("geyser.auth.login.form.notice.btn_disconnect")
                        .closedOrInvalidResultHandler(() -> buildAndShowLoginWindow(session))
                        .validResultHandler((response) -> {
                            if (response.clickedButtonId() == 0) {
                                session.setMicrosoftAccount(false);
                                buildAndShowLoginDetailsWindow(session);
                                return;
                            }

                            if (response.clickedButtonId() == 1) {
                                if (isPasswordAuthEnabled) {
                                    session.setMicrosoftAccount(true);
                                    buildAndShowMicrosoftAuthenticationWindow(session);
                                } else {
                                    // Just show the OAuth code
                                    session.authenticateWithMicrosoftCode();
                                }
                                return;
                            }

                            session.disconnect(GeyserLocale.getPlayerLocaleString("geyser.auth.login.form.disconnect", session.getLocale()));
                        }));
    }

    /**
     * Build a window that explains the user's credentials will be saved to the system.
     */
    public static void buildAndShowConsentWindow(GeyserSession session) {
        session.sendForm(
                SimpleForm.builder()
                        .translator(LoginEncryptionUtils::translate, session.getLocale())
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
        session.sendForm(
                SimpleForm.builder()
                        .translator(LoginEncryptionUtils::translate, session.getLocale())
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

    public static void buildAndShowLoginDetailsWindow(GeyserSession session) {
        session.sendForm(
                CustomForm.builder()
                        .translator(GeyserLocale::getPlayerLocaleString, session.getLocale())
                        .title("geyser.auth.login.form.details.title")
                        .label("geyser.auth.login.form.details.desc")
                        .input("geyser.auth.login.form.details.email", "account@geysermc.org", "")
                        .input("geyser.auth.login.form.details.pass", "123456", "")
                        .invalidResultHandler(() -> buildAndShowLoginDetailsWindow(session))
                        .closedResultHandler(() -> {
                            if (session.isMicrosoftAccount()) {
                                buildAndShowMicrosoftAuthenticationWindow(session);
                            } else {
                                buildAndShowLoginWindow(session);
                            }
                        })
                        .validResultHandler((response) -> session.authenticate(response.next(), response.next())));
    }

    /**
     * Prompts the user between either OAuth code login or manual password authentication
     */
    public static void buildAndShowMicrosoftAuthenticationWindow(GeyserSession session) {
        session.sendForm(
                SimpleForm.builder()
                        .translator(GeyserLocale::getPlayerLocaleString, session.getLocale())
                        .title("geyser.auth.login.form.notice.btn_login.microsoft")
                        .button("geyser.auth.login.method.browser")
                        .button("geyser.auth.login.method.password")
                        .button("geyser.auth.login.form.notice.btn_disconnect")
                        .closedOrInvalidResultHandler(() -> buildAndShowLoginWindow(session))
                        .validResultHandler((response) -> {
                            if (response.clickedButtonId() == 0) {
                                session.authenticateWithMicrosoftCode();
                            } else if (response.clickedButtonId() == 1) {
                                buildAndShowLoginDetailsWindow(session);
                            } else {
                                session.disconnect(GeyserLocale.getPlayerLocaleString("geyser.auth.login.form.disconnect", session.getLocale()));
                            }
                        }));
    }

    /**
     * Shows the code that a user must input into their browser
     */
    public static void buildAndShowMicrosoftCodeWindow(GeyserSession session, MsaAuthenticationService.MsCodeResponse msCode) {
        StringBuilder message = new StringBuilder("%xbox.signin.website\n")
                .append(ChatColor.AQUA)
                .append("%xbox.signin.url")
                .append(ChatColor.RESET)
                .append("\n%xbox.signin.enterCode\n")
                .append(ChatColor.GREEN)
                .append(msCode.user_code);
        int timeout = session.getGeyser().getConfig().getPendingAuthenticationTimeout();
        if (timeout != 0) {
            message.append("\n\n")
                    .append(ChatColor.RESET)
                    .append(GeyserLocale.getPlayerLocaleString("geyser.auth.login.timeout", session.getLocale(), String.valueOf(timeout)));
        }

        session.sendForm(
                ModalForm.builder()
                        .title("%xbox.signin")
                        .content(message.toString())
                        .button1("%gui.done")
                        .button2("%menu.disconnect")
                        .closedOrInvalidResultHandler(() -> buildAndShowMicrosoftAuthenticationWindow(session))
                        .validResultHandler((response) -> {
                            if (response.clickedButtonId() == 1) {
                                session.disconnect(GeyserLocale.getPlayerLocaleString("geyser.auth.login.form.disconnect", session.getLocale()));
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
