package org.geysermc.connector.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.nimbusds.jose.JWSObject;
import com.nukkitx.protocol.bedrock.packet.LoginPacket;
import com.nukkitx.protocol.bedrock.packet.ServerToClientHandshakePacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import net.minidev.json.JSONObject;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.auth.BedrockAuthData;
import org.geysermc.connector.utils.LoginEncryptionUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.UUID;

public class AuthenticationUtils {
    public static void encryptPlayerConnection(GeyserConnector connector, GeyserSession session, LoginPacket loginPacket) {
        JsonNode certData;
        try {
            certData = LoginEncryptionUtils.JSON_MAPPER.readTree(loginPacket.getChainData().toByteArray());
        } catch (IOException ex) {
            throw new RuntimeException("Certificate JSON can not be read.");
        }

        JsonNode certChainData = certData.get("chain");
        if (certChainData.getNodeType() != JsonNodeType.ARRAY) {
            throw new RuntimeException("Certificate data is not valid");
        }

        encryptConnectionWithCert(connector, session, loginPacket.getSkinData().toString(), certData);
    }

    private static void encryptConnectionWithCert(GeyserConnector connector, GeyserSession session, String playerSkin, JsonNode certChainData) {
        try {
            boolean validChain = LoginEncryptionUtils.validateChainData(certChainData);

            connector.getLogger().debug(String.format("Is player data valid? %s", validChain));

            JWSObject jwt = JWSObject.parse(certChainData.get(certChainData.size() - 1).asText());
            JsonNode payload = LoginEncryptionUtils.JSON_MAPPER.readTree(jwt.getPayload().toBytes());

            if (payload.get("extraData").getNodeType() != JsonNodeType.OBJECT) {
                throw new RuntimeException("AuthData was not found!");
            }

            JSONObject extraData = (JSONObject) jwt.getPayload().toJSONObject().get("extraData");
            session.setAuthenticationData(new BedrockAuthData(extraData.getAsString("displayName"), UUID.fromString(extraData.getAsString("identity")), extraData.getAsString("XUID")));

            if (payload.get("identityPublicKey").getNodeType() != JsonNodeType.STRING) {
                throw new RuntimeException("Identity Public Key was not found!");
            }

            ECPublicKey identityPublicKey = EncryptionUtils.generateKey(payload.get("identityPublicKey").textValue());
            JWSObject clientJwt = JWSObject.parse(playerSkin);
            EncryptionUtils.verifyJwt(clientJwt, identityPublicKey);

            if (EncryptionUtils.canUseEncryption()) {
                AuthenticationUtils.startEncryptionHandshake(session, identityPublicKey);
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
        session.getUpstream().enableEncryption(encryptionKey);

        ServerToClientHandshakePacket packet = new ServerToClientHandshakePacket();
        packet.setJwt(EncryptionUtils.createHandshakeJwt(serverKeyPair, token).serialize());
        session.getUpstream().sendPacketImmediately(packet);
    }


}
