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

package org.geysermc.geyser.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SystemPropertiesTest {

    @Test
    public void environmentNames() {
        assertEquals("GEYSER_UDP_PORT", SystemProperties.environmentName("geyserUdpPort"));
        assertEquals("GEYSER_ADVERTISE_ADDRESSES", SystemProperties.environmentName("geyserAdvertiseAddresses"));
        assertEquals("PLUGIN_UDP_ADDRESS", SystemProperties.environmentName("pluginUdpAddress"));
        assertEquals("GEYSER_SHOW_SCOREBOARD_LOGS", SystemProperties.environmentName("Geyser.ShowScoreboardLogs"));
        assertEquals("GEYSER_NO_PLAYER_LIST_PS", SystemProperties.environmentName("Geyser.NoPlayerListPS"));
        assertEquals("GEYSER_VIAPROXY_DISABLE_IP_PASSTHROUGH", SystemProperties.environmentName("geyser.viaproxy.disableIpPassthrough"));
    }

    @Test
    public void systemPropertyWinsOverEnvironment() {
        Map<String, String> properties = Map.of("geyserUdpPort", "19132");
        Map<String, String> environment = Map.of("GEYSER_UDP_PORT", "41234", "GEYSER_TRANSPORT", "nethernet");
        assertEquals("19132", SystemProperties.get("geyserUdpPort", properties::get, environment::get));
        assertEquals("nethernet", SystemProperties.get("geyserTransport", properties::get, environment::get));
        assertNull(SystemProperties.get("geyserSignalingMode", properties::get, environment::get));
    }
}
