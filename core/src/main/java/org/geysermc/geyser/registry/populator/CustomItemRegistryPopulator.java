/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ComponentItemData;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomRenderOffsets;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.predicate.ConditionProperty;
import org.geysermc.geyser.item.custom.predicate.ConditionPredicate;
import org.geysermc.geyser.api.item.custom.v2.predicate.CustomItemPredicate;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.event.type.GeyserDefineCustomItemsEventImpl;
import org.geysermc.geyser.item.GeyserCustomMappingData;
import org.geysermc.geyser.item.components.WearableSlot;
import org.geysermc.geyser.item.exception.InvalidItemComponentsException;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.mappings.MappingsConfigReader;
import org.geysermc.geyser.registry.type.GeyserMappingItem;
import org.geysermc.geyser.registry.type.NonVanillaItemRegistration;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.FoodProperties;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.UseCooldown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class CustomItemRegistryPopulator {
    // In behaviour packs and Java components this is set to a text value, such as "eat" or "drink"; over Bedrock network it's sent as an int.
    // TODO these don't seem to be applying correctly
    private static final Map<Consumable.ItemUseAnimation, Integer> BEDROCK_ANIMATIONS = Map.of(
        Consumable.ItemUseAnimation.NONE, 0, // Does nothing in 1st person, eating in 3rd person
        Consumable.ItemUseAnimation.EAT, 1, // Appears to look correctly
        Consumable.ItemUseAnimation.DRINK, 2, // Appears to look correctly
        Consumable.ItemUseAnimation.BLOCK, 3, // Does nothing in 1st person, eating in 3rd person
        Consumable.ItemUseAnimation.BOW, 4, // Does nothing in 1st person, eating in 3rd person
        Consumable.ItemUseAnimation.SPEAR, 6, // Does nothing, but looks like spear in 3rd person. Still has eating animation in 3rd person though, looks weird
        Consumable.ItemUseAnimation.CROSSBOW, 9, // Does nothing in 1st person, eating in 3rd person
        Consumable.ItemUseAnimation.SPYGLASS, 10, // Does nothing, but looks like spyglass in 3rd person. Same problems as spear.
        Consumable.ItemUseAnimation.BRUSH, 12 // Brush in 1st and 3rd person. Same problems as spear. Looks weird when not displayed handheld.
    );

    public static void populate(Map<String, GeyserMappingItem> items, Multimap<String, CustomItemDefinition> customItems, List<NonVanillaCustomItemData> nonVanillaCustomItems) {
        MappingsConfigReader mappingsConfigReader = new MappingsConfigReader();
        // Load custom items from mappings files
        mappingsConfigReader.loadItemMappingsFromJson((identifier, item) -> {
            Optional<String> error = validate(identifier, item, customItems, items);
            if (error.isEmpty()) {
                customItems.get(identifier).add(item);
            } else {
                GeyserImpl.getInstance().getLogger().error("Not registering custom item definition (bedrock identifier=" + item.bedrockIdentifier() + "): " + error.get());
            }
        });

        GeyserImpl.getInstance().eventBus().fire(new GeyserDefineCustomItemsEventImpl(customItems, nonVanillaCustomItems) {

            @Override
            @Deprecated
            public boolean register(@NonNull String identifier, @NonNull CustomItemData customItemData) {
                return register(identifier, customItemData.toDefinition(new Identifier(identifier)).build());
            }

            @Override
            public boolean register(@NonNull String identifier, @NonNull CustomItemDefinition definition) {
                Optional<String> error = validate(identifier, definition, customItems, items);
                if (error.isEmpty()) {
                    customItems.get(identifier).add(definition);
                    return true;
                }
                GeyserImpl.getInstance().getLogger().error("Not registering custom item definition (bedrock identifier=" + definition.bedrockIdentifier() + "): " + error.get());
                return false;
            }

            @Override
            public boolean register(@NonNull NonVanillaCustomItemData customItemData) {
                // TODO
                return false;
            }
        });

        int customItemCount = customItems.size() + nonVanillaCustomItems.size();
        if (customItemCount > 0) {
            GeyserImpl.getInstance().getLogger().info("Registered " + customItemCount + " custom items");
        }
    }

    public static GeyserCustomMappingData registerCustomItem(Item javaItem, GeyserMappingItem mapping, CustomItemDefinition customItem,
                                                             int bedrockId) throws InvalidItemComponentsException {
        checkComponents(customItem, javaItem);

        ItemDefinition itemDefinition = new SimpleItemDefinition(customItem.bedrockIdentifier().toString(), bedrockId, true);

        NbtMapBuilder builder = createComponentNbt(customItem, javaItem, mapping, bedrockId);
        ComponentItemData componentItemData = new ComponentItemData(customItem.bedrockIdentifier().toString(), builder.build());

        return new GeyserCustomMappingData(customItem, componentItemData, itemDefinition, bedrockId);
    }

    /**
     * @return an empty optional if there are no errors with the registration, and an optional with an error message if there are
     */
    private static Optional<String> validate(String vanillaIdentifier, CustomItemDefinition item, Multimap<String, CustomItemDefinition> registered, Map<String, GeyserMappingItem> mappings) {
        if (!mappings.containsKey(vanillaIdentifier)) {
            return Optional.of("Unknown Java item " + vanillaIdentifier);
        }
        Identifier bedrockIdentifier = item.bedrockIdentifier();
        if (bedrockIdentifier.namespace().equals(Key.MINECRAFT_NAMESPACE)) {
            return Optional.of("Custom item bedrock identifier namespace can't be minecraft");
        } else if (item.model().namespace().equals(Key.MINECRAFT_NAMESPACE) && item.predicates().isEmpty()) {
            return Optional.of("Custom item definition model can't be in the minecraft namespace without a predicate");
        }

        for (Map.Entry<String, CustomItemDefinition> entry : registered.entries()) {
            if (entry.getValue().bedrockIdentifier().equals(item.bedrockIdentifier())) {
                return Optional.of("Conflicts with another custom item definition with the same bedrock identifier");
            }
            Optional<String> error = checkPredicate(entry, vanillaIdentifier, item);
            if (error.isPresent()) {
                return Optional.of("Conflicts with custom item definition (bedrock identifier=" + entry.getValue().bedrockIdentifier() + "): " + error.get());
            }
        }

        return Optional.empty();
    }

    /**
     * @return an error message if there was a conflict, or an empty optional otherwise
     */
    private static Optional<String> checkPredicate(Map.Entry<String, CustomItemDefinition> existing, String vanillaIdentifier, CustomItemDefinition newItem) {
        // If the definitions are for different Java items or models then it doesn't matter
        if (!vanillaIdentifier.equals(existing.getKey()) || !newItem.model().equals(existing.getValue().model())) {
            return Optional.empty();
        }
        // If they both don't have predicates they conflict
        if (existing.getValue().predicates().isEmpty() && newItem.predicates().isEmpty()) {
            return Optional.of("Both entries don't have predicates, one must have a predicate");
        }
        // If their predicates are equal then they also conflict
        if (existing.getValue().predicates().size() == newItem.predicates().size()) {
            boolean equal = true;
            for (CustomItemPredicate predicate : existing.getValue().predicates()) {
                if (!newItem.predicates().contains(predicate)) {
                    equal = false;
                }
            }
            if (equal) {
                return Optional.of("Both entries have the same predicates");
            }
        }

        return Optional.empty();
    }

    /**
     * Check for illegal combinations of item components that can be specified in the custom item API.
     *
     * <p>Note that, this method only checks for illegal <em>combinations</em> of item components. It is expected that the values of the components separately have
     * already been validated (for example, it is expected that stack size is in the range [1, 99]).</p>
     */
    private static void checkComponents(CustomItemDefinition definition, Item javaItem) throws InvalidItemComponentsException {
        DataComponents components = patchDataComponents(javaItem, definition);
        int stackSize = components.getOrDefault(org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType.MAX_STACK_SIZE, 0);
        int maxDamage = components.getOrDefault(org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType.MAX_DAMAGE, 0);

        if (components.get(org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType.EQUIPPABLE) != null && stackSize > 1) {
            throw new InvalidItemComponentsException("Bedrock doesn't support equippable items with a stack size above 1");
        } else if (stackSize > 1 && maxDamage > 0) {
            throw new InvalidItemComponentsException("Stack size must be 1 when max damage is above 0");
        }
    }

    public static NonVanillaItemRegistration registerCustomItem(NonVanillaCustomItemData customItemData, int customItemId, int protocolVersion) {
        // TODO
        return null;
    }

    private static NbtMapBuilder createComponentNbt(CustomItemDefinition customItemDefinition, Item vanillaJavaItem, GeyserMappingItem vanillaMapping, int customItemId) {
        NbtMapBuilder builder = NbtMap.builder()
            .putString("name", customItemDefinition.bedrockIdentifier().toString())
            .putInt("id", customItemId);

        NbtMapBuilder itemProperties = NbtMap.builder();
        NbtMapBuilder componentBuilder = NbtMap.builder();

        DataComponents components = patchDataComponents(vanillaJavaItem, customItemDefinition);
        setupBasicItemInfo(customItemDefinition, components, itemProperties, componentBuilder);

        boolean canDestroyInCreative = true;
        if (vanillaMapping.getToolType() != null) {
            canDestroyInCreative = computeToolProperties(vanillaMapping.getToolType(), itemProperties, componentBuilder, vanillaJavaItem.defaultAttackDamage());
        }
        itemProperties.putBoolean("can_destroy_in_creative", canDestroyInCreative);

        Equippable equippable = components.get(org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType.EQUIPPABLE);
        if (equippable != null) {
            computeArmorProperties(equippable, itemProperties, componentBuilder);
        }

        if (vanillaMapping.getFirstBlockRuntimeId() != null) {
            computeBlockItemProperties(vanillaMapping.getBedrockIdentifier(), componentBuilder);
        }

        Consumable consumable = components.get(org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType.CONSUMABLE);
        if (consumable != null) {
            FoodProperties foodProperties = components.get(org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType.FOOD);
            computeConsumableProperties(consumable, foodProperties, itemProperties, componentBuilder);
        }

        if (vanillaMapping.isEntityPlacer()) {
            computeEntityPlacerProperties(componentBuilder);
        }

        UseCooldown useCooldown = components.get(org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType.USE_COOLDOWN);
        if (useCooldown != null) {
            computeUseCooldownProperties(useCooldown, componentBuilder);
        }

        // TODO not really a fan of this switch statement
        switch (vanillaMapping.getBedrockIdentifier()) {
            case "minecraft:fire_charge", "minecraft:flint_and_steel" -> computeBlockItemProperties("minecraft:fire", componentBuilder);
            case "minecraft:bow", "minecraft:crossbow", "minecraft:trident" -> computeChargeableProperties(itemProperties, componentBuilder, vanillaMapping.getBedrockIdentifier());
            case "minecraft:experience_bottle", "minecraft:egg", "minecraft:ender_pearl", "minecraft:ender_eye", "minecraft:lingering_potion", "minecraft:snowball", "minecraft:splash_potion" ->
                computeThrowableProperties(componentBuilder);
        }

        computeRenderOffsets(customItemDefinition.bedrockOptions(), componentBuilder);

        componentBuilder.putCompound("item_properties", itemProperties.build());
        builder.putCompound("components", componentBuilder.build());

        return builder;
    }

    private static NbtMapBuilder createComponentNbt(NonVanillaCustomItemData customItemData, String customItemName,
                                                    int customItemId, boolean isHat, boolean displayHandheld, int protocolVersion) {
        // TODO;
        return null;
    }

    private static void setupBasicItemInfo(CustomItemDefinition definition, DataComponents components, NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder) {
        CustomItemBedrockOptions options = definition.bedrockOptions();
        NbtMap iconMap = NbtMap.builder()
            .putCompound("textures", NbtMap.builder()
                .putString("default", definition.icon())
                .build())
            .build();
        itemProperties.putCompound("minecraft:icon", iconMap);

        if (options.creativeCategory() != CreativeCategory.NONE) {
            itemProperties.putInt("creative_category", options.creativeCategory().id());

            if (options.creativeGroup() != null) {
                itemProperties.putString("creative_group", options.creativeGroup());
            }
        }

        componentBuilder.putCompound("minecraft:display_name", NbtMap.builder().putString("value", definition.displayName()).build());

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

        int maxDamage = components.getOrDefault(org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType.MAX_DAMAGE, 0);
        Equippable equippable = components.get(org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType.EQUIPPABLE);
        // Java requires stack size to be 1 when max damage is above 0, and bedrock requires stack size to be 1 when the item can be equipped
        int stackSize = maxDamage > 0 || equippable != null ? 1 : components.getOrDefault(org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType.MAX_STACK_SIZE, 0); // This should never be 0 since we're patching components on top of the vanilla one's

        itemProperties.putInt("max_stack_size", stackSize);
        if (maxDamage > 0 && !isUnbreakableItem(definition)) {
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
    private static boolean computeToolProperties(String toolType, NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder, int attackDamage) {
        // TODO check this, it's probably wrong by now, also check what the minecraft:tool java component can do here, if anything
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

    private static void computeArmorProperties(Equippable equippable, /*String armorType, int protectionValue,*/ NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder) {
        int protectionValue = 0;
        // TODO protection value, enchantable stuff and armour type?
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

    private static void computeBlockItemProperties(String blockItem, NbtMapBuilder componentBuilder) {
        // carved pumpkin should be able to be worn and for that we would need to add wearable and armor with protection 0 here
        // however this would have the side effect of preventing carved pumpkins from working as an attachable on the RP side outside the head slot
        // it also causes the item to glitch when right clicked to "equip" so this should only be added here later if these issues can be overcome

        // all block items registered should be given this component to prevent double placement
        componentBuilder.putCompound("minecraft:block_placer", NbtMap.builder().putString("block", blockItem).build());
    }

    private static void computeChargeableProperties(NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder, String mapping) {
        // TODO check this, it's probably wrong by now

        // setting high use_duration prevents the consume animation from playing
        itemProperties.putInt("use_duration", Integer.MAX_VALUE);
        // display item as tool (mainly for crossbow and bow)
        itemProperties.putBoolean("hand_equipped", true);
        // Make bows, tridents, and crossbows enchantable
        itemProperties.putInt("enchantable_value", 1);

        componentBuilder.putCompound("minecraft:use_modifiers", NbtMap.builder()
            .putFloat("use_duration", 100F)
            .putFloat("movement_modifier", 0.35F)
            .build());

        switch (mapping) {
            case "minecraft:bow" -> {
                itemProperties.putString("enchantable_slot", "bow");
                itemProperties.putInt("frame_count", 3);

                componentBuilder.putCompound("minecraft:shooter", NbtMap.builder()
                    .putList("ammunition", NbtType.COMPOUND, List.of(
                        NbtMap.builder()
                            .putCompound("item", NbtMap.builder()
                                .putString("name", "minecraft:arrow")
                                .build())
                            .putBoolean("use_offhand", true)
                            .putBoolean("search_inventory", true)
                            .build()
                    ))
                    .putFloat("max_draw_duration", 0f)
                    .putBoolean("charge_on_draw", true)
                    .putBoolean("scale_power_by_draw_duration", true)
                    .build());
                componentBuilder.putInt("minecraft:use_duration", 999);
            }
            case "minecraft:trident" -> {
                itemProperties.putString("enchantable_slot", "trident");
                componentBuilder.putInt("minecraft:use_duration", 999);
            }
            case "minecraft:crossbow" -> {
                itemProperties.putString("enchantable_slot", "crossbow");
                itemProperties.putInt("frame_count", 10);

                componentBuilder.putCompound("minecraft:shooter", NbtMap.builder()
                    .putList("ammunition", NbtType.COMPOUND, List.of(
                        NbtMap.builder()
                            .putCompound("item", NbtMap.builder()
                                .putString("name", "minecraft:arrow")
                                .build())
                            .putBoolean("use_offhand", true)
                            .putBoolean("search_inventory", true)
                            .build()
                    ))
                    .putFloat("max_draw_duration", 1f)
                    .putBoolean("charge_on_draw", true)
                    .putBoolean("scale_power_by_draw_duration", true)
                    .build());
                componentBuilder.putInt("minecraft:use_duration", 999);
            }
        }
    }

    private static void computeConsumableProperties(Consumable consumable, @Nullable FoodProperties foodProperties, NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder) {
        // TODO check the animations, it didn't work properly
        // this is the duration of the use animation in ticks; note that in behavior packs this is set as a float in seconds, but over the network it is an int in ticks
        itemProperties.putInt("use_duration", (int) (consumable.consumeSeconds() * 20));
        itemProperties.putInt("use_animation", BEDROCK_ANIMATIONS.get(consumable.animation()));

        componentBuilder.putCompound("minecraft:use_animation", NbtMap.builder()
            .putString("value", consumable.animation().toString().toLowerCase())
            .build());

        int nutrition = foodProperties == null ? 0 : foodProperties.getNutrition();
        float saturationModifier = foodProperties == null ? 0.0F : foodProperties.getSaturationModifier();
        boolean canAlwaysEat = foodProperties == null || foodProperties.isCanAlwaysEat();
        componentBuilder.putCompound("minecraft:food", NbtMap.builder()
            .putBoolean("can_always_eat", canAlwaysEat)
            .putInt("nutrition", nutrition)
            .putFloat("saturation_modifier", saturationModifier)
            .putCompound("using_converts_to", NbtMap.EMPTY)
            .build());

        componentBuilder.putCompound("minecraft:use_modifiers", NbtMap.builder()
            .putFloat("movement_modifier", 0.2F) // TODO is this the right value
            .putFloat("use_duration", consumable.consumeSeconds())
            .build());
    }

    private static void computeEntityPlacerProperties(NbtMapBuilder componentBuilder) {
        // all items registered that place entities should be given this component to prevent double placement
        // it is okay that the entity here does not match the actual one since we control what entity actually spawns
        componentBuilder.putCompound("minecraft:entity_placer", NbtMap.builder().putString("entity", "minecraft:minecart").build());
    }

    private static void computeThrowableProperties(NbtMapBuilder componentBuilder) {
        // TODO check this, it's probably wrong by now

        // allows item to be thrown when holding down right click (individual presses are required w/o this component)
        componentBuilder.putCompound("minecraft:throwable", NbtMap.builder().putBoolean("do_swing_animation", true).build());
        // this must be set to something for the swing animation to play
        // it is okay that the projectile here does not match the actual one since we control what entity actually spawns
        componentBuilder.putCompound("minecraft:projectile", NbtMap.builder().putString("projectile_entity", "minecraft:snowball").build());
    }

    private static void computeUseCooldownProperties(UseCooldown cooldown, NbtMapBuilder componentBuilder) {
        // TODO the non null check can probably be removed when no longer using MCPL in API
        Objects.requireNonNull(cooldown.cooldownGroup(), "Cooldown group can't be null");
        componentBuilder.putCompound("minecraft:cooldown", NbtMap.builder()
            .putString("category", cooldown.cooldownGroup().asString())
            .putFloat("duration", cooldown.seconds())
            .build()
        );
    }

    private static void computeRenderOffsets(CustomItemBedrockOptions bedrockOptions, NbtMapBuilder componentBuilder) {
        // TODO remove this one day when, probably when removing the old format, as render offsets are deprecated
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

    private static boolean isUnbreakableItem(CustomItemDefinition definition) {
        for (CustomItemPredicate predicate : definition.predicates()) {
            if (predicate instanceof ConditionPredicate condition && condition.property() == ConditionProperty.UNBREAKABLE && condition.expected()) {
                return true;
            }
        }
        return false;
    }

    private static DataComponents patchDataComponents(Item javaItem, CustomItemDefinition definition) {
        //return javaItem.gatherComponents(definition.components());
        return javaItem.gatherComponents(new DataComponents(new HashMap<>())); // TODO FIXME
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
}
