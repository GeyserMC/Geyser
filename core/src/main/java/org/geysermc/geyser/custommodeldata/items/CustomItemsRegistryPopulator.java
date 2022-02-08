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

package org.geysermc.geyser.custommodeldata.items;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.inventory.ComponentItemData;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.custommodeldata.CustomItemData;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.populator.ItemRegistryPopulator;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.util.Map;

public class CustomItemsRegistryPopulator {
    public static void addToRegistry(String baseItem, CustomItemData customItemData) {
        if (!GeyserImpl.getInstance().getConfig().isCustomModelDataEnabled()) {
            return;
        }

        float scale1 = (float) (0.075 / (customItemData.textureSize() / 16f));
        float scale2 = (float) (0.125 / (customItemData.textureSize() / 16f));
        float scale3 = (float) (0.075 / (customItemData.textureSize() / 16f * 2.4f));

        String customItemName = "geysermc:" + customItemData.name();

        for (Map.Entry<String, ItemRegistryPopulator.PaletteVersion> palette : ItemRegistryPopulator.getPaletteVersions().entrySet()) {
            ItemMappings itemMappings = Registries.ITEMS.get(palette.getValue().protocolVersion());

            ItemMapping javaItem = itemMappings.getMapping(baseItem);
            if (javaItem == null) continue;

            int javaCustomItemId = javaItem.getJavaId();
            int customItemId = itemMappings.getItems().size() + 1;

            itemMappings.getItemEntries().add(new StartGamePacket.ItemEntry(customItemName, (short) customItemId, true));

            itemMappings.getItems().put(customItemId, ItemMapping.builder()
                    .javaIdentifier(baseItem)
                    .bedrockIdentifier(customItemName)
                    .javaId(javaCustomItemId)
                    .bedrockId(customItemId)
                    .bedrockData(0)
                    .bedrockBlockId(-1)
                    .stackSize(javaItem.getStackSize())
                    .build());

            NbtMapBuilder builder = NbtMap.builder();
            builder.putString("name", customItemName)
                    .putInt("id", customItemId);

            NbtMapBuilder itemProperties = NbtMap.builder();
            NbtMapBuilder componentBuilder = NbtMap.builder();

            itemProperties.putCompound("minecraft:icon", NbtMap.builder()
                    .putString("texture", customItemData.name())
                    .build());
            componentBuilder.putCompound("minecraft:display_name", NbtMap.builder().putString("value", customItemData.displayName()).build());

            itemProperties.putBoolean("allow_off_hand", customItemData.allowOffhand());
            itemProperties.putBoolean("hand_equipped", customItemData.isTool());
            itemProperties.putInt("max_stack_size", javaItem.getStackSize()); //Should be the same as the java base item
            if (javaItem.getMaxDamage() > 0) {
                componentBuilder.putCompound("minecraft:durability", NbtMap.builder()
                        .putInt("max_durability", javaItem.getMaxDamage())
                        .putFloat("damage_chance", 0.1f).build());
                itemProperties.putBoolean("use_duration", true);
            }

            boolean canDestroyInCreative = true;
            if (javaItem.isTool()) {
                float miningSpeed = 1.0f;
                //float toolSpeed = ToolBreakSpeeds.toolTierToSpeed(javaItem.getToolTier());

                if (baseItem.endsWith("_sword")) {
                    miningSpeed = 1.5f;
                    canDestroyInCreative = false;

                    //componentBuilder.putCompound("minecraft:digger", ToolBreakSpeeds.getSwordDigger(toolSpeed)); will crash the client
                    componentBuilder.putCompound("minecraft:weapon", NbtMap.EMPTY);
                    componentBuilder.putCompound("tag:minecraft:is_sword", NbtMap.EMPTY);
                } else if (baseItem.endsWith("_pickaxe")) {
                    //componentBuilder.putCompound("minecraft:digger", ToolBreakSpeeds.getPickaxeDigger(toolSpeed, javaItem.getToolTier())); will crash the client
                    componentBuilder.putCompound("tag:minecraft:is_pickaxe", NbtMap.EMPTY);
                } else if (baseItem.endsWith("_axe")) {
                    //componentBuilder.putCompound("minecraft:digger", ToolBreakSpeeds.getAxeDigger(toolSpeed)); will crash the client
                    componentBuilder.putCompound("tag:minecraft:is_axe", NbtMap.EMPTY);
                } else if (baseItem.endsWith("_shovel")) {
                    //componentBuilder.putCompound("minecraft:digger", ToolBreakSpeeds.getShovelDigger(toolSpeed)); will crash the client
                    componentBuilder.putCompound("tag:minecraft:is_shovel", NbtMap.EMPTY);
                } else if (baseItem.endsWith("_hoe")) {
                    componentBuilder.putCompound("tag:minecraft:is_hoe", NbtMap.EMPTY);
                }

                itemProperties.putFloat("mining_speed", miningSpeed);
            }
            itemProperties.putBoolean("can_destroy_in_creative", canDestroyInCreative);

            if (baseItem.endsWith("_boots")) {
                componentBuilder.putString("minecraft:render_offsets", "boots");
                componentBuilder.putCompound("minecraft:wearable", NbtMap.builder().putString("slot", "slot.armor.feet").build());
            } else if (baseItem.endsWith("_chestplate")) {
                componentBuilder.putString("minecraft:render_offsets", "chestplates");
                componentBuilder.putCompound("minecraft:wearable", NbtMap.builder().putString("slot", "slot.armor.chest").build());
            } else if (baseItem.endsWith("_leggings")) {
                componentBuilder.putString("minecraft:render_offsets", "leggings");
                componentBuilder.putCompound("minecraft:wearable", NbtMap.builder().putString("slot", "slot.armor.legs").build());
            } else if (baseItem.endsWith("_helmet") || customItemData.isHat()) {
                componentBuilder.putString("minecraft:render_offsets", "helmets");
                componentBuilder.putCompound("minecraft:wearable", NbtMap.builder().putString("slot", "slot.armor.head").build());
            } else if (customItemData.textureSize() != 16) {
                componentBuilder.putCompound("minecraft:render_offsets",
                        NbtMap.builder().putCompound("main_hand", NbtMap.builder()
                                        .putCompound("first_person", XYZToNbtMap(scale3, scale3, scale3))
                                        .putCompound("third_person", XYZToNbtMap(scale1, scale2, scale1)).build())
                                .putCompound("off_hand", NbtMap.builder()
                                        .putCompound("first_person", XYZToNbtMap(scale1, scale2, scale1))
                                        .putCompound("third_person", XYZToNbtMap(scale1, scale2, scale1)).build()).build());
            }

            componentBuilder.putCompound("item_properties", itemProperties.build());
            builder.putCompound("components", componentBuilder.build());

            javaItem.getCustomModelData().put(customItemData.customModelData().intValue(), customItemId);
            itemMappings.getCustomItemsData().add(new ComponentItemData(customItemName, builder.build()));
        }
    }

    private static NbtMap XYZToNbtMap(float x, float y, float z) {
        return NbtMap.builder().putCompound("scale", NbtMap.builder().putFloat("x", x).putFloat("y", y).putFloat("z", z).build()).build();
    }
}
