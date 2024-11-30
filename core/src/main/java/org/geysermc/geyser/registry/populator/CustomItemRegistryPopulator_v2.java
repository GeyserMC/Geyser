/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

import com.google.common.collect.Multimap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ComponentItemData;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.item.custom.CustomRenderOffsets;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.item.GeyserCustomMappingData;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.components.WearableSlot;
import org.geysermc.geyser.item.type.ArmorItem;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.mappings.MappingsConfigReader;
import org.geysermc.geyser.registry.type.GeyserMappingItem;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ConsumeEffect;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.FoodProperties;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.UseCooldown;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CustomItemRegistryPopulator_v2 {
    // In behaviour packs and Java components this is set to a text value, such as "eat" or "drink"; over Bedrock network it's sent as an int.
    private static final Map<Consumable.ItemUseAnimation, Integer> BEDROCK_ANIMATIONS = Map.of(
        Consumable.ItemUseAnimation.NONE, 0,
        Consumable.ItemUseAnimation.EAT, 1,
        Consumable.ItemUseAnimation.DRINK, 2,
        Consumable.ItemUseAnimation.BLOCK, 3,
        Consumable.ItemUseAnimation.BOW, 4,
        Consumable.ItemUseAnimation.SPEAR, 6,
        Consumable.ItemUseAnimation.CROSSBOW, 9,
        Consumable.ItemUseAnimation.SPYGLASS, 10,
        Consumable.ItemUseAnimation.BRUSH, 12
    );

    public static void populate(Map<String, GeyserMappingItem> items, Multimap<String, CustomItemDefinition> customItems, List<NonVanillaCustomItemData> nonVanillaCustomItems /* TODO */) {
        // TODO
        System.out.println("reading mappings");
        MappingsConfigReader mappingsConfigReader = new MappingsConfigReader();
        // Load custom items from mappings files
        mappingsConfigReader.loadItemMappingsFromJson((id, item) -> {
            if (initialCheck(item, items)) {
                customItems.get(id).add(item);
            }
        });

        int customItemCount = customItems.size() + nonVanillaCustomItems.size();
        if (customItemCount > 0) {
            GeyserImpl.getInstance().getLogger().info("Registered " + customItemCount + " custom items");
        }
    }

    public static GeyserCustomMappingData registerCustomItem(String customItemName, Item javaItem, GeyserMappingItem mapping,
                                                             CustomItemDefinition customItemDefinition, int bedrockId) {
        ItemDefinition itemDefinition = new SimpleItemDefinition(customItemName, bedrockId, true);

        NbtMapBuilder builder = createComponentNbt(customItemDefinition, javaItem, mapping, customItemName, bedrockId);
        ComponentItemData componentItemData = new ComponentItemData(customItemName, builder.build());

        return new GeyserCustomMappingData(componentItemData, itemDefinition, customItemName, bedrockId);
    }

    static boolean initialCheck(CustomItemDefinition item, Map<String, GeyserMappingItem> mappings) {
        // TODO check if there's already a same model without predicate and this hasn't a predicate either
        String name = item.name(); // TODO rename to identifier
        if (name.isEmpty()) {
            GeyserImpl.getInstance().getLogger().warning("Custom item name is empty?");
        } else if (Character.isDigit(name.charAt(0))) {
            // As of 1.19.31
            GeyserImpl.getInstance().getLogger().warning("Custom item name (" + name + ") begins with a digit. This may cause issues!");
        }
        return true;
    }

    private static NbtMapBuilder createComponentNbt(CustomItemDefinition customItemDefinition, Item vanillaJavaItem, GeyserMappingItem vanillaMapping,
                                                    String customItemName, int customItemId) {
        NbtMapBuilder builder = NbtMap.builder()
            .putString("name", customItemName)
            .putInt("id", customItemId);

        NbtMapBuilder itemProperties = NbtMap.builder();
        NbtMapBuilder componentBuilder = NbtMap.builder();

        DataComponents components = patchDataComponents(vanillaJavaItem, customItemDefinition);
        setupBasicItemInfo(customItemDefinition.name(), customItemDefinition, components, itemProperties, componentBuilder);

        boolean canDestroyInCreative = true;
        if (vanillaMapping.getToolType() != null) { // This is not using the isTool boolean because it is not just a render type here.
            canDestroyInCreative = computeToolProperties(vanillaMapping.getToolType(), itemProperties, componentBuilder, vanillaJavaItem.attackDamage());
        }
        itemProperties.putBoolean("can_destroy_in_creative", canDestroyInCreative);

        Equippable equippable = components.get(DataComponentType.EQUIPPABLE);
        if (equippable != null) {
            computeArmorProperties(equippable, itemProperties, componentBuilder);
        }

        if (vanillaMapping.getFirstBlockRuntimeId() != null) {
            computeBlockItemProperties(vanillaMapping.getBedrockIdentifier(), componentBuilder);
        }

        Consumable consumable = components.get(DataComponentType.CONSUMABLE);
        if (consumable != null) {
            FoodProperties foodProperties = components.get(DataComponentType.FOOD);
            computeConsumableProperties(consumable, foodProperties == null || foodProperties.isCanAlwaysEat(), itemProperties, componentBuilder);
        }

        if (vanillaMapping.isEntityPlacer()) {
            computeEntityPlacerProperties(componentBuilder);
        }

        UseCooldown useCooldown = components.get(DataComponentType.USE_COOLDOWN);
        if (useCooldown != null) {
            computeUseCooldownProperties(useCooldown, componentBuilder);
        }

        // TODO that switch statement

        computeRenderOffsets(customItemDefinition.bedrockOptions(), componentBuilder); // TODO check "hats" the hardcoded ones, once default components are here, check stack size

        componentBuilder.putCompound("item_properties", itemProperties.build());
        builder.putCompound("components", componentBuilder.build());

        return builder;
    }

    private static void setupBasicItemInfo(String name, CustomItemDefinition definition, DataComponents components, NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder) {
        CustomItemBedrockOptions options = definition.bedrockOptions();
        NbtMap iconMap = NbtMap.builder()
            .putCompound("textures", NbtMap.builder()
                .putString("default", definition.icon())
                .build())
            .build();
        itemProperties.putCompound("minecraft:icon", iconMap);

        if (options.creativeCategory().isPresent()) {
            itemProperties.putInt("creative_category", options.creativeCategory().getAsInt());

            if (options.creativeGroup() != null) {
                itemProperties.putString("creative_group", options.creativeGroup());
            }
        }

        componentBuilder.putCompound("minecraft:display_name", NbtMap.builder().putString("value", name).build()); // TODO

        // Add a Geyser tag to the item, allowing Molang queries
        addItemTag(componentBuilder, "geyser:is_custom");

        // Add other defined tags to the item
        Set<String> tags = options.tags();
        for (String tag : tags) {
            if (tag != null && !tag.isBlank()) {
                addItemTag(componentBuilder, tag);
            }
        }

        itemProperties.putBoolean("allow_off_hand", options.allowOffhand());
        itemProperties.putBoolean("hand_equipped", options.displayHandheld());

        int maxDamage = components.getOrDefault(DataComponentType.MAX_DAMAGE, 0);
        Equippable equippable = components.get(DataComponentType.EQUIPPABLE);
        // Java requires stack size to be 1 when max damage is above 0, and bedrock requires stack size to be 1 when the item can be equipped
        int stackSize = maxDamage > 0 || equippable != null ? 1 : components.getOrDefault(DataComponentType.MAX_STACK_SIZE, 0); // This should never be 0 since we're patching components on top of the vanilla one's

        itemProperties.putInt("max_stack_size", stackSize);
        if (maxDamage > 0/* && customItemData.customItemOptions().unbreakable() != TriState.TRUE*/) { // TODO Insert check back in once predicates are here?
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

    // TODO minecraft java tool component - also needs work elsewhere to calculate correct break speed (server authorised block breaking)
    private static boolean computeToolProperties(String toolType, NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder, int attackDamage) {
        boolean canDestroyInCreative = true;
        float miningSpeed = 1.0f;

        // This means client side the tool can never destroy a block
        // This works because the molang '1' for tags will be true for all blocks and the speed will be 0
        // We want this since we calculate break speed server side in BedrockActionTranslator
        List<NbtMap> speed = new ArrayList<>(List.of(
            NbtMap.builder()
                .putCompound("block", NbtMap.builder()
                    .putString("tags", "1")
                    .build())
                .putCompound("on_dig", NbtMap.builder()
                    .putCompound("condition", NbtMap.builder()
                        .putString("expression", "")
                        .putInt("version", -1)
                        .build())
                    .putString("event", "tool_durability")
                    .putString("target", "self")
                    .build())
                .putInt("speed", 0)
                .build()
        ));

        componentBuilder.putCompound("minecraft:digger",
            NbtMap.builder()
                .putList("destroy_speeds", NbtType.COMPOUND, speed)
                .putCompound("on_dig", NbtMap.builder()
                    .putCompound("condition", NbtMap.builder()
                        .putString("expression", "")
                        .putInt("version", -1)
                        .build())
                    .putString("event", "tool_durability")
                    .putString("target", "self")
                    .build())
                .putBoolean("use_efficiency", true)
                .build()
        );

        if (toolType.equals("sword")) {
            miningSpeed = 1.5f;
            canDestroyInCreative = false;
        }

        itemProperties.putBoolean("hand_equipped", true);
        itemProperties.putFloat("mining_speed", miningSpeed);

        // This allows custom tools - shears, swords, shovels, axes etc to be enchanted or combined in the anvil
        itemProperties.putInt("enchantable_value", 1);
        itemProperties.putString("enchantable_slot", toolType);

        // Adds a "attack damage" indicator. Purely visual!
        if (attackDamage > 0) {
            itemProperties.putInt("damage", attackDamage);
        }

        return canDestroyInCreative;
    }

    private static void computeBlockItemProperties(String blockItem, NbtMapBuilder componentBuilder) {
        // carved pumpkin should be able to be worn and for that we would need to add wearable and armor with protection 0 here
        // however this would have the side effect of preventing carved pumpkins from working as an attachable on the RP side outside the head slot
        // it also causes the item to glitch when right clicked to "equip" so this should only be added here later if these issues can be overcome

        // all block items registered should be given this component to prevent double placement
        componentBuilder.putCompound("minecraft:block_placer", NbtMap.builder().putString("block", blockItem).build());
    }

    private static void computeArmorProperties(Equippable equippable, /*String armorType, int protectionValue,*/ NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder) {
        int protectionValue = 0;
        // TODO protection value
        switch (equippable.slot()) {
            case BOOTS -> {
                componentBuilder.putString("minecraft:render_offsets", "boots");
                componentBuilder.putCompound("minecraft:wearable", WearableSlot.FEET.getSlotNbt());

                //itemProperties.putString("enchantable_slot", "armor_feet");
                //itemProperties.putInt("enchantable_value", 15); TODO
            }
            case CHESTPLATE -> {
                componentBuilder.putString("minecraft:render_offsets", "chestplates");
                componentBuilder.putCompound("minecraft:wearable", WearableSlot.CHEST.getSlotNbt());

                //itemProperties.putString("enchantable_slot", "armor_torso");
                //itemProperties.putInt("enchantable_value", 15); TODO
            }
            case LEGGINGS -> {
                componentBuilder.putString("minecraft:render_offsets", "leggings");
                componentBuilder.putCompound("minecraft:wearable", WearableSlot.LEGS.getSlotNbt());

                //itemProperties.putString("enchantable_slot", "armor_legs");
                //itemProperties.putInt("enchantable_value", 15); TODO
            }
            case HELMET -> {
                componentBuilder.putString("minecraft:render_offsets", "helmets");
                componentBuilder.putCompound("minecraft:wearable", WearableSlot.HEAD.getSlotNbt());
                //componentBuilder.putCompound("minecraft:armor", NbtMap.builder().putInt("protection", protectionValue).build());

                //itemProperties.putString("enchantable_slot", "armor_head");
                //itemProperties.putInt("enchantable_value", 15);
            }
        }
    }

    private static void computeConsumableProperties(Consumable consumable, boolean canAlwaysEat, NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder) {
        // this is the duration of the use animation in ticks; note that in behavior packs this is set as a float in seconds, but over the network it is an int in ticks
        itemProperties.putInt("use_duration", (int) (consumable.consumeSeconds() * 20)); // TODO check and confirm

        itemProperties.putInt("use_animation", BEDROCK_ANIMATIONS.get(consumable.animation()));

        // this component is required to allow the eat animation to play
        componentBuilder.putCompound("minecraft:food", NbtMap.builder().putBoolean("can_always_eat", canAlwaysEat).build());
    }

    private static void computeEntityPlacerProperties(NbtMapBuilder componentBuilder) {
        // all items registered that place entities should be given this component to prevent double placement
        // it is okay that the entity here does not match the actual one since we control what entity actually spawns
        componentBuilder.putCompound("minecraft:entity_placer", NbtMap.builder().putString("entity", "minecraft:minecart").build());
    }

    private static void computeUseCooldownProperties(UseCooldown cooldown, NbtMapBuilder componentBuilder) {
        Objects.requireNonNull(cooldown.cooldownGroup(), "Cooldown group can't be null");
        componentBuilder.putCompound("minecraft:cooldown", NbtMap.builder()
            .putString("category", cooldown.cooldownGroup().asString())
            .putFloat("duration", cooldown.seconds())
            .build()
        );
    }

    private static void computeRenderOffsets(CustomItemBedrockOptions bedrockOptions, NbtMapBuilder componentBuilder) {
        CustomRenderOffsets renderOffsets = bedrockOptions.renderOffsets();
        if (renderOffsets != null) {
            componentBuilder.remove("minecraft:render_offsets");
            componentBuilder.putCompound("minecraft:render_offsets", toNbtMap(renderOffsets));
        } else if (bedrockOptions.textureSize() != 16 && !componentBuilder.containsKey("minecraft:render_offsets")) {
            float scale1 = (float) (0.075 / (bedrockOptions.textureSize() / 16f));
            float scale2 = (float) (0.125 / (bedrockOptions.textureSize() / 16f));
            float scale3 = (float) (0.075 / (bedrockOptions.textureSize() / 16f * 2.4f));

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

    private static @Nullable NbtMap toNbtMap(CustomRenderOffsets.Hand hand) {
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

    private static @Nullable NbtMap toNbtMap(CustomRenderOffsets.@Nullable Offset offset) {
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

    private static NbtMap xyzToScaleList(float x, float y, float z) {
        return NbtMap.builder().putList("scale", NbtType.FLOAT, List.of(x, y, z)).build();
    }

    // TODO this needs to be a simpler method once we just load default vanilla components from mappings or something
    private static DataComponents patchDataComponents(Item javaItem, CustomItemDefinition definition) {
        DataComponents components = new DataComponents(new HashMap<>()); // TODO faster map ?

        components.put(DataComponentType.MAX_STACK_SIZE, javaItem.maxStackSize());
        components.put(DataComponentType.MAX_DAMAGE, javaItem.maxDamage());

        Consumable consumable = getItemConsumable(javaItem);
        if (consumable != null) {
            components.put(DataComponentType.CONSUMABLE, consumable);
        }

        if (canAlwaysEat(javaItem)) {
            components.put(DataComponentType.FOOD, new FoodProperties(0, 0, true));
        }

        if (javaItem.glint()) {
            components.put(DataComponentType.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        if (javaItem instanceof ArmorItem armor) { // TODO equippable
        }

        components.put(DataComponentType.RARITY, javaItem.rarity().ordinal());

        components.getDataComponents().putAll(definition.components().getDataComponents());
        return components;
    }

    private static Consumable getItemConsumable(Item item) {
        if (item == Items.APPLE || item == Items.BAKED_POTATO || item == Items.BEETROOT || item == Items.BEETROOT_SOUP || item == Items.BREAD
            || item == Items.CARROT || item == Items.CHORUS_FRUIT || item == Items.COOKED_CHICKEN || item == Items.COOKED_COD
            || item == Items.COOKED_MUTTON || item == Items.COOKED_PORKCHOP || item == Items.COOKED_RABBIT || item == Items.COOKED_SALMON
            || item == Items.COOKIE || item == Items.ENCHANTED_GOLDEN_APPLE || item == Items.GOLDEN_APPLE || item == Items.GLOW_BERRIES
            || item == Items.GOLDEN_CARROT || item == Items.MELON_SLICE || item == Items.MUSHROOM_STEW || item == Items.POISONOUS_POTATO
            || item == Items.POTATO || item == Items.PUFFERFISH || item == Items.PUMPKIN_PIE || item == Items.RABBIT_STEW
            || item == Items.BEEF || item == Items.CHICKEN || item == Items.COD || item == Items.MUTTON || item == Items.PORKCHOP
            || item == Items.RABBIT || item == Items.ROTTEN_FLESH || item == Items.SPIDER_EYE || item == Items.COOKED_BEEF
            || item == Items.SUSPICIOUS_STEW || item == Items.SWEET_BERRIES || item == Items.TROPICAL_FISH) {
            return Consumables.DEFAULT_FOOD;
        } else if (item == Items.POTION) {
            return Consumables.DEFAULT_DRINK;
        } else if (item == Items.HONEY_BOTTLE) {
            return Consumables.HONEY_BOTTLE;
        } else if (item == Items.OMINOUS_BOTTLE) {
            return Consumables.OMINOUS_BOTTLE;
        } else if (item == Items.DRIED_KELP) {
            return Consumables.DRIED_KELP;
        }
        return null;
    }

    private static boolean canAlwaysEat(Item item) {
        return item == Items.CHORUS_FRUIT || item == Items.ENCHANTED_GOLDEN_APPLE || item == Items.GOLDEN_APPLE || item == Items.HONEY_BOTTLE || item == Items.SUSPICIOUS_STEW;
    }

    @SuppressWarnings("unchecked")
    private static void addItemTag(NbtMapBuilder builder, String tag) {
        List<String> tagList = (List<String>) builder.get("item_tags");
        if (tagList == null) {
            builder.putList("item_tags", NbtType.STRING, tag);
        } else {
            // NbtList is immutable
            if (!tagList.contains(tag)) {
                tagList = new ArrayList<>(tagList);
                tagList.add(tag);
                builder.putList("item_tags", NbtType.STRING, tagList);
            }
        }
    }

    private static final class Consumables {
        private static final Consumable DEFAULT_FOOD = new Consumable(1.6F, Consumable.ItemUseAnimation.EAT, BuiltinSound.ENTITY_GENERIC_EAT, true, List.of());
        private static final Consumable DEFAULT_DRINK = new Consumable(1.6F, Consumable.ItemUseAnimation.DRINK, BuiltinSound.ENTITY_GENERIC_DRINK, false, List.of());
        private static final Consumable HONEY_BOTTLE = new Consumable(2.0F, Consumable.ItemUseAnimation.DRINK, BuiltinSound.ITEM_HONEY_BOTTLE_DRINK, false, List.of());
        private static final Consumable OMINOUS_BOTTLE = new Consumable(2.0F, Consumable.ItemUseAnimation.DRINK, BuiltinSound.ITEM_HONEY_BOTTLE_DRINK,
            false, List.of(new ConsumeEffect.PlaySound(BuiltinSound.ITEM_OMINOUS_BOTTLE_DISPOSE)));
        private static final Consumable DRIED_KELP = new Consumable(0.8F, Consumable.ItemUseAnimation.EAT, BuiltinSound.ENTITY_GENERIC_EAT, false, List.of());
    }
}
