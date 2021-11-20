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

package org.geysermc.geyser.registry.type;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.nukkitx.protocol.bedrock.data.inventory.ComponentItemData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.Builder;
import lombok.Value;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.item.StoredItemMappings;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Builder
@Value
public class ItemMappings {

    Map<String, ItemMapping> cachedJavaMappings = new WeakHashMap<>();

    Int2ObjectMap<ItemMapping> items;

    ItemData[] creativeItems;
    List<StartGamePacket.ItemEntry> itemEntries;

    StoredItemMappings storedItems;
    String[] itemNames;
    Set<String> javaOnlyItems;

    IntList bucketIds;
    IntList boatIds;
    IntList spawnEggIds;
    List<ItemData> carpets;

    @Nullable ComponentItemData furnaceMinecartData;

    /**
     * Gets an {@link ItemMapping} from the given {@link ItemStack}.
     *
     * @param itemStack the itemstack
     * @return an item entry from the given java edition identifier
     */
    public ItemMapping getMapping(ItemStack itemStack) {
        return this.getMapping(itemStack.getId());
    }

    /**
     * Gets an {@link ItemMapping} from the given Minecraft: Java
     * Edition id.
     *
     * @param javaId the id
     * @return an item entry from the given java edition identifier
     */
    public ItemMapping getMapping(int javaId) {
        return this.items.get(javaId);
    }

    /**
     * Gets an {@link ItemMapping} from the given Minecraft: Java Edition
     * block state identifier.
     *
     * @param javaIdentifier the block state identifier
     * @return an item entry from the given java edition identifier
     */
    public ItemMapping getMapping(String javaIdentifier) {
        return this.cachedJavaMappings.computeIfAbsent(javaIdentifier, key -> {
            for (ItemMapping mapping : this.items.values()) {
                if (mapping.getJavaIdentifier().equals(key)) {
                    return mapping;
                }
            }
            return null;
        });
    }

    /**
     * Gets an {@link ItemMapping} from the given {@link ItemData}.
     *
     * @param data the item data
     * @return an item entry from the given item data
     */
    public ItemMapping getMapping(ItemData data) {
        boolean isBlock = data.getBlockRuntimeId() != 0;
        boolean hasDamage = data.getDamage() != 0;

        for (ItemMapping mapping : this.items.values()) {
            if (mapping.getBedrockId() == data.getId()) {
                if (isBlock && !hasDamage) { // Pre-1.16.220 will not use block runtime IDs at all, so we shouldn't check either
                    if (data.getBlockRuntimeId() != mapping.getBedrockBlockId()) {
                        continue;
                    }
                } else {
                    if (!(mapping.getBedrockData() == data.getDamage() ||
                            // Make exceptions for potions, tipped arrows, and firework stars, whose damage values can vary
                            (mapping.getJavaIdentifier().endsWith("potion") || mapping.getJavaIdentifier().equals("minecraft:arrow")
                                    || mapping.getJavaIdentifier().equals("minecraft:firework_star")))) {
                        continue;
                    }
                }
                if (!this.javaOnlyItems.contains(mapping.getJavaIdentifier())) {
                    // From a Bedrock item data, we aren't getting one of these items
                    return mapping;
                }
            }
        }

        // This will hide the message when the player clicks with an empty hand
        if (data.getId() != 0 && data.getDamage() != 0) {
            GeyserImpl.getInstance().getLogger().debug("Missing mapping for bedrock item " + data.getId() + ":" + data.getDamage());
        }
        return ItemMapping.AIR;
    }
}
