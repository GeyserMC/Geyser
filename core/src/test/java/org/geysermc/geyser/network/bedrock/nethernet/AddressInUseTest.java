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

package org.geysermc.geyser.network.bedrock.nethernet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How {@link NetherNetServer#isAddressInUse(Throwable)} tells a port held by something else from other failures.
 */
class AddressInUseTest {

    @Test
    void detectsARealBindConflict() throws IOException {
        try (ServerSocket holder = new ServerSocket(0)) {
            BindException failure = assertThrows(BindException.class, () -> {
                try (ServerSocket second = new ServerSocket(holder.getLocalPort())) {
                    // Only reached if the port could be bound twice
                }
            });
            assertTrue(NetherNetServer.isAddressInUse(failure));
        }
    }

    @Test
    void detectsTheNativeTransportMessageInACause() {
        // Netty's epoll/io_uring transports throw a plain IOException rather than a BindException
        IOException nativeFailure = new IOException("bind(..) failed: Address already in use");
        assertTrue(NetherNetServer.isAddressInUse(new IllegalStateException("could not start", nativeFailure)));
    }

    @Test
    void ignoresOtherFailures() {
        assertFalse(NetherNetServer.isAddressInUse(new IOException("Permission denied")));
        assertFalse(NetherNetServer.isAddressInUse(new UnsatisfiedLinkError("no libdatachannel")));
    }
}
