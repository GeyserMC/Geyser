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
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.predicate.context.MinecraftPredicateContext;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * A predicate for a {@link MinecraftPredicateContext}.
 *
 * <p>This is used to determine if a {@link CustomItemDefinition} should be used. While this does
 * allow you to define your own predicates with custom checks, it is recommended to use commonly defined predicates and predicate creators when possible,
 * since these support conflict detection among other benefits. See {@link CustomItemDefinition#predicates()}.</p>
 *
 * @param <C> the predicate context.
 * @see CustomItemDefinition#predicates()
 * @since 2.9.3
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

    @Override
    default @NonNull MinecraftPredicate<C> or(@NonNull Predicate<? super C> other) {
        Objects.requireNonNull(other);
        return (context) -> this.test(context) || other.test(context);
    }
}
