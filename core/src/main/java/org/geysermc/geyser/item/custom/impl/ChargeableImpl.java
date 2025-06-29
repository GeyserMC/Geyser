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

package org.geysermc.geyser.item.custom.impl;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.Chargeable;
import org.geysermc.geyser.api.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record ChargeableImpl(
    @NonNegative float maxDrawDuration,
    boolean chargeOnDraw,
    @NonNull List<@NonNull Identifier> ammunition
) implements Chargeable {

    public static class Builder implements Chargeable.Builder {
        private float maxDrawDuration;
        private boolean chargeOnDraw;
        private final List<Identifier> ammunition = new ArrayList<>();

        @Override
        public Chargeable.Builder maxDrawDuration(@NonNegative float maxDrawDuration) {
            if (maxDrawDuration < 0) {
                throw new IllegalArgumentException("maxDrawDuration must be positive");
            }
            this.maxDrawDuration = maxDrawDuration;
            return this;
        }

        @Override
        public Chargeable.Builder chargeOnDraw(boolean chargeOnDraw) {
            this.chargeOnDraw = chargeOnDraw;
            return this;
        }

        @Override
        public Chargeable.Builder ammunition(@NonNull Identifier ammunition) {
            Objects.requireNonNull(ammunition, "ammunition cannot be null");
            if (this.ammunition.contains(ammunition)) {
                throw new IllegalArgumentException("duplicate ammunition " + ammunition);
            }
            this.ammunition.add(ammunition);
            return this;
        }

        @Override
        public Chargeable build() {
            Objects.requireNonNull(ammunition, "ammunition cannot be null");
            return new ChargeableImpl(maxDrawDuration, chargeOnDraw, ammunition);
        }
    }
}
