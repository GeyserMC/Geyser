/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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

import org.cloudburstmc.protocol.bedrock.codec.v1002.Bedrock_v1002;
import org.geysermc.geyser.api.util.MinecraftVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameProtocolTest {

    @Test
    void exposesEducationOnlyProtocolWithoutMakingItTheDefault() {
        int educationProtocol = Bedrock_v1002.EDUCATION_CODEC.getProtocolVersion();

        assertTrue(GameProtocol.SUPPORTED_BEDROCK_PROTOCOLS.contains(educationProtocol));
        assertTrue(GameProtocol.SUPPORTED_BEDROCK_VERSIONS.stream().anyMatch(version ->
                version.protocolVersion() == educationProtocol && version.versionString().equals("26.30")));
        assertNotEquals(educationProtocol, GameProtocol.DEFAULT_BEDROCK_PROTOCOL);

        MinecraftVersion latestVersion = GameProtocol.SUPPORTED_BEDROCK_VERSIONS.getLast();
        assertEquals(latestVersion.protocolVersion(), GameProtocol.DEFAULT_BEDROCK_PROTOCOL);
        assertEquals(latestVersion.versionString(), GameProtocol.DEFAULT_BEDROCK_VERSION);
    }
}
