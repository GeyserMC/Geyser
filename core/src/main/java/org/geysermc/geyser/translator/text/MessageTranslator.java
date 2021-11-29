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

package org.geysermc.geyser.translator.text;

import com.github.steveice10.mc.protocol.data.DefaultComponentSerializer;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.text.GsonComponentSerializerWrapper;
import org.geysermc.geyser.text.MinecraftTranslationRegistry;

import java.util.EnumMap;
import java.util.Map;

public class MessageTranslator {
    // These are used for handling the translations of the messages
    // Custom instead of TranslatableComponentRenderer#usingTranslationSource so we don't need to worry about finding a Locale class
    private static final TranslatableComponentRenderer<String> RENDERER = new MinecraftTranslationRegistry();

    // Possible TODO: replace the legacy hover event serializer with an empty one since we have no use for hover events
    private static final GsonComponentSerializer GSON_SERIALIZER;

    // Store team colors for player names
    private static final Map<TeamColor, String> TEAM_COLORS = new EnumMap<>(TeamColor.class);

    // Legacy formatting character
    private static final String BASE = "\u00a7";

    // Reset character
    private static final String RESET = BASE + "r";

    static {
        TEAM_COLORS.put(TeamColor.NONE, "");

        TEAM_COLORS.put(TeamColor.BLACK, BASE + "0");
        TEAM_COLORS.put(TeamColor.DARK_BLUE, BASE + "1");
        TEAM_COLORS.put(TeamColor.DARK_GREEN, BASE + "2");
        TEAM_COLORS.put(TeamColor.DARK_AQUA, BASE + "3");
        TEAM_COLORS.put(TeamColor.DARK_RED, BASE + "4");
        TEAM_COLORS.put(TeamColor.DARK_PURPLE, BASE + "5");
        TEAM_COLORS.put(TeamColor.GOLD, BASE + "6");
        TEAM_COLORS.put(TeamColor.GRAY, BASE + "7");
        TEAM_COLORS.put(TeamColor.DARK_GRAY, BASE + "8");
        TEAM_COLORS.put(TeamColor.BLUE, BASE + "9");
        TEAM_COLORS.put(TeamColor.GREEN, BASE + "a");
        TEAM_COLORS.put(TeamColor.AQUA, BASE + "b");
        TEAM_COLORS.put(TeamColor.RED, BASE + "c");
        TEAM_COLORS.put(TeamColor.LIGHT_PURPLE, BASE + "d");
        TEAM_COLORS.put(TeamColor.YELLOW, BASE + "e");
        TEAM_COLORS.put(TeamColor.WHITE, BASE + "f");

        // Formats, not colors
        TEAM_COLORS.put(TeamColor.OBFUSCATED, BASE + "k");
        TEAM_COLORS.put(TeamColor.BOLD, BASE + "l");
        TEAM_COLORS.put(TeamColor.STRIKETHROUGH, BASE + "m");
        TEAM_COLORS.put(TeamColor.ITALIC, BASE + "o");

        // Temporary fix for https://github.com/KyoriPowered/adventure/issues/447
        GsonComponentSerializer source = DefaultComponentSerializer.get();
        GSON_SERIALIZER = new GsonComponentSerializerWrapper(source);
        // Tell MCProtocolLib to use this serializer, too.
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
            // Translate any components that require it
            message = RENDERER.render(message, locale);

            String legacy = LegacyComponentSerializer.legacySection().serialize(message);

            StringBuilder finalLegacy = new StringBuilder();
            char[] legacyChars = legacy.toCharArray();
            boolean lastFormatReset = false;
            for (int i = 0; i < legacyChars.length; i++) {
                char legacyChar = legacyChars[i];
                if (legacyChar != ChatColor.ESCAPE || i >= legacyChars.length - 1) {
                    // No special formatting for Bedrock needed
                    // Or, we're at the end of the string
                    finalLegacy.append(legacyChar);
                    lastFormatReset = false;
                    continue;
                }

                char next = legacyChars[++i];
                if (next != 'm' && next != 'n') {
                    // Strikethrough and underline do not exist on Bedrock
                    if ((next >= '0' && next <= '9') || (next >= 'a' && next <= 'f')) {
                        // Append this color code, as well as a necessary reset code
                        if (!lastFormatReset) {
                            finalLegacy.append(RESET);
                        }
                    }
                    finalLegacy.append(BASE).append(next);
                }
                lastFormatReset = next == 'r';
            }

            return finalLegacy.toString();
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().debug(GSON_SERIALIZER.serialize(message));
            GeyserImpl.getInstance().getLogger().error("Failed to parse message", e);

            return "";
        }
    }

    public static String convertMessage(String message, String locale) {
        return convertMessage(GSON_SERIALIZER.deserialize(message), locale);
    }

    public static String convertMessage(String message) {
        return convertMessage(message, GeyserLocale.getDefaultLocale());
    }

    public static String convertMessage(Component message) {
        return convertMessage(message, GeyserLocale.getDefaultLocale());
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
        if (message == null) {
            return "";
        }
        if (message.isBlank()) {
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
        return convertMessageLenient(message, GeyserLocale.getDefaultLocale());
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
     * Convert JSON and legacy format message to plain text
     *
     * @param message Message to convert
     * @param locale Locale to use for translation strings
     * @return The plain text of the message
     */
    public static String convertToPlainText(String message, String locale) {
        if (message == null) {
            return "";
        }
        Component messageComponent = null;
        if (message.startsWith("{") && message.endsWith("}")) {
            // Message is a JSON object
            try {
                messageComponent = GSON_SERIALIZER.deserialize(message);
                // Translate any components that require it
                messageComponent = RENDERER.render(messageComponent, locale);
            } catch (Exception ignored) {
            }
        }
        if (messageComponent == null) {
            messageComponent = LegacyComponentSerializer.legacySection().deserialize(message);
        }
        return PlainTextComponentSerializer.plainText().serialize(messageComponent);
    }

    /**
     * Convert a team color to a chat color
     *
     * @param teamColor Color or format to convert
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
            session.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.chat.too_long", session.getLocale(), message.length()));
            return true;
        }

        return false;
    }

    public static void init() {
        // no-op
    }
}
