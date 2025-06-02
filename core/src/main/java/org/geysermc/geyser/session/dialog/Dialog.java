/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session.dialog;

import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.MinecraftKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class Dialog {

    private static final Key PLAIN_MESSAGE_BODY = MinecraftKey.key("plain_message");

    private final String title;
    private final String externalTitle;
    private final AfterAction afterAction;
    private final List<String> labels;
    private final List<Object> inputs; // TODO

    protected Dialog(GeyserSession session, NbtMap map) {
        title = MessageTranslator.convertFromNullableNbtTag(session, map.get("title"));
        externalTitle = MessageTranslator.convertFromNullableNbtTag(session, map.get("title"));
        afterAction = AfterAction.fromString(map.getString("after_action"));

        Object bodyTag = map.get("body");
        if (bodyTag == null) {
            labels = List.of();
        } else if (bodyTag instanceof NbtMap bodyMap) {
            String body = readBody(session, bodyMap);
            if (body != null) {
                labels = List.of(body);
            } else {
                labels = List.of();
            }
        } else if (bodyTag instanceof List<?> bodyList) {
            List<String> bodies = new ArrayList<>();
            for (Object tag : bodyList) {
                if (tag instanceof NbtMap bodyMap) {
                    String body = readBody(session, bodyMap);
                    if (body != null) {
                        bodies.add(body);
                    }
                } else {
                    throw new IllegalStateException("Found non-NBT map in list of bodies, was: " + tag);
                }
            }
            labels = List.copyOf(bodies);
        } else {
            throw new IllegalStateException("Expected body tag to either be a NBT map or list thereof, was: " + bodyTag);
        }

        inputs = List.of();
    }

    public CustomForm buildForm() {
        return createForm().build();
    }

    protected CustomForm.Builder createForm() {
        CustomForm.Builder builder = CustomForm.builder()
            .title(title);
        for (String label : labels) {
            builder.label(label);
        }
        return builder;
    }

    private static String readBody(GeyserSession session, NbtMap tag) {
        Key type = MinecraftKey.key(tag.getString("type"));
        if (type.equals(PLAIN_MESSAGE_BODY)) {
            return MessageTranslator.convertFromNullableNbtTag(session, tag.get("contents"));
        }
        // Other type is item, can't display that in forms
        return null;
    }

    public static Dialog readDialog(RegistryEntryContext context) {
        return readDialog(context.session(), context.data());
    }

    public static Dialog readDialog(GeyserSession session, NbtMap map) {
        // TYPES: notice, server_links, dialog_list, multi_action, confirmation
        Key type = MinecraftKey.key(map.getString("type"));
        if (type.equals(NoticeDialog.TYPE)) {
            return new NoticeDialog(session, map);
        }
        return new Dialog(session, map) {};
        // throw new UnsupportedOperationException("Unable to read unknown dialog type " + type + "!"); // TODO put this here once all types are implemented
    }

    public enum AfterAction {
        CLOSE,
        NONE,
        WAIT_FOR_RESPONSE;

        public static AfterAction fromString(String string) {
            for (AfterAction action : values()) {
                if (action.name().toLowerCase(Locale.ROOT).equals(string)) {
                    return action;
                }
            }
            return null;
        }
    }
}
