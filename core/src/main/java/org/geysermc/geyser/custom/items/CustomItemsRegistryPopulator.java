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

package org.geysermc.geyser.custom.items;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.inventory.ComponentItemData;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.v475.Bedrock_v475;
import com.nukkitx.protocol.bedrock.v486.Bedrock_v486;
import com.nukkitx.protocol.bedrock.v503.Bedrock_v503;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.custom.items.CustomItemData;
import org.geysermc.geyser.custom.GeyserCustomManager;
import org.geysermc.geyser.custom.GeyserCustomRenderOffsets;
import org.geysermc.geyser.custom.items.tools.ToolBreakSpeeds;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.util.*;

class CustomItemsRegistryPopulator {
    private static List<Integer> protocolVersions;

    static {
        protocolVersions = new ArrayList<>();
        protocolVersions.add(Bedrock_v475.V475_CODEC.getProtocolVersion());
        protocolVersions.add(Bedrock_v486.V486_CODEC.getProtocolVersion());
        protocolVersions.add(Bedrock_v503.V503_CODEC.getProtocolVersion());
    }

    public static GeyserCustomItemData addToRegistry(String baseItem, CustomItemData customItemData, int nameExists, int customItems) {
        String customItemName = GeyserCustomManager.CUSTOM_PREFIX + customItemData.name();
        if (nameExists != 0) {
            customItemName += "_" + nameExists;
        }

        GeyserCustomItemData returnData = new GeyserCustomItemData();
        int addNum = 0;

        for (int protocolVersion : protocolVersions) {
            addNum++;

            ItemMappings itemMappings = Registries.ITEMS.get(protocolVersion);
            if (itemMappings == null) {
                continue;
            }
            ItemMapping javaItem = itemMappings.getMapping(baseItem);
            if (javaItem == null) {
                continue;
            }

            int javaCustomItemId = javaItem.getJavaId();
            int customItemId = itemMappings.getItems().length + customItems + addNum;

            ItemMapping customItemMapping = ItemMapping.builder()
                    .javaIdentifier(baseItem)
                    .bedrockIdentifier(customItemName)
                    .javaId(javaCustomItemId)
                    .bedrockId(customItemId)
                    .bedrockData(0)
                    .bedrockBlockId(-1)
                    .stackSize(javaItem.getStackSize())
                    .build();

            StartGamePacket.ItemEntry startGamePacketItemEntry = new StartGamePacket.ItemEntry(customItemName, (short) customItemId, true);

            NbtMapBuilder builder = NbtMap.builder();
            builder.putString("name", customItemName)
                    .putInt("id", customItemId);

            NbtMapBuilder itemProperties = NbtMap.builder();
            NbtMapBuilder componentBuilder = NbtMap.builder();

            setupBasicItemInfo(javaItem.getMaxDamage(), javaItem.getStackSize(), customItemData, itemProperties, componentBuilder);

            boolean canDestroyInCreative = true;
            if (javaItem.isTool()) {
                canDestroyInCreative = computeToolProperties(javaItem.getToolTier(), javaItem.getToolType(), itemProperties, componentBuilder);
            }
            itemProperties.putBoolean("can_destroy_in_creative", canDestroyInCreative);

            if (javaItem.isArmor()) {
                computeArmorProperties(javaItem.getArmorType(), javaItem.getProtectionValue(), componentBuilder);
            }

            computeRenderOffsets(customItemData, componentBuilder);

            componentBuilder.putCompound("item_properties", itemProperties.build());
            builder.putCompound("components", componentBuilder.build());

            if (customItemData.registrationTypes().hasRegistrationTypes()) {
                javaItem.getCustomRegistrations().put(customItemData.registrationTypes(), customItemId);
            } else {
                GeyserImpl.getInstance().getLogger().warning("The custom item " + customItemData.name() + " has no registration types");
            }

            ComponentItemData componentItemData = new ComponentItemData(customItemName, builder.build());
            returnData.addMapping(protocolVersion, new GeyserCustomItemData.Mapping(componentItemData, customItemMapping, startGamePacketItemEntry, customItemName, customItemId));
        }

        return returnData;
    }

    private static void setupBasicItemInfo(int maxDamage, int stackSize, CustomItemData customItemData, NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder) {
        itemProperties.putCompound("minecraft:icon", NbtMap.builder()
                .putString("texture", customItemData.name())
                .build());
        componentBuilder.putCompound("minecraft:display_name", NbtMap.builder().putString("value", customItemData.displayName()).build());

        itemProperties.putBoolean("allow_off_hand", customItemData.allowOffhand());
        itemProperties.putBoolean("hand_equipped", customItemData.isTool());
        itemProperties.putInt("max_stack_size", stackSize);
        if (maxDamage > 0) {
            componentBuilder.putCompound("minecraft:durability", NbtMap.builder()
                    .putInt("max_durability", maxDamage)
                    .putFloat("damage_chance", 0.1f).build());
            itemProperties.putBoolean("use_duration", true);
        }
    }

    /**
     * @return can destroy in creative
     */
    private static boolean computeToolProperties(String toolTier, String toolType, NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder) {
        boolean canDestroyInCreative = true;
        float miningSpeed = 1.0f;
        int toolSpeed = ToolBreakSpeeds.toolTierToSpeed(toolTier);

        switch (toolType) {
            case "sword" -> {
                miningSpeed = 1.5f;
                canDestroyInCreative = false;
                componentBuilder.putCompound("minecraft:digger", ToolBreakSpeeds.getSwordDigger(toolSpeed));
                componentBuilder.putCompound("minecraft:weapon", NbtMap.EMPTY);
            }
            case "pickaxe" -> {
                componentBuilder.putCompound("minecraft:digger", ToolBreakSpeeds.getPickaxeDigger(toolSpeed, toolTier));
                setItemTag(componentBuilder, "pickaxe");
            }
            case "axe" -> {
                componentBuilder.putCompound("minecraft:digger", ToolBreakSpeeds.getAxeDigger(toolSpeed));
                setItemTag(componentBuilder, "axe");
            }
            case "shovel" -> {
                componentBuilder.putCompound("minecraft:digger", ToolBreakSpeeds.getShovelDigger(toolSpeed));
                setItemTag(componentBuilder, "shovel");
            }
            case "hoe" -> setItemTag(componentBuilder, "hoe");
        }

        itemProperties.putBoolean("hand_equipped", true);
        itemProperties.putFloat("mining_speed", miningSpeed);

        return canDestroyInCreative;
    }

    private static void computeArmorProperties(String armorType, int protectionValue, NbtMapBuilder componentBuilder) {
        switch (armorType) {
            case "boots" -> {
                componentBuilder.putString("minecraft:render_offsets", "boots");
                componentBuilder.putCompound("minecraft:wearable", NbtMap.builder().putString("slot", "slot.armor.feet").build());
                componentBuilder.putCompound("minecraft:armor", NbtMap.builder().putInt("protection", protectionValue).build());
            }
            case "chestplate" -> {
                componentBuilder.putString("minecraft:render_offsets", "chestplates");
                componentBuilder.putCompound("minecraft:wearable", NbtMap.builder().putString("slot", "slot.armor.chest").build());
                componentBuilder.putCompound("minecraft:armor", NbtMap.builder().putInt("protection", protectionValue).build());
            }
            case "leggings" -> {
                componentBuilder.putString("minecraft:render_offsets", "leggings");
                componentBuilder.putCompound("minecraft:wearable", NbtMap.builder().putString("slot", "slot.armor.legs").build());
                componentBuilder.putCompound("minecraft:armor", NbtMap.builder().putInt("protection", protectionValue).build());
            }
            case "helmet" -> {
                componentBuilder.putString("minecraft:render_offsets", "helmets");
                componentBuilder.putCompound("minecraft:wearable", NbtMap.builder().putString("slot", "slot.armor.head").build());
                componentBuilder.putCompound("minecraft:armor", NbtMap.builder().putInt("protection", protectionValue).build());
            }
        }
    }

    private static void computeRenderOffsets(CustomItemData customItemData, NbtMapBuilder componentBuilder) {
        if (customItemData.isHat()) {
            componentBuilder.remove("minecraft:render_offsets");
            componentBuilder.putString("minecraft:render_offsets", "helmets");

            componentBuilder.remove("minecraft:wearable");
            componentBuilder.putCompound("minecraft:wearable", NbtMap.builder().putString("slot", "slot.armor.head").build());
        }

        if (customItemData.renderOffsets() != null) {
            GeyserCustomRenderOffsets renderOffsets = GeyserCustomRenderOffsets.fromCustomRenderOffsets(customItemData.renderOffsets());
            if (renderOffsets != null) {
                componentBuilder.remove("minecraft:render_offsets");
                componentBuilder.putCompound("minecraft:render_offsets", renderOffsets.toNbtMap());
            }
        } else if (customItemData.textureSize() != 16 && !componentBuilder.containsKey("minecraft:render_offsets")) {
            float scale1 = (float) (0.075 / (customItemData.textureSize() / 16f));
            float scale2 = (float) (0.125 / (customItemData.textureSize() / 16f));
            float scale3 = (float) (0.075 / (customItemData.textureSize() / 16f * 2.4f));

            componentBuilder.putCompound("minecraft:render_offsets",
                    NbtMap.builder().putCompound("main_hand", NbtMap.builder()
                                    .putCompound("first_person", XYZToNbtMap(scale3, scale3, scale3))
                                    .putCompound("third_person", XYZToNbtMap(scale1, scale2, scale1)).build())
                            .putCompound("off_hand", NbtMap.builder()
                                    .putCompound("first_person", XYZToNbtMap(scale1, scale2, scale1))
                                    .putCompound("third_person", XYZToNbtMap(scale1, scale2, scale1)).build()).build());
        }
    }

    private static void setItemTag(NbtMapBuilder builder, String tag) {
        builder.putList("item_tags", NbtType.STRING, List.of("minecraft:is_" + tag));
    }

    private static NbtMap XYZToNbtMap(float x, float y, float z) {
        return NbtMap.builder().putCompound("scale", NbtMap.builder().putFloat("x", x).putFloat("y", y).putFloat("z", z).build()).build();
    }
}
