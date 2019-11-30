package org.geysermc.connector.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.nimbusds.jose.JWSObject;
import com.nukkitx.network.util.Preconditions;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.packet.ServerToClientHandshakePacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import org.geysermc.api.events.player.PlayerFormResponseEvent;
import org.geysermc.api.window.CustomFormBuilder;
import org.geysermc.api.window.CustomFormWindow;
import org.geysermc.api.window.FormWindow;
import org.geysermc.api.window.component.InputComponent;
import org.geysermc.api.window.component.LabelComponent;
import org.geysermc.api.window.response.CustomFormResponse;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.auth.BedrockAuthData;
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
                EncryptionUtils.verifyJwt(jwt, lastKey);
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

            JWSObject jwt = JWSObject.parse(certChainData.get(certChainData.size() - 1).asText());
            JsonNode payload = JSON_MAPPER.readTree(jwt.getPayload().toBytes());

            if (payload.get("extraData").getNodeType() != JsonNodeType.OBJECT) {
                throw new RuntimeException("AuthData was not found!");
            }

            JsonNode extraData = payload.get("extraData");
            session.setAuthenticationData(new BedrockAuthData(
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
        session.getUpstream().sendPacketImmediately(packet);
    }

    private static int AUTH_FORM_ID = 1337;

    public static void showLoginWindow(GeyserSession session) {
        CustomFormWindow window = new CustomFormBuilder("Login")
                .addComponent(new LabelComponent("Minecraft: Java Edition account authentication."))
                .addComponent(new LabelComponent("Enter the credentials for your Minecraft: Java Edition account below."))
                .addComponent(new InputComponent("Email/Username", "account@geysermc.org", ""))
                .addComponent(new InputComponent("Password", "123456", ""))
                .build();

        session.sendForm(window, AUTH_FORM_ID);
    }

    public static boolean authenticateFromForm(GeyserSession session, GeyserConnector connector, String formData) {
        WindowCache windowCache = session.getWindowCache();
        if (!windowCache.getWindows().containsKey(AUTH_FORM_ID))
            return false;

        FormWindow window = windowCache.getWindows().remove(AUTH_FORM_ID);
        window.setResponse(formData.trim());

        if (session.isLoggedIn()) {
            PlayerFormResponseEvent event = new PlayerFormResponseEvent(session, AUTH_FORM_ID, window);
            connector.getPluginManager().runEvent(event);
        } else {
            if (window instanceof CustomFormWindow) {
                CustomFormWindow customFormWindow = (CustomFormWindow) window;
                if (!customFormWindow.getTitle().equals("Login"))
                    return false;

                CustomFormResponse response = (CustomFormResponse) customFormWindow.getResponse();

                if (response != null) {
                    String email = response.getInputResponses().get(2);
                    String password = response.getInputResponses().get(3);

                    session.authenticate(email, password);
                }

                // TODO should we clear the window cache in all cases or just if not already logged in?
                // Clear windows so authentication data isn't accidentally cached
                windowCache.getWindows().clear();
            }
        }
        return true;
    }

}
