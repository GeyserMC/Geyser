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

package org.geysermc.geyser.translator.level.block.entity;

import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityType;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;

@BlockEntity(type = BlockEntityType.BRUSHABLE_BLOCK)
public class BrushableBlockEntityTranslator extends BlockEntityTranslator implements RequiresBlockState {

    @Override
    public void translateTag(NbtMapBuilder builder, CompoundTag tag, int blockState) {
        if (!(tag.remove("item") instanceof CompoundTag itemTag)) {
            return;
        }
        Tag hitDirection = tag.get("hit_direction");
        if (hitDirection == null) {
            // java server sends no direction when the item recedes back into the block (if player stops brushing)
            return;
        }

        String id = ((StringTag) itemTag.get("id")).getValue();
        if (Items.AIR.javaIdentifier().equals(id)) {
            return; // server sends air when the block contains nothing
        }

        ItemMapping mapping = Registries.ITEMS.forVersion(GameProtocol.DEFAULT_BEDROCK_CODEC.getProtocolVersion()).getMapping(id);
        if (mapping == null) {
            return;
        }
        NbtMapBuilder itemBuilder = NbtMap.builder()
            .putString("Name", mapping.getBedrockIdentifier())
            .putByte("Count", (byte) itemTag.get("Count").getValue());

        builder.putCompound("item", itemBuilder.build());
        // controls which side the item protrudes from
        builder.putByte("brush_direction", ((Number) hitDirection.getValue()).byteValue());
        // controls how much the item protrudes
        builder.putInt("brush_count", BlockStateValues.getBrushProgress(blockState));
    }
}
