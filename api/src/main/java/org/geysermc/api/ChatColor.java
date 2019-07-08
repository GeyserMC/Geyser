/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 3, 29 June 2007
 *
 * Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 *
 * You can view the LICENCE file for details.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.api;

public class ChatColor {

    public static final char ESCAPE = '§';
    public static final String BLACK = ESCAPE + "0";
    public static final String DARK_BLUE = ESCAPE + "1";
    public static final String DARK_GREEN = ESCAPE + "2";
    public static final String DARK_AQUA = ESCAPE + "3";
    public static final String DARK_RED = ESCAPE + "4";
    public static final String DARK_PURPLE = ESCAPE + "5";
    public static final String GOLD = ESCAPE + "6";
    public static final String GRAY = ESCAPE + "7";
    public static final String DARK_GRAY = ESCAPE + "8";
    public static final String BLUE = ESCAPE + "9";
    public static final String GREEN = ESCAPE + "a";
    public static final String AQUA = ESCAPE + "b";
    public static final String RED = ESCAPE + "c";
    public static final String LIGHT_PURPLE = ESCAPE + "d";
    public static final String YELLOW = ESCAPE + "e";
    public static final String WHITE = ESCAPE + "f";
    public static final String OBFUSCATED = ESCAPE + "k";
    public static final String BOLD = ESCAPE + "l";
    public static final String STRIKETHROUGH = ESCAPE + "m";
    public static final String UNDERLINE = ESCAPE + "n";
    public static final String ITALIC = ESCAPE + "o";
    public static final String RESET = ESCAPE + "r";

    public static String toANSI(String string) {
        string = string.replace(BOLD, "");
        string = string.replace(OBFUSCATED, (char) 0x1b + "[6m");
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

    public String translateAlternateColorCodes(char color, String message) {
        return message.replace(color, ESCAPE);
    }

    public static String stripColors(String message) {
        return message = message.replaceAll("(&([a-fk-or0-9]))","").replaceAll("(§([a-fk-or0-9]))","").replaceAll("s/\\x1b\\[[0-9;]*[a-zA-Z]//g","");
    }
}
