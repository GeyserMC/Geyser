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

package org.geysermc.geyser.network.nethernet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jose4j.jws.JsonWebSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks in the a=identity envelope: the client's verification flow is
 * replayed here exactly as the onboarding guide specifies it, so a format
 * regression fails the build instead of a live join.
 */
class NetherNetServerIdentityTest {

    private static final String ANSWER_SDP = "v=0\r\n"
            + "o=- 987 2 IN IP4 127.0.0.1\r\n"
            + "s=-\r\n"
            + "t=0 0\r\n"
            + "a=group:BUNDLE 0\r\n"
            + "m=application 56241 UDP/DTLS/SCTP webrtc-datachannel\r\n"
            + "c=IN IP4 192.168.1.112\r\n"
            + "a=fingerprint:sha-256 FA:1F:B9:DF:DD:42:63:3B:E0:AB:2D:09:01:7A:3C:7C:49:60:0A:10:84:77:7C:6D:69:F5:42:63:FC:E6:53:7F\r\n"
            + "a=setup:active\r\n"
            + "a=sctp-port:5000\r\n";

    @Test
    void decoratedAnswerVerifiesLikeTheClient(@TempDir Path tempDir) throws Exception {
        NetherNetServerIdentity identity = NetherNetServerIdentity.loadOrCreate(tempDir);
        String decorated = identity.decorate(ANSWER_SDP);

        // Session level placement: identity precedes the first media line.
        int identityIndex = decorated.indexOf("a=identity:");
        int mediaIndex = decorated.indexOf("m=application");
        assertTrue(identityIndex >= 0, "answer carries a=identity");
        assertTrue(identityIndex < mediaIndex, "identity is a session level attribute");

        // 1. Parse the envelope.
        String identityValue = decorated.substring(identityIndex + "a=identity:".length(), decorated.indexOf("\r\n", identityIndex));
        JsonObject envelope = JsonParser.parseString(
                new String(Base64.getDecoder().decode(identityValue), StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("self", envelope.getAsJsonObject("idp").get("domain").getAsString());
        assertEquals("default", envelope.getAsJsonObject("idp").get("protocol").getAsString());

        // 2. The assertion is a JSON string containing token + fingerprints.
        JsonObject assertion = JsonParser.parseString(envelope.get("assertion").getAsString()).getAsJsonObject();
        String token = assertion.get("token").getAsString();
        String fingerprints = assertion.get("fingerprints").getAsString();
        assertTrue(fingerprints.contains(".."), "fingerprints JWS is detached (empty payload part)");

        // 3+4. Extract cpk from the JWT and verify its self signature.
        String claimsJson = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        JsonObject claims = JsonParser.parseString(claimsJson).getAsJsonObject();
        PublicKey cpk = KeyFactory.getInstance("EC").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(claims.get("cpk").getAsString())));
        JsonWebSignature tokenJws = new JsonWebSignature();
        tokenJws.setCompactSerialization(token);
        tokenJws.setKey(cpk);
        assertTrue(tokenJws.verifySignature(), "token is self signed by cpk");
        assertTrue(claims.has("exp") && claims.has("iat"), "token carries timestamps");

        // 5. Verify the detached JWS with the payload reconstructed from the
        // answer's fingerprint lines as canonical JSON.
        String reconstructed = "{\"fingerprint\":[{\"algorithm\":\"sha-256\",\"digest\":\""
                + "FA:1F:B9:DF:DD:42:63:3B:E0:AB:2D:09:01:7A:3C:7C:49:60:0A:10:84:77:7C:6D:69:F5:42:63:FC:E6:53:7F\"}]}";
        JsonWebSignature fingerprintJws = new JsonWebSignature();
        fingerprintJws.setCompactSerialization(fingerprints.replace("..", "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(reconstructed.getBytes(StandardCharsets.UTF_8)) + "."));
        fingerprintJws.setKey(cpk);
        assertTrue(fingerprintJws.verifySignature(), "fingerprint JWS verifies against reconstructed payload");
    }

    @Test
    void keypairPersistsAcrossRestarts(@TempDir Path tempDir) throws Exception {
        String first = cpkOf(NetherNetServerIdentity.loadOrCreate(tempDir), tempDir);
        String second = cpkOf(NetherNetServerIdentity.loadOrCreate(tempDir), tempDir);
        assertEquals(first, second, "cpk survives a restart; the pin is stable");
    }

    private static String cpkOf(NetherNetServerIdentity identity, Path dir) throws Exception {
        String decorated = identity.decorate(ANSWER_SDP);
        int start = decorated.indexOf("a=identity:") + "a=identity:".length();
        JsonObject envelope = JsonParser.parseString(new String(
                Base64.getDecoder().decode(decorated.substring(start, decorated.indexOf("\r\n", start))),
                StandardCharsets.UTF_8)).getAsJsonObject();
        String token = JsonParser.parseString(envelope.get("assertion").getAsString())
                .getAsJsonObject().get("token").getAsString();
        return JsonParser.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                StandardCharsets.UTF_8)).getAsJsonObject().get("cpk").getAsString();
    }
}
