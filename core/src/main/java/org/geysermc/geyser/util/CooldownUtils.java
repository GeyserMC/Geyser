/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

import lombok.Getter;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;

/**
 * Manages the sending of a cooldown indicator to the Bedrock player as there is no cooldown indicator in Bedrock.
 * Much of the work here is from the wonderful folks from <a href="https://github.com/ViaVersion/ViaRewind">ViaRewind</a>
 */
public class CooldownUtils {
    public static String getTitle(double cooldown) {
        int darkGrey = (int) Math.floor(10d * cooldown);
        int grey = 10 - darkGrey;
        StringBuilder builder = new StringBuilder(ChatColor.DARK_GRAY);
        while (darkGrey > 0) {
            builder.append("˙");
            darkGrey--;
        }
        builder.append(ChatColor.GRAY);
        while (grey > 0) {
            builder.append("˙");
            grey--;
        }
        return builder.toString();
    }

    @Getter
    public enum CooldownType {
        TITLE,
        ACTIONBAR,
        DISABLED;

        public static final CooldownType[] VALUES = values();

        /**
         * Convert the CooldownType string (from config) to the enum, DISABLED on fail
         *
         * @param name CooldownType string
         *
         * @return The converted CooldownType
         */
        public static CooldownType getByName(String name) {
            for (CooldownType type : VALUES) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return DISABLED;
        }
    }
}
