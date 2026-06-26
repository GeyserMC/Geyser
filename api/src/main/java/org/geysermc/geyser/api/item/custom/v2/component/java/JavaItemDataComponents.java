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

import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserItemDataComponents;
import org.geysermc.geyser.api.util.Identifier;

import java.util.function.Predicate;

/**
 * Represents various Java item data components to the extent
 * that these can be translated to custom items for Bedrock edition players.
 * These can be set in {@link CustomItemDefinition.Builder#component(ItemDataComponent, Object)}
 * to specify the item's behavior. It's expected that these components would also
 * be present on the item server-side to avoid de-syncs.
 *
 * @see CustomItemDefinition#components()
 * @see GeyserItemDataComponents
 * @since 2.9.3
 */
public interface JavaItemDataComponents {

    /**
     * Marks the item as consumable. Of this component, only {@code consume_seconds} and {@code animation} properties are translated. Consume effects are done server side,
     * and consume sounds and disabling consume particles aren't possible.
     *
     * <p>Note that due to a bug on Bedrock, not all consume animations appear perfectly. See {@link JavaConsumable.Animation}.</p>
     *
     * @see JavaConsumable
     * @since 2.9.3
     */
    ItemDataComponent<JavaConsumable> CONSUMABLE = create("consumable");

    /**
     * Marks the item as equippable. Of this component, only the {@code slot} property is translated. Other properties are done server-side, are done differently on Bedrock (e.g. {@code asset_id} is done via attachables),
     * or are not possible on Bedrock at all (e.g. {@code camera_overlay}).
     *
     * <p>Note that on Bedrock, equippables can't have a stack size above 1.</p>
     *
     * @see JavaEquippable
     * @since 2.9.3
     */
    ItemDataComponent<JavaEquippable> EQUIPPABLE = create("equippable");

    /**
     * Food properties of the item. All properties properly translate over to Bedrock.
     *
     * @see JavaFoodProperties
     * @since 2.9.3
     */
    ItemDataComponent<JavaFoodProperties> FOOD = create("food");

    /**
     * Max damage value of the item. Must be at or above 0. Items with a max damage value above 0 cannot have a stack size above 1.
     * @since 2.9.3
     */
    ItemDataComponent<Integer> MAX_DAMAGE = create("max_damage", i -> i >= 0);

    /**
     * Max stack size of the item. Must be between 1 and 99. Items with a max stack size value above 1 cannot have a max damage value above 0.
     * @since 2.9.3
     */
    ItemDataComponent<Integer> MAX_STACK_SIZE = create("max_stack_size", i -> i >= 1 && i <= 99); // Reverse lambda

    /**
     * Marks the item to have a use cooldown. To properly function, the item must be able to be used: it must be consumable or have some other kind of use logic.
     *
     * <p>The cooldown group can be {@code null}, in this case the identifier of the vanilla item (in case of vanilla custom items),
     * or the item itself (in case of non-vanilla custom items) will be used.</p>
     *
     * @see JavaUseCooldown
     * @since 2.9.3
     */
    ItemDataComponent<JavaUseCooldown> USE_COOLDOWN = create("use_cooldown");

    /**
     * Marks the item to be enchantable. Must be at or above 0.
     *
     * <p>This component does not translate over perfectly, due to the way enchantments work on Bedrock. The component will be mapped to the {@code minecraft:enchantable} bedrock component with {@code slot=all},
     * and an enchantable value of what this component was set to.
     * This should, but does not guarantee, allow for compatibility with vanilla enchantments. Non-vanilla enchantments are unlikely to work.</p>
     * @since 2.9.3
     */
    ItemDataComponent<Integer> ENCHANTABLE = create("enchantable", i -> i >= 0);

    /**
     * For vanilla-item overrides, this component is only used for the {@link JavaTool#canDestroyBlocksInCreative()} option.
     * For non-vanilla custom items, this component also stores the tool's rules and default mining speed, to correctly calculate block breaking speed.
     *
     * @see JavaTool
     * @since 2.9.3
     */
    ItemDataComponent<JavaTool> TOOL = create("tool");

    /**
     * Marks which items can be used to repair the item.
     *
     * @see JavaRepairable
     * @since 2.9.3
     */
    ItemDataComponent<JavaRepairable> REPAIRABLE = create("repairable");

    /**
     * Overrides the item's enchantment glint.
     * @since 2.9.3
     */
    ItemDataComponent<Boolean> ENCHANTMENT_GLINT_OVERRIDE = create("enchantment_glint_override");

    /**
     * Specifies the attack ranges of an item. Due to Bedrock limitations, only has an effect in combination with the {@link JavaItemDataComponents#KINETIC_WEAPON} or
     * {@link JavaItemDataComponents#PIERCING_WEAPON} components.
     *
     * @see JavaAttackRange
     * @since 2.9.3
     */
    ItemDataComponent<JavaAttackRange> ATTACK_RANGE = create("attack_range");

    /**
     * Specifies a spear-like attack when the item is in use. Only properties required on the Bedrock client are translated.
     *
     * @see JavaKineticWeapon
     * @since 2.9.3
     */
    ItemDataComponent<JavaKineticWeapon> KINETIC_WEAPON = create("kinetic_weapon");

    /**
     * Specifies a stab-like attack when using the item. Only properties required on the Bedrock client are translated.
     *
     * @see JavaPiercingWeapon
     * @since 2.9.3
     */
    ItemDataComponent<JavaPiercingWeapon> PIERCING_WEAPON = create("piercing_weapon");

    /**
     * Specifies the swing animation to play when attacking or interacting using the item. Due to Bedrock limitations, the actual animation played
     * cannot be specified, only the duration of the animation.
     *
     * @see JavaSwingAnimation
     * @since 2.9.3
     */
    ItemDataComponent<JavaSwingAnimation> SWING_ANIMATION = create("swing_animation");

    /**
     * Specifies how the player behaves when using the item. Due to Bedrock limitations, the {@code can_sprint} property cannot be translated.
     *
     * @see JavaUseEffects
     * @since 2.9.3
     */
    ItemDataComponent<JavaUseEffects> USE_EFFECTS = create("use_effects");

    private static <T> ItemDataComponent<T> create(String id) {
        return create(id, t -> true);
    }

    private static <T> ItemDataComponent<T> create(String id, Predicate<T> consumer) {
        return GeyserApi.api().provider(ItemDataComponent.class, Identifier.of(id), consumer, true);
    }
}
