/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.common;

import java.util.regex.Pattern;

public class ChatColor {

    public static final char COLOR_CODE = '§';
    public static final String ALL_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

    public static final String BLACK = COLOR_CODE + "0";
    public static final String DARK_BLUE = COLOR_CODE + "1";
    public static final String DARK_GREEN = COLOR_CODE + "2";
    public static final String DARK_AQUA = COLOR_CODE + "3";
    public static final String DARK_RED = COLOR_CODE + "4";
    public static final String DARK_PURPLE = COLOR_CODE + "5";
    public static final String GOLD = COLOR_CODE + "6";
    public static final String GRAY = COLOR_CODE + "7";
    public static final String DARK_GRAY = COLOR_CODE + "8";
    public static final String BLUE = COLOR_CODE + "9";
    public static final String GREEN = COLOR_CODE + "a";
    public static final String AQUA = COLOR_CODE + "b";
    public static final String RED = COLOR_CODE + "c";
    public static final String LIGHT_PURPLE = COLOR_CODE + "d";
    public static final String YELLOW = COLOR_CODE + "e";
    public static final String WHITE = COLOR_CODE + "f";
    public static final String OBFUSCATED = COLOR_CODE + "k";
    public static final String BOLD = COLOR_CODE + "l";
    public static final String STRIKETHROUGH = COLOR_CODE + "m";
    public static final String UNDERLINE = COLOR_CODE + "n";
    public static final String ITALIC = COLOR_CODE + "o";
    public static final String RESET = COLOR_CODE + "r";

    public static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + COLOR_CODE + "[0-9A-FK-ORX]");

    /**
     * Convert chat colour codes to terminal colours
     *
     * @param string The text to replace colours for
     * @return A string ready for terminal printing
     */
    public static String toANSI(String string) {
        string = string.replace(BOLD, (char) 0x1b + "[1m");
        string = string.replace(OBFUSCATED, (char) 0x1b + "[5m");
        string = string.replace(ITALIC, (char) 0x1b + "[3m");
        string = string.replace(UNDERLINE, (char) 0x1b + "[4m");
        string = string.replace(STRIKETHROUGH, (char) 0x1b + "[9m");
        string = string.replace(RESET, (char) 0x1b + "[0m");
        string = string.replace(BLACK, (char) 0x1b + "[0;30m");
        string = string.replace(DARK_BLUE, (char) 0x1b + "[0;34m");
        string = string.replace(DARK_GREEN, (char) 0x1b + "[0;32m");
        string = string.replace(DARK_AQUA, (char) 0x1b + "[0;36m");
        string = string.replace(DARK_RED, (char) 0x1b + "[0;31m");
        string = string.replace(DARK_PURPLE, (char) 0x1b + "[0;35m");
        string = string.replace(GOLD, (char) 0x1b + "[0;33m");
        string = string.replace(GRAY, (char) 0x1b + "[0;37m");
        string = string.replace(DARK_GRAY, (char) 0x1b + "[30;1m");
        string = string.replace(BLUE, (char) 0x1b + "[34;1m");
        string = string.replace(GREEN, (char) 0x1b + "[32;1m");
        string = string.replace(AQUA, (char) 0x1b + "[36;1m");
        string = string.replace(RED, (char) 0x1b + "[31;1m");
        string = string.replace(LIGHT_PURPLE, (char) 0x1b + "[35;1m");
        string = string.replace(YELLOW, (char) 0x1b + "[33;1m");
        string = string.replace(WHITE, (char) 0x1b + "[37;1m");
        return string;
    }

    public String translateAlternateColorCodes(char code, String message) {
        char[] c = message.toCharArray();
        for (int i = 0; i < c.length - 1; i++) {
            if (c[i] == code && ALL_CODES.indexOf(c[i + 1]) > -1) {
                c[i] = COLOR_CODE;
            }
        }
        return new String(c);
    }

    /**
     * Remove all colour formatting tags from a message
     *
     * @param message Message to remove colour tags from
     * @return The sanitised message
     */
    public static String stripColors(String message) {
        if (message == null) return null;
        return STRIP_COLOR_PATTERN.matcher(message).replaceAll("");
    }
}
