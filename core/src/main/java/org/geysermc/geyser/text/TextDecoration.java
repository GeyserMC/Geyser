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

package org.geysermc.geyser.text;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatType;
import org.geysermc.mcprotocollib.protocol.data.game.chat.ChatTypeDecoration;

import java.util.*;

public record TextDecoration(String translationKey, List<Parameter> parameters, Style deserializedStyle) implements ChatTypeDecoration {

    @Override
    public NbtMap style() {
        // Should not ever be called.
        throw new UnsupportedOperationException();
    }

    public static ChatType readChatType(RegistryEntryContext context) {
        // Note: The ID is NOT ALWAYS THE SAME! ViaVersion as of 1.19 adds two registry entries that do NOT match vanilla.
        // (This note has been passed around through several classes and iterations. It stays as a warning
        // to anyone that dares to try and hardcode registry IDs.)
        NbtMap tag = context.data();
        NbtMap chat = tag.getCompound("chat", null);
        if (chat != null) {
            String translationKey = chat.getString("translation_key");

            NbtMap styleTag = chat.getCompound("style");
            Style style = deserializeStyle(styleTag);

            List<ChatTypeDecoration.Parameter> parameters = new ArrayList<>();
            List<String> parametersNbt = chat.getList("parameters", NbtType.STRING);
            for (String parameter : parametersNbt) {
                parameters.add(ChatTypeDecoration.Parameter.valueOf(parameter.toUpperCase(Locale.ROOT)));
            }
            return new ChatType(new TextDecoration(translationKey, parameters, style), null);
        }
        return new ChatType(null, null);
    }

    public static Style getStyle(ChatTypeDecoration decoration) {
        if (decoration instanceof TextDecoration textDecoration) {
            return textDecoration.deserializedStyle();
        }
        return deserializeStyle(decoration.style());
    }

    private static Style deserializeStyle(NbtMap styleTag) {
        Style.Builder builder = Style.style();
        if (!styleTag.isEmpty()) {
            String color = styleTag.getString("color", null);
            if (color != null) {
                builder.color(NamedTextColor.NAMES.value(color));
            }
            //TODO implement the rest
            boolean italic = styleTag.getBoolean("italic");
            if (italic) {
                builder.decorate(net.kyori.adventure.text.format.TextDecoration.ITALIC);
            }
        }
        return builder.build();
    }
}
