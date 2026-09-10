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

package org.geysermc.geyser.network.bedrock.nethernet.signalling.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assertions.*;

class ProviderHostIdentityTest {
    @TempDir Path directory;

    @Test void firstStartCreatesAPrivatePairedIdentityAndRestartPreservesIt() throws Exception {
        Path state = directory.resolve("state");
        var first = ProviderHostIdentity.ensure(state);
        byte[] key = Files.readAllBytes(first.privateKey()), certificate = Files.readAllBytes(first.certificate());
        Files.writeString(state.resolve("provider-state.json"), "existing-machine-identity");
        var second = ProviderHostIdentity.ensure(state);
        assertEquals(first.fingerprint(), second.fingerprint());
        assertArrayEquals(key, Files.readAllBytes(second.privateKey()));
        assertArrayEquals(certificate, Files.readAllBytes(second.certificate()));
        assertEquals("existing-machine-identity", Files.readString(state.resolve("provider-state.json")));
        assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(state));
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(second.privateKey()));
    }

    @Test void missingHalfOfAnExistingPairIsNeverRegenerated() throws Exception {
        Files.writeString(directory.resolve("host-key.pem"), "existing-private-key");
        assertThrows(Exception.class, () -> ProviderHostIdentity.ensure(directory));
        assertEquals("existing-private-key", Files.readString(directory.resolve("host-key.pem")));
        assertFalse(Files.exists(directory.resolve("host-cert.pem")));
    }

    @Test void mismatchedPairIsRejectedWithoutReplacement() throws Exception {
        var first = ProviderHostIdentity.ensure(directory.resolve("first"));
        var other = ProviderHostIdentity.ensure(directory.resolve("other"));
        byte[] key = Files.readAllBytes(first.privateKey());
        Files.copy(other.certificate(), first.certificate(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        assertThrows(Exception.class, () -> ProviderHostIdentity.ensure(directory.resolve("first")));
        assertArrayEquals(key, Files.readAllBytes(first.privateKey()));
        assertArrayEquals(Files.readAllBytes(other.certificate()), Files.readAllBytes(first.certificate()));
    }

    @Test void symbolicIdentityAndConcurrentInitializationAreRejected() throws Exception {
        Path state = directory.resolve("state");
        Files.createDirectory(state);
        Files.createSymbolicLink(state.resolve("host-key.pem"), directory.resolve("elsewhere"));
        assertThrows(Exception.class, () -> ProviderHostIdentity.ensure(state));
        Files.delete(state.resolve("host-key.pem"));
        try (var channel = FileChannel.open(state.resolve("host-identity.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            assertThrows(Exception.class, () -> ProviderHostIdentity.ensure(state));
        }
        assertFalse(Files.exists(state.resolve("host-key.pem")));
    }
}
