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

package org.geysermc.geyser.api.entity;

import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.PredicateStrategy;
import org.geysermc.geyser.api.predicate.context.entity.EntitySpawnContext;
import org.geysermc.geyser.api.util.GenericBuilder;

import java.util.List;

public interface CustomEntityDefinition {

    // TODO Identifier
    String bedrockIdentifier();

    float width();

    float height();

    float offset();

    List<MinecraftPredicate<? super EntitySpawnContext>> predicates();

    PredicateStrategy predicateStrategy();

    static Builder builder(@NonNull String bedrockIdentifier, @NonNull JavaEntityType vanillaType) {
        return GeyserApi.api().provider(Builder.class, bedrockIdentifier, vanillaType);
    }

    interface Builder extends GenericBuilder<CustomEntityDefinition> {

        @This
        Builder width(@Positive float width);

        @This
        Builder height(@Positive float height);

        @This
        Builder heightAndWidth(@Positive float value);

        @This
        Builder offset(@Positive float offset);

        @This
        Builder predicate(@NonNull MinecraftPredicate<? super EntitySpawnContext> predicate);

        @This
        Builder predicateStrategy(@NonNull PredicateStrategy strategy);

        @Override
        CustomEntityDefinition build();
    }
}
