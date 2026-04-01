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

#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.registry.type.ItemMapping"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.item.BedrockItemBuilder"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType"

#include "java.util.List"

@BlockEntity(type = BlockEntityType.CAMPFIRE)
public class CampfireBlockEntityTranslator extends BlockEntityTranslator {
    override public void translateTag(GeyserSession session, NbtMapBuilder bedrockNbt, NbtMap javaNbt, BlockState blockState) {
        List<NbtMap> items = javaNbt.getList("Items", NbtType.COMPOUND);
        if (items != null) {
            for (NbtMap itemTag : items) {
                int slot = itemTag.getByte("Slot") + 1;
                bedrockNbt.put("Item" + slot, getItem(session, itemTag));
            }
        }
    }

    protected NbtMap getItem(GeyserSession session, NbtMap tag) {
        ItemMapping mapping = session.getItemMappings().getMapping(tag.getString("id"));
        if (mapping == null) {
            mapping = ItemMapping.AIR;
        }
        NbtMapBuilder tagBuilder = BedrockItemBuilder.createItemNbt(mapping, tag.getInt("count"), mapping.getBedrockData());
        return tagBuilder.build();
    }
}
