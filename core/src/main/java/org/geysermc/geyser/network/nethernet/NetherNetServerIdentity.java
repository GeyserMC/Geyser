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
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The server identity assertion attached to every SDP answer, per the
 * NetherNet onboarding guide (section 5.2) and matching go-nethernet
 * and vanilla BDS behavior. HTTP signaled clients have always required
 * it; 26.40 clients require it over Xbox RPC signaling as well.
 *
 * The envelope is an {@code a=identity} session attribute: base64 of a JSON
 * object carrying a self signed ES384 JWT (whose {@code cpk} claim is the
 * operator public key that clients pin) and a detached JWS over the answer's
 * DTLS fingerprints, binding the identity to this specific connection.
 *
 * The keypair is the unit of trust for the TOFU flow, so it is generated once
 * and persisted; deleting the key file makes the server appear as a new
 * operator to returning players. The short lived token is re signed per
 * answer, which the spec allows and BDS does; the pin survives token rotation
 * because {@code cpk} stays constant.
 */
public final class NetherNetServerIdentity {

    private static final Pattern FINGERPRINT_PATTERN = Pattern.compile("^a=fingerprint:(\\S+) (\\S+)", Pattern.MULTILINE);
    private static final String KEY_FILE = "identity-key.pem";
    /** BDS uses "self" for self issued server identities. */
    private static final String IDP_DOMAIN = "self";

    private final PrivateKey privateKey;
    private final String encodedPublicKey;

    private NetherNetServerIdentity(PrivateKey privateKey, PublicKey publicKey) {
        this.privateKey = privateKey;
        this.encodedPublicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Loads the persisted operator keypair from {@code identity-key.pem} in
     * the given directory, generating and persisting a fresh P-384 pair on
     * first use.
     */
    public static NetherNetServerIdentity loadOrCreate(Path nethernetDir) throws Exception {
        Path keyFile = nethernetDir.resolve(KEY_FILE);
        if (Files.exists(keyFile)) {
            String pem = Files.readString(keyFile, StandardCharsets.UTF_8);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PrivateKey privateKey = keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(readPemBlock(pem, "PRIVATE KEY")));
            PublicKey publicKey = keyFactory.generatePublic(
                    new X509EncodedKeySpec(readPemBlock(pem, "PUBLIC KEY")));
            return new NetherNetServerIdentity(privateKey, publicKey);
        }

        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        KeyPair pair = generator.generateKeyPair();
        Files.createDirectories(nethernetDir);
        Files.writeString(keyFile,
                writePemBlock(pair.getPrivate().getEncoded(), "PRIVATE KEY")
                        + writePemBlock(pair.getPublic().getEncoded(), "PUBLIC KEY"),
                StandardCharsets.UTF_8);
        return new NetherNetServerIdentity(pair.getPrivate(), pair.getPublic());
    }

    /**
     * Inserts the {@code a=identity} assertion into the given SDP answer as a
     * session level attribute before the first media line. Thread safe;
     * called from engine threads per accepted connection.
     */
    public String decorate(String answerSdp) throws Exception {
        String canonicalFingerprints = canonicalFingerprintJson(answerSdp);
        String identityLine = "a=identity:" + Base64.getEncoder().encodeToString(
                buildEnvelope(canonicalFingerprints).getBytes(StandardCharsets.UTF_8));

        // Session level: before the first m= line, after the session attributes.
        int mediaIndex = answerSdp.startsWith("m=") ? 0 : answerSdp.indexOf("\nm=") + 1;
        if (mediaIndex <= 0 && !answerSdp.startsWith("m=")) {
            throw new IllegalArgumentException("SDP answer has no media description");
        }
        return answerSdp.substring(0, mediaIndex) + identityLine + "\r\n" + answerSdp.substring(mediaIndex);
    }

    /**
     * The canonical JSON payload both sides must byte identically reproduce
     * for the detached JWS: fingerprint objects in SDP order, keys sorted,
     * no whitespace.
     */
    private static String canonicalFingerprintJson(String answerSdp) {
        List<String[]> fingerprints = new ArrayList<>();
        Matcher matcher = FINGERPRINT_PATTERN.matcher(answerSdp);
        while (matcher.find()) {
            fingerprints.add(new String[]{matcher.group(1), matcher.group(2)});
        }
        if (fingerprints.isEmpty()) {
            throw new IllegalArgumentException("SDP answer has no a=fingerprint lines");
        }
        StringBuilder json = new StringBuilder("{\"fingerprint\":[");
        for (int i = 0; i < fingerprints.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"algorithm\":\"").append(fingerprints.get(i)[0])
                    .append("\",\"digest\":\"").append(fingerprints.get(i)[1]).append("\"}");
        }
        return json.append("]}").toString();
    }

    private String buildEnvelope(String canonicalFingerprints) throws Exception {
        // Detached JWS (RFC 7515 appendix F) over the fingerprint payload:
        // header..signature, the payload reconstructed by the verifier.
        JsonWebSignature fingerprintJws = new JsonWebSignature();
        fingerprintJws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P384_CURVE_AND_SHA384);
        fingerprintJws.setPayload(canonicalFingerprints);
        fingerprintJws.setKey(privateKey);
        String detachedFingerprints = fingerprintJws.getDetachedContentCompactSerialization();

        // Short lived self signed token; cpk is the pinned operator key. The
        // x5u header mirrors vanilla BDS, which carries the public key there
        // like its ServerToClientHandshake JWT.
        JwtClaims claims = new JwtClaims();
        claims.setIssuedAtToNow();
        claims.setExpirationTimeMinutesInTheFuture(1);
        claims.setClaim("cpk", encodedPublicKey);
        JsonWebSignature tokenJws = new JsonWebSignature();
        tokenJws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P384_CURVE_AND_SHA384);
        tokenJws.setHeader("x5u", encodedPublicKey);
        tokenJws.setPayload(claims.toJson());
        tokenJws.setKey(privateKey);
        String token = tokenJws.getCompactSerialization();

        // The assertion is a JSON string (nested encoding) inside the envelope.
        JsonObject assertion = new JsonObject();
        assertion.addProperty("fingerprints", detachedFingerprints);
        assertion.addProperty("token", token);
        JsonObject idp = new JsonObject();
        idp.addProperty("domain", IDP_DOMAIN);
        idp.addProperty("protocol", "default");
        JsonObject envelope = new JsonObject();
        envelope.addProperty("assertion", assertion.toString());
        envelope.add("idp", idp);
        return envelope.toString();
    }

    private static byte[] readPemBlock(String pem, String label) {
        String begin = "-----BEGIN " + label + "-----";
        String end = "-----END " + label + "-----";
        int start = pem.indexOf(begin);
        int stop = pem.indexOf(end);
        if (start < 0 || stop < 0) {
            throw new IllegalArgumentException(KEY_FILE + " is missing its " + label + " block");
        }
        String body = pem.substring(start + begin.length(), stop).replaceAll("\\s", "");
        return Base64.getDecoder().decode(body);
    }

    private static String writePemBlock(byte[] der, String label) {
        return "-----BEGIN " + label + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(der)
                + "\n-----END " + label + "-----\n";
    }
}
