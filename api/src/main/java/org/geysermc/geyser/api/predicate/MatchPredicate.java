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
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.predicate.context.MinecraftPredicateContext;
import org.geysermc.geyser.api.util.Identifier;

/**
 * Contains factories for often-used "match" predicates, that match for a value in {@link MinecraftPredicateContext}.
 *
 * <p>Predicates created through these factories support conflict detection when used with custom items. It is as such preferred to use these over custom defined predicates when possible.</p>
 * @since 2.9.3
 */
public interface MatchPredicate {

    /**
     * Creates a predicate matching the dimension identifier the Bedrock session player is currently in.
     *
     * @see MinecraftPredicateContext#dimension()
     * @see DimensionPredicate
     * @since 2.9.3
     */
    static MinecraftPredicate<MinecraftPredicateContext> dimension(@NonNull Identifier dimension) {
        return GeyserApi.api().provider(DimensionPredicate.class, dimension, false);
    }
}
