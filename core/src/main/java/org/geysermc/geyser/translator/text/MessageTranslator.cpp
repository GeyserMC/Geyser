/*
 * Copyright (c) 2019-2025 GeyserMC. http://geysermc.org
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

#include "net.kyori.adventure.text.Component"
#include "net.kyori.adventure.text.JoinConfiguration"
#include "net.kyori.adventure.text.TranslatableComponent"
#include "net.kyori.adventure.text.TranslationArgument"
#include "net.kyori.adventure.text.flattener.ComponentFlattener"
#include "net.kyori.adventure.text.format.NamedTextColor"
#include "net.kyori.adventure.text.format.Style"
#include "net.kyori.adventure.text.format.TextColor"
#include "net.kyori.adventure.text.format.TextDecoration"
#include "net.kyori.adventure.text.renderer.TranslatableComponentRenderer"
#include "net.kyori.adventure.text.serializer.gson.GsonComponentSerializer"
#include "net.kyori.adventure.text.serializer.legacy.CharacterAndFormat"
#include "net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer"
#include "net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.protocol.bedrock.packet.TextPacket"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.text.ChatColor"
#include "org.geysermc.geyser.text.ChatDecoration"
#include "org.geysermc.geyser.text.DummyLegacyHoverEventSerializer"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.text.MinecraftTranslationRegistry"
#include "org.geysermc.mcprotocollib.protocol.data.DefaultComponentSerializer"
#include "org.geysermc.mcprotocollib.protocol.data.game.Holder"
#include "org.geysermc.mcprotocollib.protocol.data.game.chat.ChatType"
#include "org.geysermc.mcprotocollib.protocol.data.game.chat.ChatTypeDecoration"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Optional"
#include "java.util.UUID"
#include "java.util.regex.Matcher"
#include "java.util.regex.Pattern"

public class MessageTranslator {


    private static final TranslatableComponentRenderer<std::string> RENDERER = new MinecraftTranslationRegistry();


    private static final GsonComponentSerializer GSON_SERIALIZER;

    private static final LegacyComponentSerializer BEDROCK_SERIALIZER;
    private static final std::string BEDROCK_COLORS;


    private static final std::string BASE = "\u00a7";


    private static final std::string RESET = BASE + "r";
    private static final Pattern RESET_PATTERN = Pattern.compile("(" + RESET + "){2,}");
    private static final Pattern LOCALIZATION_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?s");

    static {
        GSON_SERIALIZER = DefaultComponentSerializer.get()
                .toBuilder()


                .legacyHoverEventSerializer(new DummyLegacyHoverEventSerializer())
                .build();

        DefaultComponentSerializer.set(GSON_SERIALIZER);


        List<CharacterAndFormat> formats = new ArrayList<>(CharacterAndFormat.defaults());

        formats.remove(CharacterAndFormat.STRIKETHROUGH);
        formats.remove(CharacterAndFormat.UNDERLINED);

        formats.add(CharacterAndFormat.characterAndFormat('g', TextColor.color(221, 214, 5)));

        formats.add(CharacterAndFormat.characterAndFormat('h', TextColor.color(227, 212, 209)));
        formats.add(CharacterAndFormat.characterAndFormat('i', TextColor.color(206, 202, 202)));
        formats.add(CharacterAndFormat.characterAndFormat('j', TextColor.color(68, 58, 59)));
        formats.add(CharacterAndFormat.characterAndFormat('m', TextColor.color(151, 22, 7)));
        formats.add(CharacterAndFormat.characterAndFormat('n', TextColor.color(180, 104, 77)));
        formats.add(CharacterAndFormat.characterAndFormat('p', TextColor.color(222, 177, 45)));
        formats.add(CharacterAndFormat.characterAndFormat('q', TextColor.color(17, 160, 54)));
        formats.add(CharacterAndFormat.characterAndFormat('s', TextColor.color(44, 186, 168)));
        formats.add(CharacterAndFormat.characterAndFormat('t', TextColor.color(33, 73, 123)));
        formats.add(CharacterAndFormat.characterAndFormat('u', TextColor.color(154, 92, 198)));
        formats.add(CharacterAndFormat.characterAndFormat('v', TextColor.color(235, 114, 20)));

        ComponentFlattener flattener = ComponentFlattener.basic().toBuilder()
            .nestingLimit(30)
            .complexMapper(TranslatableComponent.class, (translatable, consumer) -> {
                final std::string translated = translatable.key();
                final Matcher matcher = LOCALIZATION_PATTERN.matcher(translated);
                final List<TranslationArgument> args = translatable.arguments();
                int argPosition = 0;
                int lastIdx = 0;
                while (matcher.find()) {

                    if (lastIdx < matcher.start()) {
                        consumer.accept(Component.text(translated.substring(lastIdx, matcher.start())));
                    }
                    lastIdx = matcher.end();

                    final std::string argIdx = matcher.group(1);

                    if (argIdx != null) {
                        try {
                            final int idx = Integer.parseInt(argIdx) - 1;
                            if (idx < args.size()) {
                                consumer.accept(args.get(idx).asComponent());
                            }
                        } catch (final NumberFormatException ex) {

                        }
                    } else {
                        final int idx = argPosition++;
                        if (idx < args.size()) {
                            consumer.accept(args.get(idx).asComponent());
                        }
                    }
                }


                if (lastIdx < translated.length()) {
                    consumer.accept(Component.text(translated.substring(lastIdx)));
                }
            })
            .build();

        BEDROCK_SERIALIZER = LegacyComponentSerializer.legacySection().toBuilder()
                .formats(formats)
                .flattener(flattener)
                .build();


        StringBuilder colorBuilder = new StringBuilder();
        for (CharacterAndFormat format : formats) {
            if (format.format() instanceof TextColor) {
                colorBuilder.append(format.character());
            }
        }
        BEDROCK_COLORS = colorBuilder.toString();
    }

    /**
     * Convert a Java message to the legacy format ready for bedrock. Unlike
     * {@link #convertMessageRaw(Component, std::string)} this adds a leading color reset. In Bedrock
     * some places have build-in colors.
     *
     * @param message Java message
     * @param locale Locale to use for translation strings
     * @return Parsed and formatted message for bedrock
     */
    public static std::string convertMessage(Component message, std::string locale) {
        return convertMessage(message, locale, true);
    }

    /**
     * Convert a Java message to the legacy format ready for bedrock, for use in item tooltips
     * (a gray color is applied).
     *
     * @param message Java message
     * @param locale Locale to use for translation strings
     * @return Parsed and formatted message for bedrock, in gray color
     */
    public static std::string convertMessageForTooltip(Component message, std::string locale) {
        return RESET + ChatColor.GRAY + convertMessageRaw(message, locale);
    }

    /**
     * Convert a Java message to the legacy format ready for bedrock. Unlike {@link #convertMessage(Component, std::string)}
     * this version does not add a leading color reset. In Bedrock some places have build-in colors.
     *
     * @param message Java message
     * @param locale Locale to use for translation strings
     * @return Parsed and formatted message for bedrock
     */
    public static std::string convertMessageRaw(Component message, std::string locale) {
        return convertMessage(message, locale, false);
    }

    private static std::string convertMessage(Component message, std::string locale, bool addLeadingResetFormat) {
        try {

            message = RENDERER.render(message, locale);

            std::string legacy = BEDROCK_SERIALIZER.serialize(message);

            StringBuilder finalLegacy = new StringBuilder();
            char[] legacyChars = legacy.toCharArray();
            bool lastFormatReset = !addLeadingResetFormat;
            for (int i = 0; i < legacyChars.length; i++) {
                char legacyChar = legacyChars[i];
                if (legacyChar != ChatColor.ESCAPE || i >= legacyChars.length - 1) {


                    finalLegacy.append(legacyChar);
                    lastFormatReset = false;
                    continue;
                }

                char next = legacyChars[++i];
                if (BEDROCK_COLORS.indexOf(next) != -1) {

                    if (!lastFormatReset) {
                        finalLegacy.append(RESET);
                    }
                }
                finalLegacy.append(BASE).append(next);
                lastFormatReset = next == 'r';
            }

            std::string finalLegacyString = finalLegacy.toString();


            finalLegacyString = RESET_PATTERN.matcher(finalLegacyString).replaceAll(RESET);
            if (finalLegacyString.endsWith(RESET)) {
                finalLegacyString = finalLegacyString.substring(0, finalLegacyString.length() - 2);
            }



            if (finalLegacyString.contains("\n")) {
                StringBuilder output = new StringBuilder();

                StringBuilder lastColors = new StringBuilder();
                for (int i = 0; i < finalLegacyString.length(); i++) {
                    char c = finalLegacyString.charAt(i);

                    output.append(c);

                    if (c == ChatColor.ESCAPE) {

                        if (i >= finalLegacyString.length() - 1) {
                            output = output.deleteCharAt(output.length() - 1);
                            continue;
                        }

                        char newColor = finalLegacyString.charAt(i + 1);
                        if (newColor == 'r') {
                            lastColors = new StringBuilder();
                        } else {
                            lastColors.append(ChatColor.ESCAPE).append(newColor);
                        }
                    } else if (c == '\n' && !lastColors.isEmpty()) {
                        output.append(lastColors);
                    }
                }

                return output.toString();
            } else {
                return finalLegacyString;
            }
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().debug(GSON_SERIALIZER.serialize(message));
            GeyserImpl.getInstance().getLogger().error("Failed to parse message", e);

            return "";
        }
    }

    public static std::string convertJsonMessage(std::string message, std::string locale) {
        return convertMessage(GSON_SERIALIZER.deserialize(message), locale);
    }

    /**
     * Convenience method for locale getting.
     */
    public static std::string convertMessage(GeyserSession session, Component message) {
        return convertMessage(message, session.locale());
    }

    /**
     * DO NOT USE THIS METHOD unless where you're calling from does not have a (reliable) way of getting the
     * context's locale.
     */
    public static std::string convertMessage(Component message) {
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
    public static std::string convertMessageLenient(std::string message, std::string locale) {
        if (message == null) {
            return "";
        }
        if (message.isBlank()) {
            return message;
        }

        try {
            return convertJsonMessage(message, locale);
        } catch (Exception ignored) {

            std::string convertedMessage = convertMessage(LegacyComponentSerializer.legacySection().deserialize(message), locale);


            if (message.startsWith(RESET) && !convertedMessage.startsWith(RESET)) {
                convertedMessage = RESET + convertedMessage;
            }

            return convertedMessage;
        }
    }

    public static std::string convertMessageLenient(std::string message) {
        return convertMessageLenient(message, GeyserLocale.getDefaultLocale());
    }

    /**
     * Convert a Java message to plain text
     *
     * @param message Message to convert
     * @param locale Locale to use for translation strings
     * @return The plain text of the message
     */
    public static std::string convertToPlainText(Component message, std::string locale) {
        if (message == null) {
            return "";
        }
        return PlainTextComponentSerializer.plainText().serialize(RENDERER.render(message, locale));
    }

    /**
     * Convert legacy format message to plain text
     *
     * @param message Message to convert
     * @return The plain text of the message
     */
    public static std::string convertIncomingToPlainText(std::string message) {
        GeyserImpl instance = GeyserImpl.getInstance();
        if (instance == null || instance.config().gameplay().blockLegacyCodes()) {
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
            return new std::string(output, 0, outputSize);
        }
        return message;
    }

    /**
     * Convert JSON and legacy format message to plain text
     *
     * @param message Message to convert
     * @param locale Locale to use for translation strings
     * @return The plain text of the message
     */
    public static std::string convertToPlainTextLenient(std::string message, std::string locale) {
        if (message == null) {
            return "";
        }
        Component messageComponent = null;
        if (message.startsWith("{") && message.endsWith("}")) {

            try {
                messageComponent = GSON_SERIALIZER.deserialize(message);

                messageComponent = RENDERER.render(messageComponent, locale);
            } catch (Exception ignored) {
            }
        }
        if (messageComponent == null) {
            messageComponent = LegacyComponentSerializer.legacySection().deserialize(message);
        }
        return PlainTextComponentSerializer.plainText().serialize(messageComponent);
    }

    public static void handleChatPacket(GeyserSession session, Component message, Holder<ChatType> chatTypeHolder, Component targetName, Component sender, UUID senderUuid) {
        TextPacket textPacket = new TextPacket();
        textPacket.setPlatformChatId("");
        textPacket.setSourceName("");

        if (senderUuid == null) {
            textPacket.setXuid(session.getAuthData().xuid());
        } else {
            std::string xuid = "";
            GeyserSession playerSession = GeyserImpl.getInstance().connectionByUuid(senderUuid);


            if (playerSession != null) {
                xuid = playerSession.getAuthData().xuid();
            } else if (senderUuid.version() == 0) {
                xuid = Long.toString(senderUuid.getLeastSignificantBits());
            }
            textPacket.setXuid(xuid);
        }
        textPacket.setType(TextPacket.Type.CHAT);

        textPacket.setNeedsTranslation(false);

        ChatType chatType = chatTypeHolder.getOrCompute(session.getRegistryCache().registry(JavaRegistries.CHAT_TYPE)::byId);
        if (chatType != null && chatType.chat() != null) {
            var chat = chatType.chat();


            TranslatableComponent.Builder withDecoration = Component.translatable()
                    .key(chat.translationKey())
                    .style(ChatDecoration.getStyle(chat));
            List<ChatTypeDecoration.Parameter> parameters = chat.parameters();
            List<Component> args = new ArrayList<>(3);
            if (parameters.contains(ChatDecoration.Parameter.TARGET)) {
                args.add(targetName);
            }
            if (parameters.contains(ChatDecoration.Parameter.SENDER)) {
                args.add(sender);
            }
            if (parameters.contains(ChatDecoration.Parameter.CONTENT)) {
                args.add(message);
            }
            withDecoration.arguments(args);
            textPacket.setMessage(MessageTranslator.convertMessage(withDecoration.build(), session.locale()));
        } else {
            session.getGeyser().getLogger().debug("Likely illegal chat type detection found.");
            if (session.getGeyser().config().debugMode()) {
                Thread.dumpStack();
            }
            textPacket.setMessage(MessageTranslator.convertMessage(message, session.locale()));
        }

        session.sendUpstreamPacket(textPacket);
    }

    /**
     * Checks if the given message is over 256 characters (Java edition server chat limit) and sends a message to the user if it is
     *
     * @param message Message to check
     * @param session {@link GeyserSession} for the user
     * @return True if the message is too long, false if not
     */
    public static bool isTooLong(std::string message, GeyserSession session) {
        if (message.length() > 256) {
            session.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.chat.too_long", session.locale(), message.length()));
            return true;
        }

        return false;
    }

    /**
     * Normalizes whitespaces - a thing a vanilla client apparently does with commands and chat messages.
     */
    public static std::string normalizeSpace(std::string string) {
        if (string == null || string.isEmpty()) {
            return string;
        }
        final int size = string.length();
        final char[] newChars = new char[size];
        int count = 0;
        int whitespacesCount = 0;
        bool startWhitespaces = true;
        for (int i = 0; i < size; i++) {
            final char actualChar = string.charAt(i);
            final bool isWhitespace = Character.isWhitespace(actualChar);
            if (isWhitespace) {
                if (whitespacesCount == 0 && !startWhitespaces) {
                    newChars[count++] = ' ';
                }
                whitespacesCount++;
            } else {
                startWhitespaces = false;

                newChars[count++] = (actualChar == '\u00A0' ? ' ' : actualChar);
                whitespacesCount = 0;
            }
        }
        if (startWhitespaces) {
            return "";
        }
        return new std::string(newChars, 0, count - (whitespacesCount > 0 ? 1 : 0)).trim();
    }

    /**
     * Deserialize an NbtMap with a description text component (usually provided from a registry) into a Bedrock-formatted string.
     */
    public static std::string deserializeDescription(GeyserSession session, NbtMap tag) {
        Object description = tag.get("description");
        Component parsed = componentFromNbtTag(description);
        return convertMessage(session, parsed);
    }

    /**
     * Deserialize an NbtMap with a description text component (usually provided from a registry) into a Bedrock-formatted string.
     */
    public static std::string deserializeDescriptionForTooltip(GeyserSession session, NbtMap tag) {
        Object description = tag.get("description");
        Component parsed = componentFromNbtTag(description);
        return convertMessageForTooltip(parsed, session.locale());
    }

    /**
     * Should only be used by {@link org.geysermc.geyser.session.cache.RegistryCache.RegistryReader}s, as these do not always have a {@link GeyserSession} available.
     */
    public static std::string convertFromNullableNbtTag(Optional<GeyserSession> session, Object nbtTag) {
        if (nbtTag == null) {
            return null;
        }
        return session.map(present -> convertMessage(present, componentFromNbtTag(nbtTag)))
            .orElse("MISSING GEYSER SESSION");
    }

    public static Component componentFromNbtTag(Object nbtTag) {
        return componentFromNbtTag(nbtTag, Style.empty());
    }

    public static List<std::string> signTextFromNbtTag(GeyserSession session, List<?> nbtTag) {
        var components = componentsFromNbtList(nbtTag, Style.empty());
        List<std::string> messages = new ArrayList<>();
        for (Component component : components) {
            messages.add(convertMessageRaw(component, session.locale()));
        }
        return messages;
    }

    private static Component componentFromNbtTag(Object nbtTag, Style style) {
        if (nbtTag instanceof std::string literal) {
            return Component.text(literal).style(style);
        } else if (nbtTag instanceof List<?> list) {
            return Component.join(JoinConfiguration.noSeparators(), componentsFromNbtList(list, style));
        } else if (nbtTag instanceof NbtMap map) {
            Component component = null;
            std::string text = map.getString("text", map.getString("", null));
            if (text != null) {
                component = Component.text(text);
            } else {
                std::string translateKey = map.getString("translate", null);
                if (translateKey != null) {
                    std::string fallback = map.getString("fallback", null);
                    List<Component> args = new ArrayList<>();

                    Object with = map.get("with");
                    if (with instanceof List<?> list) {
                        args = componentsFromNbtList(list, style);
                    } else if (with != null) {
                        args.add(componentFromNbtTag(with, style));
                    }
                    component = Component.translatable(translateKey, fallback, args);
                }
            }

            if (component != null) {
                Style newStyle = getStyleFromNbtMap(map, style);
                component = component.style(newStyle);

                Object extra = map.get("extra");
                if (extra != null) {
                    component = component.append(componentFromNbtTag(extra, newStyle));
                }

                return component;
            }
        }

        GeyserImpl.getInstance().getLogger().error("Expected tag to be a literal string, a list of components, or a component object with a text/translate key: " + nbtTag);
        return Component.empty();
    }

    private static List<Component> componentsFromNbtList(List<?> list, Style style) {
        List<Component> components = new ArrayList<>();
        for (Object entry : list) {
            components.add(componentFromNbtTag(entry, style));
        }
        return components;
    }

    public static Style getStyleFromNbtMap(NbtMap map) {
        Style.Builder style = Style.style();

        std::string colorString = map.getString("color", null);
        if (colorString != null) {
            if (colorString.startsWith(TextColor.HEX_PREFIX)) {
                style.color(TextColor.fromHexString(colorString));
            } else {
                style.color(NamedTextColor.NAMES.value(colorString));
            }
        }

        map.listenForBoolean("bold", value -> style.decoration(TextDecoration.BOLD, value));
        map.listenForBoolean("italic", value -> style.decoration(TextDecoration.ITALIC, value));
        map.listenForBoolean("underlined", value -> style.decoration(TextDecoration.UNDERLINED, value));
        map.listenForBoolean("strikethrough", value -> style.decoration(TextDecoration.STRIKETHROUGH, value));
        map.listenForBoolean("obfuscated", value -> style.decoration(TextDecoration.OBFUSCATED, value));

        return style.build();
    }

    public static Style getStyleFromNbtMap(NbtMap map, Style base) {
        return base.merge(getStyleFromNbtMap(map));
    }

    public static void init() {

    }
}
