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
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserItemDataComponents;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponentMap;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.PredicateStrategy;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * Defines a custom item introduced by mods and therefore not based on a vanilla item and its properties.
 *
 * <p>A definition will be used when an item is received with the ID of the definition. Predicate matching, as is possible
 * right now with vanilla custom item definitions, is currently not implemented, so only one definition can be created for each
 * Java non-vanilla item.</p>
 *
 * <p>Non-vanilla item definitions can be configured with additional components defined in {@link GeyserItemDataComponents}.</p>
 * @since 2.9.3
 */
@ApiStatus.NonExtendable
public interface NonVanillaCustomItemDefinition extends CustomItemDefinition {

    /**
     * The item's Java identifier.
     *
     * @return the item's Java identifier
     * @since 2.9.3
     */
    @NonNull Identifier identifier();

    /**
     * The item's Java network ID.
     *
     * <p>In mods, you can get this by using the {@code getId} method on the item {@code Registry} (Mojmap): {@code BuiltInRegistries.ITEM.getId(<item>)}</p>
     *
     * @return the item's Java network ID
     * @since 2.9.3
     */
    @NonNegative int javaId();

    /**
     * The item's Java translation string. When present, Geyser will translate this string using its loaded locales and send it to the bedrock client as the item's name.
     *
     * @return the item's Java translation string
     * @since 2.9.3
     */
    @Nullable String translationString();

    /**
     * Predicates are currently not supported for use with non-vanilla custom item definitions.
     *
     * <p>Trying to use predicates will result in an error.</p>
     *
     * @throws UnsupportedOperationException always, since predicate usage is not supported
     * @since 2.9.3
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
     *
     * @throws UnsupportedOperationException always, since predicate usage is not supported
     * @since 2.9.3
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
     *
     * @throws UnsupportedOperationException always, since predicate usage is not supported
     * @since 2.9.3
     */
    @Override
    default int priority() {
        throw new UnsupportedOperationException("Predicates are currently not supported for use with non-vanilla custom item definitions");
    }

    /**
     * On top of vanilla Minecraft's item components, custom ones defined by Geyser in {@link GeyserItemDataComponents} can
     * also be used. Like with vanilla data components, it is still expected that the item <em>always</em> has the behavior defined by its components.
     *
     * <p>Default component removals are not supported for non-vanilla items, since here the data component map defines default components, instead of
     * a patch on top of a vanilla base item.</p>
     *
     * @see CustomItemDefinition#components()
     * @see GeyserItemDataComponents
     * @return the item's default data components
     * @since 2.9.3
     */
    @Override
    @NonNull
    ItemDataComponentMap components();

    /**
     * Creates a builder for a non-vanilla custom item definition, using the {@code javaIdentifier} as {@code bedrockIdentifier}.
     *
     * @param javaIdentifier the item's Java identifier
     * @param javaId the item's Java network ID
     * @see NonVanillaCustomItemDefinition#identifier()
     * @see NonVanillaCustomItemDefinition#javaId()
     * @return a new builder
     * @since 2.9.3
     */
    static Builder builder(@NonNull Identifier javaIdentifier, int javaId) {
        return builder(javaIdentifier, javaIdentifier, javaId);
    }

    /**
     * Creates a new builder for a non-vanilla custom item definition.
     *
     * @param javaIdentifier the item's Java identifier
     * @param bedrockIdentifier the item's bedrock identifier
     * @param javaId the item's Java network ID
     * @see NonVanillaCustomItemDefinition#identifier()
     * @see NonVanillaCustomItemDefinition#bedrockIdentifier()
     * @see NonVanillaCustomItemDefinition#javaId()
     * @return a new builder
     * @since 2.9.3
     */
    static Builder builder(@NonNull Identifier javaIdentifier, @NonNull Identifier bedrockIdentifier, int javaId) {
        return GeyserApi.api().provider(Builder.class, javaIdentifier, bedrockIdentifier, javaId);
    }

    /**
     * Builder for non-vanilla custom item definitions.
     * @since 2.9.3
     */
    interface Builder extends CustomItemDefinition.Builder {

        /**
         * {@inheritDoc}
         * @since 2.9.3
         */
        @Override
        @This
        Builder displayName(@NonNull String displayName);

        /**
         * {@inheritDoc}
         * @since 2.9.3
         */
        @Override
        @This
        Builder priority(int priority);

        /**
         * {@inheritDoc}
         * @since 2.9.3
         */
        @Override
        @This
        Builder bedrockOptions(CustomItemBedrockOptions.@NonNull Builder options);

        /**
         * {@inheritDoc}
         * @since 2.9.3
         */
        @Override
        @This
        <T> Builder component(@NonNull ItemDataComponent<T> component, @NonNull T value);

        /**
         * Sets the Java translation string of the item.
         *
         * @param translationString the Java translation string of the item
         * @return this builder
         * @since 2.9.3
         */
        @This
        Builder translationString(@Nullable String translationString);

        /**
         * Creates the non-vanilla custom item definition.
         *
         * @return the new non-vanilla custom item definition
         * @since 2.9.3
         */
        @Override
        NonVanillaCustomItemDefinition build();
    }
}
