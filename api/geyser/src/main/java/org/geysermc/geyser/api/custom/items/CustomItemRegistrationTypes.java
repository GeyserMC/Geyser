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

package org.geysermc.geyser.api.custom.items;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * This class represents the different types of custom item registration
 */
public class CustomItemRegistrationTypes {
    private Boolean unbreakable = null;
    private Integer customModelData = null;
    private Integer damagePredicate = null;

    public @Nullable Boolean unbreakable() {
        return unbreakable;
    }

    public void unbreakable(@Nullable Boolean unbreakable) {
        this.unbreakable = unbreakable;
    }

    public @Nullable Integer customModelData() {
        return customModelData;
    }

    public void customModelData(@Nullable Integer customModelData) {
        this.customModelData = customModelData;
    }

    public @Nullable Integer damagePredicate() {
        return damagePredicate;
    }

    public void damagePredicate(@Nullable Integer damagePredicate) {
        this.damagePredicate = damagePredicate;
    }

    /**
     * Checks if the item has at least one registration type set
     *
     * @return true if the item has any registrations set
     */
    public boolean hasRegistrationType() {
        return this.unbreakable != null ||
                this.customModelData != null ||
                this.damagePredicate != null;
    }
}
