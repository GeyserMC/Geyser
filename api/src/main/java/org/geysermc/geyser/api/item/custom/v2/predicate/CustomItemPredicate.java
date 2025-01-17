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

    static ConditionItemPredicate condition(ConditionProperty property) {
        return condition(property, true);
    }

    static ConditionItemPredicate condition(ConditionProperty property, boolean expected) {
        return condition(property, expected, 0);
    }

    /**
     * A predicate that checks for a certain boolean property of the item stack and returns true if it matches the expected value.
     *
     * @param property the property to check.
     * @param expected whether the property should be true or false. Defaults to true.
     * @param index only used for the {@link ConditionProperty#CUSTOM_MODEL_DATA} property, determines which flag of the item's custom model data to check. Defaults to 0.
     */
    static ConditionItemPredicate condition(ConditionProperty property, boolean expected, int index) {
        return GeyserApi.api().provider(ConditionItemPredicate.class, property, expected, index);
    }

    /**
     * A predicate that matches a property of the item stack and returns true if it matches the expected value.
     *
     * @param property the property to check for.
     * @param data the value expected.
     */
    static <T> MatchItemPredicate<T> match(MatchPredicateProperty<T> property, T data) {
        return GeyserApi.api().provider(MatchItemPredicate.class, property, data);
    }

    static RangeDispatchItemPredicate rangeDispatch(RangeDispatchPredicateProperty property, double threshold) {
        return rangeDispatch(property, threshold, 1.0);
    }

    static RangeDispatchItemPredicate rangeDispatch(RangeDispatchPredicateProperty property, double threshold, double scale) {
        return rangeDispatch(property, threshold, scale, false, 0);
    }

    static RangeDispatchItemPredicate rangeDispatch(RangeDispatchPredicateProperty property, double threshold, boolean normalizeIfPossible) {
        return rangeDispatch(property, threshold, 1.0, normalizeIfPossible, 0);
    }

    static RangeDispatchItemPredicate rangeDispatch(RangeDispatchPredicateProperty property, double threshold, double scale, boolean normalizeIfPossible) {
        return rangeDispatch(property, threshold, scale, normalizeIfPossible, 0);
    }

    /**
     * A predicate that checks for a certain numeric property of the item stack and returns true if it is above the specified threshold.
     *
     * @param property the property to check.
     * @param threshold the threshold the property should be above.
     * @param scale factor to multiply the property value with before comparing it with the threshold. Defaults to 1.0.
     * @param normalizeIfPossible if the property value should be normalised to a value between 0.0 and 1.0 before scaling and comparing. Defaults to false. Only works for certain properties.
     * @param index only used for the {@link RangeDispatchPredicateProperty#CUSTOM_MODEL_DATA} property, determines which float of the item's custom model data to check. Defaults to 0.
     */
    static RangeDispatchItemPredicate rangeDispatch(RangeDispatchPredicateProperty property, double threshold, double scale, boolean normalizeIfPossible, int index) {
        return GeyserApi.api().provider(RangeDispatchItemPredicate.class, property, threshold, scale, normalizeIfPossible, index);
    }
}
