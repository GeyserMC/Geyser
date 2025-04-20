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

package org.geysermc.geyser.api.predicate.item;

import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.PredicateCreator;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.util.Identifier;

/**
 * Contains often-used predicates and predicate creators for simple conditions in {@link ItemPredicateContext}.
 */
public interface ItemConditionPredicate {

    /**
     * Checks if the item is damageable (not unbreakable and has a max damage value above 0).
     *
     * @see ItemPredicateContext#unbreakable()
     * @see ItemPredicateContext#maxDamage()
     */
    MinecraftPredicate<ItemPredicateContext> DAMAGEABLE = context -> !context.unbreakable() && context.maxDamage() > 0;

    /**
     * Checks if the item is broken (damageable and has 1 durability point left).
     *
     * @see ItemConditionPredicate#DAMAGEABLE
     * @see ItemPredicateContext#damage()
     * @see ItemPredicateContext#maxDamage()
     */
    MinecraftPredicate<ItemPredicateContext> BROKEN = DAMAGEABLE.and(context -> context.damage() >= context.maxDamage() - 1);

    /**
     * Checks if the item is damaged (damageable and has a damage value above 0).
     *
     * @see ItemConditionPredicate#DAMAGEABLE
     * @see ItemPredicateContext#damage()
     */
    MinecraftPredicate<ItemPredicateContext> DAMAGED = DAMAGEABLE.and(context -> context.damage() >= 0);

    /**
     * Checks for one of the item's custom model data flags.
     *
     * @see ItemPredicateContext#customModelDataFlag(int)
     */
    PredicateCreator<ItemPredicateContext, Integer> CUSTOM_MODEL_DATA = index -> new CustomModelDataPredicate.FlagPredicate(index, false);

    /**
     * Returns true if the item stack has a component with the specified identifier.
     *
     * @see ItemPredicateContext#components()
     */
    PredicateCreator<ItemPredicateContext, Identifier> HAS_COMPONENT = component -> new HasComponentPredicate(component, false);
}
