/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.edition.mcee.shims;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nukkitx.protocol.bedrock.packet.ServerToClientHandshakePacket;
import com.nukkitx.protocol.bedrock.util.EncryptionUtils;
import org.geysermc.connector.GeyserEdition;
import org.geysermc.connector.edition.mcee.utils.TokenManager;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LoginEncryptionUtils;

import javax.crypto.SecretKey;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

public class LoginEncryptionUtilsShim implements LoginEncryptionUtils.Shim {

    private TokenManager tokenManager;

    public LoginEncryptionUtilsShim(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    protected JWSObject createHandshakeJwt(KeyPair serverKeyPair, byte[] token, String signedToken) throws JOSEException {
        URI x5u = URI.create(Base64.getEncoder().encodeToString(serverKeyPair.getPublic().getEncoded()));

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();
        claimsBuilder.claim("salt", Base64.getEncoder().encodeToString(token));

        if (signedToken != null && !signedToken.isEmpty()) {
            claimsBuilder.claim("signedToken", signedToken);
        }

        SignedJWT jwt = new SignedJWT((new com.nimbusds.jose.JWSHeader.Builder(JWSAlgorithm.ES384)).x509CertURL(x5u).build(),
                claimsBuilder.build());
        EncryptionUtils.signJwt(jwt, (ECPrivateKey)serverKeyPair.getPrivate());
        return jwt;
    }

    @Override
    public void startEncryptionHandshake(GeyserSession session, PublicKey key) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        KeyPair serverKeyPair = generator.generateKeyPair();

        byte[] token = EncryptionUtils.generateRandomToken();
        SecretKey encryptionKey = EncryptionUtils.getSecretKey(serverKeyPair.getPrivate(), key, token);
        session.getUpstream().getSession().enableEncryption(encryptionKey);

        // Get signedToken for connecting client
        String signedToken = null;
        if (session.getClientData().getTenantId() != null) {
            if (tokenManager.getTokenMap().containsKey(session.getClientData().getTenantId())) {
                // This may block if it needs to be refreshed
                signedToken = tokenManager.getTokenMap().get(session.getClientData().getTenantId()).getSignedToken();
            } else {
                session.getConnector().getLogger().warning("Unknown Tenant tried to connect: " + session.getClientData().getTenantId());
            }
        }

        ServerToClientHandshakePacket packet = new ServerToClientHandshakePacket();
        packet.setJwt(createHandshakeJwt(serverKeyPair, token, signedToken).serialize());
        session.sendUpstreamPacketImmediately(packet);
    }
}
