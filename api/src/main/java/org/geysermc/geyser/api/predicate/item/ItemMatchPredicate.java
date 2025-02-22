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

import org.geysermc.geyser.api.predicate.context.item.CustomModelDataString;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.PredicateCreator;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.predicate.context.item.ChargedProjectile;
import org.geysermc.geyser.api.util.Identifier;

/**
 * Contains creators for often-used "match" predicates, that match for a value in {@link ItemPredicateContext}.
 */
public interface ItemMatchPredicate {

    PredicateCreator<ItemPredicateContext, ChargedProjectile.ChargeType> CHARGE_TYPE = data ->
        context -> context.chargedProjectiles().stream().anyMatch(projectile -> projectile.type() == data);

    PredicateCreator<ItemPredicateContext, Identifier> TRIM_MATERIAL = data -> MinecraftPredicate.isEqual(ItemPredicateContext::trimMaterial, data);

    PredicateCreator<ItemPredicateContext, CustomModelDataString> CUSTOM_MODEL_DATA = data ->
        MinecraftPredicate.isEqual(context -> context.customModelDataString(data.index()), data.value());
}
