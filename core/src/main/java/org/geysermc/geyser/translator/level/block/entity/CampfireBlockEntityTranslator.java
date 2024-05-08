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

package org.geysermc.geyser.translator.level.block.entity;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;

import java.util.List;

@BlockEntity(type = BlockEntityType.CAMPFIRE)
public class CampfireBlockEntityTranslator extends BlockEntityTranslator {
    @Override
    public void translateTag(GeyserSession session, NbtMapBuilder bedrockNbt, NbtMap javaNbt, int blockState) {
        List<NbtMap> items = javaNbt.getList("Items", NbtType.COMPOUND);
        if (items != null) {
            int i = 1;
            for (NbtMap itemTag : items) {
                bedrockNbt.put("Item" + i, getItem(session, itemTag));
                i++;
            }
        }
    }

    protected NbtMap getItem(GeyserSession session, NbtMap tag) {
        ItemMapping mapping = session.getItemMappings().getMapping(tag.getString("id"));
        if (mapping == null) {
            mapping = ItemMapping.AIR;
        }
        NbtMapBuilder tagBuilder = BedrockItemBuilder.createItemNbt(mapping, tag.getByte("Count"), mapping.getBedrockData());
        tagBuilder.put("tag", NbtMap.builder().build()); // I don't think this is necessary... - Camo, 1.20.5/1.20.80
        return tagBuilder.build();
    }
}
