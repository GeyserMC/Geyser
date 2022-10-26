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
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomRenderOffsets;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.item.GeyserCustomMappingData;
import org.geysermc.geyser.item.components.ToolBreakSpeedsUtils;
import org.geysermc.geyser.item.components.WearableSlot;
import org.geysermc.geyser.registry.type.GeyserMappingItem;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.NonVanillaItemRegistration;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public class CustomItemRegistryPopulator {
    public static GeyserCustomMappingData registerCustomItem(String customItemName, GeyserMappingItem javaItem, CustomItemData customItemData, int bedrockId) {
        StartGamePacket.ItemEntry startGamePacketItemEntry = new StartGamePacket.ItemEntry(customItemName, (short) bedrockId, true);

        NbtMapBuilder builder = createComponentNbt(customItemData, javaItem, customItemName, bedrockId);
        ComponentItemData componentItemData = new ComponentItemData(customItemName, builder.build());

        return new GeyserCustomMappingData(componentItemData, startGamePacketItemEntry, customItemName, bedrockId);
    }

    static boolean initialCheck(String identifier, CustomItemData item, Map<String, GeyserMappingItem> mappings) {
        if (!mappings.containsKey(identifier)) {
            GeyserImpl.getInstance().getLogger().error("Could not find the Java item to add custom item properties to for " + item.name());
            return false;
        }
        if (!item.customItemOptions().hasCustomItemOptions()) {
            GeyserImpl.getInstance().getLogger().error("The custom item " + item.name() + " has no registration types");
        }
        String name = item.name();
        if (name.isEmpty()) {
            GeyserImpl.getInstance().getLogger().warning("Custom item name is empty?");
        } else if (Character.isDigit(name.charAt(0))) {
            // As of 1.19.31
            GeyserImpl.getInstance().getLogger().warning("Custom item name (" + name + ") begins with a digit. This may cause issues!");
        }
        return true;
    }

    public static NonVanillaItemRegistration registerCustomItem(NonVanillaCustomItemData customItemData, int customItemId) {
        String customIdentifier = customItemData.identifier();

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
                .translationString(customItemData.translationString())
                .maxDamage(customItemData.maxDamage())
                .repairMaterials(customItemData.repairMaterials())
                .hasSuspiciousStewEffect(false)
                .customItemOptions(Collections.emptyList())
                .build();

        NbtMapBuilder builder = createComponentNbt(customItemData, customItemData.identifier(), customItemId,
                customItemData.creativeCategory(), customItemData.creativeGroup(), customItemData.isHat(), customItemData.isTool());
        ComponentItemData componentItemData = new ComponentItemData(customIdentifier, builder.build());

        return new NonVanillaItemRegistration(componentItemData, customItemMapping);
    }

    private static NbtMapBuilder createComponentNbt(CustomItemData customItemData, GeyserMappingItem mapping,
                                                    String customItemName, int customItemId) {
        NbtMapBuilder builder = NbtMap.builder();
        builder.putString("name", customItemName)
                .putInt("id", customItemId);

        NbtMapBuilder itemProperties = NbtMap.builder();
        NbtMapBuilder componentBuilder = NbtMap.builder();

        setupBasicItemInfo(mapping.getMaxDamage(), mapping.getStackSize(), mapping.getToolType() != null, customItemData, itemProperties, componentBuilder);

        boolean canDestroyInCreative = true;
        if (mapping.getToolType() != null) { // This is not using the isTool boolean because it is not just a render type here.
            canDestroyInCreative = computeToolProperties(mapping.getToolTier(), mapping.getToolType(), itemProperties, componentBuilder);
        }
        itemProperties.putBoolean("can_destroy_in_creative", canDestroyInCreative);

        if (mapping.getArmorType() != null) {
            computeArmorProperties(mapping.getArmorType(), mapping.getProtectionValue(), componentBuilder);
        }

        computeRenderOffsets(false, customItemData, componentBuilder);

        componentBuilder.putCompound("item_properties", itemProperties.build());
        builder.putCompound("components", componentBuilder.build());

        return builder;
    }

    private static NbtMapBuilder createComponentNbt(NonVanillaCustomItemData customItemData, String customItemName,
                                                    int customItemId, OptionalInt creativeCategory,
                                                    String creativeGroup, boolean isHat, boolean isTool) {
        NbtMapBuilder builder = NbtMap.builder();
        builder.putString("name", customItemName)
                .putInt("id", customItemId);

        NbtMapBuilder itemProperties = NbtMap.builder();
        NbtMapBuilder componentBuilder = NbtMap.builder();

        setupBasicItemInfo(customItemData.maxDamage(), customItemData.stackSize(), isTool, customItemData, itemProperties, componentBuilder);

        boolean canDestroyInCreative = true;
        if (customItemData.toolType() != null) { // This is not using the isTool boolean because it is not just a render type here.
            canDestroyInCreative = computeToolProperties(customItemData.toolTier(), customItemData.toolType(), itemProperties, componentBuilder);
        }
        itemProperties.putBoolean("can_destroy_in_creative", canDestroyInCreative);

        String armorType = customItemData.armorType();
        if (armorType != null) {
            computeArmorProperties(armorType, customItemData.protectionValue(), componentBuilder);
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
                .putString("texture", customItemData.icon())
                .build());
        componentBuilder.putCompound("minecraft:display_name", NbtMap.builder().putString("value", customItemData.displayName()).build());

        itemProperties.putBoolean("allow_off_hand", customItemData.allowOffhand());
        itemProperties.putBoolean("hand_equipped", isTool);
        itemProperties.putInt("max_stack_size", stackSize);
        // Ignore durability if the item's predicate requires that it be unbreakable
        if (maxDamage > 0 && customItemData.customItemOptions().unbreakable() != TriState.TRUE) {
            componentBuilder.putCompound("minecraft:durability", NbtMap.builder()
                    .putCompound("damage_chance", NbtMap.builder()
                            .putInt("max", 1)
                            .putInt("min", 1)
                            .build())
                    .putInt("max_durability", maxDamage)
                    .build());
            itemProperties.putBoolean("use_duration", true);
        }
    }

    /**
     * @return can destroy in creative
     */
    private static boolean computeToolProperties(String toolTier, String toolType, NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder) {
        boolean canDestroyInCreative = true;
        float miningSpeed = 1.0f;

        if (toolType.equals("shears")) {
            componentBuilder.putCompound("minecraft:digger", ToolBreakSpeedsUtils.getShearsDigger(15));
        } else {
            int toolSpeed = ToolBreakSpeedsUtils.toolTierToSpeed(toolTier);
            switch (toolType) {
                case "sword" -> {
                    miningSpeed = 1.5f;
                    canDestroyInCreative = false;
                    componentBuilder.putCompound("minecraft:digger", ToolBreakSpeedsUtils.getSwordDigger(toolSpeed));
                    componentBuilder.putCompound("minecraft:weapon", NbtMap.EMPTY);
                }
                case "pickaxe" -> {
                    componentBuilder.putCompound("minecraft:digger", ToolBreakSpeedsUtils.getPickaxeDigger(toolSpeed, toolTier));
                    setItemTag(componentBuilder, "pickaxe");
                }
                case "axe" -> {
                    componentBuilder.putCompound("minecraft:digger", ToolBreakSpeedsUtils.getAxeDigger(toolSpeed));
                    setItemTag(componentBuilder, "axe");
                }
                case "shovel" -> {
                    componentBuilder.putCompound("minecraft:digger", ToolBreakSpeedsUtils.getShovelDigger(toolSpeed));
                    setItemTag(componentBuilder, "shovel");
                }
                case "hoe" -> {
                    componentBuilder.putCompound("minecraft:digger", ToolBreakSpeedsUtils.getHoeDigger(toolSpeed));
                    setItemTag(componentBuilder, "hoe");
                }
            }
        }

        itemProperties.putBoolean("hand_equipped", true);
        itemProperties.putFloat("mining_speed", miningSpeed);

        return canDestroyInCreative;
    }

    private static void computeArmorProperties(String armorType, int protectionValue, NbtMapBuilder componentBuilder) {
        switch (armorType) {
            case "boots" -> {
                componentBuilder.putString("minecraft:render_offsets", "boots");
                componentBuilder.putCompound("minecraft:wearable", WearableSlot.FEET.getSlotNbt());
                componentBuilder.putCompound("minecraft:armor", NbtMap.builder().putInt("protection", protectionValue).build());
            }
            case "chestplate" -> {
                componentBuilder.putString("minecraft:render_offsets", "chestplates");
                componentBuilder.putCompound("minecraft:wearable", WearableSlot.CHEST.getSlotNbt());
                componentBuilder.putCompound("minecraft:armor", NbtMap.builder().putInt("protection", protectionValue).build());
            }
            case "leggings" -> {
                componentBuilder.putString("minecraft:render_offsets", "leggings");
                componentBuilder.putCompound("minecraft:wearable", WearableSlot.LEGS.getSlotNbt());
                componentBuilder.putCompound("minecraft:armor", NbtMap.builder().putInt("protection", protectionValue).build());
            }
            case "helmet" -> {
                componentBuilder.putString("minecraft:render_offsets", "helmets");
                componentBuilder.putCompound("minecraft:wearable", WearableSlot.HEAD.getSlotNbt());
                componentBuilder.putCompound("minecraft:armor", NbtMap.builder().putInt("protection", protectionValue).build());
            }
        }
    }

    private static void computeRenderOffsets(boolean isHat, CustomItemData customItemData, NbtMapBuilder componentBuilder) {
        if (isHat) {
            componentBuilder.remove("minecraft:render_offsets");
            componentBuilder.putString("minecraft:render_offsets", "helmets");

            componentBuilder.remove("minecraft:wearable");
            componentBuilder.putCompound("minecraft:wearable", WearableSlot.HEAD.getSlotNbt());
        }

        CustomRenderOffsets renderOffsets = customItemData.renderOffsets();
        if (renderOffsets != null) {
            componentBuilder.remove("minecraft:render_offsets");
            componentBuilder.putCompound("minecraft:render_offsets", toNbtMap(renderOffsets));
        } else if (customItemData.textureSize() != 16 && !componentBuilder.containsKey("minecraft:render_offsets")) {
            float scale1 = (float) (0.075 / (customItemData.textureSize() / 16f));
            float scale2 = (float) (0.125 / (customItemData.textureSize() / 16f));
            float scale3 = (float) (0.075 / (customItemData.textureSize() / 16f * 2.4f));

            componentBuilder.putCompound("minecraft:render_offsets",
                    NbtMap.builder().putCompound("main_hand", NbtMap.builder()
                                    .putCompound("first_person", xyzToScaleList(scale3, scale3, scale3))
                                    .putCompound("third_person", xyzToScaleList(scale1, scale2, scale1)).build())
                            .putCompound("off_hand", NbtMap.builder()
                                    .putCompound("first_person", xyzToScaleList(scale1, scale2, scale1))
                                    .putCompound("third_person", xyzToScaleList(scale1, scale2, scale1)).build()).build());
        }
    }

    private static NbtMap toNbtMap(CustomRenderOffsets renderOffsets) {
        NbtMapBuilder builder = NbtMap.builder();

        CustomRenderOffsets.Hand mainHand = renderOffsets.mainHand();
        if (mainHand != null) {
            NbtMap nbt = toNbtMap(mainHand);
            if (nbt != null) {
                builder.putCompound("main_hand", nbt);
            }
        }
        CustomRenderOffsets.Hand offhand = renderOffsets.offhand();
        if (offhand != null) {
            NbtMap nbt = toNbtMap(offhand);
            if (nbt != null) {
                builder.putCompound("off_hand", nbt);
            }
        }

        return builder.build();
    }

    private static NbtMap toNbtMap(CustomRenderOffsets.Hand hand) {
        NbtMap firstPerson = toNbtMap(hand.firstPerson());
        NbtMap thirdPerson = toNbtMap(hand.thirdPerson());

        if (firstPerson == null && thirdPerson == null) {
            return null;
        }

        NbtMapBuilder builder = NbtMap.builder();
        if (firstPerson != null) {
            builder.putCompound("first_person", firstPerson);
        }
        if (thirdPerson != null) {
            builder.putCompound("third_person", thirdPerson);
        }

        return builder.build();
    }

    private static NbtMap toNbtMap(@Nullable CustomRenderOffsets.Offset offset) {
        if (offset == null) {
            return null;
        }

        CustomRenderOffsets.OffsetXYZ position = offset.position();
        CustomRenderOffsets.OffsetXYZ rotation = offset.rotation();
        CustomRenderOffsets.OffsetXYZ scale = offset.scale();

        if (position == null && rotation == null && scale == null) {
            return null;
        }

        NbtMapBuilder builder = NbtMap.builder();
        if (position != null) {
            builder.putList("position", NbtType.FLOAT, toList(position));
        }
        if (rotation != null) {
            builder.putList("rotation", NbtType.FLOAT, toList(rotation));
        }
        if (scale != null) {
            builder.putList("scale", NbtType.FLOAT, toList(scale));
        }

        return builder.build();
    }

    private static List<Float> toList(CustomRenderOffsets.OffsetXYZ xyz) {
        return List.of(xyz.x(), xyz.y(), xyz.z());
    }

    private static void setItemTag(NbtMapBuilder builder, String tag) {
        builder.putList("item_tags", NbtType.STRING, List.of("minecraft:is_" + tag));
    }

    private static NbtMap xyzToScaleList(float x, float y, float z) {
        return NbtMap.builder().putList("scale", NbtType.FLOAT, List.of(x, y, z)).build();
    }
}
