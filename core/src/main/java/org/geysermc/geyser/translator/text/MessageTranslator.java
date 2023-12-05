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

package org.geysermc.geyser.translator.text;

import com.github.steveice10.mc.protocol.data.DefaultComponentSerializer;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ScoreComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.CharacterAndFormat;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.cloudburstmc.protocol.bedrock.packet.TextPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.*;

import java.util.*;

public class MessageTranslator {
    // These are used for handling the translations of the messages
    // Custom instead of TranslatableComponentRenderer#usingTranslationSource so we don't need to worry about finding a Locale class
    private static final TranslatableComponentRenderer<String> RENDERER = new MinecraftTranslationRegistry();

    // Possible TODO: replace the legacy hover event serializer with an empty one since we have no use for hover events
    private static final GsonComponentSerializer GSON_SERIALIZER;

    private static final LegacyComponentSerializer BEDROCK_SERIALIZER;
    private static final String BEDROCK_COLORS;

    // Store team colors for player names
    private static final Map<TeamColor, String> TEAM_COLORS = new EnumMap<>(TeamColor.class);

    // Legacy formatting character
    private static final String BASE = "\u00a7";

    // Reset character
    private static final String RESET = BASE + "r";

    static {
        TEAM_COLORS.put(TeamColor.RESET, RESET);

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

        // Temporary fix for https://github.com/KyoriPowered/adventure/issues/447 - TODO resolve properly
        GsonComponentSerializer source = DefaultComponentSerializer.get()
                .toBuilder()
                // Use a custom legacy hover event deserializer since we don't use any of this data anyway, and
                // fixes issues where legacy hover events throw deserialization errors
                .legacyHoverEventSerializer(new DummyLegacyHoverEventSerializer())
                .build();
        GSON_SERIALIZER = new GsonComponentSerializerWrapper(source);
        // Tell MCProtocolLib to use this serializer, too.
        DefaultComponentSerializer.set(GSON_SERIALIZER);

        // Customize the formatting characters of our legacy serializer for bedrock edition
        List<CharacterAndFormat> formats = new ArrayList<>(CharacterAndFormat.defaults());
        // The following two do not yet exist on Bedrock - https://bugs.mojang.com/browse/MCPE-41729
        formats.remove(CharacterAndFormat.STRIKETHROUGH);
        formats.remove(CharacterAndFormat.UNDERLINED);

        formats.add(CharacterAndFormat.characterAndFormat('g', TextColor.color(221, 214, 5))); // Minecoin Gold
        // Add the new characters implemented in 1.19.80
        formats.add(CharacterAndFormat.characterAndFormat('h', TextColor.color(227, 212, 209))); // Quartz
        formats.add(CharacterAndFormat.characterAndFormat('i', TextColor.color(206, 202, 202))); // Iron
        formats.add(CharacterAndFormat.characterAndFormat('j', TextColor.color(68, 58, 59))); // Netherite
        formats.add(CharacterAndFormat.characterAndFormat('m', TextColor.color(151, 22, 7))); // Redstone
        formats.add(CharacterAndFormat.characterAndFormat('n', TextColor.color(180, 104, 77))); // Copper
        formats.add(CharacterAndFormat.characterAndFormat('p', TextColor.color(222, 177, 45))); // Gold
        formats.add(CharacterAndFormat.characterAndFormat('q', TextColor.color(17, 160, 54))); // Emerald
        formats.add(CharacterAndFormat.characterAndFormat('s', TextColor.color(44, 186, 168))); // Diamond
        formats.add(CharacterAndFormat.characterAndFormat('t', TextColor.color(33, 73, 123))); // Lapis
        formats.add(CharacterAndFormat.characterAndFormat('u', TextColor.color(154, 92, 198))); // Amethyst

        // Can be removed once Adventure 1.15.0 is released (see https://github.com/KyoriPowered/adventure/pull/954)
        ComponentFlattener flattener = ComponentFlattener.basic().toBuilder()
                .mapper(ScoreComponent.class, component -> "")
                .build();

        BEDROCK_SERIALIZER = LegacyComponentSerializer.legacySection().toBuilder()
                .formats(formats)
                .flattener(flattener)
                .build();

        // cache all the legacy character codes
        StringBuilder colorBuilder = new StringBuilder();
        for (CharacterAndFormat format : formats) {
            if (format.format() instanceof TextColor) {
                colorBuilder.append(format.character());
            }
        }
        BEDROCK_COLORS = colorBuilder.toString();
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

            String legacy = BEDROCK_SERIALIZER.serialize(message);

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
                if (BEDROCK_COLORS.indexOf(next) != -1) {
                    // Append this color code, as well as a necessary reset code
                    if (!lastFormatReset) {
                        finalLegacy.append(RESET);
                    }
                }
                finalLegacy.append(BASE).append(next);
                lastFormatReset = next == 'r';
            }

            return finalLegacy.toString();
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().debug(GSON_SERIALIZER.serialize(message));
            GeyserImpl.getInstance().getLogger().error("Failed to parse message", e);

            return "";
        }
    }

    public static String convertJsonMessage(String message, String locale) {
        return convertMessage(GSON_SERIALIZER.deserialize(message), locale);
    }

    public static String convertJsonMessage(String message) {
        return convertJsonMessage(message, GeyserLocale.getDefaultLocale());
    }

    public static String convertMessage(Component message) {
        return convertMessage(message, GeyserLocale.getDefaultLocale());
    }

    /**
     * Verifies the message is valid JSON in case it's plaintext. Works around GsonComponentSerializer not using lenient mode.
     * See <a href="https://wiki.vg/Chat">here</a> for messages sent in lenient mode, and for a description on leniency.
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
            return convertJsonMessage(message, locale);
        } catch (Exception ignored) {
            // Use the default legacy serializer since message is java-legacy
            String convertedMessage = convertMessage(LegacyComponentSerializer.legacySection().deserialize(message), locale);

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
        Component component = BEDROCK_SERIALIZER.deserialize(message);
        return GSON_SERIALIZER.serialize(component);
    }

    /**
     * Convert legacy format message to plain text
     *
     * @param message Message to convert
     * @return The plain text of the message
     */
    public static String convertToPlainText(String message) {
        char[] input = message.toCharArray();
        char[] output = new char[input.length];
        int outputSize = 0;
        for (int i = 0, inputLength = input.length; i < inputLength; i++) {
            char c = input[i];
            if (c == ChatColor.ESCAPE) {
                i++;
            } else {
                output[outputSize++] = c;
            }
        }
        return new String(output, 0, outputSize);
    }

    /**
     * Convert JSON and legacy format message to plain text
     *
     * @param message Message to convert
     * @param locale Locale to use for translation strings
     * @return The plain text of the message
     */
    public static String convertToPlainTextLenient(String message, String locale) {
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

    public static void handleChatPacket(GeyserSession session, Component message, int chatType, Component targetName, Component sender) {
        TextPacket textPacket = new TextPacket();
        textPacket.setPlatformChatId("");
        textPacket.setSourceName("");
        textPacket.setXuid(session.getAuthData().xuid());
        textPacket.setType(TextPacket.Type.CHAT);

        textPacket.setNeedsTranslation(false);

        TextDecoration decoration = session.getChatTypes().get(chatType);
        if (decoration != null) {
            // As of 1.19 - do this to apply all the styling for signed messages
            // Though, Bedrock cannot care about the signed stuff.
            TranslatableComponent.Builder withDecoration = Component.translatable()
                    .key(decoration.translationKey())
                    .style(decoration.style());
            Set<TextDecoration.Parameter> parameters = decoration.parameters();
            List<Component> args = new ArrayList<>(3);
            if (parameters.contains(TextDecoration.Parameter.TARGET)) {
                args.add(targetName);
            }
            if (parameters.contains(TextDecoration.Parameter.SENDER)) {
                args.add(sender);
            }
            if (parameters.contains(TextDecoration.Parameter.CONTENT)) {
                args.add(message);
            }
            withDecoration.args(args);
            textPacket.setMessage(MessageTranslator.convertMessage(withDecoration.build(), session.locale()));
        } else {
            session.getGeyser().getLogger().debug("Likely illegal chat type detection found.");
            if (session.getGeyser().getConfig().isDebugMode()) {
                Thread.dumpStack();
            }
            textPacket.setMessage(MessageTranslator.convertMessage(message, session.locale()));
        }

        session.sendUpstreamPacket(textPacket);
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
            session.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.chat.too_long", session.locale(), message.length()));
            return true;
        }

        return false;
    }

    public static void init() {
        // no-op
    }
}
