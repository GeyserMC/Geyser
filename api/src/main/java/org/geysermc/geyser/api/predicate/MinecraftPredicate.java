/*
 * Copyright (c) 2024-2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.predicate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.predicate.context.MinecraftPredicateContext;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A predicate for a {@link MinecraftPredicateContext}.
 *
 * <p>Right now this is used to determine if a {@link org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition} should be used.</p>
 *
 * @param <C> the predicate context.
 */
@FunctionalInterface
public interface MinecraftPredicate<C extends MinecraftPredicateContext> extends Predicate<C> {

    @Override
    default @NonNull MinecraftPredicate<C> and(@NonNull Predicate<? super C> other) {
        Objects.requireNonNull(other);
        return (context) -> this.test(context) && other.test(context);
    }

    @Override
    default @NonNull MinecraftPredicate<C> negate() {
        return (context) -> !this.test(context);
    }

    // TODO javadoc?
    @Override
    default @NonNull MinecraftPredicate<C> or(@NonNull Predicate<? super C> other) {
        Objects.requireNonNull(other);
        return (context) -> this.test(context) || other.test(context);
    }

    static <C extends MinecraftPredicateContext, T> MinecraftPredicate<C> isEqual(Function<C, T> getter, @Nullable T data) {
        return context -> Objects.equals(getter.apply(context), data);
    }
}
