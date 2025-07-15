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

package org.geysermc.geyser.api.item.custom.v2.component.geyser;

import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.GenericBuilder;

/**
 * The throwable component allows creating items that can be thrown.
 * This allows bedrock players to continuously throw an item stack by holding down the use button,
 * instead of having to use individual presses.
 *
 * <p>The component also allows specifying whether bedrock clients should display a swing animation when throwing the item. This defaults to true.</p>
 */
public interface ThrowableComponent {

    /**
     * Whether bedrock clients should display a swing animation when throwing the item. Defaults to true.
     *
     * @return true if bedrock clients should display a swing animation when throwing the item
     */
    boolean doSwingAnimation();

    /**
     * Creates a new builder for the throwable component.
     *
     * @return a new builder
     */
    static Builder builder() {
        return GeyserApi.api().provider(ThrowableComponent.Builder.class);
    }

    /**
     * Creates a throwable component with the given {@code doSwingAnimation} value.
     *
     * @param doSwingAnimation whether bedrock clients should display a swing animation when throwing the item
     * @return a throwable component
     */
    static ThrowableComponent of(boolean doSwingAnimation) {
        return builder().doSwingAnimation(doSwingAnimation).build();
    }

    /**
     * Builder for the throwable component.
     */
    interface Builder extends GenericBuilder<ThrowableComponent> {

        /**
         * Sets whether bedrock clients should display a swing animation when throwing the item.
         *
         * @param doSwingAnimation whether bedrock clients should display a swing animation when throwing the item
         * @see ThrowableComponent#doSwingAnimation()
         * @return this builder
         */
        @This
        Builder doSwingAnimation(boolean doSwingAnimation);

        /**
         * Creates the throwable component.
         *
         * @return the new component
         */
        @Override
        ThrowableComponent build();
    }
}
