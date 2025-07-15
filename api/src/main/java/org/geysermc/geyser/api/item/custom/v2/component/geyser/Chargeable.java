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

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.GenericBuilder;
import org.geysermc.geyser.api.util.Identifier;

import java.util.List;

/**
 * The chargeable component allows creating crossbows
 * or bows. This includes the draw duration, whether the item
 * charges on being drawn, and the ammunition that can be
 * used by the item.
 */
public interface Chargeable {

    /**
     * The maximum draw duration determines how long the weapon
     * can be drawn before releasing automatically. Defaults to {@code 0.0}.
     *
     * @return the maximum draw duration
     */
    @NonNegative float maxDrawDuration();

    /**
     * Whether the item is being charged when being drawn, like a crossbow. Defaults to {@code false}.
     *
     * @return whether drawing the item charges it
     */
    boolean chargeOnDraw();

    /**
     * The identifiers of the Bedrock items that can be
     * used as ammunition by this bow.
     * For example, this can contain {@code minecraft:arrow} to allow arrows to be shot.
     *
     * <p>Items listed <em>must</em> have a {@code minecraft:projectile} component on bedrock to work.
     * Non-vanilla custom items can mark an item as a projectile and add this component by specifying the {@link GeyserDataComponent#PROJECTILE} component.</p>
     *
     * @return all valid ammunition items
     */
    List<@NonNull Identifier> ammunition();

    /**
     * Creates a builder for the Chargeable component.
     *
     * @return a new builder
     */
    static Builder builder() {
        return GeyserApi.api().provider(Chargeable.Builder.class);
    }

    /**
     * Builder for the chargeable component.
     */
    interface Builder extends GenericBuilder<Chargeable> {

        /**
         * Sets the maximum draw duration before the item is released.
         *
         * @param maxDrawDuration the non-negative maximum charging duration
         * @see Chargeable#maxDrawDuration()
         * @return this builder
         */
        @This
        Builder maxDrawDuration(@NonNegative float maxDrawDuration);

        /**
         * Sets whether the item is charged when drawing.
         *
         * @param chargeOnDraw whether drawing charges the item
         * @see Chargeable#chargeOnDraw()
         * @return this builder
         */
        @This
        Builder chargeOnDraw(boolean chargeOnDraw);

        /**
         * Adds an item that can be used as ammunition, such as {@code minecraft:arrow}.
         * This will throw when trying to add an item that was already added.
         *
         * @param ammunition the Bedrock item identifier of possible ammunition
         * @see Chargeable#ammunition()
         * @return this builder
         */
        @This
        Builder ammunition(@NonNull Identifier ammunition);

        /**
         * Creates the chargeable component.
         *
         * @return the new component
         */
        @Override
        Chargeable build();
    }
}
