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
import org.geysermc.common.window.*;
import org.geysermc.common.window.button.FormButton;
import org.geysermc.common.window.component.InputComponent;
import org.geysermc.common.window.component.LabelComponent;
import org.geysermc.common.window.response.CustomFormResponse;
import org.geysermc.common.window.response.ModalFormResponse;
import org.geysermc.common.window.response.SimpleFormResponse;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.auth.AuthData;
import org.geysermc.connector.network.session.auth.BedrockClientData;
import org.geysermc.connector.network.session.cache.WindowCache;

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
                    extraData.get("XUID").asText()
            ));

            if (payload.get("identityPublicKey").getNodeType() != JsonNodeType.STRING) {
                throw new RuntimeException("Identity Public Key was not found!");
            }

            ECPublicKey identityPublicKey = EncryptionUtils.generateKey(payload.get("identityPublicKey").textValue());
            JWSObject clientJwt = JWSObject.parse(clientData);
            EncryptionUtils.verifyJwt(clientJwt, identityPublicKey);

            session.setClientData(JSON_MAPPER.convertValue(JSON_MAPPER.readTree(clientJwt.getPayload().toBytes()), BedrockClientData.class));

            if (EncryptionUtils.canUseEncryption()) {
                LoginEncryptionUtils.startEncryptionHandshake(session, identityPublicKey);
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

    private static final int AUTH_MSA_DETAILS_FORM_ID = 1334;
    private static final int AUTH_MSA_CODE_FORM_ID = 1335;
    private static final int AUTH_FORM_ID = 1336;
    private static final int AUTH_DETAILS_FORM_ID = 1337;

    public static void showLoginWindow(GeyserSession session) {
        // Set DoDaylightCycle to false so the time doesn't accelerate while we're here
        session.setDaylightCycle(false);

        String userLanguage = session.getLocale();
        SimpleFormWindow window = new SimpleFormWindow(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.notice.title", userLanguage), LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.notice.desc", userLanguage));
        if (session.getConnector().getConfig().getRemote().isPasswordAuthentication()) {
            window.getButtons().add(new FormButton(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.notice.btn_login.mojang", userLanguage)));
        }
        window.getButtons().add(new FormButton(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.notice.btn_login.microsoft", userLanguage)));
        window.getButtons().add(new FormButton(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.notice.btn_disconnect", userLanguage)));

        session.sendForm(window, AUTH_FORM_ID);
    }

    public static void showLoginDetailsWindow(GeyserSession session) {
        String userLanguage = session.getLocale();
        CustomFormWindow window = new CustomFormBuilder(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.details.title", userLanguage))
                .addComponent(new LabelComponent(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.details.desc", userLanguage)))
                .addComponent(new InputComponent(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.details.email", userLanguage), "account@geysermc.org", ""))
                .addComponent(new InputComponent(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.details.pass", userLanguage), "123456", ""))
                .build();

        session.sendForm(window, AUTH_DETAILS_FORM_ID);
    }

    /**
     * Prompts the user between either OAuth code login or manual password authentication
     */
    public static void showMicrosoftAuthenticationWindow(GeyserSession session) {
        String userLanguage = session.getLocale();
        SimpleFormWindow window = new SimpleFormWindow(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.notice.btn_login.microsoft", userLanguage), "");
        window.getButtons().add(new FormButton(LanguageUtils.getPlayerLocaleString("geyser.auth.login.method.browser", userLanguage)));
        window.getButtons().add(new FormButton(LanguageUtils.getPlayerLocaleString("geyser.auth.login.method.password", userLanguage))); // This form won't show if password authentication is disabled
        window.getButtons().add(new FormButton(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.notice.btn_disconnect", userLanguage)));
        session.sendForm(window, AUTH_MSA_DETAILS_FORM_ID);
    }

    /**
     * Shows the code that a user must input into their browser
     */
    public static void showMicrosoftCodeWindow(GeyserSession session, MsaAuthenticationService.MsCodeResponse response) {
        ModalFormWindow msaCodeWindow = new ModalFormWindow("%xbox.signin", "%xbox.signin.website\n%xbox.signin.url\n%xbox.signin.enterCode\n" +
                response.user_code, "%gui.done", "%menu.disconnect");
        session.sendForm(msaCodeWindow, LoginEncryptionUtils.AUTH_MSA_CODE_FORM_ID);
    }

    public static boolean authenticateFromForm(GeyserSession session, GeyserConnector connector, int formId, String formData) {
        WindowCache windowCache = session.getWindowCache();
        if (!windowCache.getWindows().containsKey(formId))
            return false;

        if (formId == AUTH_MSA_DETAILS_FORM_ID || formId == AUTH_FORM_ID || formId == AUTH_DETAILS_FORM_ID || formId == AUTH_MSA_CODE_FORM_ID) {
            FormWindow window = windowCache.getWindows().remove(formId);
            window.setResponse(formData.trim());

            if (!session.isLoggedIn()) {
                if (formId == AUTH_DETAILS_FORM_ID && window instanceof CustomFormWindow) {
                    CustomFormWindow customFormWindow = (CustomFormWindow) window;

                    CustomFormResponse response = (CustomFormResponse) customFormWindow.getResponse();
                    if (response != null) {
                        String email = response.getInputResponses().get(1);
                        String password = response.getInputResponses().get(2);

                        session.authenticate(email, password);

                        // Clear windows so authentication data isn't accidentally cached
                        windowCache.getWindows().clear();
                    } else {
                        showLoginDetailsWindow(session);
                    }
                } else if (formId == AUTH_FORM_ID && window instanceof SimpleFormWindow) {
                    boolean isPasswordAuthentication = session.getConnector().getConfig().getRemote().isPasswordAuthentication();
                    int microsoftButton = isPasswordAuthentication ? 1 : 0;
                    int disconnectButton = isPasswordAuthentication ? 2 : 1;
                    SimpleFormResponse response = (SimpleFormResponse) window.getResponse();
                    if (response != null) {
                        if (isPasswordAuthentication && response.getClickedButtonId() == 0) {
                            session.setMicrosoftAccount(false);
                            showLoginDetailsWindow(session);
                        } else if (response.getClickedButtonId() == microsoftButton) {
                            session.setMicrosoftAccount(true);
                            if (isPasswordAuthentication) {
                                showMicrosoftAuthenticationWindow(session);
                            } else {
                                // Just show the OAuth code
                                session.authenticateWithMicrosoftCode();
                            }
                        } else if (response.getClickedButtonId() == disconnectButton) {
                            session.disconnect(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.disconnect", session.getLocale()));
                        }
                    } else {
                        showLoginWindow(session);
                    }
                } else if (formId == AUTH_MSA_DETAILS_FORM_ID && window instanceof SimpleFormWindow) {
                    SimpleFormResponse response = (SimpleFormResponse) window.getResponse();
                    if (response != null) {
                        if (response.getClickedButtonId() == 0) {
                            session.authenticateWithMicrosoftCode();
                        } else if (response.getClickedButtonId() == 1) {
                            showLoginDetailsWindow(session);
                        } else if (response.getClickedButtonId() == 2) {
                            session.disconnect(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.disconnect", session.getLocale()));
                        }
                    } else {
                        showLoginWindow(session);
                    }
                } else if (formId == AUTH_MSA_CODE_FORM_ID && window instanceof ModalFormWindow) {
                    ModalFormResponse response = (ModalFormResponse) window.getResponse();
                    if (response != null) {
                        if (response.getClickedButtonId() == 1) {
                            session.disconnect(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.disconnect", session.getLocale()));
                        }
                    } else {
                        showMicrosoftAuthenticationWindow(session);
                    }
                }
            }
        }
        return true;
    }

}
