/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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
import com.nimbusds.jose.JWSObject;
import com.nukkitx.network.util.Preconditions;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.packet.ServerToClientHandshakePacket;
import com.nukkitx.protocol.bedrock.packet.SubClientLoginPacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import io.netty.util.AsciiString;
import org.geysermc.common.window.CustomFormBuilder;
import org.geysermc.common.window.CustomFormWindow;
import org.geysermc.common.window.FormWindow;
import org.geysermc.common.window.SimpleFormWindow;
import org.geysermc.common.window.button.FormButton;
import org.geysermc.common.window.component.InputComponent;
import org.geysermc.common.window.component.LabelComponent;
import org.geysermc.common.window.response.CustomFormResponse;
import org.geysermc.common.window.response.SimpleFormResponse;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.UpstreamPacketHandler;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.auth.AuthData;
import org.geysermc.connector.network.session.auth.BedrockClientData;
import org.geysermc.connector.network.session.cache.WindowCache;

import javax.crypto.SecretKey;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
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
                EncryptionUtils.verifyJwt(jwt, lastKey);
            }

            JsonNode payloadNode = JSON_MAPPER.readTree(jwt.getPayload().toString());
            JsonNode ipkNode = payloadNode.get("identityPublicKey");
            Preconditions.checkState(ipkNode != null && ipkNode.getNodeType() == JsonNodeType.STRING, "identityPublicKey node is missing in chain");
            lastKey = EncryptionUtils.generateKey(ipkNode.asText());
        }
        return validChain;
    }

    public static void handleCertChainData(GeyserConnector connector, GeyserSession session, LoginPacket loginPacket) {
        handleCertChainData(connector, session, loginPacket.getChainData());
    }

    public static void handleCertChainData(GeyserConnector connector, GeyserSession session, SubClientLoginPacket loginPacket) {
        handleCertChainData(connector, session, loginPacket.getChainData());
    }

    private static void handleCertChainData(GeyserConnector connector, GeyserSession session, AsciiString chainData) {
        JsonNode certChainData;
        try {
            certChainData = getCertChainData(connector, chainData);
        } catch (Exception ex) {
            throw new RuntimeException("Cannot get cert chain data: " + ex.getMessage(), ex);
        }

        AuthData authData = getAuthData(connector, session, certChainData);
        session.setAuthData(authData);

        ECPublicKey identityPublicKey = getIdentityPublicKey(certChainData);
        session.setIdentityPublicKey(identityPublicKey);
    }
    public static void handleClientData(GeyserConnector connector, GeyserSession session, LoginPacket loginPacket) {
        handleClientData(connector, session, loginPacket.getSkinData());
    }

    public static void handleClientData(GeyserConnector connector, GeyserSession session, SubClientLoginPacket loginPacket) {
        handleClientData(connector, session, loginPacket.getSkinData());
    }

    private static void handleClientData(GeyserConnector connector, GeyserSession session, AsciiString skinData) {
        BedrockClientData bedrockClientData = getClientData(skinData, session.getIdentityPublicKey());

        // SubClientLoginPacket is missing some clientData fields:
        // * ServerAddress
        // * LanguageCode
        // * DeviceModel
        // * UiProfile
        // We only really need LanguageCode right now        
        if (session.getClientId() != 0) {
            BedrockClientData mainClientData = ((UpstreamPacketHandler)session.getUpstream().getSession().getPacketHandler()).getSessions().get(0).getClientData();

            connector.getLogger().debug("" + mainClientData);
            connector.getLogger().debug("" + bedrockClientData);

            bedrockClientData.setLanguageCode(mainClientData.getLanguageCode());
        }

        session.setClientData(bedrockClientData);
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

    private static AuthData getAuthData(GeyserConnector connector, GeyserSession session, JsonNode payload) {
        if (payload.get("extraData").getNodeType() != JsonNodeType.OBJECT) {
            throw new RuntimeException("AuthData was not found!");
        }

        JsonNode extraData = payload.get("extraData");
        connector.getLogger().debug("" + extraData);

        AuthData authData = new AuthData(
                extraData.get("displayName").asText(),
                UUID.fromString(extraData.get("identity").asText()),
                extraData.get("XUID").asText()
        );
        connector.getLogger().debug("" + session.getAuthData());
        return authData;
    }

    private static JsonNode getCertChainData(GeyserConnector connector, AsciiString chainData) {
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

        try {
            boolean validChain = validateChainData(certChainData);
            connector.getLogger().debug(String.format("Is player data valid? %s", validChain));
        } catch (Exception ex) {
            // rethrow until we can wrap in custom exceptions
            throw new RuntimeException("Unable to validate chain data: " + ex.getMessage(), ex);
        }

        JsonNode payload;
        try {
            JWSObject jwt = JWSObject.parse(certChainData.get(certChainData.size() - 1).asText());
            payload = JSON_MAPPER.readTree(jwt.getPayload().toBytes());
        } catch (Exception ex) {
            throw new RuntimeException("Unable to set auth data: " + ex.getMessage(), ex);
        }
        return payload;
    }

    public static void encryptPlayerConnection(GeyserConnector connector, GeyserSession session, LoginPacket loginPacket) {
        try {
            if (EncryptionUtils.canUseEncryption()) {
                LoginEncryptionUtils.startEncryptionHandshake(session);
            }
        } catch (Exception ex) {
            session.disconnect("disconnectionScreen.internalError.cantConnect");
            throw new RuntimeException("Unable to complete login: " + ex.getMessage(), ex);
        }
    }

    private static void startEncryptionHandshake(GeyserSession session) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        KeyPair serverKeyPair = generator.generateKeyPair();

        byte[] token = EncryptionUtils.generateRandomToken();
        SecretKey encryptionKey = EncryptionUtils.getSecretKey(serverKeyPair.getPrivate(), session.getIdentityPublicKey(), token);
        session.getUpstream().getSession().enableEncryption(encryptionKey);

        ServerToClientHandshakePacket packet = new ServerToClientHandshakePacket();
        packet.setJwt(EncryptionUtils.createHandshakeJwt(serverKeyPair, token).serialize());
        session.sendUpstreamPacketImmediately(packet);
    }

    private static int AUTH_FORM_ID = 1336;
    private static int AUTH_DETAILS_FORM_ID = 1337;

    public static void showLoginWindow(GeyserSession session) {
        String userLanguage = session.getClientData().getLanguageCode();
        SimpleFormWindow window = new SimpleFormWindow(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.notice.title", userLanguage), LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.notice.desc", userLanguage));
        window.getButtons().add(new FormButton(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.notice.btn_login", userLanguage)));
        window.getButtons().add(new FormButton(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.notice.btn_disconnect", userLanguage)));

        session.sendForm(window, AUTH_FORM_ID);
    }

    public static void showLoginDetailsWindow(GeyserSession session) {
        String userLanguage = session.getClientData().getLanguageCode();
        CustomFormWindow window = new CustomFormBuilder(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.details.title", userLanguage))
                .addComponent(new LabelComponent(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.details.desc", userLanguage)))
                .addComponent(new InputComponent(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.details.email", userLanguage), "account@geysermc.org", ""))
                .addComponent(new InputComponent(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.details.pass", userLanguage), "123456", ""))
                .build();

        session.sendForm(window, AUTH_DETAILS_FORM_ID);
    }

    public static boolean authenticateFromForm(GeyserSession session, GeyserConnector connector, int formId, String formData) {
        WindowCache windowCache = session.getWindowCache();
        if (!windowCache.getWindows().containsKey(formId))
            return false;

        if(formId == AUTH_FORM_ID || formId == AUTH_DETAILS_FORM_ID) {
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
                    } else {
                        showLoginDetailsWindow(session);
                    }

                    // Clear windows so authentication data isn't accidentally cached
                    windowCache.getWindows().clear();
                } else if (formId == AUTH_FORM_ID && window instanceof SimpleFormWindow) {
                    SimpleFormResponse response = (SimpleFormResponse) window.getResponse();
                    if (response != null) {
                        if (response.getClickedButtonId() == 0) {
                            showLoginDetailsWindow(session);
                        } else if(response.getClickedButtonId() == 1) {
                            session.disconnect(LanguageUtils.getPlayerLocaleString("geyser.auth.login.form.disconnect", session.getClientData().getLanguageCode()));
                        }
                    } else {
                        showLoginWindow(session);
                    }
                }
            }
        }
        return true;
    }

}
