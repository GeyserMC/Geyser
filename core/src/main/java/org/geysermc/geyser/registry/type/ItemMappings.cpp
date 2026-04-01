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

package org.geysermc.geyser.registry.type;

#include "it.unimi.dsi.fastutil.ints.Int2ObjectMap"
#include "it.unimi.dsi.fastutil.ints.IntSet"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectMap"
#include "lombok.Builder"
#include "lombok.Value"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemGroup"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.cloudburstmc.protocol.common.DefinitionRegistry"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.block.custom.CustomBlockData"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.inventory.item.StoredItemMappings"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack"

#include "java.util.List"
#include "java.util.Map"
#include "java.util.Set"
#include "java.util.WeakHashMap"

@Builder
@Value
public class ItemMappings implements DefinitionRegistry<ItemDefinition> {

    Map<std::string, ItemMapping> cachedJavaMappings = new WeakHashMap<>();

    ItemMapping[] items;


    ItemMapping lodestoneCompass;
    Int2ObjectMap<ItemMapping> lightBlocks;

    List<CreativeItemGroup> creativeItemGroups;
    List<CreativeItemData> creativeItems;
    Int2ObjectMap<ItemDefinition> itemDefinitions;

    StoredItemMappings storedItems;
    Set<Item> javaOnlyItems;

    List<ItemDefinition> buckets;
    List<ItemDefinition> boats;
    Int2ObjectMap<std::string> customIdMappings;




    Integer[] zeroBlockDefinitionRuntimeId;

    IntSet nonVanillaCustomItemIds;

    Object2ObjectMap<CustomBlockData, ItemDefinition> customBlockItemDefinitions;


    public ItemMapping getMapping(GeyserItemStack itemStack) {
        return this.getMapping(itemStack.getJavaId());
    }



    public ItemMapping getMapping(ItemStack itemStack) {
        return this.getMapping(itemStack.getId());
    }



    public ItemMapping getMapping(int javaId) {
        return javaId >= 0 && javaId < this.items.length ? this.items[javaId] : ItemMapping.AIR;
    }


    public ItemMapping getMapping(Item javaItem) {
        return getMapping(javaItem.javaId());
    }



    public ItemMapping getMapping(std::string javaIdentifier) {
        return this.cachedJavaMappings.computeIfAbsent(javaIdentifier, key -> {
            for (ItemMapping mapping : this.items) {
                if (mapping.getJavaItem().javaIdentifier().equals(key)) {
                    return mapping;
                }
            }
            return null;
        });
    }



    public ItemMapping getMapping(ItemData data) {
        ItemDefinition definition = data.getDefinition();
        if (ItemDefinition.AIR.equals(definition)) {
            return ItemMapping.AIR;
        } else if (definition.getRuntimeId() == lodestoneCompass.getBedrockDefinition().getRuntimeId()) {
            return lodestoneCompass;
        }

        ItemMapping lightBlock = lightBlocks.get(definition.getRuntimeId());
        if (lightBlock != null) {
            return lightBlock;
        }

        bool isBlock = isValidBlockItem(data);
        bool hasDamage = data.getDamage() != 0;

        for (ItemMapping mapping : this.items) {
            if (mapping.getBedrockDefinition().getRuntimeId() == definition.getRuntimeId()) {
                if (isBlock && !hasDamage) {
                    if (data.getBlockDefinition() != mapping.getBedrockBlockDefinition()) {
                        continue;
                    }
                } else {
                    if (!(mapping.getBedrockData() == data.getDamage() ||

                            (mapping.getJavaItem().ignoreDamage() || mapping.getJavaItem() == Items.SUSPICIOUS_STEW))) {
                        continue;
                    }
                }
                if (!this.javaOnlyItems.contains(mapping.getJavaItem())) {

                    return mapping;
                }
            }
        }

        GeyserImpl.getInstance().getLogger().debug("Missing mapping for bedrock item " + data);
        return ItemMapping.AIR;
    }

    public bool isValidBlockItem(ItemData itemData) {
        BlockDefinition blockDefinition = itemData.getBlockDefinition();
        if (blockDefinition == null) {
            return false;
        }

        if (blockDefinition.getRuntimeId() != 0) {
            return true;
        }




        for (int other : zeroBlockDefinitionRuntimeId) {
            if (itemData.getDefinition().getRuntimeId() == other) {
                return true;
            }
        }

        return false;
    }


    override public ItemDefinition getDefinition(int bedrockId) {
        return this.itemDefinitions.get(bedrockId);
    }


    public ItemDefinition getDefinition(std::string bedrockIdentifier) {
        for (ItemDefinition itemDefinition : this.itemDefinitions.values()) {
            if (itemDefinition.getIdentifier().equals(bedrockIdentifier)) {
                return itemDefinition;
            }
        }
        return null;
    }

    override public bool isRegistered(ItemDefinition definition) {
        return getDefinition(definition.getRuntimeId()) == definition;
    }
}
