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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.item.custom.v2.component.java.Repairable;
import org.geysermc.geyser.api.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record RepairableImpl(@NonNull List<@NonNull Identifier> items) implements Repairable {

    public static class Builder implements Repairable.Builder {
        private final List<Identifier> items = new ArrayList<>();

        @Override
        public Builder item(@NonNull Identifier item) {
            Objects.requireNonNull(items, "item cannot be null");
            if (this.items.contains(item)) {
                throw new IllegalArgumentException("duplicate repairable item: " + item);
            }
            this.items.add(item);
            return this;
        }

        @Override
        public Repairable build() {
            return new RepairableImpl(items);
        }
    }
}
