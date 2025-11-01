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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.predicate.MatchPredicate;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ChargedProjectile;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

/**
 * Contains factories for often-used "match" predicates, that match for a value in {@link ItemPredicateContext}.
 *
 * <p>Predicates created through these factories support conflict detection when used with custom items.
 * It is as such preferred to use these over custom defined predicates when possible.</p>
 */
@ApiStatus.NonExtendable
public interface ItemMatchPredicate extends MatchPredicate {

    /**
     * Creates a predicate matching the item's charged projectile. Usually used with crossbows, but checks any item with the {@code minecraft:charged_projectiles} component.
     *
     * @see ItemPredicateContext#chargedProjectiles()
     * @see ChargeTypePredicate
     */
    static MinecraftPredicate<ItemPredicateContext> chargeType(ChargedProjectile.@NonNull ChargeType type) {
        return GeyserApi.api().provider(ChargeTypePredicate.class, type);
    }

    /**
     * Creates a predicate matching the item's trim material identifier. Works for any item with the {@code minecraft:trim} component.
     *
     * @see ItemPredicateContext#trimMaterial()
     * @see TrimMaterialPredicate
     */
    static MinecraftPredicate<ItemPredicateContext> trimMaterial(@NonNull Identifier material) {
        return GeyserApi.api().provider(TrimMaterialPredicate.class, material);
    }

    /**
     * Creates a predicate matching a string of the item's custom model data strings.
     *
     * @see ItemPredicateContext#customModelDataString(int)
     * @see CustomModelDataPredicate.StringPredicate
     */
    static MinecraftPredicate<ItemPredicateContext> customModelData(@NonNegative int index, @Nullable String string) {
        return GeyserApi.api().provider(CustomModelDataPredicate.StringPredicate.class, string, index);
    }
}
