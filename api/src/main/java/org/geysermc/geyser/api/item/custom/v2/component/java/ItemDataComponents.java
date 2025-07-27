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
import org.geysermc.geyser.api.item.custom.v2.component.DataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserDataComponent;
import org.geysermc.geyser.api.util.Identifier;

import java.util.function.Predicate;

/**
 * Represents various Java item data components to the extent
 * that these can be translated to custom items for Bedrock edition players.
 * These can be set in {@link CustomItemDefinition.Builder#component(DataComponent, Object)}
 * to specify the item's behavior. It's expected that these components would also
 * be present on the item server-side to avoid de-syncs.
 *
 * @see CustomItemDefinition#components()
 * @see GeyserDataComponent
 */
public interface ItemDataComponents {

    /**
     * Marks the item as consumable. Of this component, only {@code consume_seconds} and {@code animation} properties are translated. Consume effects are done server side,
     * and consume sounds and disabling consume particles aren't possible.
     *
     * <p>Note that due to a bug on Bedrock, not all consume animations appear perfectly. See {@link Consumable.Animation}.</p>
     *
     * @see Consumable
     */
    DataComponent<Consumable> CONSUMABLE = create("consumable");

    /**
     * Marks the item as equippable. Of this component, only the {@code slot} property is translated. Other properties are done server-side, are done differently on Bedrock (e.g. {@code asset_id} is done via attachables),
     * or are not possible on Bedrock at all (e.g. {@code camera_overlay}).
     *
     * <p>Note that on Bedrock, equippables can't have a stack size above 1.</p>
     *
     * @see Equippable
     */
    DataComponent<Equippable> EQUIPPABLE = create("equippable");

    /**
     * Food properties of the item. All properties properly translate over to Bedrock.
     *
     * @see FoodProperties
     */
    DataComponent<FoodProperties> FOOD = create("food");

    /**
     * Max damage value of the item. Must be at or above 0. Items with a max damage value above 0 cannot have a stack size above 1.
     */
    DataComponent<Integer> MAX_DAMAGE = create("max_damage", i -> i >= 0);

    /**
     * Max stack size of the item. Must be between 1 and 99. Items with a max stack size value above 1 cannot have a max damage value above 0.
     */
    DataComponent<Integer> MAX_STACK_SIZE = create("max_stack_size", i -> i >= 1 && i <= 99); // Reverse lambda

    /**
     * Marks the item to have a use cooldown. To properly function, the item must be able to be used: it must be consumable or have some other kind of use logic.
     *
     * <p>The cooldown group can be {@code null}, in this case the identifier of the vanilla item (in case of vanilla custom items),
     * or the item itself (in case of non-vanilla custom items) will be used.</p>
     *
     * @see UseCooldown
     */
    DataComponent<UseCooldown> USE_COOLDOWN = create("use_cooldown");

    /**
     * Marks the item to be enchantable. Must be at or above 0.
     *
     * <p>This component does not translate over perfectly, due to the way enchantments work on Bedrock. The component will be mapped to the {@code minecraft:enchantable} bedrock component with {@code slot=all},
     * and an enchantable value of what this component was set to.
     * This should, but does not guarantee, allow for compatibility with vanilla enchantments. Non-vanilla enchantments are unlikely to work.</p>
     */
    DataComponent<Integer> ENCHANTABLE = create("enchantable", i -> i >= 0);

    /**
     * For vanilla-item overrides, this component is only used for the {@link ToolProperties#canDestroyBlocksInCreative()} option.
     * For non-vanilla custom items, this component also stores the tool's rules and default mining speed, to correctly calculate block breaking speed.
     *
     * @see ToolProperties
     */
    DataComponent<ToolProperties> TOOL = create("tool");

    /**
     * Marks which items can be used to repair the item.
     *
     * @see Repairable
     */
    DataComponent<Repairable> REPAIRABLE = create("repairable");

    /**
     * Overrides the item's enchantment glint.
     */
    DataComponent<Boolean> ENCHANTMENT_GLINT_OVERRIDE = create("enchantment_glint_override");

    private static <T> DataComponent<T> create(String id) {
        return create(id, t -> true);
    }

    private static <T> DataComponent<T> create(String id, Predicate<T> consumer) {
        return GeyserApi.api().provider(DataComponent.class, Identifier.of(id), consumer, true);
    }
}
