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

#include "org.checkerframework.checker.index.qual.NonNegative"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.GeyserApi"
#include "org.geysermc.geyser.api.predicate.MatchPredicate"
#include "org.geysermc.geyser.api.predicate.MinecraftPredicate"
#include "org.geysermc.geyser.api.predicate.context.item.ChargedProjectile"
#include "org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext"
#include "org.geysermc.geyser.api.util.Identifier"
#include "org.jetbrains.annotations.ApiStatus"


@ApiStatus.NonExtendable
public interface ItemMatchPredicate extends MatchPredicate {


    static MinecraftPredicate<ItemPredicateContext> chargeType(ChargedProjectile.ChargeType type) {
        return GeyserApi.api().provider(ChargeTypePredicate.class, type);
    }


    static MinecraftPredicate<ItemPredicateContext> trimMaterial(Identifier material) {
        return GeyserApi.api().provider(TrimMaterialPredicate.class, material);
    }


    static MinecraftPredicate<ItemPredicateContext> customModelData(@NonNegative int index, std::string string) {
        return GeyserApi.api().provider(CustomModelDataPredicate.StringPredicate.class, string, index);
    }
}
