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

import org.cloudburstmc.protocol.bedrock.BedrockPong;
import org.cloudburstmc.netty.signalling.ServerStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeyserStatusCollectorTest {
    @Test void usesBedrockQueryAndCurrentSessionsWithExplicitUndiscoverableFields() {
        BedrockPong pong = new BedrockPong().motd("Geyser").protocolVersion(1234).version("preview-fixture").playerCount(99).maximumPlayerCount(50);
        Assertions.assertEquals(new ServerStatus("Geyser", 1234, "preview-fixture", "world", 2, 50, 1), GeyserStatusCollector.snapshot(pong, 2, "world", 1));
        pong.motd("Reloaded").maximumPlayerCount(70);
        assertEquals(new ServerStatus("Reloaded", 1234, "preview-fixture", "new world", 3, 70, 2), GeyserStatusCollector.snapshot(pong, 3, "new world", 2));
        assertEquals(1, GeyserStatusCollector.snapshot(pong, 1, "", 0).players());
    }
    @Test void invalidOrUnavailableQueryCannotFabricateAStatus() {
        assertThrows(NullPointerException.class, () -> GeyserStatusCollector.snapshot(null, 1, "", 0));
        assertThrows(IllegalArgumentException.class, () -> GeyserStatusCollector.snapshot(new BedrockPong().motd("x").protocolVersion(0).version("unknown"), 0, "", 0));
    }
}
