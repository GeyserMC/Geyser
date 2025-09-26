/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.platform.mod;

import net.minecraft.DetectedVersion;
import net.minecraft.WorldVersion;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class ModConstants {
    public static final String MODERN_VERSION = "1.21.7";
    public static final String CURRENT_VERSION;
    public static final int CURRENT_PROTOCOL;

    public static boolean isModernVersion() {
        return MODERN_VERSION.equals(CURRENT_VERSION);
    }

    private ModConstants() {}

    static {
        WorldVersion worldVersion = DetectedVersion.tryDetectVersion();
        List<String> potentialNameMethods = List.of("name", "getName", "comp_4024", "method_48019");
        List<String> potentialProtocolMethods = List.of("protocolVersion", "getProtocolVersion", "comp_4027", "method_48020");

        Method nameMethod = null;
        Method protocolMethod = null;

        for (String methodName : potentialNameMethods) {
            try {
                nameMethod = worldVersion.getClass().getMethod(methodName);
                break;
            } catch (NoSuchMethodException ignored) {}
        }

        for (String methodName : potentialProtocolMethods) {
            try {
                protocolMethod = worldVersion.getClass().getMethod(methodName);
                break;
            } catch (NoSuchMethodException ignored) {}
        }

        if (nameMethod == null || protocolMethod == null) throw new IllegalStateException("Unable to determine suitable method for getting Minecraft version.");

        try {
            CURRENT_VERSION = (String) nameMethod.invoke(worldVersion);
            CURRENT_PROTOCOL = (int) protocolMethod.invoke(worldVersion);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
