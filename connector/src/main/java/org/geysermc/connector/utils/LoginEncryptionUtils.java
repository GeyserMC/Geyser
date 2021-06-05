/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.steveice10.mc.auth.service.MsaAuthenticationService;
import com.nimbusds.jose.JWSObject;
import com.nukkitx.network.util.Preconditions;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.packet.ServerToClientHandshakePacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.configuration.GeyserConfiguration;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.auth.AuthData;
import org.geysermc.connector.network.session.auth.BedrockClientData;
import org.geysermc.cumulus.CustomForm;
import org.geysermc.cumulus.ModalForm;
import org.geysermc.cumulus.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.response.ModalFormResponse;
import org.geysermc.cumulus.response.SimpleFormResponse;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.UUID;

public class LoginEncryptionUtils {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private static boolean HAS_SENT_ENCRYPTION_MESSAGE = false;

    private static boolean validateChainData(JsonNode data) throws Exception {
        ECPublicKey lastKey = null;
        boolean validChain = false;
        for (JsonNode node : data) {
            JWSObject jwt = JWSObject.parse(node.asText());

            if (!validChain) {
                validChain = EncryptionUtils.verifyJwt(jwt, EncryptionUtils.getMojangPublicKey());
            }

            if (lastKey != null) {
                if (!EncryptionUtils.verifyJwt(jwt, lastKey)) return false;
            }

            JsonNode payloadNode = JSON_MAPPER.readTree(jwt.getPayload().toString());
            JsonNode ipkNode = payloadNode.get("identityPublicKey");
            Preconditions.checkState(ipkNode != null && ipkNode.getNodeType() == JsonNodeType.STRING, "identityPublicKey node is missing in chain");
            lastKey = EncryptionUtils.generateKey(ipkNode.asText());
        }
        return validChain;
    }

    public static void encryptPlayerConnection(GeyserConnector connector, GeyserSession session, LoginPacket loginPacket) {
        JsonNode certData;
        try {
            certData = JSON_MAPPER.readTree(loginPacket.getChainData().toByteArray());
        } catch (IOException ex) {
            throw new RuntimeException("Certificate JSON can not be read.");
        }

        JsonNode certChainData = certData.get("chain");
        if (certChainData.getNodeType() != JsonNodeType.ARRAY) {
            throw new RuntimeException("Certificate data is not valid");
        }

        encryptConnectionWithCert(connector, session, loginPacket.getSkinData().toString(), certChainData);
    }

    private static void encryptConnectionWithCert(GeyserConnector connector, GeyserSession session, String clientData, JsonNode certChainData) {
        try {
            boolean validChain = validateChainData(certChainData);

            connector.getLogger().debug(String.format("Is player data valid? %s", validChain));

            if (!validChain && !session.getConnector().getConfig().isEnableProxyConnections()) {
                session.disconnect(LanguageUtils.getLocaleStringLog("geyser.network.remote.invalid_xbox_account"));
                return;
            }
            JWSObject jwt = JWSObject.parse(certChainData.get(certChainData.size() - 1).asText());
            JsonNode payload = JSON_MAPPER.readTree(jwt.getPayload().toBytes());

            if (payload.get("extraData").getNodeType() != JsonNodeType.OBJECT) {
                throw new RuntimeException("AuthData was not found!");
            }

            JsonNode extraData = payload.get("extraData");
            session.setAuthenticationData(new AuthData(
                    extraData.get("displayName").asText(),
                    UUID.fromString(extraData.get("identity").asText()),
                    extraData.get("XUID").asText(),
                    certChainData, clientData
            ));

            if (payload.get("identityPublicKey").getNodeType() != JsonNodeType.STRING) {
                throw new RuntimeException("Identity Public Key was not found!");
            }

            ECPublicKey identityPublicKey = EncryptionUtils.generateKey(payload.get("identityPublicKey").textValue());
            JWSObject clientJwt = JWSObject.parse(clientData);
            EncryptionUtils.verifyJwt(clientJwt, identityPublicKey);

            JsonNode clientDataJson = JSON_MAPPER.readTree(clientJwt.getPayload().toBytes());
            BedrockClientData data = JSON_MAPPER.convertValue(clientDataJson, BedrockClientData.class);
            session.setClientData(data);

            if (EncryptionUtils.canUseEncryption()) {
                try {
                    LoginEncryptionUtils.startEncryptionHandshake(session, identityPublicKey);
                } catch (Throwable e) {
                    // An error can be thrown on older Java 8 versions about an invalid key
                    if (connector.getConfig().isDebugMode()) {
                        e.printStackTrace();
                    }

                    sendEncryptionFailedMessage(connector);
                }
            } else {
                sendEncryptionFailedMessage(connector);
            }
        } catch (Exception ex) {
            session.disconnect("disconnectionScreen.internalError.cantConnect");
            throw new RuntimeException("Unable to complete login", ex);
        }
    }

    private static void startEncryptionHandshake(GeyserSession session, PublicKey key) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        KeyPair serverKeyPair = generator.generateKeyPair();

        byte[] token = EncryptionUtils.generateRandomToken();
        SecretKey encryptionKey = EncryptionUtils.getSecretKey(serverKeyPair.getPrivate(), key, token);
        session.getUpstream().getSession().enableEncryption(encryptionKey);

        ServerToClientHandshakePacket packet = new ServerToClientHandshakePacket();
        packet.setJwt(EncryptionUtils.createHandshakeJwt(serverKeyPair, token).serialize());
        session.sendUpstreamPacketImmediately(packet);
    }

    private static void sendEncryptionFailedMessage(GeyserConnector connector) {
        if (!HAS_SENT_ENCRYPTION_MESSAGE) {
            connector.getLogger().warning(LanguageUtils.getLocaleStringLog("geyser.network.encryption.line_1"));
            connector.getLogger().warning(LanguageUtils.getLocaleStringLog("geyser.network.encryption.line_2", "https://geysermc.org/supported_java"));
            HAS_SENT_ENCRYPTION_MESSAGE = true;
        }
    }

    public static void buildAndShowLoginWindow(GeyserSession session) {
        // Set DoDaylightCycle to false so the time doesn't accelerate while we're here
        session.setDaylightCycle(false);

        GeyserConfiguration config = session.getConnector().getConfig();
        boolean isPasswordAuthEnabled = config.getRemote().isPasswordAuthentication();

        session.sendForm(
                SimpleForm.builder()
                        .translator(LanguageUtils::getPlayerLocaleString, session.getLocale())
                        .title("geyser.auth.login.form.notice.title")
                        .content("geyser.auth.login.form.notice.desc")
                        .optionalButton("geyser.auth.login.form.notice.btn_login.mojang", isPasswordAuthEnabled)
                        .button("geyser.auth.login.form.notice.btn_login.microsoft")
                        .button("geyser.auth.login.form.notice.btn_disconnect")
                        .responseHandler((form, responseData) -> {
                            SimpleFormResponse response = form.parseResponse(responseData);
                            if (!response.isCorrect()) {
                                buildAndShowLoginWindow(session);
                                return;
                            }

                            if (isPasswordAuthEnabled && response.getClickedButtonId() == 0) {
                                session.setMicrosoftAccount(false);
                                buildAndShowLoginDetailsWindow(session);
                                return;
                            }

                            if (isPasswordAuthEnabled && response.getClickedButtonId() == 1) {
                                session.setMicrosoftAccount(true);
                                buildAndShowMicrosoftAuthenticationWindow(session);
                                return;
                            }

                            if (response.getClickedButtonId() == 0) {
                                // Just show the OAuth code
                                session.authenticateWithMicrosoftCode();
                                return;
                            }

                            session.disconnect(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.disconnect", session.getLocale()));
                        }));
    }

    public static void buildAndShowLoginDetailsWindow(GeyserSession session) {
        session.sendForm(
                CustomForm.builder()
                        .translator(LanguageUtils::getPlayerLocaleString, session.getLocale())
                        .title("geyser.auth.login.form.details.title")
                        .label("geyser.auth.login.form.details.desc")
                        .input("geyser.auth.login.form.details.email", "account@geysermc.org", "")
                        .input("geyser.auth.login.form.details.pass", "123456", "")
                        .responseHandler((form, responseData) -> {
                            CustomFormResponse response = form.parseResponse(responseData);
                            if (!response.isCorrect()) {
                                buildAndShowLoginDetailsWindow(session);
                                return;
                            }

                            session.authenticate(response.next(), response.next());
                        }));
    }

    /**
     * Prompts the user between either OAuth code login or manual password authentication
     */
    public static void buildAndShowMicrosoftAuthenticationWindow(GeyserSession session) {
        session.sendForm(
                SimpleForm.builder()
                        .translator(LanguageUtils::getPlayerLocaleString, session.getLocale())
                        .title("geyser.auth.login.form.notice.btn_login.microsoft")
                        .button("geyser.auth.login.method.browser")
                        .button("geyser.auth.login.method.password")
                        .button("geyser.auth.login.form.notice.btn_disconnect")
                        .responseHandler((form, responseData) -> {
                            SimpleFormResponse response = form.parseResponse(responseData);
                            if (!response.isCorrect()) {
                                buildAndShowLoginWindow(session);
                                return;
                            }

                            if (response.getClickedButtonId() == 0) {
                                session.authenticateWithMicrosoftCode();
                            } else if (response.getClickedButtonId() == 1) {
                                buildAndShowLoginDetailsWindow(session);
                            } else {
                                session.disconnect(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.disconnect", session.getLocale()));
                            }
                        }));
    }

    /**
     * Shows the code that a user must input into their browser
     */
    public static void buildAndShowMicrosoftCodeWindow(GeyserSession session, MsaAuthenticationService.MsCodeResponse msCode) {
        session.sendForm(
                ModalForm.builder()
                        .title("%xbox.signin")
                        .content("%xbox.signin.website\n%xbox.signin.url\n%xbox.signin.enterCode\n" + msCode.user_code)
                        .button1("%gui.done")
                        .button2("%menu.disconnect")
                        .responseHandler((form, responseData) -> {
                            ModalFormResponse response = form.parseResponse(responseData);
                            if (!response.isCorrect()) {
                                buildAndShowMicrosoftAuthenticationWindow(session);
                                return;
                            }

                            if (response.getClickedButtonId() == 1) {
                                session.disconnect(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.disconnect", session.getLocale()));
                            }
                        })
        );
    }
}
