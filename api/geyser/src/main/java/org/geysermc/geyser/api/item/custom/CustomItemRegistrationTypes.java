/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.item.custom;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.GeyserApi;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * This class represents the different types of custom item registration
 */
public interface CustomItemRegistrationTypes {
    @NonNull Optional<Boolean> unbreaking();

    @NonNull OptionalInt customModelData();

    @NonNull OptionalInt damagePredicate();

    /**
     * Checks if the item has at least one registration type set
     *
     * @return true if the item has any registrations set
     */
    default boolean hasRegistrationTypes() {
        return this.unbreaking().isEmpty() ||
                this.customModelData().isEmpty() ||
                this.damagePredicate().isEmpty();
    }

    static CustomItemRegistrationTypes.Builder builder() {
        return GeyserApi.api().providerManager().builderProvider().provideBuilder(CustomItemRegistrationTypes.Builder.class);
    }

    interface Builder {
        Builder unbreaking(@NonNull Optional<Boolean> unbreaking);
        Builder unbreaking(boolean unbreaking);

        Builder customModelData(@NonNull OptionalInt customModelData);
        Builder customModelData(int customModelData);

        Builder damagePredicate(@NonNull OptionalInt damagePredicate);
        Builder damagePredicate(int damagePredicate);

        CustomItemRegistrationTypes build();
    }
}
