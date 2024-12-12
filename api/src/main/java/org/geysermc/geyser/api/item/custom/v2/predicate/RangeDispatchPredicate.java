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

public record RangeDispatchPredicate(RangeDispatchProperty property, double threshold, double scale, boolean normalizeIfPossible, int index) implements CustomItemPredicate {

    public RangeDispatchPredicate(RangeDispatchProperty property, double threshold, double scale, boolean normalizeIfPossible) {
        this(property, threshold, scale, normalizeIfPossible, 0);
    }

    public RangeDispatchPredicate(RangeDispatchProperty property, double threshold, double scale) {
        this(property, threshold, scale, false);
    }

    public RangeDispatchPredicate(RangeDispatchProperty property, double threshold) {
        this(property, threshold, 1.0);
    }

    // TODO check if we can change items while bedrock is using them, and if bedrock will continue to use them
    public enum RangeDispatchProperty {
        BUNDLE_FULLNESS,
        DAMAGE,
        COUNT,
        CUSTOM_MODEL_DATA
    }
}