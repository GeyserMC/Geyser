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
import org.geysermc.geyser.api.item.custom.v2.predicate.CustomItemPredicate;
import org.geysermc.geyser.api.item.custom.v2.predicate.PredicateStrategy;
import org.geysermc.geyser.api.util.Identifier;

// TODO document predicates
public interface NonVanillaCustomItemDefinition extends CustomItemDefinition {

    // TODO: attack damage?

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
     * If the item is chargeable, like a bow.
     */
    boolean chargeable();

    /**
     * The block the item places.
     */
    @Nullable String block();

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

        Builder chargeable(boolean chargeable);

        Builder block(String block);

        @Override
        NonVanillaCustomItemDefinition build();
    }
}
