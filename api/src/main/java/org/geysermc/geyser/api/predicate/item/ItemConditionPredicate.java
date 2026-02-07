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

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

/**
 * Contains often-used predicates and predicate factories for simple conditions for {@link ItemPredicateContext}.
 *
 * <p>Predicates created through factories here support conflict detection when used with custom items.
 * It is as such preferred to use these over custom defined predicates when possible.</p>
 * @since 2.9.3
 */
@ApiStatus.NonExtendable
public interface ItemConditionPredicate {

    /**
     * Checks if the item is unbreakable.
     *
     * @see ItemPredicateContext#unbreakable()
     * @since 2.9.3
     */
    MinecraftPredicate<ItemPredicateContext> UNBREAKABLE = ItemPredicateContext::unbreakable;

    /**
     * Checks if the item is damageable (not unbreakable and has a max damage value above 0).
     *
     * @see ItemPredicateContext#unbreakable()
     * @see ItemPredicateContext#maxDamage()
     * @since 2.9.3
     */
    MinecraftPredicate<ItemPredicateContext> DAMAGEABLE = context -> !context.unbreakable() && context.maxDamage() > 0;

    /**
     * Checks if the item is broken (damageable and has 1 durability point left).
     *
     * @see ItemConditionPredicate#DAMAGEABLE
     * @see ItemPredicateContext#damage()
     * @see ItemPredicateContext#maxDamage()
     * @since 2.9.3
     */
    MinecraftPredicate<ItemPredicateContext> BROKEN = DAMAGEABLE.and(context -> context.damage() >= context.maxDamage() - 1);

    /**
     * Checks if the item is damaged (damageable and has a damage value above 0).
     *
     * @see ItemConditionPredicate#DAMAGEABLE
     * @see ItemPredicateContext#damage()
     * @since 2.9.3
     */
    MinecraftPredicate<ItemPredicateContext> DAMAGED = DAMAGEABLE.and(context -> context.damage() >= 0);

    /**
     * Checks if the session player is holding a fishing rod cast.
     *
     * @see ItemPredicateContext#hasFishingRodCast()
     * @since 2.9.3
     */
    MinecraftPredicate<ItemPredicateContext> FISHING_ROD_CAST = ItemPredicateContext::hasFishingRodCast;

    /**
     * Creates a predicate checking for one of the item's custom model data flags.
     *
     * @see ItemPredicateContext#customModelDataFlag(int)
     * @see CustomModelDataPredicate.FlagPredicate
     * @since 2.9.3
     */
    static MinecraftPredicate<ItemPredicateContext> customModelData(@NonNegative int index) {
        return GeyserApi.api().provider(CustomModelDataPredicate.FlagPredicate.class, index);
    }

    /**
     * Creates a predicate checking if the item stack has a component with the specified identifier.
     *
     * @see ItemPredicateContext#components()
     * @see HasComponentPredicate
     * @since 2.9.3
     */
    static MinecraftPredicate<ItemPredicateContext> hasComponent(@NonNull Identifier component) {
        return GeyserApi.api().provider(HasComponentPredicate.class, component);
    }
}
