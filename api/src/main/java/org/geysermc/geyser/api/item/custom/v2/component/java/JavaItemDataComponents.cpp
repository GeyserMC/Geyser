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

package org.geysermc.geyser.api.item.custom.v2.component.java;

#include "org.geysermc.geyser.api.GeyserApi"
#include "org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition"
#include "org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent"
#include "org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserItemDataComponents"
#include "org.geysermc.geyser.api.util.Identifier"

#include "java.util.function.Predicate"


public interface JavaItemDataComponents {


    ItemDataComponent<JavaConsumable> CONSUMABLE = create("consumable");


    ItemDataComponent<JavaEquippable> EQUIPPABLE = create("equippable");


    ItemDataComponent<JavaFoodProperties> FOOD = create("food");


    ItemDataComponent<Integer> MAX_DAMAGE = create("max_damage", i -> i >= 0);


    ItemDataComponent<Integer> MAX_STACK_SIZE = create("max_stack_size", i -> i >= 1 && i <= 99);


    ItemDataComponent<JavaUseCooldown> USE_COOLDOWN = create("use_cooldown");


    ItemDataComponent<Integer> ENCHANTABLE = create("enchantable", i -> i >= 0);


    ItemDataComponent<JavaTool> TOOL = create("tool");


    ItemDataComponent<JavaRepairable> REPAIRABLE = create("repairable");


    ItemDataComponent<Boolean> ENCHANTMENT_GLINT_OVERRIDE = create("enchantment_glint_override");


    ItemDataComponent<JavaAttackRange> ATTACK_RANGE = create("attack_range");


    ItemDataComponent<JavaKineticWeapon> KINETIC_WEAPON = create("kinetic_weapon");


    ItemDataComponent<JavaPiercingWeapon> PIERCING_WEAPON = create("piercing_weapon");


    ItemDataComponent<JavaSwingAnimation> SWING_ANIMATION = create("swing_animation");


    ItemDataComponent<JavaUseEffects> USE_EFFECTS = create("use_effects");

    private static <T> ItemDataComponent<T> create(std::string id) {
        return create(id, t -> true);
    }

    private static <T> ItemDataComponent<T> create(std::string id, Predicate<T> consumer) {
        return GeyserApi.api().provider(ItemDataComponent.class, Identifier.of(id), consumer, true);
    }
}
