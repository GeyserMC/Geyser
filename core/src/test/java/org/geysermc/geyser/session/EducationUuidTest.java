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

    @Test
    void msbIsAlwaysFixed() {
        UUID uuid = GeyserSessionAdapter.createEducationUuid("tenant-abc", "student1");
        assertEquals(EXPECTED_MSB, uuid.getMostSignificantBits());
    }

    @Test
    void msbFixedAcrossInputs() {
        String[] tenants = {"tenant-a", "tenant-b", "tenant-c"};
        String[] users = {"alice", "bob", "charlie"};
        for (String tenant : tenants) {
            for (String user : users) {
                UUID uuid = GeyserSessionAdapter.createEducationUuid(tenant, user);
                assertEquals(EXPECTED_MSB, uuid.getMostSignificantBits(),
                        "MSB mismatch for tenant=" + tenant + ", user=" + user);
            }
        }
    }

    @Test
    void sameInputsProduceSameUuid() {
        UUID first = GeyserSessionAdapter.createEducationUuid("tenant-xyz", "player1");
        UUID second = GeyserSessionAdapter.createEducationUuid("tenant-xyz", "player1");
        assertEquals(first, second);
    }

    @Test
    void deterministic_acrossInvocations() {
        // Call many times, always same result
        UUID expected = GeyserSessionAdapter.createEducationUuid("stable-tenant", "stable-user");
        for (int i = 0; i < 100; i++) {
            assertEquals(expected, GeyserSessionAdapter.createEducationUuid("stable-tenant", "stable-user"));
        }
    }

    @Test
    void differentTenantsProduceDifferentUuids() {
        UUID a = GeyserSessionAdapter.createEducationUuid("tenant-a", "same-user");
        UUID b = GeyserSessionAdapter.createEducationUuid("tenant-b", "same-user");
        assertNotEquals(a, b);
    }

    @Test
    void differentUsernamesProduceDifferentUuids() {
        UUID a = GeyserSessionAdapter.createEducationUuid("same-tenant", "user-a");
        UUID b = GeyserSessionAdapter.createEducationUuid("same-tenant", "user-b");
        assertNotEquals(a, b);
    }

    @Test
    void completelyDifferentInputsProduceDifferentUuids() {
        UUID a = GeyserSessionAdapter.createEducationUuid("tenant-1", "alice");
        UUID b = GeyserSessionAdapter.createEducationUuid("tenant-2", "bob");
        assertNotEquals(a, b);
    }

    @Test
    void noCollisionsAcrossManyInputs() {
        Set<UUID> seen = new HashSet<>();
        for (int t = 0; t < 50; t++) {
            for (int u = 0; u < 50; u++) {
                UUID uuid = GeyserSessionAdapter.createEducationUuid("tenant-" + t, "user-" + u);
                assertTrue(seen.add(uuid),
                        "Collision detected for tenant-" + t + "/user-" + u);
            }
        }
        assertEquals(2500, seen.size());
    }

    @Test
    void lsbDiffersFromMsb() {
        UUID uuid = GeyserSessionAdapter.createEducationUuid("any-tenant", "any-user");
        // LSB is derived from SHA-256, extremely unlikely to equal the fixed MSB
        assertNotEquals(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    @Test
    void swappedInputsProduceDifferentUuids() {
        // "tenant:user" != "user:tenant" — the colon separator prevents ambiguity
        UUID a = GeyserSessionAdapter.createEducationUuid("foo", "bar");
        UUID b = GeyserSessionAdapter.createEducationUuid("bar", "foo");
        assertNotEquals(a, b);
    }

    @Test
    void emptyInputsStillWork() {
        // Edge case: empty strings shouldn't throw
        UUID uuid = GeyserSessionAdapter.createEducationUuid("", "");
        assertNotNull(uuid);
        assertEquals(EXPECTED_MSB, uuid.getMostSignificantBits());
    }

    @Test
    void specialCharactersInInputs() {
        UUID uuid = GeyserSessionAdapter.createEducationUuid(
                "4cf5151d-0705-4be5-839d-fa2abe1b4206",
                "Student With Spaces & Special!@#"
        );
        assertNotNull(uuid);
        assertEquals(EXPECTED_MSB, uuid.getMostSignificantBits());
    }
}
