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

package org.geysermc.connector.utils;

import lombok.Getter;

public enum FireworkColor {
    // Vanilla colors
    BLACK((byte) 0, 1973019),
    RED((byte) 1, 11743532),
    GREEN((byte) 2, 3887386),
    BROWN((byte) 3, 5320730),
    BLUE((byte) 4, 2437522),
    PURPLE((byte) 5, 8073150),
    CYAN((byte) 6, 2651799),
    LIGHT_GRAY((byte) 7, 11250603),
    GRAY((byte) 8, 4408131),
    PINK((byte) 9, 14188952),
    LIME((byte) 10, 4312372),
    YELLOW((byte) 11, 14602026),
    LIGHT_BLUE((byte) 12, 6719955),
    MAGENTA((byte) 13, 12801229),
    ORANGE((byte) 14, 15435844),
    WHITE((byte) 15, 15790320),

    // Bukkit colors
    // https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Color.html
    BUKKIT_WHITE((byte) 15, 0xFFFFFF),
    BUKKIT_SILVER((byte) 7, 0xC0C0C0),
    BUKKIT_GRAY((byte) 8, 0x808080),
    BUKKIT_BLACK((byte) 0, 0x000000),
    BUKKIT_RED((byte) 1, 0xFF0000),
    BUKKIT_MAROON((byte) 1, 0x800000), // No perfect map but this is as close as it can be
    BUKKIT_YELLOW((byte) 11, 0xFFFF00),
    BUKKIT_OLIVE((byte) 2, 0x808000), // No perfect map but this is as close as it can be
    BUKKIT_LIME((byte) 10, 0x00FF00),
    BUKKIT_GREEN((byte) 2, 0x008000),
    BUKKIT_AQUA((byte) 12, 0x00FFFF),
    BUKKIT_TEAL((byte) 6, 0x008080),
    BUKKIT_BLUE((byte) 4, 0x0000FF),
    BUKKIT_NAVY((byte) 4, 0x000080), // No perfect map but this is as close as it can be
    BUKKIT_FUCHSIA((byte) 9, 0xFF00FF), // No perfect map but this is as close as it can be
    BUKKIT_PURPLE((byte) 5, 0x800080),
    BUKKIT_ORANGE((byte) 14, 0xFFA500);

    private static final FireworkColor[] VALUES = values();

    @Getter
    private final byte bedrockID;
    @Getter
    private final int javaID;

    FireworkColor(byte bedrockID, int javaID) {
        this.bedrockID = bedrockID;
        this.javaID = javaID;
    }

    public static FireworkColor fromJavaID(int id) {
        for (FireworkColor color : VALUES) {
            if (color.javaID == id) {
                return color;
            }
        }

        return WHITE;
    }

    public static FireworkColor fromBedrockID(int id) {
        for (FireworkColor color : VALUES) {
            if (color.bedrockID == id) {
                return color;
            }
        }

        return WHITE;
    }
}
