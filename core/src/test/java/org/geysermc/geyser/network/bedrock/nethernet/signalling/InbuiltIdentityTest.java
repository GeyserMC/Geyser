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
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class InbuiltIdentityTest {
    @Test void createsAndReusesPrivateSigningIdentity(@TempDir Path directory) throws Exception {
        InbuiltIdentity.ensure(directory);
        byte[] before = Files.readAllBytes(directory.resolve("identity.p12"));
        InbuiltIdentity.ensure(directory);
        assertArrayEquals(before, Files.readAllBytes(directory.resolve("identity.p12")));
        assertEquals("rw-------", java.nio.file.attribute.PosixFilePermissions.toString(Files.getPosixFilePermissions(directory.resolve("identity.p12"))));
    }
    @Test void refusesToReplaceInvalidIdentity(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("identity.p12"), "existing-invalid-identity");
        assertThrows(Exception.class, () -> InbuiltIdentity.ensure(directory));
        assertEquals("existing-invalid-identity", Files.readString(directory.resolve("identity.p12")));
    }
}
