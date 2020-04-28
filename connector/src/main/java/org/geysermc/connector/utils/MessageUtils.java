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
import com.github.steveice10.mc.protocol.data.message.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

    public static List<String> getTranslationParams(Message[] messages, String locale) {
        List<String> strings = new ArrayList<>();
        for (Message message : messages) {
            if (message instanceof TranslationMessage) {
                TranslationMessage translation = (TranslationMessage) message;

                if (locale == null) {
                    String builder = "%" + translation.getTranslationKey();
                    strings.add(builder);
                }

                if (translation.getTranslationKey().equals("commands.gamemode.success.other")) {
                    strings.add("");
                }

                if (translation.getTranslationKey().equals("command.context.here")) {
                    strings.add(" - no permission or invalid command!");
                }

                List<String> furtherParams = getTranslationParams(translation.getTranslationParams(), locale);
                if (locale != null) {
                    strings.add(insertParams(LocaleUtils.getLocaleString(translation.getTranslationKey(), locale), furtherParams));
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

    public static List<String> getTranslationParams(Message[] messages) {
        return getTranslationParams(messages, null);
    }

    public static String getTranslationText(TranslationMessage message) {
        return getFormat(message.getStyle().getFormats()) + getColorOrParent(message.getStyle())
                + "%" + message.getTranslationKey();
    }

    public static String getTranslatedBedrockMessage(Message message, String locale, boolean shouldTranslate) {
        JsonParser parser = new JsonParser();
        if (isMessage(message.getText())) {
            JsonObject object = parser.parse(message.getText()).getAsJsonObject();
            message = Message.fromJson(formatJson(object));
        }

        String messageText = message.getText();
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
            if (!(msg.getText() == null)) {
                boolean isTranslationMessage = (msg instanceof TranslationMessage);
                builder.append(getTranslatedBedrockMessage(msg, locale, isTranslationMessage));
            }
        }
        return builder.toString();
    }

    public static String getTranslatedBedrockMessage(Message message, String locale) {
        return getTranslatedBedrockMessage(message, locale, true);
    }

    public static String getBedrockMessage(Message message) {
        Component component;
        if (isMessage(message.getText())) {
            component = GsonComponentSerializer.INSTANCE.deserialize(message.getText());
        } else {
            component = GsonComponentSerializer.INSTANCE.deserialize(message.toJsonString());
        }
        return LegacyComponentSerializer.legacy().serialize(component);
    }

    public static String getBedrockMessage(String message) {
        Component component = GsonComponentSerializer.INSTANCE.deserialize(message);
        return LegacyComponentSerializer.legacy().serialize(component);
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
            newMessage = newMessage.replaceFirst("%s", text);
        }

        return newMessage;
    }

    /**
     * Gets the colour for the message style or fetches it from the parent (recursive)
     *
     * @param style The style to get the colour from
     * @return Colour string to be used
     */
    private static String getColorOrParent(MessageStyle style) {
        ChatColor chatColor = style.getColor();

        if (chatColor == ChatColor.NONE && style.getParent() != null) {
            return getColorOrParent(style.getParent());
        }

        return getColor(chatColor);
    }

    /**
     * Convert a ChatColor into a string for inserting into messages
     *
     * @param color ChatColor to convert
     * @return The converted color string
     */
    private static String getColor(ChatColor color) {
        String base = "\u00a7";
        switch (color) {
            case BLACK:
                base += "0";
                break;
            case DARK_BLUE:
                base += "1";
                break;
            case DARK_GREEN:
                base += "2";
                break;
            case DARK_AQUA:
                base += "3";
                break;
            case DARK_RED:
                base += "4";
                break;
            case DARK_PURPLE:
                base += "5";
                break;
            case GOLD:
                base += "6";
                break;
            case GRAY:
                base += "7";
                break;
            case DARK_GRAY:
                base += "8";
                break;
            case BLUE:
                base += "9";
                break;
            case GREEN:
                base += "a";
                break;
            case AQUA:
                base += "b";
                break;
            case RED:
                base += "c";
                break;
            case LIGHT_PURPLE:
                base += "d";
                break;
            case YELLOW:
                base += "e";
                break;
            case WHITE:
                base += "f";
                break;
            case RESET:
            case NONE:
                base += "r";
                break;
            default:
                return "";
        }

        return base;
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
                Message.fromJson(formatJson(object));
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
        for (ChatColor color : ChatColor.values()) {
            if (color.name().equals(teamColor.name())) {
                return getColor(color);
            }
        }
        for (ChatFormat format : ChatFormat.values()) {
            if (format.name().equals(teamColor.name())) {
                return getFormat(Collections.singletonList(format));
            }
        }
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
