/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

import io.netty.channel.DefaultChannelPromise;
import io.netty.util.DefaultAttributeMap;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.cloudburstmc.netty.channel.nethernet.AddressSource;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChildChannel;
import org.cloudburstmc.netty.channel.nethernet.SourcedAddress;
import org.cloudburstmc.protocol.bedrock.data.auth.CertificateChainPayload;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.geysermc.geyser.network.bedrock.CodecProcessor;
import org.geysermc.geyser.network.bedrock.nethernet.NetherNetPeer;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.auth.AuthData;
import org.geysermc.geyser.text.GeyserLocale;
import org.jose4j.jws.JsonWebSignature;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LanLoginIdentityTest {
    @Test
    void actualClientSignatureNonceAndLanOriginAreAllRequired() throws Exception {
        for (boolean signed : new boolean[]{true, false}) {
            for (boolean validSignature : new boolean[]{true, false}) {
                for (boolean lan : new boolean[]{true, false}) {
                    for (String nonce : new String[]{"original-server-nonce", "stale-server-nonce", null}) {
                        check(signed, validSignature, lan, nonce);
                    }
                }
            }
        }
    }

    private void check(boolean signed, boolean validSignature, boolean lan, String nonce) throws Exception {
        var key = EncryptionUtils.createKeyPair();
        var channel = mock(NetherNetChildChannel.class);
        var attributes = new DefaultAttributeMap();
        when(channel.attr(any())).thenAnswer(call -> attributes.attr(call.getArgument(0)));
        when(channel.closeFuture()).thenReturn(new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE));
        if (lan) {
            when(channel.signaledAddress()).thenReturn(new SourcedAddress(
                new InetSocketAddress("127.0.0.1", 1234), AddressSource.SIGNALING_LAN, null));
        }
        var session = mock(GeyserSession.class, RETURNS_DEEP_STUBS);
        var peer = mock(NetherNetPeer.class);
        when(peer.getChannel()).thenReturn(channel);
        when(session.getUpstream().getSession().getPeer()).thenReturn(peer);
        when(session.getGeyser().config().advanced().bedrock().validateBedrockLogin()).thenReturn(true);
        when(session.getGeyser().getNetherNetServer().isLanNonce("original-server-nonce")).thenReturn(true);

        // The service chain verifier is a test boundary; the client JWT below uses real ES384 signatures.
        var payload = new CertificateChainPayload(List.of("verified-service-boundary-fixture"));
        String encodedKey = Base64.getEncoder().encodeToString(key.getPublic().getEncoded());
        var result = new ChainValidationResult(signed, """
            {"identityPublicKey":"%s","extraData":{"displayName":"Fixture",
             "identity":"00000000-0000-0000-0000-000000000001","XUID":"2535412345678901"}}
            """.formatted(encodedKey));
        var jwt = new JsonWebSignature();
        jwt.setAlgorithmHeaderValue("ES384");
        jwt.setKey(validSignature ? key.getPrivate() : EncryptionUtils.createKeyPair().getPrivate());
        jwt.setPayload(nonce == null ? "{\"GameVersion\":\"26.60\"}"
            : "{\"GameVersion\":\"26.60\",\"Nonce\":\"" + nonce + "\"}");
        var packet = new LoginPacket();
        packet.setAuthPayload(payload);
        packet.setClientJwt(jwt.getCompactSerialization());

        try (var encryption = mockStatic(EncryptionUtils.class, CALLS_REAL_METHODS);
             var locale = mockStatic(GeyserLocale.class);
             var codec = mockStatic(CodecProcessor.class)) {
            encryption.when(() -> EncryptionUtils.validatePayload(payload)).thenReturn(result);
            locale.when(() -> GeyserLocale.getLocaleStringLog(anyString())).thenReturn("invalid account");
            if (signed && !validSignature) {
                assertThrows(RuntimeException.class, () -> LoginEncryptionUtils.encryptPlayerConnection(session, packet));
            } else {
                LoginEncryptionUtils.encryptPlayerConnection(session, packet);
            }
            if (signed && validSignature && lan && "original-server-nonce".equals(nonce)) {
                verify(session).setAuthData(any(AuthData.class));
            } else {
                verify(session, never()).setAuthData(any());
            }
        }
    }
}
