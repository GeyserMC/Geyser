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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;

public class FilledMapItem extends MapItem {
    public FilledMapItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public ItemData.Builder translateToBedrock(ItemStack itemStack, ItemMapping mapping, ItemMappings mappings) {
        ItemData.Builder builder = super.translateToBedrock(itemStack, mapping, mappings);
        CompoundTag nbt = itemStack.getNbt();
        if (nbt == null) {
            // This is a fallback for maps with no nbt (Change added back in June 2020; is it needed in 2023?)
            return builder.tag(NbtMap.builder().putInt("map", 0).build());
        } else if (nbt.get("display") instanceof CompoundTag display) {
            // Note: damage 5 treasure map, 6 ???
            Tag mapColor = display.get("MapColor");
            if (mapColor != null && mapColor.getValue() instanceof Number color) {
                // Java Edition allows any color; Bedrock only allows some. So let's take what colors we can get
                switch (color.intValue()) {
                    case 3830373 -> builder.damage(3); // Ocean Monument
                    case 5393476 -> builder.damage(4); // Woodland explorer
                }
            }
        }
        return builder;
    }
}
