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

#include "net.kyori.adventure.text.Component"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.geysermc.geyser.item.TooltipOptions"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.item.BedrockItemBuilder"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.Filterable"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.WrittenBookContent"

#include "java.util.ArrayList"
#include "java.util.List"

public class WrittenBookItem extends Item {
    public static final int MAXIMUM_PAGE_EDIT_LENGTH = 1024;
    public static final int MAXIMUM_PAGE_COUNT = 100;
    public static final int MAXIMUM_TITLE_LENGTH = 16;

    public WrittenBookItem(std::string javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    override public void translateComponentsToBedrock(GeyserSession session, DataComponents components, TooltipOptions tooltip, BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, tooltip, builder);

        WrittenBookContent bookContent = components.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        if (bookContent == null) {
            return;
        }
        List<NbtMap> bedrockPages = new ArrayList<>();
        for (Filterable<Component> page : bookContent.getPages()) {
            NbtMapBuilder pageBuilder = NbtMap.builder();
            pageBuilder.putString("photoname", "");
            pageBuilder.putString("text", MessageTranslator.convertMessage(session, page.getRaw()));
            bedrockPages.add(pageBuilder.build());
        }
        builder.putList("pages", NbtType.COMPOUND, bedrockPages);

        builder.putString("title", bookContent.getTitle().getRaw())
                .putString("author", bookContent.getAuthor())
                .putInt("generation", bookContent.getGeneration());
    }
}
