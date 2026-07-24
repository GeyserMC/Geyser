/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item.custom;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponentMap;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserItemDataComponents;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaItemDataComponents;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaKineticWeapon;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.item.components.resolvable.ResolvableComponent;
import org.geysermc.geyser.item.components.resolvable.ResolvableRepairable;
import org.geysermc.geyser.item.components.resolvable.ResolvableToolProperties;
import org.geysermc.geyser.item.exception.InvalidItemComponentsException;
import org.geysermc.geyser.registry.populator.CustomItemRegistryPopulator;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.AttackRange;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
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
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// Why is this coded so weirdly, you ask?
// Why, it's because of two reasons!
// First of all, Java generics are a bit limited :(
// Second, the API module has its own set of component classes, because MCPL can't be used in there.
// However, those component classes have the same names as the MCPL ones, which causes some issues when they both have to be used in the same file.
// One can't be imported, and as such its full qualifier (e.g. org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable) would have to be used.
// That would be a mess to code in, and as such this code here was carefully designed to only require one set of component classes by name (the MCPL ones).
//
// It is VERY IMPORTANT to note that for every component in the API, a converter to MCPL must be put here (there are some exceptions as noted in the Javadoc, better solutions are welcome).
/**
 * This class is used to convert components from the API module to MCPL ones.
 *
 * <p>Most components convert over nicely, and it is very much preferred to have every API component have a converter in here. However, this is not always possible. At the moment, there is one exception:
 * <ul>
 *     <li>Non-vanilla data components (from {@link GeyserItemDataComponents}) don't have converters registered, for obvious reasons.
 *     They're used directly in the custom item registry populator. Eventually, some may have converters introduced as Mojang introduces such components in Java.</li>
 * </ul>
 * For both of these cases proper accommodations have been made in the {@link CustomItemRegistryPopulator}.
 */
public class ComponentConverters {
    private static final Map<ItemDataComponent<?>, ResolvableComponentConverter<?>> converters = new HashMap<>();

    static {
        registerConverter(JavaItemDataComponents.CONSUMABLE, (itemMap, value) -> {
            Consumable.ItemUseAnimation convertedAnimation = switch (value.animation()) {
                case NONE -> Consumable.ItemUseAnimation.NONE;
                case EAT -> Consumable.ItemUseAnimation.EAT;
                case DRINK -> Consumable.ItemUseAnimation.DRINK;
                case BLOCK -> Consumable.ItemUseAnimation.BLOCK;
                case BOW -> Consumable.ItemUseAnimation.BOW;
                case SPEAR -> Consumable.ItemUseAnimation.SPEAR;
                case CROSSBOW -> Consumable.ItemUseAnimation.CROSSBOW;
                case SPYGLASS -> Consumable.ItemUseAnimation.SPYGLASS;
                case BRUSH -> Consumable.ItemUseAnimation.BRUSH;
            };
            itemMap.put(DataComponentTypes.CONSUMABLE, new Consumable(value.consumeSeconds(), convertedAnimation, BuiltinSound.ENTITY_GENERIC_EAT,
                true, List.of()));
        });

        registerConverter(JavaItemDataComponents.EQUIPPABLE, (itemMap, value) -> {
            EquipmentSlot convertedSlot = switch (value.slot()) {
                case HEAD -> EquipmentSlot.HELMET;
                case CHEST -> EquipmentSlot.CHESTPLATE;
                case LEGS -> EquipmentSlot.LEGGINGS;
                case FEET -> EquipmentSlot.BOOTS;
                case BODY -> EquipmentSlot.BODY;
                case SADDLE -> EquipmentSlot.SADDLE;
            };
            itemMap.put(DataComponentTypes.EQUIPPABLE, new Equippable(convertedSlot, BuiltinSound.ITEM_ARMOR_EQUIP_GENERIC,
                null, null, null, false, false, false, false, false, null));
        });

        registerConverter(JavaItemDataComponents.FOOD, (itemMap, value) -> itemMap.put(DataComponentTypes.FOOD,
            new FoodProperties(value.nutrition(), value.saturation(), value.canAlwaysEat())));

        registerConverter(JavaItemDataComponents.MAX_DAMAGE, DataComponentTypes.MAX_DAMAGE);
        registerConverter(JavaItemDataComponents.MAX_STACK_SIZE, DataComponentTypes.MAX_STACK_SIZE);

        registerConverter(JavaItemDataComponents.USE_COOLDOWN, (itemMap, value) -> itemMap.put(DataComponentTypes.USE_COOLDOWN,
            new UseCooldown(value.seconds(), MinecraftKey.identifierToKey(value.cooldownGroup()))));

        registerConverter(JavaItemDataComponents.ENCHANTABLE, DataComponentTypes.ENCHANTABLE);

        registerConverter(JavaItemDataComponents.TOOL, (itemMap, value, consumer) -> {
            itemMap.put(DataComponentTypes.TOOL,
                new ToolData(List.of(), 1.0F, 1, value.canDestroyBlocksInCreative()));
            consumer.accept(new ResolvableToolProperties(value));
        });

        registerConverter(JavaItemDataComponents.REPAIRABLE, (itemMap, value, consumer) -> {
            // Can't convert to MCPL HolderSet here, and custom item registry populator will just use the identifiers of the Holders
            // and pass them to bedrock, if possible. This won't be perfect of course, since identifiers don't have to match bedrock ones
            consumer.accept(new ResolvableRepairable(value));
        });

        registerConverter(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);

        registerConverter(JavaItemDataComponents.ATTACK_RANGE, (itemMap, value) -> itemMap.put(DataComponentTypes.ATTACK_RANGE,
            new AttackRange(value.minReach(), value.maxReach(), value.minCreativeReach(), value.maxCreativeReach(),
                value.hitboxMargin(), 1.0F)));

        registerConverter(JavaItemDataComponents.KINETIC_WEAPON, (itemMap, value) -> itemMap.put(DataComponentTypes.KINETIC_WEAPON,
            new KineticWeapon(0, value.delayTicks(), convertKineticWeaponCondition(value.dismountConditions()),
                null, null, 0.0F, 1.0F, null, null)));

        registerConverter(JavaItemDataComponents.PIERCING_WEAPON, (itemMap, value) -> itemMap.put(DataComponentTypes.PIERCING_WEAPON,
            new PiercingWeapon(false, false, null, null)));

        registerConverter(JavaItemDataComponents.SWING_ANIMATION, (itemMap, value) -> itemMap.put(DataComponentTypes.SWING_ANIMATION,
            new SwingAnimation(SwingAnimation.Type.WHACK, value.duration())));

        registerConverter(JavaItemDataComponents.USE_EFFECTS, (itemMap, value) -> itemMap.put(DataComponentTypes.USE_EFFECTS,
            new UseEffects(false, true, value.speedMultiplier())));
    }

    private static <T> void registerConverter(ItemDataComponent<T> component, DataComponentType<T> converted) {
        registerConverter(component, (itemMap, value) -> itemMap.put(converted, value));
    }

    private static <T> void registerConverter(ItemDataComponent<T> component, ComponentConverter<T> converter) {
        registerConverter(component, (itemMap, value, resolvableConsumer) -> converter.convertAndPut(itemMap, value));
    }

    private static <T> void registerConverter(ItemDataComponent<T> component, ResolvableComponentConverter<T> converter) {
        converters.put(component, converter);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DataComponents convertComponentPatch(ItemDataComponentMap customDefinitionPatch, List<Identifier> customDefinitionRemovals, Consumer<ResolvableComponent<?>> resolvableConsumer) throws InvalidItemComponentsException {
        DataComponents converted = new DataComponents(new HashMap<>());
        for (ItemDataComponent<?> component : customDefinitionPatch.keySet()) {
            if (customDefinitionRemovals.contains(component.identifier())) {
                throw new InvalidItemComponentsException("Component " + component.identifier() + " was present both in the components to add and the components to remove");
            }
            ResolvableComponentConverter converter = converters.get(component);
            if (converter != null) {
                Object value = customDefinitionPatch.get(component);
                converter.convert(converted, value, resolvableConsumer);
            }
        }

        for (Identifier removed : customDefinitionRemovals) {
            DataComponentType<?> component = DataComponentTypes.fromKey(MinecraftKey.identifierToKey(removed));
            if (component != null) {
                converted.put(component, null);
            }
        }
        return converted;
    }

    @FunctionalInterface
    public interface ComponentConverter<T> {

        void convertAndPut(DataComponents itemMap, T value);
    }

    @FunctionalInterface
    public interface ResolvableComponentConverter<T> {

        void convert(DataComponents itemMap, T value, Consumer<ResolvableComponent<?>> resolvableConsumer);
    }

    private static KineticWeapon.Condition convertKineticWeaponCondition(JavaKineticWeapon.@Nullable Condition condition) {
        if (condition == null) {
            return null;
        }
        return new KineticWeapon.Condition(condition.maxDurationTicks(), condition.minSpeed(), condition.minRelativeSpeed());
    }
}
