package org.geysermc.geyser.util;

import io.netty.channel.DefaultChannelPromise;
import io.netty.util.DefaultAttributeMap;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.cloudburstmc.netty.channel.nethernet.NetherNetChildChannel;
import org.cloudburstmc.netty.util.nethernet.IdentityKeyVerifier;
import org.cloudburstmc.netty.util.nethernet.IdentityPublicKey;
import org.cloudburstmc.netty.util.nethernet.TransportIdentityBinding;
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

import java.security.KeyPair;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginIdentityBindingTest {
    @Test
    void validatedLoginKeyMustMatchTheDirectTransportBeforeAuthDataIsAccepted() throws Exception {
        for (boolean matching : new boolean[]{true, false}) run(matching, false, true, true);
    }

    @Test
    void configuredForwardingCompletesBindingOnlyAfterExistingFieldsAreAccepted() throws Exception {
        run(false, true, true, true);
        run(false, true, false, true);
        run(false, true, true, true, false);
    }

    @Test
    void invalidClientSignatureCannotConsumeTransportBinding() throws Exception {
        run(true, false, true, false);
    }

    private void run(boolean matching, boolean forwarded, boolean validForwardedFields, boolean validSignature) throws Exception {
        run(matching, forwarded, validForwardedFields, validSignature, true);
    }

    private void run(boolean matching, boolean forwarded, boolean validForwardedFields, boolean validSignature, boolean hasBinding) throws Exception {
        KeyPair admitted = EncryptionUtils.createKeyPair();
        KeyPair login = matching ? admitted : EncryptionUtils.createKeyPair();
        byte[] expected = IdentityPublicKey.canonical(admitted.getPublic());
        var verifier = new IdentityKeyVerifier() {
            protected boolean matches(byte[] key) { return MessageDigest.isEqual(expected, key); }
            protected void release() { }
        };
        var channel = mock(NetherNetChildChannel.class);
        var attributes = new DefaultAttributeMap();
        when(channel.attr(any())).thenAnswer(call -> attributes.attr(call.getArgument(0)));
        when(channel.closeFuture()).thenReturn(new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE));
        if (hasBinding) TransportIdentityBinding.install(channel, verifier);
        var session = mock(GeyserSession.class, RETURNS_DEEP_STUBS);
        var peer = mock(NetherNetPeer.class);
        when(peer.getChannel()).thenReturn(channel);
        when(session.getUpstream().getSession().getPeer()).thenReturn(peer);
        when(session.getGeyser().config().advanced().bedrock().validateBedrockLogin()).thenReturn(true);
        when(session.getGeyser().config().advanced().bedrock().useWaterdogpeForwarding()).thenReturn(forwarded);
        var payload = new CertificateChainPayload(List.of("service-validated-fixture"));
        // Service JWT trust is stubbed at its verified-result boundary. The client's
        // ES384 JWT verification and the transport comparison run unchanged.
        var result = new ChainValidationResult(true, "{\"identityPublicKey\":\""
                + Base64.getEncoder().encodeToString(login.getPublic().getEncoded())
                + "\",\"extraData\":{\"displayName\":\"Fixture\",\"identity\":\"00000000-0000-0000-0000-000000000001\",\"XUID\":\"2535412345678901\"}}");
        JsonWebSignature jwt = new JsonWebSignature();
        jwt.setAlgorithmHeaderValue("ES384");
        jwt.setKey(validSignature ? login.getPrivate() : EncryptionUtils.createKeyPair().getPrivate());
        jwt.setPayload(validForwardedFields ? "{\"GameVersion\":\"26.30\",\"Waterdog_IP\":\"127.0.0.1\",\"Waterdog_XUID\":\"2535412345678901\"}"
                : "{\"GameVersion\":\"26.30\"}");
        var packet = new LoginPacket();
        packet.setAuthPayload(payload);
        packet.setClientJwt(jwt.getCompactSerialization());
        try (var encryption = mockStatic(EncryptionUtils.class, CALLS_REAL_METHODS);
             var locale = mockStatic(GeyserLocale.class);
             var codec = mockStatic(CodecProcessor.class)) {
            encryption.when(() -> EncryptionUtils.validatePayload(payload)).thenReturn(result);
            locale.when(() -> GeyserLocale.getLocaleStringLog(anyString())).thenReturn("invalid account");
            if (validSignature) LoginEncryptionUtils.encryptPlayerConnection(session, packet);
            else assertThrows(RuntimeException.class, () -> LoginEncryptionUtils.encryptPlayerConnection(session, packet));
            boolean accepted = validSignature && (forwarded ? validForwardedFields : matching);
            if (accepted) {
                verify(session).setAuthData(any(AuthData.class));
                verify(session, never()).disconnect(anyString());
                assertEquals(!hasBinding, verifier.pending());
                assertFalse(verifier.rejected());
            } else {
                verify(session, never()).setAuthData(any());
                verify(session).disconnect(anyString());
            }
            if (!validSignature || (forwarded && !validForwardedFields)) assertTrue(verifier.pending());
        } finally { verifier.close(); }
    }
}
