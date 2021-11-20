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

package org.geysermc.geyser.platform.standalone.gui;

import lombok.Getter;

import java.awt.*;
import java.util.regex.Pattern;

public enum ANSIColor {
    // Normal colors
    BLACK("(0;)?30(0;)?m", Color.getHSBColor(0.000f, 0.000f, 0.000f)),
    RED("(0;)?31(0;)?m", Color.getHSBColor(0.000f, 1.000f, 0.502f)),
    GREEN("(0;)?32(0;)?m", Color.getHSBColor(0.333f, 1.000f, 0.502f)),
    YELLOW("(0;)?33(0;)?m", Color.getHSBColor(0.167f, 1.000f, 0.502f)),
    BLUE("(0;)?34(0;)?m", Color.getHSBColor(0.667f, 1.000f, 0.502f)),
    MAGENTA("(0;)?35(0;)?m", Color.getHSBColor(0.833f, 1.000f, 0.502f)),
    CYAN("(0;)?36(0;)?m", Color.getHSBColor(0.500f, 1.000f, 0.502f)),
    WHITE("(0;)?37(0;)?m", Color.getHSBColor(0.000f, 0.000f, 0.753f)),

    // Bold colors
    B_BLACK("(1;30|30;1)m", Color.getHSBColor(0.000f, 0.000f, 0.502f)),
    B_RED("(1;31|31;1)m", Color.getHSBColor(0.000f, 1.000f, 1.000f)),
    B_GREEN("(1;32|32;1)m", Color.getHSBColor(0.333f, 1.000f, 1.000f)),
    B_YELLOW("(1;33|33;1)m", Color.getHSBColor(0.167f, 1.000f, 1.000f)),
    B_BLUE("(1;34|34;1)m", Color.getHSBColor(0.667f, 1.000f, 1.000f)),
    B_MAGENTA("(1;35|35;1)m", Color.getHSBColor(0.833f, 1.000f, 1.000f)),
    B_CYAN("(1;36|36;1)m", Color.getHSBColor(0.500f, 1.000f, 1.000f)),
    B_WHITE("(1;37|37;1)m", Color.getHSBColor(0.000f, 0.000f, 1.000f)),

    RESET("0m", Color.getHSBColor(0.000f, 0.000f, 1.000f));

    private static final ANSIColor[] VALUES = values();
    private static final String PREFIX = Pattern.quote("\u001B[");

    private final String ANSICode;

    @Getter
    private final Color color;

    ANSIColor(String ANSICode, Color color) {
        this.ANSICode = ANSICode;
        this.color = color;
    }

    public static ANSIColor fromANSI(String code) {
        for (ANSIColor value : VALUES) {
            if (code.matches(PREFIX + value.ANSICode)) {
                return value;
            }
        }

        return B_WHITE;
    }
}
