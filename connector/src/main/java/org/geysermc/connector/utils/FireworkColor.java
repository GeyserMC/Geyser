/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.utils;

import lombok.Getter;

public enum FireworkColor {
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
    WHITE((byte) 15, 15790320);

    private static final FireworkColor[] VALUES = values();

    @Getter
    private byte bedrockID;
    @Getter
    private int javaID;

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

        return null;
    }

    public static FireworkColor fromBedrockID(int id) {
        for (FireworkColor color : VALUES) {
            if (color.bedrockID == id) {
                return color;
            }
        }

        return null;
    }
}
