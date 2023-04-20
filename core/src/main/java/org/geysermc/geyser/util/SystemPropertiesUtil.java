/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

public class SystemPropertiesUtil {

    /**
     * Get the default UDP address for Geyser.
     * This will first check the geyserUdpAddress system property, then the pluginUdpAddress system property.
     * If neither are set, it will return "0.0.0.0".
     * @return The default UDP address for Geyser.
     */
    public static String getDefaultUDPAddress() {
        String address = System.getProperty("geyserUdpAddress");

        if (address == null) {
            address = System.getProperty("pluginUdpAddress");
        }

        return address == null ? "0.0.0.0" : address;
    }

    /**
     * Get the default UDP port for Geyser.
     * This will first check the geyserUdpPort system property, then the pluginUdpPort system property.
     * If neither are set, it will return 19132.
     * @return The default UDP port for Geyser.
     */
    public static int getDefaultUdpPort() {
        Integer port = getInteger("geyserUdpPort");

        if (port == null) {
            port = getInteger("pluginUdpPort");
        }

        return port == null ? 19132 : port;
    }

    /**
     * Get an integer from a system property.
     * @param key The system property key.
     * @return The integer value of the system property, or null if it is not set or invalid.
     */
    public static Integer getInteger(String key)
    {
        String property = System.getProperty(key);
        if (property != null) {
            try {
                return Integer.parseInt(property);
            }
            catch (NumberFormatException e) {
                System.err.printf("Invalid value for system property %s. Must be an integer.%n", key);
            }
        }
        return null;
    }
}
