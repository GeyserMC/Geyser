/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.inventory.item.nbt;

import com.github.steveice10.opennbt.tag.builtin.*;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemRemapper;
import org.geysermc.geyser.translator.inventory.item.NbtItemStackTranslator;
import org.geysermc.geyser.registry.type.ItemMapping;

@ItemRemapper
public class MapItemTranslator extends NbtItemStackTranslator {

    @Override
    public void translateToBedrock(GeyserSession session, CompoundTag itemTag, ItemMapping mapping) {
        // Can be either an IntTag or ShortTag
        Tag mapId = itemTag.get("map");
        if (mapId == null) return;

        int mapValue;
        if (mapId.getValue() instanceof Short) {
            // Convert to int if necessary
            mapValue = (int) (short) mapId.getValue();
        } else {
            mapValue = (int) mapId.getValue();
        }

        itemTag.put(new LongTag("map_uuid", mapValue));
        itemTag.put(new IntTag("map_name_index", mapValue));
        itemTag.put(new ByteTag("map_display_players", (byte) 1));
        itemTag.remove("map");
    }

    @Override
    public void translateToJava(CompoundTag itemTag, ItemMapping mapping) {
        IntTag tag = itemTag.get("map_name_index");
        if (tag != null) {
            itemTag.put(new IntTag("map", tag.getValue()));
            itemTag.remove("map_name_index");
            itemTag.remove("map_uuid");
        }
    }

    @Override
    public boolean acceptItem(ItemMapping mapping) {
        return mapping.getJavaIdentifier().equals("minecraft:filled_map");
    }
}
