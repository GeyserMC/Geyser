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

package org.geysermc.geyser.api.item.custom.v2;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserDataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.DataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.DataComponentMap;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.PredicateStrategy;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.util.Identifier;

import java.util.List;

/**
 * Defines a custom item introduced by mods and therefore not based on a vanilla item and its properties.
 *
 * <p>A definition will be used when an item is received with the ID of the definition. Predicate matching, as is possible
 * right now with vanilla custom item definitions, is currently not implemented, so only one definition can be created for each
 * Java non-vanilla item.</p>
 *
 * <p>Non-vanilla item definitions can be configured with additional components defined in {@link GeyserDataComponent}.</p>
 */
public interface NonVanillaCustomItemDefinition extends CustomItemDefinition {

    /**
     * The java identifier for this item.
     */
    @NonNull Identifier identifier();

    /**
     * The java item network ID of the item.
     *
     * <p>In mods, you can get this by using the {@code getId} method on the item {@code Registry} (Mojmap): {@code BuiltInRegistries.ITEM.getId(<item>)}</p>
     */
    @NonNegative int javaId();

    /**
     * The item's translation string. TODO document
     */
    @Nullable String translationString();

    /**
     * Predicates are currently not supported for use with non-vanilla custom item definitions.
     *
     * <p>Trying to use predicates will result in an error.</p>
     */
    @Override
    @NonNull
    default List<MinecraftPredicate<? super ItemPredicateContext>> predicates() {
        throw new UnsupportedOperationException("Predicates are currently not supported for use with non-vanilla custom item definitions");
    }

    /**
     * Predicates are currently not supported for use with non-vanilla custom item definitions.
     *
     * <p>Trying to use predicates will result in an error.</p>
     */
    @Override
    @NonNull
    default PredicateStrategy predicateStrategy() {
        throw new UnsupportedOperationException("Predicates are currently not supported for use with non-vanilla custom item definitions");
    }

    /**
     * Predicates are currently not supported for use with non-vanilla custom item definitions.
     *
     * <p>Trying to use predicates will result in an error.</p>
     */
    @Override
    default int priority() {
        throw new UnsupportedOperationException("Predicates are currently not supported for use with non-vanilla custom item definitions");
    }

    /**
     * On top of vanilla Minecraft's item components, custom ones defined by Geyser in {@link GeyserDataComponent} can
     * also be used. Like with vanilla data components, it is still expected that the item <em>always</em> has the behaviour defined by its components.
     *
     * <p>Default component removals are not supported for non-vanilla items, since here the data component map defines default components, instead of
     * a patch on top of a vanilla base item.</p>
     *
     * @see CustomItemDefinition#components()
     * @see GeyserDataComponent
     * @return the item's default data components
     */
    @Override
    @NonNull DataComponentMap components();

    static Builder builder(@NonNull Identifier javaIdentifier, int javaId) {
        return builder(javaIdentifier, javaIdentifier, javaId);
    }

    static Builder builder(@NonNull Identifier javaIdentifier, @NonNull Identifier bedrockIdentifier, int javaId) {
        return GeyserApi.api().provider(Builder.class, javaIdentifier, bedrockIdentifier, javaId);
    }

    interface Builder extends CustomItemDefinition.Builder {

        @Override
        @This
        Builder displayName(@NonNull String displayName);

        @Override
        @This
        Builder priority(int priority);

        @Override
        @This
        Builder bedrockOptions(CustomItemBedrockOptions.@NonNull Builder options);

        @Override
        @This
        <T> Builder component(@NonNull DataComponent<T> component, @NonNull T value);

        @This
        Builder translationString(@Nullable String translationString);

        @Override
        NonVanillaCustomItemDefinition build();
    }
}
