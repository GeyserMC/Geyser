/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.type;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;

import java.util.ArrayList;
import java.util.List;

public class WritableBookItem extends Item {
    public WritableBookItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateNbtToBedrock(@NonNull GeyserSession session, @NonNull CompoundTag tag) {
        super.translateNbtToBedrock(session, tag);

        ListTag pagesTag = tag.remove("pages");
        if (pagesTag == null) {
            return;
        }
        List<Tag> pages = new ArrayList<>();
        for (Tag subTag : pagesTag.getValue()) {
            if (!(subTag instanceof StringTag textTag))
                continue;

            CompoundTag pageTag = new CompoundTag("");
            pageTag.put(new StringTag("photoname", ""));
            pageTag.put(new StringTag("text", MessageTranslator.convertMessageLenient(textTag.getValue())));
            pages.add(pageTag);
        }

        tag.put(new ListTag("pages", pages));
    }

    @Override
    public void translateNbtToJava(@NonNull CompoundTag tag, @NonNull ItemMapping mapping) {
        super.translateNbtToJava(tag, mapping);

        if (!tag.contains("pages")) {
            return;
        }
        List<Tag> pages = new ArrayList<>();
        ListTag pagesTag = tag.get("pages");
        for (Tag subTag : pagesTag.getValue()) {
            if (!(subTag instanceof CompoundTag pageTag))
                continue;

            StringTag textTag = pageTag.get("text");
            pages.add(new StringTag("", textTag.getValue()));
        }
        tag.remove("pages");
        tag.put(new ListTag("pages", pages));
    }
}
