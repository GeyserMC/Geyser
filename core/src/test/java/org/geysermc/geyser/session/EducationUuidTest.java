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

package org.geysermc.geyser.session;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EducationUuidTest {

    private static final long EXPECTED_MSB = 0x0000000100000001L;

    // Real OIDs from test accounts
    private static final String OID_1 = "bb1430cb-bdcf-48b0-bd66-4b58bbb0a9dd";
    private static final String OID_2 = "1c309846-d918-41a5-b5a7-200a274f3599";

    @Test
    void msbIsAlwaysFixed() {
        UUID uuid = GeyserSessionAdapter.createEducationUuid(OID_1);
        assertEquals(EXPECTED_MSB, uuid.getMostSignificantBits());
    }

    @Test
    void msbFixedAcrossOids() {
        for (int i = 0; i < 100; i++) {
            String oid = UUID.randomUUID().toString();
            UUID uuid = GeyserSessionAdapter.createEducationUuid(oid);
            assertEquals(EXPECTED_MSB, uuid.getMostSignificantBits(),
                    "MSB mismatch for OID=" + oid);
        }
    }

    @Test
    void sameOidProducesSameUuid() {
        UUID first = GeyserSessionAdapter.createEducationUuid(OID_1);
        UUID second = GeyserSessionAdapter.createEducationUuid(OID_1);
        assertEquals(first, second);
    }

    @Test
    void deterministicAcrossInvocations() {
        UUID expected = GeyserSessionAdapter.createEducationUuid(OID_1);
        for (int i = 0; i < 100; i++) {
            assertEquals(expected, GeyserSessionAdapter.createEducationUuid(OID_1));
        }
    }

    @Test
    void differentOidsProduceDifferentUuids() {
        UUID a = GeyserSessionAdapter.createEducationUuid(OID_1);
        UUID b = GeyserSessionAdapter.createEducationUuid(OID_2);
        assertNotEquals(a, b);
    }

    @Test
    void noCollisionsAcrossManyOids() {
        Set<UUID> seen = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            String oid = UUID.randomUUID().toString();
            UUID uuid = GeyserSessionAdapter.createEducationUuid(oid);
            assertTrue(seen.add(uuid), "Collision detected for OID=" + oid);
        }
        assertEquals(10000, seen.size());
    }

    @Test
    void lsbDiffersFromMsb() {
        UUID uuid = GeyserSessionAdapter.createEducationUuid(OID_1);
        assertNotEquals(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    @Test
    void lsbContainsOnlyRandomBits() {
        // The LSB is derived from the OID's first 64 random bits (bits 0-69,
        // skipping version at 48-51 and variant at 64-65). Changing a byte
        // within this range must change the result.
        String base =    "bb1430cb-bdcf-48b0-bd66-4b58bbb0a9dd";
        String tweaked = "cc1430cb-bdcf-48b0-bd66-4b58bbb0a9dd"; // first byte changed
        UUID a = GeyserSessionAdapter.createEducationUuid(base);
        UUID b = GeyserSessionAdapter.createEducationUuid(tweaked);
        assertNotEquals(a.getLeastSignificantBits(), b.getLeastSignificantBits());
    }

    @Test
    void versionAndVariantBitsAreStripped() {
        // Two UUIDs with identical random bits but different version/variant
        // should produce the SAME education UUID, since those bits are stripped.
        String v4 = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee"; // version=4, variant=8 (10xx)
        String v5 = "aaaaaaaa-bbbb-5ccc-cddd-eeeeeeeeeeee"; // version=5, variant=c (11xx)
        UUID a = GeyserSessionAdapter.createEducationUuid(v4);
        UUID b = GeyserSessionAdapter.createEducationUuid(v5);
        assertEquals(a, b, "Stripping version/variant bits should yield the same UUID when random bits match");
    }

    @Test
    void realOidsProduceValidUuids() {
        // Verify with the real OIDs from our test accounts
        UUID uuid1 = GeyserSessionAdapter.createEducationUuid(OID_1);
        UUID uuid2 = GeyserSessionAdapter.createEducationUuid(OID_2);
        assertEquals(EXPECTED_MSB, uuid1.getMostSignificantBits());
        assertEquals(EXPECTED_MSB, uuid2.getMostSignificantBits());
        assertNotEquals(uuid1, uuid2);
    }

    // ---- Legacy scheme: SHA-256(tenantId:username) ----

    @Test
    void legacyKnownVectors() {
        // Golden vectors that pin the legacy algorithm exactly. The legacy scheme exists to
        // keep identity stable with existing player data, so this derivation must never change.
        assertEquals(UUID.fromString("00000001-0000-0001-2efe-3e68242fd6a0"),
                GeyserSessionAdapter.createLegacyEducationUuid("contoso", "alice"));
        assertEquals(UUID.fromString("00000001-0000-0001-c2d8-72e7a929f7fb"),
                GeyserSessionAdapter.createLegacyEducationUuid("contoso", "bob"));
    }

    @Test
    void legacyMsbIsFixed() {
        UUID uuid = GeyserSessionAdapter.createLegacyEducationUuid("contoso", "alice");
        assertEquals(EXPECTED_MSB, uuid.getMostSignificantBits());
    }

    @Test
    void legacyIsDeterministic() {
        UUID first = GeyserSessionAdapter.createLegacyEducationUuid("contoso", "alice");
        for (int i = 0; i < 100; i++) {
            assertEquals(first, GeyserSessionAdapter.createLegacyEducationUuid("contoso", "alice"));
        }
    }

    @Test
    void legacyDifferentInputsDiffer() {
        UUID a = GeyserSessionAdapter.createLegacyEducationUuid("contoso", "alice");
        UUID b = GeyserSessionAdapter.createLegacyEducationUuid("contoso", "bob");
        UUID c = GeyserSessionAdapter.createLegacyEducationUuid("fabrikam", "alice");
        assertNotEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void legacyAndModernSchemesDiffer() {
        UUID legacy = GeyserSessionAdapter.createLegacyEducationUuid("contoso", "alice");
        UUID modern = GeyserSessionAdapter.createEducationUuid(OID_1);
        assertNotEquals(legacy, modern);
    }
}
