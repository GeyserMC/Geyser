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

package org.geysermc.geyser.item.type;

import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

public class FilledMapItem extends MapItem {
    public FilledMapItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public ItemData.Builder translateToBedrock(GeyserSession session, int count, DataComponents components, ItemMapping mapping, ItemMappings mappings) {
        ItemData.Builder builder = super.translateToBedrock(session, count, components, mapping, mappings);
        if (components == null) {
            // This is a fallback for maps with no nbt (Change added back in June 2020; is it needed in 2023?)
            //return builder.tag(NbtMap.builder().putInt("map", 0).build()); TODO if this is *still* broken, let's move it to translateComponentsToBedrock
            return builder;
        } else {
            Integer mapColor = components.get(DataComponentType.MAP_COLOR);
            if (mapColor != null) {
                // Note: damage 5 treasure map, 6 ???
                // Java Edition allows any color; Bedrock only allows some. So let's take what colors we can get
                switch (mapColor) {
                    case 3830373 -> builder.damage(3); // Ocean Monument
                    case 5393476 -> builder.damage(4); // Woodland explorer
                    case 12741452 -> builder.damage(14); // Trial Chamber
                }
            }
        }
        return builder;
    }
}
