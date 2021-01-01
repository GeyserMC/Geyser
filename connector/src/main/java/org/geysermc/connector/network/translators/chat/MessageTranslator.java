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

package org.geysermc.connector.network.translators.chat;

import com.github.steveice10.mc.protocol.data.DefaultComponentSerializer;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.gson.legacyimpl.NBTLegacyHoverEventSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.LanguageUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MessageTranslator {

    // These are used for handling the translations of the messages
    private static final TranslatableComponentRenderer<Locale> RENDERER = TranslatableComponentRenderer.usingTranslationSource(new MinecraftTranslationRegistry());

    // Construct our own {@link GsonComponentSerializer} since we need to change a setting
    private static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.builder()
            // Specify that we may be expecting legacy hover events
            .legacyHoverEventSerializer(NBTLegacyHoverEventSerializer.get())
            .build();

    // Store team colors for player names
    private static final Map<TeamColor, TextDecoration> TEAM_FORMATS = new HashMap<>();

    // Legacy formatting character
    private static final String BASE = "\u00a7";

    // Reset character
    private static final String RESET = BASE + "r";

    static {
        TEAM_FORMATS.put(TeamColor.OBFUSCATED, TextDecoration.OBFUSCATED);
        TEAM_FORMATS.put(TeamColor.BOLD, TextDecoration.BOLD);
        TEAM_FORMATS.put(TeamColor.STRIKETHROUGH, TextDecoration.STRIKETHROUGH);
        TEAM_FORMATS.put(TeamColor.ITALIC, TextDecoration.ITALIC);

        // Tell MCProtocolLib to use our serializer
        DefaultComponentSerializer.set(GSON_SERIALIZER);
    }

    /**
     * Convert a Java message to the legacy format ready for bedrock
     *
     * @param message Java message
     * @param locale Locale to use for translation strings
     * @return Parsed and formatted message for bedrock
     */
    public static String convertMessage(Component message, String locale) {
        try {
            // Get a Locale from the given locale string
            Locale localeCode = Locale.forLanguageTag(locale.replace('_', '-'));
            message = RENDERER.render(message, localeCode);

            String legacy = LegacyComponentSerializer.legacySection().serialize(message);

            // Strip strikethrough and underline as they are not supported on bedrock
            legacy = legacy.replaceAll("\u00a7[mn]", "");

            // Make color codes reset formatting like Java
            // See https://minecraft.gamepedia.com/Formatting_codes#Usage
            legacy = legacy.replaceAll("\u00a7([0-9a-f])", "\u00a7r\u00a7$1");
            legacy = legacy.replaceAll("\u00a7r\u00a7r", "\u00a7r");

            return legacy;
        } catch (Exception e) {
            GeyserConnector.getInstance().getLogger().debug(GSON_SERIALIZER.serialize(message));
            GeyserConnector.getInstance().getLogger().error("Failed to parse message", e);

            return "";
        }
    }

    public static String convertMessage(String message, String locale) {
        return convertMessage(GSON_SERIALIZER.deserialize(message), locale);
    }

    public static String convertMessage(String message) {
        return convertMessage(message, LanguageUtils.getDefaultLocale());
    }

    public static String convertMessage(Component message) {
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
        if (message.trim().isEmpty()) {
            return message;
        }

        try {
            return convertMessage(message, locale);
        } catch (Exception ignored) {
            String convertedMessage = convertMessage(convertToJavaMessage(message), locale);

            // We have to do this since Adventure strips the starting reset character
            if (message.startsWith(RESET) && !convertedMessage.startsWith(RESET)) {
                convertedMessage = RESET + convertedMessage;
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
        return GSON_SERIALIZER.serialize(component);
    }

    /**
     * Convert a {@link NamedTextColor} into a string for inserting into messages
     *
     * @param color {@link NamedTextColor} to convert
     * @return The converted color string
     */
    private static String getColor(NamedTextColor color) {
        StringBuilder str = new StringBuilder(BASE);
        if (color.equals(NamedTextColor.BLACK)) {
            str.append("0");
        } else if (color.equals(NamedTextColor.DARK_BLUE)) {
            str.append("1");
        } else if (color.equals(NamedTextColor.DARK_GREEN)) {
            str.append("2");
        } else if (color.equals(NamedTextColor.DARK_AQUA)) {
            str.append("3");
        } else if (color.equals(NamedTextColor.DARK_RED)) {
            str.append("4");
        } else if (color.equals(NamedTextColor.DARK_PURPLE)) {
            str.append("5");
        } else if (color.equals(NamedTextColor.GOLD)) {
            str.append("6");
        } else if (color.equals(NamedTextColor.GRAY)) {
            str.append("7");
        } else if (color.equals(NamedTextColor.DARK_GRAY)) {
            str.append("8");
        } else if (color.equals(NamedTextColor.BLUE)) {
            str.append("9");
        } else if (color.equals(NamedTextColor.GREEN)) {
            str.append("a");
        } else if (color.equals(NamedTextColor.AQUA)) {
            str.append("b");
        } else if (color.equals(NamedTextColor.RED)) {
            str.append("c");
        } else if (color.equals(NamedTextColor.LIGHT_PURPLE)) {
            str.append("d");
        } else if (color.equals(NamedTextColor.YELLOW)) {
            str.append("e");
        } else if (color.equals(NamedTextColor.WHITE)) {
            str.append("f");
        } else {
            return "";
        }

        return str.toString();
    }

    /**
     * Convert a {@link TextDecoration} into a string for inserting into messages
     *
     * @param format {@link TextDecoration} to convert
     * @return The converted chat formatting string
     */
    private static String getFormat(TextDecoration format) {
        StringBuilder str = new StringBuilder(BASE);
        switch (format) {
            case OBFUSCATED:
                str.append("k");
                break;
            case BOLD:
                str.append("l");
                break;
            case STRIKETHROUGH:
                str.append("m");
                break;
            case UNDERLINED:
                str.append("n");
                break;
            case ITALIC:
                str.append("o");
                break;
            default:
                return "";
        }

        return str.toString();
    }

    /**
     * Convert a team color to a chat color
     *
     * @param teamColor Color or format to convert
     * @return The chat color character
     */
    public static String toChatColor(TeamColor teamColor) {
        if (teamColor.equals(TeamColor.NONE)) {
            return "";
        }

        NamedTextColor textColor = NamedTextColor.NAMES.value(teamColor.name().toLowerCase());
        if (textColor != null) {
            return getColor(textColor);
        }

        return getFormat(TEAM_FORMATS.get(teamColor));
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
