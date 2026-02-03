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

package org.geysermc.geyser.api.predicate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.predicate.context.MinecraftPredicateContext;
import org.geysermc.geyser.api.util.GeyserProvided;
import org.geysermc.geyser.api.util.Identifier;

/**
 * A predicate that tests for a Minecraft dimension.
 *
 * @see MatchPredicate#dimension(Identifier)
 * @since 2.9.3
 */
@GeyserProvided
public interface DimensionPredicate extends MinecraftPredicate<MinecraftPredicateContext> {

    /**
     * The dimension to check for. This can be a vanilla Minecraft dimension, or a custom one.
     * Here are examples:
     * <ul>
     *     <li>{@code minecraft:nether}</li>
     *     <li>{@code my_mod:aether}</li>
     * </ul>
     *
     * @return the dimension to test for
     * @since 2.9.3
     */
    @NonNull Identifier dimension();

    /**
     * @return whether this predicate is negated
     * @since 2.9.3
     */
    boolean negated();
}
