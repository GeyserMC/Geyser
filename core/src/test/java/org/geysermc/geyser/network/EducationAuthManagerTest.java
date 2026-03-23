/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.network;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EducationAuthManagerTest {

    private EducationAuthManager authManager;

    @BeforeEach
    void setUp() {
        authManager = new EducationAuthManager();
    }

    // ---- extractTenantIdFromServerToken ----

    @Test
    void extractTenantIdFromServerToken_validFormat() {
        String token = "tenant-abc|server-123|2026-03-27T22:00:00Z|signature-data";
        assertEquals("tenant-abc", authManager.extractTenantIdFromServerToken(token));
    }

    @Test
    void extractTenantIdFromServerToken_uuidTenantId() {
        String token = "4cf5151d-0705-4be5-839d-fa2abe1b4206|b3142e0a-8e42-4ccb-882a|2026-03-27|sig";
        assertEquals("4cf5151d-0705-4be5-839d-fa2abe1b4206", authManager.extractTenantIdFromServerToken(token));
    }

    @Test
    void extractTenantIdFromServerToken_tooFewSegments() {
        assertNull(authManager.extractTenantIdFromServerToken("only-two|parts"));
        assertNull(authManager.extractTenantIdFromServerToken("single"));
    }

    @Test
    void extractTenantIdFromServerToken_emptyTenantId() {
        assertNull(authManager.extractTenantIdFromServerToken("|server|expiry|sig"));
    }

    @Test
    void extractTenantIdFromServerToken_null() {
        assertNull(authManager.extractTenantIdFromServerToken(null));
    }

    @Test
    void extractTenantIdFromServerToken_empty() {
        assertNull(authManager.extractTenantIdFromServerToken(""));
    }

    // ---- extractTenantIdFromEduTokenChain ----

    @Test
    void extractTenantIdFromEduTokenChain_validJwt() {
        // Build a JWT with payload: {"chain": "tenant-xyz|signature|expiry|nonce"}
        String header = base64Url("{\"alg\":\"ES384\"}");
        String payload = base64Url("{\"chain\":\"tenant-xyz|sig|exp|nonce\"}");
        String jwt = header + "." + payload + ".fake-signature";
        assertEquals("tenant-xyz", authManager.extractTenantIdFromEduTokenChain(jwt));
    }

    @Test
    void extractTenantIdFromEduTokenChain_missingChainField() {
        String header = base64Url("{\"alg\":\"ES384\"}");
        String payload = base64Url("{\"other\":\"value\"}");
        String jwt = header + "." + payload + ".fake-signature";
        assertNull(authManager.extractTenantIdFromEduTokenChain(jwt));
    }

    @Test
    void extractTenantIdFromEduTokenChain_emptyChain() {
        String header = base64Url("{\"alg\":\"ES384\"}");
        String payload = base64Url("{\"chain\":\"\"}");
        String jwt = header + "." + payload + ".fake-signature";
        assertNull(authManager.extractTenantIdFromEduTokenChain(jwt));
    }

    @Test
    void extractTenantIdFromEduTokenChain_notAJwt() {
        assertNull(authManager.extractTenantIdFromEduTokenChain("not-a-jwt"));
    }

    @Test
    void extractTenantIdFromEduTokenChain_null() {
        assertNull(authManager.extractTenantIdFromEduTokenChain(null));
    }

    @Test
    void extractTenantIdFromEduTokenChain_empty() {
        assertNull(authManager.extractTenantIdFromEduTokenChain(""));
    }

    // ---- isConfigTrustTenant ----

    @Test
    void isConfigTrustTenant_registeredViaConfig() {
        String token = "config-tenant|server-id|2026-12-31|signature";
        authManager.registerServerTokenFromConfig(token, "test");
        assertTrue(authManager.isConfigTrustTenant("config-tenant"));
    }

    @Test
    void isConfigTrustTenant_registeredViaMess() {
        authManager.registerServerToken("mess-token", "mess-tenant", "MESS registration");
        assertFalse(authManager.isConfigTrustTenant("mess-tenant"));
    }

    @Test
    void isConfigTrustTenant_unknownTenant() {
        assertFalse(authManager.isConfigTrustTenant("unknown-tenant"));
    }

    @Test
    void isConfigTrustTenant_null() {
        assertFalse(authManager.isConfigTrustTenant(null));
    }

    @Test
    void isConfigTrustTenant_empty() {
        assertFalse(authManager.isConfigTrustTenant(""));
    }

    @Test
    void configTrustAndMessTenantsSeparate() {
        // Register one tenant via config, another via MESS
        String configToken = "config-t|srv|exp|sig";
        authManager.registerServerTokenFromConfig(configToken, "config");
        authManager.registerServerToken("mess-token-value", "mess-t", "MESS");

        assertTrue(authManager.isConfigTrustTenant("config-t"));
        assertFalse(authManager.isConfigTrustTenant("mess-t"));
        // Both should be in the token pool for routing
        assertEquals(2, authManager.getRegisteredTenantCount());
    }

    // ---- formatExpiry ----

    @Test
    void formatExpiry_zero() {
        assertEquals("never/unset", authManager.formatExpiry(0));
    }

    @Test
    void formatExpiry_negative() {
        assertEquals("never/unset", authManager.formatExpiry(-1));
    }

    @Test
    void formatExpiry_validEpoch() {
        String result = authManager.formatExpiry(1700000000L);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("2023")); // Nov 14, 2023
    }

    // ---- isActive ----

    @Test
    void isActive_noTokens() {
        assertFalse(authManager.isActive());
    }

    @Test
    void isActive_withConfigTokens() {
        String token = "tenant|server|expiry|sig";
        authManager.registerServerTokenFromConfig(token, "test");
        assertTrue(authManager.isActive());
    }

    // ---- Nonce cache ----

    @Test
    void verifyNonce_notInCache() {
        // Without MESS connection, verifyNonce should return false for unknown nonces
        assertFalse(authManager.verifyNonce("nonexistent-nonce"));
    }

    @Test
    void verifyNonce_null() {
        assertFalse(authManager.verifyNonce(null));
    }

    @Test
    void verifyNonce_empty() {
        assertFalse(authManager.verifyNonce(""));
    }

    // ---- Join counters ----

    @Test
    void joinCounters_incrementCorrectly() {
        assertEquals(0, authManager.getVerifiedJoins());
        assertEquals(0, authManager.getUnverifiedJoins());
        assertEquals(0, authManager.getRejectedJoins());

        authManager.recordVerifiedJoin();
        authManager.recordVerifiedJoin();
        authManager.recordUnverifiedJoin();
        authManager.recordRejectedJoin();
        authManager.recordRejectedJoin();
        authManager.recordRejectedJoin();

        assertEquals(2, authManager.getVerifiedJoins());
        assertEquals(1, authManager.getUnverifiedJoins());
        assertEquals(3, authManager.getRejectedJoins());
    }

    // ---- Helper ----

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
