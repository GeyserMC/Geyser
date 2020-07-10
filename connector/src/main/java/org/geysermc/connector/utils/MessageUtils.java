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

package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamColor;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.mc.protocol.data.message.MessageSerializer;
import com.github.steveice10.mc.protocol.data.message.TextMessage;
import com.github.steveice10.mc.protocol.data.message.TranslationMessage;
import com.github.steveice10.mc.protocol.data.message.style.ChatColor;
import com.github.steveice10.mc.protocol.data.message.style.ChatFormat;
import com.github.steveice10.mc.protocol.data.message.style.MessageStyle;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

    private static final Map<String, Integer> COLORS = new HashMap<>();
    private static final Map<TeamColor, String> TEAM_COLORS = new HashMap<>();

    static {
        COLORS.put(ChatColor.BLACK, 0x000000);
        COLORS.put(ChatColor.DARK_BLUE, 0x0000aa);
        COLORS.put(ChatColor.DARK_GREEN, 0x00aa00);
        COLORS.put(ChatColor.DARK_AQUA, 0x00aaaa);
        COLORS.put(ChatColor.DARK_RED, 0xaa0000);
        COLORS.put(ChatColor.DARK_PURPLE, 0xaa00aa);
        COLORS.put(ChatColor.GOLD, 0xffaa00);
        COLORS.put(ChatColor.GRAY, 0xaaaaaa);
        COLORS.put(ChatColor.DARK_GRAY, 0x555555);
        COLORS.put(ChatColor.BLUE, 0x5555ff);
        COLORS.put(ChatColor.GREEN, 0x55ff55);
        COLORS.put(ChatColor.AQUA, 0x55ffff);
        COLORS.put(ChatColor.RED, 0xff5555);
        COLORS.put(ChatColor.LIGHT_PURPLE, 0xff55ff);
        COLORS.put(ChatColor.YELLOW, 0xffff55);
        COLORS.put(ChatColor.WHITE, 0xffffff);

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
        TEAM_COLORS.put(TeamColor.OBFUSCATED, getFormat(Collections.singletonList(ChatFormat.OBFUSCATED)));
        TEAM_COLORS.put(TeamColor.BOLD, getFormat(Collections.singletonList(ChatFormat.BOLD)));
        TEAM_COLORS.put(TeamColor.STRIKETHROUGH, getFormat(Collections.singletonList(ChatFormat.STRIKETHROUGH)));
        TEAM_COLORS.put(TeamColor.ITALIC, getFormat(Collections.singletonList(ChatFormat.ITALIC)));
    }

    /**
     * Recursively parse each message from a list for usage in a {@link TranslationMessage}
     *
     * @param messages A {@link List} of {@link Message} to parse
     * @param locale A locale loaded to get the message for
     * @param parent A {@link Message} to use as the parent (can be null)
     * @return
     */
    public static List<String> getTranslationParams(List<Message> messages, String locale, Message parent) {
        List<String> strings = new ArrayList<>();
        for (Message message : messages) {
            message = fixMessageStyle(message, parent);

            if (message instanceof TranslationMessage) {
                TranslationMessage translation = (TranslationMessage) message;

                if (locale == null) {
                    String builder = "%" + translation.getKey();
                    strings.add(builder);
                }

                if (translation.getKey().equals("commands.gamemode.success.other")) {
                    strings.add("");
                }

                if (translation.getKey().equals("command.context.here")) {
                    strings.add(" - no permission or invalid command!");
                }

                // Collect all params and add format corrections to the end of them
                List<String> furtherParams = new ArrayList<>();
                for (String param : getTranslationParams(translation.getWith(), locale, message)) {
                    String newParam = param;
                    if (parent.getStyle().getFormats().size() != 0) {
                        newParam += getFormat(parent.getStyle().getFormats());
                    }
                    if (parent.getStyle().getColor() != ChatColor.NONE) {
                        newParam += getColor(parent.getStyle().getColor());
                    }

                    furtherParams.add(newParam);
                }

                if (locale != null) {
                    strings.add(insertParams(LocaleUtils.getLocaleString(translation.getKey(), locale), furtherParams));
                } else {
                    strings.addAll(furtherParams);
                }
            } else {
                String builder = getFormat(message.getStyle().getFormats()) +
                        getColor(message.getStyle().getColor());
                builder += getTranslatedBedrockMessage(message, locale, false, parent);
                strings.add(builder);
            }
        }

        return strings;
    }

    public static String getTranslatedBedrockMessage(Message message, String locale) {
        return getTranslatedBedrockMessage(message, locale, true);
    }

    public static String getTranslatedBedrockMessage(Message message, String locale, boolean shouldTranslate) {
        return getTranslatedBedrockMessage(message, locale, shouldTranslate, null);
    }

    /**
     * Translate a given {@link TranslationMessage} to the given locale
     *
     * @param message The {@link Message} to send
     * @param locale
     * @param shouldTranslate
     * @param parent
     * @return
     */
    public static String getTranslatedBedrockMessage(Message message, String locale, boolean shouldTranslate, Message parent) {
        JsonParser parser = new JsonParser();
        if (isMessage(message.toString())) {
            JsonObject object = parser.parse(message.toString()).getAsJsonObject();
            message = MessageSerializer.fromJson(object);
        }

        message = fixMessageStyle(message, parent);

        String messageText = (message instanceof TranslationMessage) ? ((TranslationMessage) message).getKey() : ((TextMessage) message).getText();
        if (locale != null && shouldTranslate) {
            messageText = LocaleUtils.getLocaleString(messageText, locale);
        }

        StringBuilder builder = new StringBuilder();
        builder.append(getFormat(message.getStyle().getFormats()));
        builder.append(getColor(message.getStyle().getColor()));
        builder.append(messageText);

        for (Message msg : message.getExtra()) {
            builder.append(getFormat(msg.getStyle().getFormats()));
            builder.append(getColor(msg.getStyle().getColor()));
            if (!(msg.toString() == null)) {
                boolean isTranslationMessage = (msg instanceof TranslationMessage);
                String extraText = "";

                if (isTranslationMessage) {
                    List<String> paramsTranslated =  getTranslationParams(((TranslationMessage) msg).getWith(), locale, message);
                    extraText = insertParams(getTranslatedBedrockMessage(msg, locale, isTranslationMessage, message), paramsTranslated);
                } else {
                    extraText = getTranslatedBedrockMessage(msg, locale, isTranslationMessage, message);
                }

                builder.append(extraText);
                builder.append("\u00a7r");
            }
        }

        return builder.toString();
    }

    /**
     * If the passed {@link Message} color or format are empty then copy from parent
     *
     * @param message {@link Message} to update
     * @param parent Parent {@link Message} for style
     * @return The updated {@link Message}
     */
    private static Message fixMessageStyle(Message message, Message parent) {
        if (parent == null) {
            return message;
        }
        MessageStyle.Builder styleBuilder = message.getStyle().toBuilder();

        // Copy color from parent
        if (message.getStyle().getColor() == ChatColor.NONE) {
            styleBuilder.color(parent.getStyle().getColor());
        }

        // Copy formatting from parent
        if (message.getStyle().getFormats().size() == 0) {
            styleBuilder.formats(parent.getStyle().getFormats());
        }

        return message.toBuilder().style(styleBuilder.build()).build();
    }

    public static String getBedrockMessage(Message message) {
        if (isMessage(((TextMessage) message).getText())) {
            return getBedrockMessage(((TextMessage) message).getText());
        } else {
            return getBedrockMessage(MessageSerializer.toJsonString(message));
        }
    }

    /**
     * Verifies the message is valid JSON in case it's plaintext. Works around GsonComponentSeraializer not using lenient mode.
     * See https://wiki.vg/Chat for messages sent in lenient mode, and for a description on leniency.
     *
     * @param message Potentially lenient JSON message
     * @return Bedrock formatted message
     */
    public static String getBedrockMessageLenient(String message) {
        if (isMessage(message)) {
            return getBedrockMessage(message);
        } else {
            final JsonObject obj = new JsonObject();
            obj.addProperty("text", message);
            return getBedrockMessage(obj.toString());
        }
    }

    public static String getBedrockMessage(String message) {
        Component component = phraseJavaMessage(message);
        return LegacyComponentSerializer.legacy().serialize(component);
    }

    public static Component phraseJavaMessage(String message) {
        return GsonComponentSerializer.gson().deserialize(message);
    }

    public static String getJavaMessage(String message) {
        Component component = LegacyComponentSerializer.legacy().deserialize(message);
        return GsonComponentSerializer.gson().serialize(component);
    }

    /**
     * Inserts the given parameters into the given message both in sequence and as requested
     *
     * @param message Message containing possible parameter replacement strings
     * @param params  A list of parameter strings
     * @return Parsed message with all params inserted as needed
     */
    public static String insertParams(String message, List<String> params) {
        String newMessage = message;

        Pattern p = Pattern.compile("%([1-9])\\$s");
        Matcher m = p.matcher(message);
        while (m.find()) {
            try {
                newMessage = newMessage.replaceFirst("%" + m.group(1) + "\\$s", params.get(Integer.parseInt(m.group(1)) - 1));
            } catch (Exception e) {
                // Couldn't find the param to replace
            }
        }

        for (String text : params) {
            newMessage = newMessage.replaceFirst("%s", text.replaceAll("%s", "%r"));
        }

        newMessage = newMessage.replaceAll("%r", "MISSING!");

        return newMessage;
    }

    /**
     * Convert a ChatColor into a string for inserting into messages
     *
     * @param color ChatColor to convert
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
            //case NONE:
                base += "r";
                break;
            case "": // To stop recursion
                return "";
            default:
                return getClosestColor(color);
        }

        return base;
    }

    /**
     * Based on https://github.com/ViaVersion/ViaBackwards/blob/master/core/src/main/java/nl/matsv/viabackwards/protocol/protocol1_15_2to1_16/chat/TranslatableRewriter1_16.java
     *
     * @param color A color string
     * @return The closest color to that string
     */
    private static String getClosestColor(String color) {
        if (!color.startsWith("#")) {
            return "";
        }

        int rgb = Integer.parseInt(color.substring(1), 16);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        String closest = null;
        int smallestDiff = 0;

        for (Map.Entry<String, Integer> testColor : COLORS.entrySet()) {
            if (testColor.getValue() == rgb) {
                closest = testColor.getKey();
                break;
            }

            int testR = (testColor.getValue() >> 16) & 0xFF;
            int testG = (testColor.getValue() >> 8) & 0xFF;
            int testB = testColor.getValue() & 0xFF;

            // Check by the greatest diff of the 3 values
            int rDiff = Math.abs(testR - r);
            int gDiff = Math.abs(testG - g);
            int bDiff = Math.abs(testB - b);
            int maxDiff = Math.max(Math.max(rDiff, gDiff), bDiff);
            if (closest == null || maxDiff < smallestDiff) {
                closest = testColor.getKey();
                smallestDiff = maxDiff;
            }
        }

        return getColor(closest);
    }

    /**
     * Convert a list of ChatFormats into a string for inserting into messages
     *
     * @param formats ChatFormats to convert
     * @return The converted chat formatting string
     */
    private static String getFormat(List<ChatFormat> formats) {
        StringBuilder str = new StringBuilder();
        for (ChatFormat cf : formats) {
            String base = "\u00a7";
            switch (cf) {
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
        }

        return str.toString();
    }

    /**
     * Checks if the given text string is a json message
     *
     * @param text String to test
     * @return True if its a valid message json string, false if not
     */
    public static boolean isMessage(String text) {
        JsonParser parser = new JsonParser();
        try {
            JsonObject object = parser.parse(text).getAsJsonObject();
            try {
                MessageSerializer.fromJson(object);
            } catch (Exception ex) {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public static String toChatColor(TeamColor teamColor) {
        return TEAM_COLORS.getOrDefault(teamColor, "");
    }

    /**
     * Checks if the given message is over 256 characters (Java edition server chat limit) and sends a message to the user if it is
     *
     * @param message Message to check
     * @param session GeyserSession for the user
     * @return True if the message is too long, false if not
     */
    public static boolean isTooLong(String message, GeyserSession session) {
        if (message.length() > 256) {
            session.sendMessage(LanguageUtils.getPlayerLocaleString("geyser.chat.too_long", session.getClientData().getLanguageCode(), message.length()));
            return true;
        }

        return false;
    }
}
