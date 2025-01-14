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

import org.geysermc.geyser.api.item.custom.v2.component.DataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.DataComponentMap;
import org.geysermc.geyser.api.item.custom.v2.component.ToolProperties;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.FoodProperties;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.UseCooldown;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Why is this coded so weirdly, you ask?
// Why, it's because of two reasons!
// First of all, Java generics are a bit limited :(
// Second, the API module has its own set of component classes, because MCPL can't be used in there.
// However, those component classes have the same names as the MCPL ones, which causes some issues when they both have to be used in the same file.
// One can't be imported, and as such its full qualifier (e.g. org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable) would have to be used.
// That would be a mess to code in, and as such this code here was carefully designed to only require one set of component classes by name (the MCPL ones).
//
// It is VERY IMPORTANT to note that for every component in the API, a converter to MCPL must be put here (better solutions are welcome).
public class ComponentConverters {
    private static final Map<DataComponent<?>, ComponentConverter<?>> converters = new HashMap<>();

    static {
        registerConverter(DataComponent.CONSUMABLE, (itemMap, value) -> {
            Consumable.ItemUseAnimation convertedAnimation = switch (value.animation()) {
                case NONE -> Consumable.ItemUseAnimation.NONE;
                case EAT -> Consumable.ItemUseAnimation.EAT;
                case DRINK -> Consumable.ItemUseAnimation.DRINK;
                case BLOCK -> Consumable.ItemUseAnimation.BLOCK;
                case BOW -> Consumable.ItemUseAnimation.BOW;
                case SPEAR -> Consumable.ItemUseAnimation.SPEAR;
                case CROSSBOW -> Consumable.ItemUseAnimation.CROSSBOW;
                case SPYGLASS -> Consumable.ItemUseAnimation.SPYGLASS;
                case TOOT_HORN -> Consumable.ItemUseAnimation.TOOT_HORN;
                case BRUSH -> Consumable.ItemUseAnimation.BRUSH;
            };
            itemMap.put(DataComponentType.CONSUMABLE, new Consumable(value.consumeSeconds(), convertedAnimation, BuiltinSound.ENTITY_GENERIC_EAT,
                true, List.of()));
        });

        registerConverter(DataComponent.EQUIPPABLE, (itemMap, value) -> {
            EquipmentSlot convertedSlot = switch (value.slot()) {
                case HEAD -> EquipmentSlot.HELMET;
                case CHEST -> EquipmentSlot.CHESTPLATE;
                case LEGS -> EquipmentSlot.LEGGINGS;
                case FEET -> EquipmentSlot.BOOTS;
            };
            itemMap.put(DataComponentType.EQUIPPABLE, new Equippable(convertedSlot, BuiltinSound.ITEM_ARMOR_EQUIP_GENERIC,
                null, null, null, false, false, false));
        });

        registerConverter(DataComponent.FOOD, (itemMap, value) -> itemMap.put(DataComponentType.FOOD,
            new FoodProperties(value.nutrition(), value.saturation(), value.canAlwaysEat())));

        registerConverter(DataComponent.MAX_DAMAGE, (itemMap, value) -> itemMap.put(DataComponentType.MAX_DAMAGE, value));
        registerConverter(DataComponent.MAX_STACK_SIZE, (itemMap, value) -> itemMap.put(DataComponentType.MAX_STACK_SIZE, value));

        registerConverter(DataComponent.USE_COOLDOWN, (itemMap, value) -> itemMap.put(DataComponentType.USE_COOLDOWN,
            new UseCooldown(value.seconds(), MinecraftKey.identifierToKey(value.cooldownGroup()))));

        registerConverter(DataComponent.ENCHANTABLE, (itemMap, value) -> itemMap.put(DataComponentType.ENCHANTABLE, value));
    }

    private static <T> void registerConverter(DataComponent<T> component, ComponentConverter<T> converter) {
        converters.put(component, converter);
    }

    /**
     * Temporary workaround: while 1.21.5 has not released yet, this method returns the {@link ToolProperties#canDestroyBlocksInCreative()} value.
     *
     * <p>When 1.21.5 does release, this value will be mapped into the MCPL tool component, and this method will return nothing again.</p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static TriState convertAndPutComponents(DataComponents itemMap, DataComponentMap customDefinitionMap) {
        TriState canDestroyInCreative = TriState.NOT_SET;
        for (DataComponent<?> component : customDefinitionMap.keySet()) {
            if (component == DataComponent.TOOL) {
                canDestroyInCreative = TriState.fromBoolean(((ToolProperties) customDefinitionMap.get(component)).canDestroyBlocksInCreative());
                continue;
            }
            ComponentConverter converter = converters.get(component);
            Object value = customDefinitionMap.get(component);
            converter.convertAndPut(itemMap, value);
        }
        return canDestroyInCreative;
    }

    @FunctionalInterface
    public interface ComponentConverter<T> {

        void convertAndPut(DataComponents itemMap, T value);
    }
}
