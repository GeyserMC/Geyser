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
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TextDecoration {
    private final String translationKey;
    private final Style style;
    private final Set<Parameter> parameters;

    public TextDecoration(NbtMap tag) {
        translationKey = tag.getString("translation_key");

        NbtMap styleTag = tag.getCompound("style");
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
        style = builder.build();

        this.parameters = EnumSet.noneOf(Parameter.class);
        List<String> parameters = tag.getList("parameters", NbtType.STRING);
        for (String parameter : parameters) {
            this.parameters.add(Parameter.valueOf(parameter.toUpperCase(Locale.ROOT)));
        }
    }

    public String translationKey() {
        return translationKey;
    }

    public Style style() {
        return style;
    }

    public Set<Parameter> parameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "TextDecoration{" +
                "translationKey='" + translationKey + '\'' +
                ", style=" + style +
                ", parameters=" + parameters +
                '}';
    }

    public static TextDecoration readChatType(RegistryEntry entry) {
        // Note: The ID is NOT ALWAYS THE SAME! ViaVersion as of 1.19 adds two registry entries that do NOT match vanilla.
        NbtMap tag = entry.getData();
        NbtMap chat = tag.getCompound("chat", null);
        TextDecoration textDecoration = null;
        if (chat != null) {
            textDecoration = new TextDecoration(chat);
        }
        return textDecoration;
    }

    public enum Parameter {
        CONTENT,
        SENDER,
        TARGET
    }
}
