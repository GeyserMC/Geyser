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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.function.Function;

/**
 * Geyser's system properties, which can also be set as environment variables.
 * <p>
 * Every property Geyser reads (e.g. {@code -DgeyserUdpPort=19132} or {@code -DGeyser.ShowScoreboardLogs=false})
 * can be given as an environment variable instead, named after the property in upper snake case:
 * {@code GEYSER_UDP_PORT=19132}, {@code GEYSER_SHOW_SCOREBOARD_LOGS=false}. A system property that is set
 * takes priority over the environment variable. This helps on hosts and containers where the start
 * command is fixed but the environment is not.
 */
public final class SystemProperties {

    private SystemProperties() {
    }

    /**
     * The value of a property: the system property, else its environment variable, else null.
     */
    public static @Nullable String get(String name) {
        return get(name, System::getProperty, System::getenv);
    }

    /**
     * The value of a property, or {@code fallback} when neither the system property nor its environment variable is set.
     */
    public static String get(String name, String fallback) {
        String value = get(name);
        return value == null ? fallback : value;
    }

    /**
     * True when the property is set to "true", ignoring case, like {@link Boolean#getBoolean(String)}.
     */
    public static boolean getBoolean(String name) {
        return Boolean.parseBoolean(get(name));
    }

    /**
     * The property as an integer, or {@code fallback} when it is unset or not a number, like {@link Integer#getInteger(String, int)}.
     */
    public static int getInteger(String name, int fallback) {
        String value = get(name);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.decode(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static @Nullable String get(String name, Function<String, String> properties, Function<String, String> environment) {
        String value = properties.apply(name);
        if (value == null) {
            value = environment.apply(environmentName(name));
        }
        return value;
    }

    /**
     * The environment variable a property can also be set with: {@code geyserUdpPort} is {@code GEYSER_UDP_PORT},
     * {@code Geyser.ShowScoreboardLogs} is {@code GEYSER_SHOW_SCOREBOARD_LOGS}.
     */
    public static String environmentName(String property) {
        StringBuilder name = new StringBuilder(property.length() + 8);
        char previous = 0;
        for (int i = 0; i < property.length(); i++) {
            char c = property.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                if (name.length() > 0 && name.charAt(name.length() - 1) != '_') {
                    name.append('_');
                }
            } else {
                if (Character.isUpperCase(c) && (Character.isLowerCase(previous) || Character.isDigit(previous))) {
                    name.append('_');
                }
                name.append(c);
            }
            previous = c;
        }
        return name.toString().toUpperCase(Locale.ROOT);
    }
}
