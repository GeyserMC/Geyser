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

package org.geysermc.geyser.registry.populator;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.data.inventory.ComponentItemData;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;
import org.geysermc.geyser.item.GeyserCustomItemManager;
import org.geysermc.geyser.item.GeyserCustomRenderOffsets;
import org.geysermc.geyser.item.GeyserCustomMappingData;
import org.geysermc.geyser.item.tools.ToolBreakSpeeds;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.util.*;
import java.util.regex.Pattern;

public class CustomItemRegistryPopulator {
    private static IntSet protocolVersions = Registries.ITEMS.get().keySet();

    public static GeyserCustomMappingData populateRegistry(String baseItem, CustomItemData customItemData, int customItems) {
        String customItemName = GeyserCustomItemManager.CUSTOM_PREFIX + customItemData.name();

        GeyserCustomMappingData returnData = new GeyserCustomMappingData();
        int addNum = 0;

        for (Int2ObjectMap.Entry<ItemMappings> entry : Registries.ITEMS.get().int2ObjectEntrySet()) {
            addNum++;

            int protocolVersion = entry.getIntKey();
            ItemMappings itemMappings = entry.getValue();

            ItemMapping javaItem = itemMappings.getMapping(baseItem);
            if (javaItem == null) {
                GeyserImpl.getInstance().getLogger().error("Could not find the java item to add custom model data to for " + baseItem + " in protocol version " + protocolVersion);
                continue;
            }

            int nameExists = 0;
            for (String mappingName : itemMappings.getCustomIdMappings().values()) {
                if (Pattern.matches("^" + customItemName + "(_([0-9])+)?$", mappingName)) {
                    nameExists++;
                }
            }
            if (nameExists != 0) {
                customItemName = customItemName + "_" + nameExists; //Since we update the name outside the for loop, this won't be run on each protocol version
                GeyserImpl.getInstance().getLogger().warning("Custom item name '" + customItemData.name() + "' already exists, new item was registered as " + customItemName.substring(GeyserCustomItemManager.CUSTOM_PREFIX.length()) + "!");
            }

            int javaCustomItemId = javaItem.getJavaId();
            int customItemId = itemMappings.getItems().size() + (customItems * protocolVersions.size()) + addNum;

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

            NbtMapBuilder builder = createItemData(customItemData, javaItem, customItemName, customItemId, OptionalInt.empty(), null, false, javaItem.isTool());

            if (customItemData.registrationTypes().hasRegistrationTypes()) {
                javaItem.getCustomRegistrations().put(customItemData.registrationTypes(), customItemId);
            } else {
                GeyserImpl.getInstance().getLogger().warning("The custom item " + customItemData.name() + " has no registration types");
            }

            ComponentItemData componentItemData = new ComponentItemData(customItemName, builder.build());
            returnData.addMapping(protocolVersion, new GeyserCustomMappingData.Mapping(componentItemData, customItemMapping, startGamePacketItemEntry, customItemName, customItemId));
            itemMappings.getCustomIdMappings().put(customItemId, customItemName);
        }

        return returnData;
    }

    public static Int2ObjectMap<ComponentItemData> populateRegistry(NonVanillaCustomItemData customItemData, int customItems) {
        Int2ObjectMap<ComponentItemData> componentItemDataMap = new Int2ObjectOpenHashMap<>();
        int addNum = 0;

        for (Int2ObjectMap.Entry<ItemMappings> entry : Registries.ITEMS.get().int2ObjectEntrySet()) {
            addNum++;

            int protocolVersion = entry.getIntKey();
            ItemMappings itemMappings = entry.getValue();

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
            int customItemId = itemMappings.getItems().size() + (customItems * protocolVersions.size()) + addNum;

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
                    .hasSuspiciousStewEffect(false)
                    .build();


            NbtMapBuilder builder = createItemData(customItemData, customItemMapping, customItemData.identifier(), customItemId, customItemData.creativeCategory(), customItemData.creativeGroup(), customItemData.isHat(), customItemData.isTool());

            itemMappings.getItems().put(customItemData.javaId(), customItemMapping);
            componentItemDataMap.put(protocolVersion, new ComponentItemData(customIdentifier, builder.build()));
            itemMappings.getItemEntries().add(new StartGamePacket.ItemEntry(customIdentifier, (short) customItemId, true));

            if (customItemData.creativeGroup() != null || customItemData.creativeCategory().isPresent()) {
                int netId = itemMappings.getCreativeItems().get(itemMappings.getCreativeItems().size() - 1).getNetId() + 1;

                itemMappings.getCreativeItems().add(ItemData.builder()
                        .id(customItemId)
                        .netId(netId)
                        .count(1).build());
            }
        }

        return componentItemDataMap;
    }

    private static NbtMapBuilder createItemData(CustomItemData customItemData, ItemMapping mapping, String customItemName, int customItemId, OptionalInt creativeCategory, String creativeGroup, boolean isHat, boolean isTool) {
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
        if (creativeCategory.isPresent()) {
            itemProperties.putInt("creative_category", creativeCategory.getAsInt());
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
                                    .putCompound("first_person", xyzToNbtMap(scale3, scale3, scale3))
                                    .putCompound("third_person", xyzToNbtMap(scale1, scale2, scale1)).build())
                            .putCompound("off_hand", NbtMap.builder()
                                    .putCompound("first_person", xyzToNbtMap(scale1, scale2, scale1))
                                    .putCompound("third_person", xyzToNbtMap(scale1, scale2, scale1)).build()).build());
        }
    }

    private static void setItemTag(NbtMapBuilder builder, String tag) {
        builder.putList("item_tags", NbtType.STRING, List.of("minecraft:is_" + tag));
    }

    private static NbtMap xyzToNbtMap(float x, float y, float z) {
        return NbtMap.builder().putCompound("scale", NbtMap.builder().putFloat("x", x).putFloat("y", y).putFloat("z", z).build()).build();
    }
}
