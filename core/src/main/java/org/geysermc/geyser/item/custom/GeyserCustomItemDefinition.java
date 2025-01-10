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

package org.geysermc.geyser.item.custom;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.predicate.CustomItemPredicate;
import org.geysermc.geyser.api.item.custom.v2.predicate.PredicateStrategy;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public record GeyserCustomItemDefinition(@NonNull Identifier bedrockIdentifier, String displayName, @NonNull Identifier model, @NonNull List<CustomItemPredicate> predicates,
                                         PredicateStrategy predicateStrategy,
                                         int priority, @NonNull CustomItemBedrockOptions bedrockOptions, @NonNull DataComponents components) implements CustomItemDefinition {

    public static class Builder implements CustomItemDefinition.Builder {
        private final Identifier bedrockIdentifier;
        private final Identifier model;
        private final List<CustomItemPredicate> predicates = new ArrayList<>();

        private String displayName;
        private int priority = 0;
        private CustomItemBedrockOptions bedrockOptions = CustomItemBedrockOptions.builder().build();
        private PredicateStrategy predicateStrategy = PredicateStrategy.AND;
        private DataComponents components = new DataComponents(new HashMap<>());

        public Builder(Identifier bedrockIdentifier, Identifier model) {
            this.bedrockIdentifier = bedrockIdentifier;
            this.displayName = bedrockIdentifier.toString();
            this.model = model;
        }

        @Override
        public CustomItemDefinition.Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        public CustomItemDefinition.Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public CustomItemDefinition.Builder bedrockOptions(CustomItemBedrockOptions.@NonNull Builder options) {
            this.bedrockOptions = options.build();
            return this;
        }

        @Override
        public CustomItemDefinition.Builder predicate(@NonNull CustomItemPredicate predicate) {
            predicates.add(predicate);
            return this;
        }

        @Override
        public CustomItemDefinition.Builder predicateStrategy(@NonNull PredicateStrategy strategy) {
            predicateStrategy = strategy;
            return this;
        }

        @Override
        public CustomItemDefinition.Builder components(@NonNull DataComponents components) {
            this.components = components;
            return this;
        }

        @Override
        public CustomItemDefinition build() {
            return new GeyserCustomItemDefinition(bedrockIdentifier, displayName, model, List.copyOf(predicates), predicateStrategy, priority, bedrockOptions, components);
        }
    }
}
