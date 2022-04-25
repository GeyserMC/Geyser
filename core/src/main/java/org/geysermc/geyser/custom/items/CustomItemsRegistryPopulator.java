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
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.v475.Bedrock_v475;
import com.nukkitx.protocol.bedrock.v486.Bedrock_v486;
import com.nukkitx.protocol.bedrock.v503.Bedrock_v503;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.custom.items.CustomItemData;
import org.geysermc.geyser.api.custom.items.FullyCustomItemData;
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
            int customItemId = itemMappings.getItems().size() + customItems + addNum;

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

            NbtMapBuilder builder = createItemData(customItemData, javaItem, customItemName, customItemId, null, null, false, javaItem.isTool());

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

    public static Int2ObjectMap<ComponentItemData> addToRegistry(FullyCustomItemData customItemData, int customItems) {
        Int2ObjectMap<ComponentItemData> componentItemDataMap = new Int2ObjectOpenHashMap<>();
        int addNum = 0;

        for (int protocolVersion : protocolVersions) {
            addNum++;

            ItemMappings itemMappings = Registries.ITEMS.get(protocolVersion);
            if (itemMappings == null) {
                GeyserImpl.getInstance().getLogger().error("Could not find item mappings for protocol version " + protocolVersion);
                continue;
            }

            boolean exists = false;
            for (ItemMapping mapping : itemMappings.getItems().values()) {
                if (mapping.getJavaIdentifier().equals(customItemData.identifier()) || mapping.getBedrockIdentifier().equals(customItemData.identifier()) || mapping.getJavaId() == customItemData.javaId()) {
                    exists = true;
                    break;
                }
            }
            if (exists) {
                GeyserImpl.getInstance().getLogger().error("The custom item " + customItemData.identifier() + " is overwriting an existing item in protocol version " + protocolVersion +"! Could not register it.");
                continue;
            }

            String customIdentifier = customItemData.identifier();
            int customItemId = itemMappings.getItems().size() + customItems + addNum;

            ItemMapping customItemMapping = ItemMapping.builder()
                    .javaIdentifier(customIdentifier)
                    .bedrockIdentifier(customIdentifier)
                    .javaId(customItemData.javaId())
                    .bedrockId(customItemId)
                    .bedrockData(0)
                    .bedrockBlockId(0)
                    .stackSize(customItemData.stackSize())
                    .toolType(customItemData.toolType())
                    .toolTier(customItemData.toolTier())
                    .armorType(customItemData.armorType())
                    .armorTier(customItemData.armorTier())
                    .protectionValue(customItemData.protectionValue())
                    .translationString(customItemData.translationString())
                    .maxDamage(customItemData.maxDamage())
                    .repairMaterials(customItemData.repairMaterials())
                    .hasSuspiciousStewEffect(customItemData.hasSuspiciousStewEffect())
                    .build();


            NbtMapBuilder builder = createItemData(customItemData, customItemMapping, customItemData.identifier(), customItemId, customItemData.creativeCategory(), customItemData.creativeGroup(), customItemData.isHat(), customItemData.isTool());

            itemMappings.getItems().put(customItemData.javaId(), customItemMapping);
            componentItemDataMap.put(protocolVersion, new ComponentItemData(customIdentifier, builder.build()));
            itemMappings.getItemEntries().add(new StartGamePacket.ItemEntry(customIdentifier, (short) customItemId, true));

            if (customItemData.creativeGroup() != null || customItemData.creativeCategory() != null) {
                int netId = itemMappings.getCreativeItems().get(itemMappings.getCreativeItems().size() - 1).getNetId() + 1;

                itemMappings.getCreativeItems().add(ItemData.builder()
                        .id(customItemId)
                        .netId(netId)
                        .count(1).build());
            }
        }

        return componentItemDataMap;
    }

    private static NbtMapBuilder createItemData(CustomItemData customItemData, ItemMapping mapping, String customItemName, int customItemId, Integer creativeCategory, String creativeGroup, boolean isHat, boolean isTool) {
        NbtMapBuilder builder = NbtMap.builder();
        builder.putString("name", customItemName)
                .putInt("id", customItemId);

        NbtMapBuilder itemProperties = NbtMap.builder();
        NbtMapBuilder componentBuilder = NbtMap.builder();

        setupBasicItemInfo(mapping.getMaxDamage(), mapping.getStackSize(), isTool, customItemData, itemProperties, componentBuilder);

        boolean canDestroyInCreative = true;
        if (mapping.isTool()) { // This is not using the isTool boolean because it is not just a render type here.
            canDestroyInCreative = computeToolProperties(mapping.getToolTier(), mapping.getToolType(), itemProperties, componentBuilder);
        }
        itemProperties.putBoolean("can_destroy_in_creative", canDestroyInCreative);

        if (mapping.isArmor()) {
            computeArmorProperties(mapping.getArmorType(), mapping.getProtectionValue(), componentBuilder);
        }

        computeRenderOffsets(isHat, customItemData, componentBuilder);

        if (creativeGroup != null) {
            itemProperties.putString("creative_group", creativeGroup);
        }
        if (creativeCategory != null) {
            itemProperties.putInt("creative_category", creativeCategory);
        }

        componentBuilder.putCompound("item_properties", itemProperties.build());
        builder.putCompound("components", componentBuilder.build());

        return builder;
    }

    private static void setupBasicItemInfo(int maxDamage, int stackSize, boolean isTool, CustomItemData customItemData, NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder) {
        itemProperties.putCompound("minecraft:icon", NbtMap.builder()
                .putString("texture", customItemData.name())
                .build());
        componentBuilder.putCompound("minecraft:display_name", NbtMap.builder().putString("value", customItemData.displayName()).build());

        itemProperties.putBoolean("allow_off_hand", customItemData.allowOffhand());
        itemProperties.putBoolean("hand_equipped", isTool);
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

    private static void computeRenderOffsets(boolean isHat, CustomItemData customItemData, NbtMapBuilder componentBuilder) {
        if (isHat) {
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
