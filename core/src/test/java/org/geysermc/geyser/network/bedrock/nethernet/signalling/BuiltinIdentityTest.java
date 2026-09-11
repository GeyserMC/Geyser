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

package org.geysermc.geyser.network.bedrock.nethernet.signalling;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.PosixFileAttributeView;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BuiltinIdentityTest {
    @Test
    void createsAndReusesPrivateSigningIdentity(@TempDir Path directory) throws Exception {
        Path file = BuiltinIdentity.ensure(directory);
        byte[] before = Files.readAllBytes(file);
        assertEquals(file, BuiltinIdentity.ensure(directory));
        assertArrayEquals(before, Files.readAllBytes(file));
        if (Files.getFileStore(directory).supportsFileAttributeView(PosixFileAttributeView.class)) {
            assertEquals("rw-------", java.nio.file.attribute.PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
        }
    }

    @Test
    void identityNeverExpires(@TempDir Path directory) throws Exception {
        // The certificate expiry becomes the token expiry, and a replaced key makes every player confirm the server again
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(BuiltinIdentity.ensure(directory))) {
            store.load(input, new char[0]);
        }
        X509Certificate certificate = (X509Certificate) store.getCertificate("identity");
        assertEquals(Instant.parse("9999-12-31T23:59:59Z"), certificate.getNotAfter().toInstant());
    }

    @Test
    void refusesToReplaceInvalidIdentity(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("identity.p12"), "existing-invalid-identity");
        assertThrows(Exception.class, () -> BuiltinIdentity.ensure(directory));
        assertEquals("existing-invalid-identity", Files.readString(directory.resolve("identity.p12")));
    }
}
