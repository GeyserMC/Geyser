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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.ByteTag;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;

public class CompassItem extends Item {
    public CompassItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public ItemData.Builder translateToBedrock(ItemStack itemStack, ItemMapping mapping, ItemMappings mappings) {
        if (isLodestoneCompass(itemStack.getNbt())) {
            return super.translateToBedrock(itemStack, mappings.getLodestoneCompass(), mappings);
        }
        return super.translateToBedrock(itemStack, mapping, mappings);
    }

    @Override
    public ItemMapping toBedrockDefinition(CompoundTag nbt, ItemMappings mappings) {
        if (isLodestoneCompass(nbt)) {
            return mappings.getLodestoneCompass();
        }
        return super.toBedrockDefinition(nbt, mappings);
    }

    @Override
    public void translateNbtToBedrock(@NonNull GeyserSession session, @NonNull CompoundTag tag) {
        super.translateNbtToBedrock(session, tag);

        Tag lodestoneTag = tag.get("LodestoneTracked");
        if (lodestoneTag instanceof ByteTag) {
            int trackId = session.getLodestoneCache().store(tag);
            // Set the bedrock tracking id - will return 0 if invalid
            tag.put(new IntTag("trackingHandle", trackId));
        }
    }

    private boolean isLodestoneCompass(CompoundTag nbt) {
        if (nbt != null) {
            Tag lodestoneTag = nbt.get("LodestoneTracked");
            return lodestoneTag instanceof ByteTag;
        }
        return false;
    }

    @Override
    public @NonNull ItemStack translateToJava(@NonNull ItemData itemData, @NonNull ItemMapping mapping, @NonNull ItemMappings mappings) {
        if (mapping.getBedrockIdentifier().equals("minecraft:lodestone_compass")) {
            // Revert the entry back to the compass
            mapping = mappings.getStoredItems().compass();
        }

        return super.translateToJava(itemData, mapping, mappings);
    }
}
