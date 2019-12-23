package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.message.ChatColor;
import com.github.steveice10.mc.protocol.data.message.ChatFormat;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.mc.protocol.data.message.TranslationMessage;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

public class MessageUtils {

    public static List<String> getTranslationParams(Message[] messages) {
        List<String> strings = new ArrayList<String>();
        for (int i = 0; i < messages.length; i++) {
            if (messages[i] instanceof TranslationMessage) {
                TranslationMessage translation = (TranslationMessage) messages[i];

                StringBuilder builder = new StringBuilder("");
                builder.append("%");
                builder.append(translation.getTranslationKey());
                strings.add(builder.toString());

                if (translation.getTranslationKey().equals("commands.gamemode.success.other")) {
                    strings.add("");
                }

                if (translation.getTranslationKey().equals("command.context.here")) {
                    strings.add(" - no permission or invalid command!");
                }

                for (int j = 0; j < getTranslationParams(translation.getTranslationParams()).size(); j++) {
                    strings.add(getTranslationParams(translation.getTranslationParams()).get(j));
                }
            } else {
                StringBuilder builder = new StringBuilder("");
                builder.append(getFormat(messages[i].getStyle().getFormats()));
                builder.append(getColor(messages[i].getStyle().getColor()));
                builder.append(getBedrockMessage(messages[i]));
                strings.add(builder.toString());
            }
        }

        return strings;
    }

    public static String getTranslationText(TranslationMessage message) {
        StringBuilder builder = new StringBuilder("");
        builder.append(getFormat(message.getStyle().getFormats()));
        builder.append(getColor(message.getStyle().getColor()));
        builder.append("%");
        builder.append(message.getTranslationKey());
        return builder.toString();
    }

    public static String getBedrockMessage(Message message) {
        JsonParser parser = new JsonParser();
        if (isMessage(message.getText())) {
            JsonObject object = parser.parse(message.getText()).getAsJsonObject();
            message = Message.fromJson(formatJson(object));
        }

        StringBuilder builder = new StringBuilder(message.getText());
        for (Message msg : message.getExtra()) {
            builder.append(getFormat(msg.getStyle().getFormats()));
            builder.append(getColor(msg.getStyle().getColor()));
            if (!(msg.getText() == null)) {
                builder.append(getBedrockMessage(msg));
            }
        }

        return builder.toString();
    }

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
                base += "r";
                break;
            default:
                return "";
        }

        return base;
    }

    private static String getFormat(List<ChatFormat> formats) {
        String str = "";
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
                    break;
            }

            str += base;
        }

        return str;
    }

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
}
