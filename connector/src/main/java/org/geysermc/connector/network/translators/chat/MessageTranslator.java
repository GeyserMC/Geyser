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

package org.geysermc.connector.network.translators.chat;

import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamColor;
import com.github.steveice10.mc.protocol.data.message.style.ChatColor;
import com.github.steveice10.mc.protocol.data.message.style.ChatFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LanguageUtils;

import java.util.*;

public class MessageTranslator {

    // These are used for handling the translations of the messages
    private static final TranslatableComponentRenderer<Locale> RENDERER = TranslatableComponentRenderer.usingTranslationSource(new MinecraftTranslationRegistry());

    // Store team colors for player names
    private static final Map<TeamColor, String> TEAM_COLORS = new HashMap<>();

    static {
        TEAM_COLORS.put(TeamColor.BLACK, getColor(ChatColor.BLACK));
        TEAM_COLORS.put(TeamColor.DARK_BLUE, getColor(ChatColor.DARK_BLUE));
        TEAM_COLORS.put(TeamColor.DARK_GREEN, getColor(ChatColor.DARK_GREEN));
        TEAM_COLORS.put(TeamColor.DARK_AQUA, getColor(ChatColor.DARK_AQUA));
        TEAM_COLORS.put(TeamColor.DARK_RED, getColor(ChatColor.DARK_RED));
        TEAM_COLORS.put(TeamColor.DARK_PURPLE, getColor(ChatColor.DARK_PURPLE));
        TEAM_COLORS.put(TeamColor.GOLD, getColor(ChatColor.GOLD));
        TEAM_COLORS.put(TeamColor.GRAY, getColor(ChatColor.GRAY));
        TEAM_COLORS.put(TeamColor.DARK_GRAY, getColor(ChatColor.DARK_GRAY));
        TEAM_COLORS.put(TeamColor.BLUE, getColor(ChatColor.BLUE));
        TEAM_COLORS.put(TeamColor.GREEN, getColor(ChatColor.GREEN));
        TEAM_COLORS.put(TeamColor.AQUA, getColor(ChatColor.AQUA));
        TEAM_COLORS.put(TeamColor.RED, getColor(ChatColor.RED));
        TEAM_COLORS.put(TeamColor.LIGHT_PURPLE, getColor(ChatColor.LIGHT_PURPLE));
        TEAM_COLORS.put(TeamColor.YELLOW, getColor(ChatColor.YELLOW));
        TEAM_COLORS.put(TeamColor.WHITE, getColor(ChatColor.WHITE));
        TEAM_COLORS.put(TeamColor.OBFUSCATED, getFormat(ChatFormat.OBFUSCATED));
        TEAM_COLORS.put(TeamColor.BOLD, getFormat(ChatFormat.BOLD));
        TEAM_COLORS.put(TeamColor.STRIKETHROUGH, getFormat(ChatFormat.STRIKETHROUGH));
        TEAM_COLORS.put(TeamColor.ITALIC, getFormat(ChatFormat.ITALIC));
    }

    /**
     * Convert a Java message to the legacy format ready for bedrock
     *
     * @param message Java message
     * @param locale Locale to use for translation strings
     * @return Parsed and formatted message for bedrock
     */
    public static String convertMessage(String message, String locale) {
        Component component = GsonComponentSerializer.gson().deserialize(message);

        // Get a Locale from the given locale string
        Locale localeCode = Locale.forLanguageTag(locale.replace('_', '-'));
        component = RENDERER.render(component, localeCode);

        String legacy = LegacyComponentSerializer.legacySection().serialize(component);

        // Strip strikethrough and underline as they are not supported on bedrock
        legacy = legacy.replaceAll("\u00a7[mn]", "");

        // Make color codes reset formatting like Java
        // See https://minecraft.gamepedia.com/Formatting_codes#Usage
        legacy = legacy.replaceAll("\u00a7([0-9a-f])", "\u00a7r\u00a7$1");
        legacy = legacy.replaceAll("\u00a7r\u00a7r", "\u00a7r");

        return legacy;
    }

    public static String convertMessage(String message) {
        return convertMessage(message, LanguageUtils.getDefaultLocale());
    }

    /**
     * Verifies the message is valid JSON in case it's plaintext. Works around GsonComponentSeraializer not using lenient mode.
     * See https://wiki.vg/Chat for messages sent in lenient mode, and for a description on leniency.
     *
     * @param message Potentially lenient JSON message
     * @param locale Locale to use for translation strings
     * @return Bedrock formatted message
     */
    public static String convertMessageLenient(String message, String locale) {
        if (isMessage(message)) {
            return convertMessage(message, locale);
        } else {
            String convertedMessage = convertMessage(convertToJavaMessage(message), locale);

            // We have to do this since Adventure strips the starting reset character
            if (message.startsWith(getColor(ChatColor.RESET)) && !convertedMessage.startsWith(getColor(ChatColor.RESET))) {
                convertedMessage = getColor(ChatColor.RESET) + convertedMessage;
            }

            return convertedMessage;
        }
    }

    public static String convertMessageLenient(String message) {
        return convertMessageLenient(message, LanguageUtils.getDefaultLocale());
    }

    /**
     * Convert a Bedrock message string back to a format Java can understand
     *
     * @param message Message to convert
     * @return The formatted JSON string
     */
    public static String convertToJavaMessage(String message) {
        Component component = LegacyComponentSerializer.legacySection().deserialize(message);
        return GsonComponentSerializer.gson().serialize(component);
    }

    /**
     * Checks if the given text string is a JSON message
     *
     * @param text String to test
     * @return True if its a valid message JSON string, false if not
     */
    public static boolean isMessage(String text) {
        if (text.trim().isEmpty()) {
            return false;
        }

        try {
            GsonComponentSerializer.gson().deserialize(text);
        } catch (Exception ex) {
            return false;
        }

        return true;
    }

    /**
     * Convert a {@link ChatColor} into a string for inserting into messages
     *
     * @param color {@link ChatColor} to convert
     * @return The converted color string
     */
    private static String getColor(String color) {
        String base = "\u00a7";
        switch (color) {
            case ChatColor.BLACK:
                base += "0";
                break;
            case ChatColor.DARK_BLUE:
                base += "1";
                break;
            case ChatColor.DARK_GREEN:
                base += "2";
                break;
            case ChatColor.DARK_AQUA:
                base += "3";
                break;
            case ChatColor.DARK_RED:
                base += "4";
                break;
            case ChatColor.DARK_PURPLE:
                base += "5";
                break;
            case ChatColor.GOLD:
                base += "6";
                break;
            case ChatColor.GRAY:
                base += "7";
                break;
            case ChatColor.DARK_GRAY:
                base += "8";
                break;
            case ChatColor.BLUE:
                base += "9";
                break;
            case ChatColor.GREEN:
                base += "a";
                break;
            case ChatColor.AQUA:
                base += "b";
                break;
            case ChatColor.RED:
                base += "c";
                break;
            case ChatColor.LIGHT_PURPLE:
                base += "d";
                break;
            case ChatColor.YELLOW:
                base += "e";
                break;
            case ChatColor.WHITE:
                base += "f";
                break;
            case ChatColor.RESET:
                base += "r";
                break;
            default:
                return "";
        }

        return base;
    }

    /**
     * Convert a {@link ChatFormat} into a string for inserting into messages
     *
     * @param format {@link ChatFormat} to convert
     * @return The converted chat formatting string
     */
    private static String getFormat(ChatFormat format) {
        StringBuilder str = new StringBuilder();
        String base = "\u00a7";
        switch (format) {
            case OBFUSCATED:
                base += "k";
                break;
            case BOLD:
                base += "l";
                break;
            case STRIKETHROUGH:
                base += "m";
                break;
            case UNDERLINED:
                base += "n";
                break;
            case ITALIC:
                base += "o";
                break;
            default:
                return "";
        }

        str.append(base);

        return str.toString();
    }

    /**
     * Convert a team color to a chat color
     *
     * @param teamColor
     * @return The chat color character
     */
    public static String toChatColor(TeamColor teamColor) {
        return TEAM_COLORS.getOrDefault(teamColor, "");
    }

    /**
     * Checks if the given message is over 256 characters (Java edition server chat limit) and sends a message to the user if it is
     *
     * @param message Message to check
     * @param session {@link GeyserSession} for the user
     * @return True if the message is too long, false if not
     */
    public static boolean isTooLong(String message, GeyserSession session) {
        if (message.length() > 256) {
            session.sendMessage(LanguageUtils.getPlayerLocaleString("geyser.chat.too_long", session.getLocale(), message.length()));
            return true;
        }

        return false;
    }
}
