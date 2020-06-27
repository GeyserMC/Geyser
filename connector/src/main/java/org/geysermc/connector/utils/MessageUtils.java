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
import com.google.gson.*;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

    private static final Map<String, Integer> COLORS = new HashMap<>();

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
    };

    public static List<String> getTranslationParams(List<Message> messages, String locale) {
        List<String> strings = new ArrayList<>();
        for (Message message : messages) {
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

                List<String> furtherParams = getTranslationParams(translation.getWith(), locale);
                if (locale != null) {
                    strings.add(insertParams(LocaleUtils.getLocaleString(translation.getKey(), locale), furtherParams));
                } else {
                    strings.addAll(furtherParams);
                }
            } else {
                String builder = getFormat(message.getStyle().getFormats()) +
                        getColorOrParent(message.getStyle());
                builder += getTranslatedBedrockMessage(message, locale, false);
                strings.add(builder);
            }
        }

        return strings;
    }

    public static List<String> getTranslationParams(List<Message> messages) {
        return getTranslationParams(messages, null);
    }

    public static String getTranslationText(TranslationMessage message) {
        return getFormat(message.getStyle().getFormats()) + getColorOrParent(message.getStyle())
                + "%" + message.getKey();
    }

    public static String getTranslatedBedrockMessage(Message message, String locale, boolean shouldTranslate) {
        JsonParser parser = new JsonParser();
        if (isMessage(message.toString())) {
            JsonObject object = parser.parse(message.toString()).getAsJsonObject();
            message = MessageSerializer.fromJson(formatJson(object));
        }

        String messageText = (message instanceof TranslationMessage) ? ((TranslationMessage) message).getKey() : ((TextMessage) message).getText();
        if (locale != null && shouldTranslate) {
            messageText = LocaleUtils.getLocaleString(messageText, locale);
        }

        StringBuilder builder = new StringBuilder();
        builder.append(getFormat(message.getStyle().getFormats()));
        builder.append(getColorOrParent(message.getStyle()));
        builder.append(messageText);

        for (Message msg : message.getExtra()) {
            builder.append(getFormat(msg.getStyle().getFormats()));
            builder.append(getColorOrParent(msg.getStyle()));
            if (!(msg.toString() == null)) {
                boolean isTranslationMessage = (msg instanceof TranslationMessage);
                String extraText = "";

                if (isTranslationMessage) {
                    List<String> paramsTranslated =  getTranslationParams(((TranslationMessage) msg).getWith(), locale);
                    extraText = insertParams(getTranslatedBedrockMessage(msg, locale, isTranslationMessage), paramsTranslated);
                } else {
                    extraText = getTranslatedBedrockMessage(msg, locale, isTranslationMessage);
                }

                builder.append(extraText);
                builder.append("\u00a7r");
            }
        }

        return builder.toString();
    }

    public static String getTranslatedBedrockMessage(Message message, String locale) {
        return getTranslatedBedrockMessage(message, locale, true);
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
        return GsonComponentSerializer.INSTANCE.deserialize(message);
    }

    public static String getJavaMessage(String message) {
        Component component = LegacyComponentSerializer.legacy().deserialize(message);
        return GsonComponentSerializer.INSTANCE.serialize(component);
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
     * Gets the colour for the message style or fetches it from the parent (recursive)
     *
     * @param style The style to get the colour from
     * @return Colour string to be used
     */
    private static String getColorOrParent(MessageStyle style) {
        String color = style.getColor();

        /*if (color == ChatColor.NONE && style.getParent() != null) {
            return getColorOrParent(style.getParent());
        }*/

        return getColor(color);
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
                return testColor.getKey();
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
                MessageSerializer.fromJson(formatJson(object));
            } catch (Exception ex) {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public static JsonObject formatJson(JsonObject object) {
        if (object.has("hoverEvent")) {
            JsonObject sub = (JsonObject) object.get("hoverEvent");
            JsonElement element = sub.get("value");

            if (element instanceof JsonArray) {
                JsonObject newobj = new JsonObject();
                newobj.add("extra", element);
                newobj.addProperty("text", "");
                sub.remove("value");
                sub.add("value", newobj);
            }
        }

        if (object.has("extra")) {
            JsonArray a = object.getAsJsonArray("extra");
            for (int i = 0; i < a.size(); i++) {
                if (!(a.get(i) instanceof JsonPrimitive))
                    formatJson((JsonObject) a.get(i));
            }
        }
        return object;
    }

    public static String toChatColor(TeamColor teamColor) {
//        for (ChatColor color : ChatColor.) {
//            if (color.name().equals(teamColor.name())) {
//                return getColor(color);
//            }
//        }
//        for (ChatFormat format : ChatFormat.values()) {
//            if (format.name().equals(teamColor.name())) {
//                return getFormat(Collections.singletonList(format));
//            }
//        } Not dealing with this
        return "";
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
            // TODO: Add Geyser localization and translate this based on language
            session.sendMessage("Your message is bigger than 256 characters (" + message.length() + ") so it has not been sent.");
            return true;
        }

        return false;
    }
}
