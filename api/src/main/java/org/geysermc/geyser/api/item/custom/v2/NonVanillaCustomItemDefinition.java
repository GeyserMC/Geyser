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
import org.geysermc.geyser.api.item.custom.v2.component.DataComponent;
import org.geysermc.geyser.api.item.custom.v2.predicate.CustomItemPredicate;
import org.geysermc.geyser.api.item.custom.v2.predicate.PredicateStrategy;

public interface NonVanillaCustomItemDefinition extends CustomItemDefinition {

    // TODO: attack damage?

    /**
     * The java item network ID of the item.
     *
     * <p>In mods, you can get this by using the {@code getId} method on the item {@code Registry} (Mojmap): {@code BuiltInRegistries.ITEM.getId(<item>)}</p>
     *
     * @return the java item network ID of the item
     */
    @NonNegative
    int javaId();

    /**
     * The item's translation string. TODO document
     *
     * @return the item's translation string
     */
    @Nullable
    String translationString();

    /**
     * If the item is chargeable, like a bow.
     *
     * @return if the item should act like a chargeable item
     */
    boolean isChargeable();

    /**
     * The block the item places.
     *
     * @return the block the item places
     */
    String block();

    interface Builder extends CustomItemDefinition.Builder {

        @Override
        Builder displayName(String displayName);

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

        Builder chargeable(boolean isChargeable);

        Builder block(String block);

        @Override
        NonVanillaCustomItemDefinition build();
    }
}
