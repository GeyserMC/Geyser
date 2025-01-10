/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.item.custom.v2.predicate;

import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.item.custom.v2.predicate.match.MatchPredicateProperty;

public interface CustomItemPredicate {

    int CONDITION = 0;
    int MATCH = 1;
    int RANGE_DISPATCH = 2;

    static CustomItemPredicate condition(ConditionProperty property) {
        return condition(property, true);
    }

    static CustomItemPredicate condition(ConditionProperty property, boolean expected) {
        return condition(property, expected, 0);
    }

    static CustomItemPredicate condition(ConditionProperty property, boolean expected, int index) {
        return GeyserApi.api().provider(CustomItemPredicate.class, CONDITION, property, expected, index);
    }

    static <T> CustomItemPredicate match(MatchPredicateProperty<T> property, T data) {
        return GeyserApi.api().provider(CustomItemPredicate.class, MATCH, property, data);
    }

    static CustomItemPredicate rangeDispatch(RangeDispatchProperty property, double threshold) {
        return rangeDispatch(property, threshold, 1.0);
    }

    static CustomItemPredicate rangeDispatch(RangeDispatchProperty property, double threshold, double scale) {
        return rangeDispatch(property, threshold, scale, false, 0);
    }

    static CustomItemPredicate rangeDispatch(RangeDispatchProperty property, double threshold, boolean normalizeIfPossible) {
        return rangeDispatch(property, threshold, 1.0, normalizeIfPossible, 0);
    }

    static CustomItemPredicate rangeDispatch(RangeDispatchProperty property, double threshold, double scale, boolean normalizeIfPossible) {
        return rangeDispatch(property, threshold, scale, normalizeIfPossible, 0);
    }

    static CustomItemPredicate rangeDispatch(RangeDispatchProperty property, double threshold, double scale, boolean normalizeIfPossible, int index) {
        return GeyserApi.api().provider(CustomItemPredicate.class, RANGE_DISPATCH, property, threshold, scale, normalizeIfPossible, index);
    }
}
