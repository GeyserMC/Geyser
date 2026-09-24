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

package org.geysermc.geyser.network.bedrock.nethernet.signaling.provider;

import org.cloudburstmc.netty.signaling.ProviderDiagnostic;
import org.geysermc.geyser.GeyserLogger;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class GeyserProviderLoggerTest {
    @Test
    void preservesFailureRetryAndRecoveryLevels() {
        var logger = mock(GeyserLogger.class);
        var sink = new GeyserProviderLogger(logger);
        sink.accept(new ProviderDiagnostic(ProviderDiagnostic.Level.WARN, "Connection failed."));
        sink.accept(new ProviderDiagnostic(ProviderDiagnostic.Level.DEBUG, "Connection failed."));
        sink.accept(new ProviderDiagnostic(ProviderDiagnostic.Level.INFO, "Reconnected."));
        var order = inOrder(logger);
        order.verify(logger).warning("NXS: Connection failed.");
        order.verify(logger).debug("NXS: Connection failed.");
        order.verify(logger).info("NXS: Reconnected.");
        order.verifyNoMoreInteractions();
    }
}
