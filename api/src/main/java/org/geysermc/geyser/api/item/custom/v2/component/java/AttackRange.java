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

import org.checkerframework.common.returnsreceiver.qual.This;
import org.checkerframework.common.value.qual.IntRange;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.GenericBuilder;

public interface AttackRange {

    @IntRange(from = 0, to = 64) float minReach();

    @IntRange(from = 0, to = 64) float maxReach();

    @IntRange(from = 0, to = 64) float minCreativeReach();

    @IntRange(from = 0, to = 64) float maxCreativeReach();

    @IntRange(from = 0, to = 1) float hitboxMargin();

    static Builder builder() {
        return GeyserApi.api().provider(Builder.class);
    }

    interface Builder extends GenericBuilder<AttackRange> {

        @This
        Builder minReach(@IntRange(from = 0, to = 64) float minReach);

        @This
        Builder maxReach(@IntRange(from = 0, to = 64) float maxReach);

        @This
        Builder minCreativeReach(@IntRange(from = 0, to = 64) float minCreativeReach);

        @This
        Builder maxCreativeReach(@IntRange(from = 0, to = 64) float maxCreativeReach);

        @This
        Builder hitboxMargin(@IntRange(from = 0, to = 1) float hitboxMargin);

        @Override
        AttackRange build();
    }
}
