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

package org.geysermc.geyser.api.predicate.context.item;

import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.GeyserApi;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents charged projectiles which are stored in the {@code minecraft:charged_projectiles} component.
 *
 * @see ItemPredicateContext#chargedProjectiles()
 * @since 2.9.3
 */
@ApiStatus.NonExtendable
public interface ChargedProjectile {

    /**
     * @return the type of the projectile
     * @since 2.9.3
     */
    @NonNull ChargeType type();

    /**
     * @return the amount present of this projectile
     * @since 2.9.3
     */
    @Positive int count();

    /**
     * Creates a new charged projectile representation.
     *
     * @param type the charge type of the projectile
     * @param count the amount of charges present
     * @return the charged projectile
     * @since 2.9.3
     */
    static ChargedProjectile of(@NonNull ChargeType type, @Positive int count) {
        return GeyserApi.api().provider(ChargedProjectile.class, type, count);
    }

    /**
     * Represents the type of the charge
     * @since 2.9.3
     */
    enum ChargeType {
        /**
         * Any item that is not {@code minecraft:firework_rocket}.
         * @since 2.9.3
         */
        ARROW,
        /**
         * {@code minecraft:firework_rocket}
         * @since 2.9.3
         */
        ROCKET
    }
}
