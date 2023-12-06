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

/**
 * Provides utilities for interacting with signs. Mainly, it deals with the widths of each character.
 * Since Bedrock auto-wraps signs and Java does not, we have to take this into account when translating signs.
 */
public class SignUtils {

    // TODO: If we send the Java font via resource pack, does width change?
    /**
     * The maximum character width that a non-hanging sign can hold in both Java and Bedrock
     */
    public static final int SIGN_WIDTH_MAX = 90;

    /**
     * The maximum character width that a hanging sign can hold in both Java and Bedrock. Hanging signs are narrower.
     */
    public static final int HANGING_SIGN_WIDTH_MAX = 60;


    /**
     * Gets the Minecraft width of a character
     * @param c character to determine
     * @return width of the character
     */
    public static int getCharacterWidth(char c) {
        return switch (c) {
            case '!', ',', '.', ':', ';', 'i', '|', '¡' -> 2;
            case '\'', 'l', 'ì', 'í' -> 3;
            case ' ', 'I', '[', ']', 't', '×', 'ï' -> 4;
            case '"', '(', ')', '*', '<', '>', 'f', 'k', '{', '}' -> 5;
            case '@', '~', '®' -> 7;
            default -> 6;
        };
    }

    public static int getSignWidthMax(boolean hanging) {
        if (hanging) {
            return HANGING_SIGN_WIDTH_MAX;
        }
        return SIGN_WIDTH_MAX;
    }
}
