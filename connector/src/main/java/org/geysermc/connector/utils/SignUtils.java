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

/**
 * Provides utilities for interacting with signs. Mainly, it deals with the widths of each character.
 * Since Bedrock auto-wraps signs and Java does not, we have to take this into account when translating signs.
 */
public class SignUtils {

    // TODO: If we send the Java font via resource pack, does width change?
    /**
     * The maximum character width that a sign can hold in Bedrock
     */
    public static final int BEDROCK_CHARACTER_WIDTH_MAX = 88;

    /**
     * The maximum character width that a sign can hold in Java
     */
    public static final int JAVA_CHARACTER_WIDTH_MAX = 90;

    /**
     * Gets the Minecraft width of a character
     * @param c character to determine
     * @return width of the character
     */
    public static int getCharacterWidth(char c) {
        switch (c) {
            case '!':
            case ',':
            case '.':
            case ':':
            case ';':
            case 'i':
            case '|':
            case '¡':
                return 2;

            case '\'':
            case 'l':
            case 'ì':
            case 'í':
                return 3;

            case ' ':
            case 'I':
            case '[':
            case ']':
            case 't':
            case '×':
            case 'ï':
                return 4;

            case '"':
            case '(':
            case ')':
            case '*':
            case '<':
            case '>':
            case 'f':
            case 'k':
            case '{':
            case '}':
                return 5;

            case '@':
            case '~':
            case '®':
                return 7;

            default:
                return 6;
        }
    }

}
