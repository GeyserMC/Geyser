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

package org.geysermc.geyser.api.item.custom.v2.component.java;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.GenericBuilder;

/**
 * The equippable component is used to mark an item as equippable.
 * Bedrock allows specifying the slot where an item can be worn.
 * @since 2.9.3
 */
public interface JavaEquippable {

    /**
     * The equipment slot where this item
     * can be worn.
     *
     * @return the equipment slot
     */
    @NonNull EquipmentSlot slot();

    /**
     * Creates a builder for the equippable component.
     *
     * @return a new builder
     */
    static @NonNull Builder builder() {
        return GeyserApi.api().provider(JavaEquippable.Builder.class);
    }

    /**
     * Creates an equippable component for an equipment slot.
     *
     * @param slot the slot in which the item can be equipped
     * @return the Equippable component
     */
    static @NonNull JavaEquippable of(EquipmentSlot slot) {
        return builder().slot(slot).build();
    }

    /**
     * Builder for the equippable component
     */
    interface Builder extends GenericBuilder<JavaEquippable> {

        /**
         * The equipment slot where the item can be equipped
         * 
         * @param slot the equipment slot
         * @see JavaEquippable#slot()
         * @return this builder
         */
        @This
        Builder slot(@NonNull EquipmentSlot slot);

        /**
         * Creates the equippable component.
         *
         * @return the new component
         */
        @Override
        JavaEquippable build();
    }

    /**
     * The slot in which the equipment can be worn.
     */
    enum EquipmentSlot {
        HEAD,
        CHEST,
        LEGS,
        FEET,
        BODY,
        SADDLE
    }
}
