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
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.item.custom.v2.component.DataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.DataComponentMap;
import org.geysermc.geyser.api.item.custom.v2.predicate.CustomItemPredicate;
import org.geysermc.geyser.api.item.custom.v2.predicate.PredicateStrategy;
import org.geysermc.geyser.api.util.Identifier;

import java.util.List;

/**
 * Defines an entirely custom item, introduced by mods and therefore not based on a vanilla item, and its properties.
 *
 * <p>A definition will be used when an item is received with the ID of the definition. Predicate matching, as is possible
 * right now with vanilla custom item definitions, is currently not implemented, so only one definition can be created for each
 * Java non-vanilla item.</p>
 *
 * <p>Non-vanilla item definitions can be configured with additional components defined in {@link org.geysermc.geyser.api.item.custom.v2.component.GeyserDataComponent}.</p>
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
     * Predicates are currently not supported for non-vanilla custom item definitions.
     *
     * <p>Trying to use predicates will result in an error.</p>
     */
    @Override
    @NonNull List<CustomItemPredicate> predicates();

    /**
     * Predicates are currently not supported for non-vanilla custom item definitions.
     *
     * <p>Trying to use predicates will result in an error.</p>
     */
    @Override
    PredicateStrategy predicateStrategy();

    /**
     * Predicates are currently not supported for non-vanilla custom item definitions.
     *
     * <p>Trying to use predicates will result in an error.</p>
     */
    @Override
    int priority();

    /**
     * On top of vanilla Minecraft's item components, custom ones defined by Geyser in {@link org.geysermc.geyser.api.item.custom.v2.component.GeyserDataComponent} can
     * also be used. Like with vanilla data components, it is still expected that the item <em>always</em> has the behaviour defined by its components.
     *
     * @see CustomItemDefinition#components()
     * @see org.geysermc.geyser.api.item.custom.v2.component.GeyserDataComponent
     */
    @Override
    @NonNull DataComponentMap components();

    static Builder builder(Identifier javaIdentifier, int javaId) {
        return builder(javaIdentifier, javaIdentifier, javaId);
    }

    static Builder builder(Identifier javaIdentifier, Identifier bedrockIdentifier, int javaId) {
        return GeyserApi.api().provider(Builder.class, javaIdentifier, bedrockIdentifier, javaId);
    }

    interface Builder extends CustomItemDefinition.Builder {

        @Override
        Builder displayName(@NonNull String displayName);

        @Override
        Builder priority(int priority);

        @Override
        Builder bedrockOptions(CustomItemBedrockOptions.@NonNull Builder options);

        @Override
        Builder predicate(@NonNull CustomItemPredicate predicate);

        @Override
        Builder predicateStrategy(@NonNull PredicateStrategy strategy);

        @Override
        <T> Builder component(@NonNull DataComponent<T> component, @NonNull T value);

        Builder translationString(@Nullable String translationString);

        @Override
        NonVanillaCustomItemDefinition build();
    }
}
