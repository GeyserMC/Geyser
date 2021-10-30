/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.dump;

import org.geysermc.connector.GeyserConnector;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Properties;

/**
 * Utility class to get information from Floodgate through reflection, if Floodgate is installed alongside Geyser.
 */
public class FloodgateInfo {

    private static Class<?> infoHolderClazz;
    private static Field gitProperties;
    private static Field config;

    static {
        try {
            infoHolderClazz = Class.forName("org.geysermc.floodgate.util.GeyserDumpInfoHolder");
            try {
                gitProperties = infoHolderClazz.getDeclaredField("gitProperties");
                config = infoHolderClazz.getDeclaredField("config");
            } catch (NoSuchFieldException e) {
                GeyserConnector.getInstance().getLogger().error("Failed to get Fields in Floodgate for information for Geyser dump");
                e.printStackTrace();
            }
        } catch (ClassNotFoundException ignored) { }
        // If the class is not found, then Geyser isn't installed, which is fine
    }

    /**
     * @return Floodgate's git Properties if Floodgate classes/fields are available and were successfully accessed, otherwise null.
     */
    @Nullable
    public static Properties getGitProperties() {
        if (gitProperties != null) {
            try {
                return (Properties) gitProperties.get(infoHolderClazz);
            } catch (IllegalAccessException e) {
                GeyserConnector.getInstance().getLogger().error("Failed to get Floodgate git.properties for Geyser dump");
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * @return Floodgate's config if Floodgate classes/fields are available and were successfully accessed, otherwise null.
     */
    @Nullable
    public static Object getConfig() {
        if (config != null) {
            try {
                return config.get(infoHolderClazz);
            } catch (IllegalAccessException e) {
                GeyserConnector.getInstance().getLogger().error("Failed to get Floodgate config for Geyser dump");
                e.printStackTrace();
            }
        }

        return null;
    }
}
