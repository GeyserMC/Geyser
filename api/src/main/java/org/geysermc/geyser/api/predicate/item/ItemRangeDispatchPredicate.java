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

import org.geysermc.geyser.api.predicate.PredicateCreator;
import org.geysermc.geyser.api.predicate.context.item.CustomModelDataFloat;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;

/**
 * Contains creators for often-used "range dispatch" predicates, which check if a value in {@link org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext} is at or above a certain threshold.
 */
public interface ItemRangeDispatchPredicate {

    PredicateCreator<ItemPredicateContext, Double> BUNDLE_FULLNESS = data -> context -> context.bundleFullness() >= data;

    PredicateCreator<ItemPredicateContext, Double> DAMAGE = data -> context -> context.damage() >= data;

    PredicateCreator<ItemPredicateContext, Double> DAMAGE_NORMALISED = data -> context -> context.maxDamage() != 0.0 && (double) context.damage() / context.maxDamage() >= data;

    PredicateCreator<ItemPredicateContext, Double> COUNT = data -> context -> context.count() >= data;

    PredicateCreator<ItemPredicateContext, Double> COUNT_NORMALISED = data -> context -> (double) context.count() / context.maxStackSize() >= data;

    PredicateCreator<ItemPredicateContext, CustomModelDataFloat> CUSTOM_MODEL_DATA = data -> context -> context.customModelDataFloat(data.index()) >= data.value();
}
