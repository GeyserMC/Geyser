/*
 * Copyright (c) 2019-2025 GeyserMC. http://geysermc.org
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
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemVersion;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.item.custom.CustomRenderOffsets;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinitionRegisterException;
import org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserBlockPlacer;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserChargeable;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserItemDataComponents;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserThrowableComponent;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaItemDataComponents;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaRepairable;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.predicate.item.ItemConditionPredicate;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.api.util.Unit;
import org.geysermc.geyser.event.type.GeyserDefineCustomItemsEventImpl;
import org.geysermc.geyser.impl.HoldersImpl;
import org.geysermc.geyser.item.GeyserCustomMappingData;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.custom.GeyserCustomItemBedrockOptions;
import org.geysermc.geyser.item.custom.GeyserCustomItemDefinition;
import org.geysermc.geyser.item.exception.InvalidItemComponentsException;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.item.type.NonVanillaItem;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.registry.mappings.MappingsConfigReader;
import org.geysermc.geyser.registry.populator.custom.CustomItemContext;
import org.geysermc.geyser.registry.type.GeyserMappingItem;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.NonVanillaItemRegistration;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.AttackRange;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.FoodProperties;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.KineticWeapon;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.PiercingWeapon;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.SwingAnimation;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.UseCooldown;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.UseEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CustomItemRegistryPopulator {
    // In behaviour packs and Java components this is set to a text value, such as "eat" or "drink"; over Bedrock network it's sent as an int.
    // These don't all work correctly on Bedrock - see the Consumable.Animation Javadoc in the API
    // Last checked for bedrock 1.21.94 - IDs might have changed since then
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
    // default to high use duration to slow down the player
    private static final float DEFAULT_ITEM_USE_DURATION = 1000.0F;

    private static final AttackRange DEFAULT_ATTACK_RANGE = new AttackRange(0.0F, 3.0F, 0.0F, 5.0F, 0.3F, 1.0F);
    private static final UseEffects DEFAULT_USE_EFFECTS = new UseEffects(false, true, 0.2F);

    public static void populate(Map<String, GeyserMappingItem> items, Multimap<Identifier, CustomItemDefinition> customItems,
                                Multimap<Identifier, NonVanillaCustomItemDefinition> nonVanillaCustomItems) {
        MappingsConfigReader mappingsConfigReader = new MappingsConfigReader();
        // Load custom items from mappings files
        mappingsConfigReader.loadItemMappingsFromJson((identifier, item) -> {
            try {
                validateVanillaOverride(identifier, item, customItems, items);
                customItems.get(identifier).add(item);
            } catch (CustomItemDefinitionRegisterException exception) {
                GeyserImpl.getInstance().getLogger().error("Not registering custom item definition (bedrock identifier=" + item.bedrockIdentifier() + "): " + exception.getMessage());
            }
        });

        GeyserImpl.getInstance().eventBus().fire(new GeyserDefineCustomItemsEventImpl(customItems, nonVanillaCustomItems) {

            @Override
            public void register(@NonNull Identifier identifier, @NonNull CustomItemDefinition definition) {
                try {
                    validateVanillaOverride(identifier, definition, customItems, items);
                    customItems.get(identifier).add(definition);
                } catch (CustomItemDefinitionRegisterException registerException) {
                    throw new CustomItemDefinitionRegisterException("Not registering custom item definition (bedrock identifier=" + definition.bedrockIdentifier() + "): " + registerException.getMessage());
                }
            }

            @Override
            public void register(@NonNull NonVanillaCustomItemDefinition definition) {
                if (definition.identifier().vanilla()) {
                    throw new CustomItemDefinitionRegisterException("Non-vanilla custom item definition (identifier=" + definition.identifier() + ") is attempting to masquerade as a vanilla Minecraft item!");
                } else if (definition.bedrockIdentifier().vanilla()) {
                    throw new CustomItemDefinitionRegisterException("Non-vanilla custom item definition (identifier=" + definition.identifier() + ")' bedrock identifier's namespace is minecraft!");
                } else if (definition.javaId() < items.size()) {
                    throw new CustomItemDefinitionRegisterException("Non-vanilla custom item definition (identifier=" + definition.identifier() + ") is attempting to overwrite a vanilla Minecraft item! (item network ID taken)");
                }

                for (NonVanillaCustomItemDefinition existing : nonVanillaCustomItems.values()) {
                    if (existing.identifier().equals(definition.identifier()) || existing.javaId() == definition.javaId()) {
                        // Until predicates are a thing, then predicate conflict detection should be used like with vanilla items
                        throw new CustomItemDefinitionRegisterException("A non-vanilla custom item definition (identifier=" + definition.identifier() + ", network ID=" + definition.javaId() + ") is already registered!");
                    }
                }
                nonVanillaCustomItems.put(definition.identifier(), definition);
            }
        });

        int customItemCount = customItems.size() + nonVanillaCustomItems.size();
        if (customItemCount > 0) {
            GeyserImpl.getInstance().getLogger().info("Registered " + customItemCount + " custom items");
        }
    }

    public static GeyserCustomMappingData registerCustomItem(Item javaItem, GeyserMappingItem vanillaMapping, CustomItemDefinition customItem,
                                                             int bedrockId, int protocolVersion) throws InvalidItemComponentsException {
        CustomItemContext context = CustomItemContext.createVanillaAndValidateComponents(javaItem, vanillaMapping, customItem, bedrockId, protocolVersion);

        NbtMapBuilder bedrockComponents = createComponentNbt(javaItem.javaKey(), context);
        ItemDefinition itemDefinition = new SimpleItemDefinition(customItem.bedrockIdentifier().toString(), bedrockId, ItemVersion.DATA_DRIVEN, true, bedrockComponents.build());

        return new GeyserCustomMappingData(customItem, itemDefinition, bedrockId);
    }

    public static NonVanillaItemRegistration registerCustomItem(NonVanillaCustomItemDefinition customItem, int bedrockId, int protocolVersion) throws InvalidItemComponentsException {
        CustomItemContext context = CustomItemContext.createNonVanillaAndValidateComponents(customItem, bedrockId, protocolVersion);

        String bedrockIdentifier = customItem.bedrockIdentifier().toString();
        NbtMapBuilder bedrockComponents = createComponentNbt(MinecraftKey.identifierToKey(customItem.identifier()), context);

        Item javaItem = new NonVanillaItem(customItem.identifier().toString(), Item.builder().components(context.components()), context.resolvableComponents());
        Items.register(javaItem, customItem.javaId());

        ItemMapping customMapping = ItemMapping.builder()
            .bedrockIdentifier(bedrockIdentifier)
            .bedrockDefinition(new SimpleItemDefinition(bedrockIdentifier, bedrockId, ItemVersion.DATA_DRIVEN, true, bedrockComponents.build()))
            .bedrockData(0)
            .bedrockBlockDefinition(null)
            .translationString(customItem.translationString())
            .javaItem(javaItem)
            .build();

        return new NonVanillaItemRegistration(javaItem, customMapping);
    }

    private static void validateVanillaOverride(Identifier vanillaIdentifier, CustomItemDefinition item, Multimap<Identifier, CustomItemDefinition> registered,
                                                Map<String, GeyserMappingItem> mappings) throws CustomItemDefinitionRegisterException {
        if (!mappings.containsKey(vanillaIdentifier.toString())) {
            throw new CustomItemDefinitionRegisterException("unknown Java item " + vanillaIdentifier);
        }
        Identifier bedrockIdentifier = item.bedrockIdentifier();
        if (bedrockIdentifier.vanilla()) {
            throw new CustomItemDefinitionRegisterException("custom item bedrock identifier namespace can't be minecraft");
        } else if (item.model().equals(vanillaIdentifier) && item.predicates().isEmpty()) {
            throw new CustomItemDefinitionRegisterException("custom item definition model can't equal vanilla item identifier without a predicate");
        }

        for (Map.Entry<Identifier, CustomItemDefinition> entry : registered.entries()) {
            if (entry.getValue().bedrockIdentifier().equals(item.bedrockIdentifier())) {
                throw new CustomItemDefinitionRegisterException("conflicts with another custom item definition with the same bedrock identifier");
            }
            try {
                checkPredicate(entry, vanillaIdentifier, item);
            } catch (CustomItemDefinitionRegisterException exception) {
                throw new CustomItemDefinitionRegisterException("conflicts with custom item definition (bedrock identifier=" + entry.getValue().bedrockIdentifier() + "): " + exception.getMessage());
            }
        }
    }

    private static void checkPredicate(Map.Entry<Identifier, CustomItemDefinition> existing, Identifier vanillaIdentifier,
                                       CustomItemDefinition newItem) throws CustomItemDefinitionRegisterException {
        // If the definitions are for different Java items or models then it doesn't matter
        if (!vanillaIdentifier.equals(existing.getKey()) || !newItem.model().equals(existing.getValue().model())) {
            return;
        }
        // If they both don't have predicates they conflict
        if (existing.getValue().predicates().isEmpty() && newItem.predicates().isEmpty()) {
            throw new CustomItemDefinitionRegisterException("both entries don't have predicates, one must have a predicate");
        }

        // If their amount of predicates is equal, and the new definition contains all the existing predicates, then they also conflict
        if (existing.getValue().predicates().size() == newItem.predicates().size()) {
            boolean equal = true;

            for (MinecraftPredicate<?> predicate : existing.getValue().predicates()) {
                // This .contains() only works for common predicates that are backed using record classes in the API module!! This is because those properly work with .equals()
                // Custom predicates defined by API users (not by JSON mappings, those only use common predicates) will not work with conflict detection here!
                if (!newItem.predicates().contains(predicate)) {
                    equal = false;
                    break;
                }
            }
            if (equal) {
                throw new CustomItemDefinitionRegisterException("both entries have the same predicates");
            }
        }
    }

    private static NbtMapBuilder createComponentNbt(Key itemIdentifier, CustomItemContext context) {
        NbtMapBuilder builder = NbtMap.builder()
            .putString("name", context.definition().bedrockIdentifier().toString())
            .putInt("id", context.customItemId());

        NbtMapBuilder itemProperties = NbtMap.builder();
        NbtMapBuilder componentBuilder = NbtMap.builder();

        setupBasicItemInfo(context.definition(), context.components(), itemProperties, componentBuilder);

        computeToolProperties(itemProperties, componentBuilder);
        Integer attackDamage = context.definition().components().get(GeyserItemDataComponents.ATTACK_DAMAGE);
        if (attackDamage != null) {
            itemProperties.putInt("damage", attackDamage);
            componentBuilder.putCompound("minecraft:damage", NbtMap.builder()
                .putByte("value", attackDamage.byteValue())
                .build());
        }

        ToolData toolData = context.components().get(DataComponentTypes.TOOL);
        boolean canDestroyInCreative = toolData == null || toolData.isCanDestroyBlocksInCreative();
        computeCreativeDestroyProperties(canDestroyInCreative, itemProperties, componentBuilder);

        // Using API component here because MCPL one is just an ID holder set, and we can't get identifiers from that
        JavaRepairable repairable = context.definition().components().get(JavaItemDataComponents.REPAIRABLE);
        if (repairable != null) {
            computeRepairableProperties(repairable, componentBuilder);
        }

        Equippable equippable = context.components().get(DataComponentTypes.EQUIPPABLE);
        if (equippable != null) {
            boolean renderOffsets = context.definition() instanceof GeyserCustomItemDefinition definition && definition.isOldConvertedItem();
            int protectionValue = context.definition().bedrockOptions().protectionValue();
            if (context.definition().bedrockOptions() instanceof GeyserCustomItemBedrockOptions options) {
                // Fallback to vanilla item's protection value if no protection value is set
                protectionValue = options.protectionValue(context);
            }
            computeArmorProperties(equippable, protectionValue, componentBuilder, renderOffsets);
        }

        Integer enchantmentValue = context.components().get(DataComponentTypes.ENCHANTABLE);
        if (enchantmentValue != null && enchantmentValue > 0) {
            computeEnchantableProperties(enchantmentValue, itemProperties, componentBuilder);
        }

        Boolean glint = context.components().get(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
        if (glint != null) {
            itemProperties.putBoolean("foil", glint);
            componentBuilder.putCompound("minecraft:glint", NbtMap.builder()
                .putBoolean("value", glint)
                .build());
        }

        if (GameProtocol.is1_21_130orHigher(context.protocolVersion())) {
            AttackRange attackRange = context.components().getOrDefault(DataComponentTypes.ATTACK_RANGE, DEFAULT_ATTACK_RANGE);

            KineticWeapon kineticWeapon = context.components().get(DataComponentTypes.KINETIC_WEAPON);
            if (kineticWeapon != null) {
                computeKineticWeaponProperties(componentBuilder, kineticWeapon, attackRange);
            }

            PiercingWeapon piercingWeapon = context.components().get(DataComponentTypes.PIERCING_WEAPON);
            if (piercingWeapon != null) {
                computePiercingWeaponProperties(componentBuilder, attackRange);
            }

            // Please note that technically this component is present on all items in vanilla Minecraft, which, if we think about consistency, would mean
            // we'd have to translate its default value if the component is removed using a patch or not present on a non-vanilla item
            // It doesn't really matter though, since Bedrock has its own default values if the component isn't present
            SwingAnimation swingAnimation = context.components().get(DataComponentTypes.SWING_ANIMATION);
            if (swingAnimation != null) {
                computeSwingAnimationProperties(componentBuilder, swingAnimation);
            }
        }

        Optional<Consumable> consumableComponent = Optional.ofNullable(context.components().get(DataComponentTypes.CONSUMABLE))
            .or(() -> context.vanillaMapping().flatMap(mapping -> {
                // If there is no consumable component, and the vanilla item is a trident, manually add a consumable component
                // with a spear animation and a really high consume duration, to get the trident animation in 3rd-person working
                if (mapping.getBedrockIdentifier().equals("minecraft:trident")) {
                    return Optional.of(new Consumable(DEFAULT_ITEM_USE_DURATION, Consumable.ItemUseAnimation.TRIDENT, null, false, List.of()));
                }
                return Optional.empty();
            }));

        consumableComponent.ifPresent(consumable -> {
            FoodProperties foodProperties = context.components().get(DataComponentTypes.FOOD);
            computeConsumableProperties(consumable, foodProperties, itemProperties, componentBuilder);
        });

        UseCooldown useCooldown = context.components().get(DataComponentTypes.USE_COOLDOWN);
        if (useCooldown != null) {
            computeUseCooldownProperties(useCooldown, itemIdentifier, componentBuilder);
        }

        GeyserBlockPlacer blockPlacer = context.vanillaMapping().map(mapping -> {
            String bedrockIdentifier = mapping.getBedrockIdentifier();
            if (bedrockIdentifier.equals("minecraft:fire_charge") || bedrockIdentifier.equals("minecraft:flint_and_steel")) {
                return GeyserBlockPlacer.builder().block(Identifier.of("fire")).build();
            } else if (mapping.getFirstBlockRuntimeId() != null) {
                return GeyserBlockPlacer.builder().block(Identifier.of(mapping.getBedrockIdentifier())).build();
            }
            return null;
        }).orElse(context.definition().components().get(GeyserItemDataComponents.BLOCK_PLACER));

        if (blockPlacer != null) {
            computeBlockItemProperties(blockPlacer, componentBuilder);
        }

        GeyserChargeable chargeable = context.vanillaMapping().map(GeyserMappingItem::getBedrockIdentifier).map(identifier -> switch (identifier) {
            case "minecraft:bow" -> GeyserChargeable.builder().maxDrawDuration(1.0F).ammunition(Identifier.of("arrow")).build();
            case "minecraft:crossbow" -> GeyserChargeable.builder().chargeOnDraw(true).ammunition(Identifier.of("arrow")).build();
            default -> null;
        }).orElse(context.definition().components().get(GeyserItemDataComponents.CHARGEABLE));

        if (chargeable != null) {
            computeChargeableProperties(itemProperties, componentBuilder, chargeable);
        }

        GeyserThrowableComponent throwable = context.vanillaMapping().map(GeyserMappingItem::getBedrockIdentifier).map(identifier -> switch (identifier) {
            case "minecraft:experience_bottle", "minecraft:egg", "minecraft:ender_pearl", "minecraft:ender_eye",
                 "minecraft:lingering_potion", "minecraft:snowball", "minecraft:splash_potion" -> GeyserThrowableComponent.of(true);
            default -> null;
        }).orElse(context.definition().components().get(GeyserItemDataComponents.THROWABLE));

        if (throwable != null) {
            computeThrowableProperties(componentBuilder, throwable);
        } else if (context.definition().components().get(GeyserItemDataComponents.PROJECTILE) != null) {
            // Is already called in computeThrowableProperties, which is why this is an else if statement
            computeProjectileProperties(componentBuilder);
        }

        // Bedrock doesn't really use these otherwise
        if (throwable != null || chargeable != null || consumableComponent.isPresent()) {
            computeUseEffectsProperties(itemProperties, componentBuilder,
                context.components().getOrDefault(DataComponentTypes.USE_EFFECTS, DEFAULT_USE_EFFECTS),
                consumableComponent.map(Consumable::consumeSeconds));
        }

        Unit entityPlacer = context.vanillaMapping().map(mapping -> {
            if (mapping.isEntityPlacer()) {
                return Unit.INSTANCE;
            }
            return null;
        }).orElse(context.definition().components().get(GeyserItemDataComponents.ENTITY_PLACER));

        if (entityPlacer != null) {
            computeEntityPlacerProperties(componentBuilder);
        }

        // this is not ideal, remove once v1 is removed!
        if (context.definition() instanceof GeyserCustomItemDefinition definition && definition.isOldConvertedItem()) {
            CustomRenderOffsets renderOffsets = definition.getRenderOffsets();
            if (renderOffsets != null) {
                componentBuilder.remove("minecraft:render_offsets");
                componentBuilder.putCompound("minecraft:render_offsets", toNbtMap(renderOffsets));
            } else if (definition.getTextureSize() != 16 && !componentBuilder.containsKey("minecraft:render_offsets")) {
                float scale1 = (float) (0.075 / (definition.getTextureSize() / 16f));
                float scale2 = (float) (0.125 / (definition.getTextureSize() / 16f));
                float scale3 = (float) (0.075 / (definition.getTextureSize() / 16f * 2.4f));

                componentBuilder.putCompound("minecraft:render_offsets",
                    NbtMap.builder().putCompound("main_hand", NbtMap.builder()
                            .putCompound("first_person", xyzToScaleList(scale3, scale3, scale3))
                            .putCompound("third_person", xyzToScaleList(scale1, scale2, scale1)).build())
                        .putCompound("off_hand", NbtMap.builder()
                            .putCompound("first_person", xyzToScaleList(scale1, scale2, scale1))
                            .putCompound("third_person", xyzToScaleList(scale1, scale2, scale1)).build()).build());
            }
        }

        componentBuilder.putCompound("item_properties", itemProperties.build());
        builder.putCompound("components", componentBuilder.build());

        return builder;
    }

    private static void setupBasicItemInfo(CustomItemDefinition definition, DataComponents components, NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder) {
        CustomItemBedrockOptions options = definition.bedrockOptions();

        // Don't send an icon if the item has a block placer component, and is set to use its block as icon
        // This makes bedrock use a 3D render of the block this item places as icon
        GeyserBlockPlacer blockPlacer = definition.components().get(GeyserItemDataComponents.BLOCK_PLACER);
        if (blockPlacer == null || !blockPlacer.useBlockIcon()) {
            NbtMap iconMap = NbtMap.builder()
                .putCompound("textures", NbtMap.builder()
                    .putString("default", definition.icon())
                    .build())
                .build();
            itemProperties.putCompound("minecraft:icon", iconMap);
        }

        if (options.creativeCategory() != CreativeCategory.NONE) {
            itemProperties.putInt("creative_category", options.creativeCategory().id());

            if (options.creativeGroup() != null) {
                itemProperties.putString("creative_group", options.creativeGroup());
            }
        }

        componentBuilder.putCompound("minecraft:display_name", NbtMap.builder().putString("value", definition.displayName()).build());

        // Add a Geyser tag to the item, allowing Molang queries
        addItemTag(componentBuilder, Identifier.of("geyser:is_custom"));

        // Add other defined tags to the item
        Set<Identifier> tags = options.tags();
        for (Identifier tag : tags) {
            addItemTag(componentBuilder, tag);
        }

        itemProperties.putBoolean("allow_off_hand", options.allowOffhand());
        itemProperties.putBoolean("hand_equipped", options.displayHandheld());

        int maxDamage = components.getOrDefault(DataComponentTypes.MAX_DAMAGE, 0);
        // Note that Java requires stack size to be 1 when max damage is above 0, and bedrock requires stack size to be 1 when the item can be equipped
        // We already checked and threw for these cases in CustomItemContext#checkComponents though
        // This can be missing if a non-vanilla item didn't specify a max stack size, or if a component patch removed the component. In that case vanilla Minecraft defaults to 1
        int stackSize = components.getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1);

        itemProperties.putInt("max_stack_size", stackSize);

        // Ignore durability if the item's predicates requires that it be unbreakable
        if (maxDamage > 0 && !isUnbreakableItem(definition)) {
            componentBuilder.putCompound("minecraft:durability", NbtMap.builder()
                .putCompound("damage_chance", NbtMap.builder()
                    .putInt("max", 1)
                    .putInt("min", 1)
                    .build())
                .putInt("max_durability", maxDamage)
                .build());
        }
    }

    /**
     * Adds properties to make the Bedrock client unable to destroy any block with this custom item.
     * This works because the molang '1' for tags will be true for all blocks and the speed will be 0.
     * We want this since we calculate break speed server side in BedrockActionTranslator
     */
    private static void computeToolProperties(NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder) {
        List<NbtMap> speed = List.of(
            NbtMap.builder()
                .putCompound("block", NbtMap.builder()
                    .putString("name", "")
                    .putCompound("states", NbtMap.EMPTY)
                    .putString("tags", "1")
                    .build())
                .putInt("speed", 1)
                .build()
        );

        componentBuilder.putCompound("minecraft:digger", NbtMap.builder()
            .putList("destroy_speeds", NbtType.COMPOUND, speed)
            .putBoolean("use_efficiency", false)
            .build());

        itemProperties.putFloat("mining_speed", 1.0F);
    }

    private static void computeCreativeDestroyProperties(boolean canDestroyInCreative, NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder) {
        itemProperties.putBoolean("can_destroy_in_creative", canDestroyInCreative);
        componentBuilder.putCompound("minecraft:can_destroy_in_creative", NbtMap.builder()
            .putBoolean("value", canDestroyInCreative)
            .build());
    }

    /**
     * This method passes the Java identifiers straight to bedrock - which isn't perfect. Also doesn't work with holder sets that use a tag.
     */
    private static void computeRepairableProperties(JavaRepairable repairable, NbtMapBuilder componentBuilder) {
        List<Identifier> identifiers = ((HoldersImpl) repairable.items()).identifiers();
        if (identifiers == null) {
            return;
        }
        List<NbtMap> items = identifiers.stream()
            .map(identifier -> NbtMap.builder()
                .putString("name", identifier.toString())
                .build()).toList();

        componentBuilder.putCompound("minecraft:repairable", NbtMap.builder()
            .putList("repair_items", NbtType.COMPOUND, NbtMap.builder()
                .putList("items", NbtType.COMPOUND, items)
                .putFloat("repair_amount", 0.0F)
                .build())
            .build());
    }

    private static void computeArmorProperties(Equippable equippable, int protectionValue, NbtMapBuilder componentBuilder, boolean includeRenderOffsets) {
        String slotName = switch (equippable.slot()) {
            case BOOTS -> "armor.feet";
            case LEGGINGS -> "armor.legs";
            case CHESTPLATE -> "armor.chest";
            case HELMET -> "armor.head";
            case BODY -> "armor";
            case SADDLE -> "saddle";
            default -> "";
        };
        if (slotName.isEmpty()) {
            return;
        }

        if (includeRenderOffsets) {
            String renderOffsetType = switch (equippable.slot()) {
                case BOOTS -> "boots";
                case LEGGINGS -> "leggings";
                case CHESTPLATE -> "chestplates";
                case HELMET -> "helmets";
                default -> null;
            };

            if (renderOffsetType != null) {
                componentBuilder.putString("render_offsets", renderOffsetType);
            }
        }

        componentBuilder.putCompound("minecraft:wearable", NbtMap.builder()
            .putString("slot", "slot." + slotName)
            .putInt("protection", protectionValue)
            .build());
    }

    private static void computeEnchantableProperties(int enchantmentValue, NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder) {
        itemProperties.putString("enchantable_slot", "all");
        itemProperties.putInt("enchantable_value", enchantmentValue);
        componentBuilder.putCompound("minecraft:enchantable", NbtMap.builder()
            .putString("slot", "all")
            .putByte("value", (byte) enchantmentValue)
            .build());
    }

    private static void computeBlockItemProperties(GeyserBlockPlacer blockPlacer, NbtMapBuilder componentBuilder) {
        // carved pumpkin should be able to be worn and for that we would need to add wearable and armor with protection 0 here
        // however this would have the side effect of preventing carved pumpkins from working as an attachable on the RP side outside the head slot
        // it also causes the item to glitch when right-clicked to "equip" so this should only be added here later if these issues can be overcome

        // all block items registered should be given this component to prevent double placement
        componentBuilder.putCompound("minecraft:block_placer", NbtMap.builder()
            .putString("block", blockPlacer.block().toString())
            .putBoolean("canUseBlockAsIcon", blockPlacer.useBlockIcon())
            .putList("use_on", NbtType.STRING)
            .build());
    }

    private static void computeChargeableProperties(NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder, GeyserChargeable chargeable) {
        if (chargeable.chargeOnDraw()) {
            itemProperties.putInt("frame_count", 10);
        } else {
            itemProperties.putInt("frame_count", 3);
        }

        componentBuilder.putCompound("minecraft:shooter", NbtMap.builder()
            .putList("ammunition", NbtType.COMPOUND, chargeable.ammunition().stream()
                .map(ammunition ->
                    NbtMap.builder()
                        .putCompound("item", NbtMap.builder()
                            .putString("name", ammunition.toString())
                            .build())
                        .putBoolean("use_offhand", true)
                        .putBoolean("use_in_creative", false)
                        .putBoolean("search_inventory", true)
                        .build()
                )
                .toList())
            .putBoolean("charge_on_draw", chargeable.chargeOnDraw())
            .putFloat("max_draw_duration", chargeable.maxDrawDuration())
            .putBoolean("scale_power_by_draw_duration", true)
            .build());
    }

    private static void computeConsumableProperties(Consumable consumable, @Nullable FoodProperties foodProperties, NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder) {
        String animationName = switch (consumable.animation()) {
            case TRIDENT -> "spear"; // SPEAR is not supported in bedrock and not in the BEDROCK_ANIMATIONS map, so it'll be skipped
            default -> consumable.animation().toString().toLowerCase();
        };
        Integer animationId = BEDROCK_ANIMATIONS.get(consumable.animation());
        if (animationId != null) {
            itemProperties.putInt("use_animation", animationId);
            componentBuilder.putCompound("minecraft:use_animation", NbtMap.builder()
                .putString("value", animationName)
                .build());
        }

        int nutrition = foodProperties == null ? 0 : foodProperties.getNutrition();
        float saturationModifier = foodProperties == null ? 0.0F : foodProperties.getSaturationModifier();
        boolean canAlwaysEat = foodProperties == null || foodProperties.isCanAlwaysEat();
        componentBuilder.putCompound("minecraft:food", NbtMap.builder()
            .putBoolean("can_always_eat", canAlwaysEat)
            .putInt("nutrition", nutrition)
            .putFloat("saturation_modifier", saturationModifier)
            .putCompound("using_converts_to", NbtMap.EMPTY)
            .build());
    }

    private static void computeEntityPlacerProperties(NbtMapBuilder componentBuilder) {
        // all items registered that place entities should be given this component to prevent double placement
        // it is okay that the entity here does not match the actual one since we control what entity actually spawns
        componentBuilder.putCompound("minecraft:entity_placer", NbtMap.builder()
            .putList("dispense_on", NbtType.STRING)
            .putString("entity", "minecraft:minecart")
            .putList("use_on", NbtType.STRING)
            .build());
    }

    private static void computeThrowableProperties(NbtMapBuilder componentBuilder, GeyserThrowableComponent throwable) {
        // allows item to be thrown when holding down right click (individual presses are required w/o this component)
        componentBuilder.putCompound("minecraft:throwable", NbtMap.builder().putBoolean("do_swing_animation", throwable.doSwingAnimation()).build());

        // Required for the swing animation to play correctly
        computeProjectileProperties(componentBuilder);
    }

    private static void computeProjectileProperties(NbtMapBuilder componentBuilder) {
        // it is okay that the projectile here does not match the actual one since we control what entity actually spawns
        componentBuilder.putCompound("minecraft:projectile", NbtMap.builder().putString("projectile_entity", "minecraft:snowball").build());
    }

    private static void computeUseCooldownProperties(UseCooldown cooldown, Key itemIdentifier, NbtMapBuilder componentBuilder) {
        Key group = cooldown.cooldownGroup() == null ? itemIdentifier : cooldown.cooldownGroup();
        componentBuilder.putCompound("minecraft:cooldown", NbtMap.builder()
            .putString("category", group.asString())
            .putFloat("duration", cooldown.seconds())
            .build()
        );
    }

    private static void computeKineticWeaponProperties(NbtMapBuilder componentBuilder, KineticWeapon weapon, AttackRange attackRange) {
        NbtMapBuilder component = NbtMap.builder()
            .putShort("delay", (short) weapon.delayTicks())
            .putFloat("damage_modifier", 0.0F)
            .putFloat("damage_multiplier", 1.0F);

        addAttackRangeProperties(component, attackRange);
        addKineticConditionMap(component, "dismount_conditions", weapon.dismountConditions());

        componentBuilder.putCompound("minecraft:kinetic_weapon", component.build());
    }

    private static void computePiercingWeaponProperties(NbtMapBuilder componentBuilder, AttackRange attackRange) {
        componentBuilder.putCompound("minecraft:piercing_weapon", addAttackRangeProperties(NbtMap.builder(), attackRange).build());
    }

    private static void computeSwingAnimationProperties(NbtMapBuilder componentBuilder, SwingAnimation swingAnimation) {
        componentBuilder.putCompound("minecraft:swing_duration", NbtMap.builder()
            .putFloat("value", swingAnimation.duration() / 20.0F) // Java is in ticks, bedrock is in seconds
            .build());
    }

    private static void computeUseEffectsProperties(NbtMapBuilder itemProperties, NbtMapBuilder componentBuilder,
                                                    UseEffects effects, Optional<Float> setUseDuration) {
        float useDuration = setUseDuration.orElse(DEFAULT_ITEM_USE_DURATION);

        // this is the duration of the use animation in ticks; note that in behavior packs this is set as a float in seconds, but over the network it is an int in ticks
        itemProperties.putInt("use_duration", (int) (useDuration * 20));

        componentBuilder.putCompound("minecraft:use_modifiers", NbtMap.builder()
            .putFloat("movement_modifier", effects.speedMultiplier()) // TODO: test if this is 1-to-1 with Java, it probably isn't
            .putFloat("use_duration", useDuration)
            .build());
    }

    private static NbtMapBuilder addAttackRangeProperties(NbtMapBuilder component, AttackRange attackRange) {
        return component
            .putCompound("reach", createReachMap(attackRange.minRange(), attackRange.maxRange()))
            .putCompound("creative_reach", createReachMap(attackRange.minCreativeRange(), attackRange.maxCreativeRange()))
            .putFloat("hitbox_margin", attackRange.hitboxMargin()); // TODO is this 1-to-1 with Java?
    }

    private static void addKineticConditionMap(NbtMapBuilder component, String key, KineticWeapon.@Nullable Condition condition) {
        if (condition == null) {
            return;
        }
        component.putCompound(key, NbtMap.builder()
            .putShort("max_duration", (short) condition.maxDurationTicks())
            .putFloat("min_speed", condition.minSpeed())
            .putFloat("min_relative_speed", condition.minRelativeSpeed())
            .build());
    }

    private static NbtMap createReachMap(float min, float max) {
        return NbtMap.builder()
            .putFloat("min", min)
            .putFloat("max", max)
            .build();
    }

    private static boolean isUnbreakableItem(CustomItemDefinition definition) {
        for (MinecraftPredicate<? super ItemPredicateContext> predicate : definition.predicates()) {
            if (predicate.equals(ItemConditionPredicate.UNBREAKABLE)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static void addItemTag(NbtMapBuilder builder, Identifier tag) {
        List<String> tagList = (List<String>) builder.get("item_tags");
        if (tagList == null) {
            builder.putList("item_tags", NbtType.STRING, tag.toString());
        } else {
            // NbtList is immutable
            if (!tagList.contains(tag.toString())) {
                tagList = new ArrayList<>(tagList);
                tagList.add(tag.toString());
                builder.putList("item_tags", NbtType.STRING, tagList);
            }
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
}
