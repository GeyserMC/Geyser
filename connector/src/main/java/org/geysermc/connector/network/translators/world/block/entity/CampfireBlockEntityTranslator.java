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

package org.geysermc.connector.network.translators.world.block.entity;

import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.Tag;

import org.geysermc.connector.network.translators.Translators;
import org.geysermc.connector.network.translators.item.ItemEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@BlockEntity(name = "Campfire", regex = "campfire")
public class CampfireBlockEntityTranslator extends BlockEntityTranslator {

    @Override
    public List<Tag<?>> translateTag(CompoundTag tag, BlockState blockState) {
        List<Tag<?>> tags = new ArrayList<>();
        ListTag items = tag.get("Items");
        int i = 1;
        for (com.github.steveice10.opennbt.tag.builtin.Tag itemTag : items.getValue()) {
            tags.add(getItem((CompoundTag) itemTag).toBuilder().build("Item" + i));
            i++;
        }
        return tags;
    }

    @Override
    public CompoundTag getDefaultJavaTag(String javaId, int x, int y, int z) {
        CompoundTag tag = getConstantJavaTag(javaId, x, y, z);
        tag.put(new ListTag("Items"));
        return tag;
    }

    @Override
    public com.nukkitx.nbt.tag.CompoundTag getDefaultBedrockTag(String bedrockId, int x, int y, int z) {
        CompoundTagBuilder tagBuilder = getConstantBedrockTag(bedrockId, x, y, z).toBuilder();
        tagBuilder.tag(new com.nukkitx.nbt.tag.CompoundTag("Item1", new HashMap<>()));
        tagBuilder.tag(new com.nukkitx.nbt.tag.CompoundTag("Item2", new HashMap<>()));
        tagBuilder.tag(new com.nukkitx.nbt.tag.CompoundTag("Item3", new HashMap<>()));
        tagBuilder.tag(new com.nukkitx.nbt.tag.CompoundTag("Item4", new HashMap<>()));
        return tagBuilder.buildRootTag();
    }

    protected com.nukkitx.nbt.tag.CompoundTag getItem(CompoundTag tag) {
        ItemEntry entry = Translators.getItemTranslator().getItemEntry((String) tag.get("id").getValue());
        CompoundTagBuilder tagBuilder = CompoundTagBuilder.builder()
                .shortTag("id", (short) entry.getBedrockId())
                .byteTag("Count", (byte) tag.get("Count").getValue())
                .shortTag("Damage", (short) entry.getBedrockData())
                .tag(CompoundTagBuilder.builder().build("tag"));
        return tagBuilder.buildRootTag();
    }
}
