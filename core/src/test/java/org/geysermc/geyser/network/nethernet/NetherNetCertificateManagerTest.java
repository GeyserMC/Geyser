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

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetherNetCertificateManagerTest {

    @Test
    void renewsAtTwoThirdsOfTheShortlivedLifetime() {
        // The Let's Encrypt shortlived profile: 160 hours of validity.
        Instant notBefore = Instant.parse("2026-07-03T00:00:00Z");
        Instant notAfter = notBefore.plus(Duration.ofHours(160));

        assertFalse(NetherNetCertificateManager.needsRenewal(notBefore, notAfter, notBefore),
                "fresh certificate is kept");
        assertFalse(NetherNetCertificateManager.needsRenewal(notBefore, notAfter,
                notBefore.plus(Duration.ofHours(100))), "kept before the two thirds point");
        assertTrue(NetherNetCertificateManager.needsRenewal(notBefore, notAfter,
                notBefore.plus(Duration.ofHours(107))), "renewed past the two thirds point (~106.7h)");
        assertTrue(NetherNetCertificateManager.needsRenewal(notBefore, notAfter,
                notAfter.plus(Duration.ofHours(1))), "renewed when already expired");
    }
}
