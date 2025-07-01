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

import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.geyser.api.item.custom.v2.component.DataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.DataComponentMap;
import org.geysermc.geyser.api.item.custom.v2.component.java.ItemDataComponents;
import org.geysermc.geyser.api.item.custom.v2.component.java.Repairable;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.item.components.resolvable.ResolvableComponent;
import org.geysermc.geyser.item.components.resolvable.ResolvableToolProperties;
import org.geysermc.geyser.registry.populator.CustomItemRegistryPopulator;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.FoodProperties;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.UseCooldown;
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
 * <p>Most components convert over nicely, and it is very much preferred to have every API component have a converter in here. However, this is not always possible. At the moment, there are 2 exceptions:
 * <ul>
 *     <li>The MCPL counterpart of the {@link ItemDataComponents#REPAIRABLE} component is just an ID holder set, which can't be used in the custom item registry populator.
 *     Also see {@link CustomItemRegistryPopulator#computeRepairableProperties(Repairable, NbtMapBuilder)}.</li>
 *     <li>Non-vanilla data components (from {@link org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserDataComponent}) don't have converters registered, for obvious reasons.
 *     They're used directly in the custom item registry populator. Eventually, some may have converters introduced as Mojang introduces such components in Java.</li>
 * </ul>
 * For both of these cases proper accommodations have been made in the {@link CustomItemRegistryPopulator}.
 */
public class ComponentConverters {
    private static final Map<DataComponent<?>, ResolvableComponentConverter<?>> converters = new HashMap<>();

    // TODO maybe clean this up where possible, quick conversion for types that don't require casting possibly
    static {
        registerConverter(ItemDataComponents.CONSUMABLE, (itemMap, value) -> {
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

        registerConverter(ItemDataComponents.EQUIPPABLE, (itemMap, value) -> {
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

        registerConverter(ItemDataComponents.FOOD, (itemMap, value) -> itemMap.put(DataComponentTypes.FOOD,
            new FoodProperties(value.nutrition(), value.saturation(), value.canAlwaysEat())));

        registerConverter(ItemDataComponents.MAX_DAMAGE, (itemMap, value) -> itemMap.put(DataComponentTypes.MAX_DAMAGE, value));
        registerConverter(ItemDataComponents.MAX_STACK_SIZE, (itemMap, value) -> itemMap.put(DataComponentTypes.MAX_STACK_SIZE, value));

        registerConverter(ItemDataComponents.USE_COOLDOWN, (itemMap, value) -> itemMap.put(DataComponentTypes.USE_COOLDOWN,
            new UseCooldown(value.seconds(), MinecraftKey.identifierToKey(value.cooldownGroup()))));

        registerConverter(ItemDataComponents.ENCHANTABLE, (itemMap, value) -> itemMap.put(DataComponentTypes.ENCHANTABLE, value));

        registerConverter(ItemDataComponents.TOOL, (itemMap, value, consumer) -> {
            itemMap.put(DataComponentTypes.TOOL,
                new ToolData(List.of(), 1.0F, 1, value.canDestroyBlocksInCreative()));
            consumer.accept(new ResolvableToolProperties(value));
        });

        registerConverter(ItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE, (itemMap, value) -> itemMap.put(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, value));
    }

    private static <T> void registerConverter(DataComponent<T> component, ComponentConverter<T> converter) {
        registerConverter(component, (itemMap, value, resolvableConsumer) -> converter.convertAndPut(itemMap, value));
    }

    private static <T> void registerConverter(DataComponent<T> component, ResolvableComponentConverter<T> converter) {
        converters.put(component, converter);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static DataComponents convertComponentPatch(DataComponentMap customDefinitionPatch, List<Identifier> customDefinitionRemovals, Consumer<ResolvableComponent<?>> resolvableConsumer) {
        DataComponents converted = new DataComponents(new HashMap<>());
        for (DataComponent<?> component : customDefinitionPatch.keySet()) {
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
}
