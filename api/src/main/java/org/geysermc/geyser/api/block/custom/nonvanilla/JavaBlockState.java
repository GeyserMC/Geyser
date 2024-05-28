/*
 * Copyright (c) 2023 GeyserMC. http://geysermc.org
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
package org.geysermc.geyser.api.block.custom.nonvanilla;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.GeyserApi;

public interface JavaBlockState {
    /**
     * Gets the identifier of the block state
     * 
     * @return the identifier of the block state
     */
    @NonNull String identifier();

    /**
     * Gets the Java ID of the block state
     * 
     * @return the Java ID of the block state
     */
    @NonNegative int javaId();

    /**
     * Gets the state group ID of the block state
     * 
     * @return the state group ID of the block state
     */
    @NonNegative int stateGroupId();
    
    /**
     * Gets the block hardness of the block state
     * 
     * @return the block hardness of the block state
     */
    @NonNegative float blockHardness();

    /**
     * Gets whether the block state is waterlogged
     * 
     * @return whether the block state is waterlogged
     */
    boolean waterlogged();

    /**
     * Gets the collision of the block state
     * 
     * @return the collision of the block state
     */
    @NonNull JavaBoundingBox[] collision();

    /**
     * Gets whether the block state can be broken with hand
     * 
     * @return whether the block state can be broken with hand
     */
    boolean canBreakWithHand();

    /**
     * Gets the pick item of the block state
     * 
     * @return the pick item of the block state
     */
    @Nullable String pickItem();

    /**
     * Gets the piston behavior of the block state
     * 
     * @return the piston behavior of the block state
     */
    @Nullable String pistonBehavior();

    /**
     * Gets whether the block state has block entity
     * 
     * @return whether the block state has block entity
     */
    boolean hasBlockEntity();

    /**
     * Creates a new {@link JavaBlockState.Builder} instance
     * 
     * @return a new {@link JavaBlockState.Builder} instance
     */
    static JavaBlockState.Builder builder() {
        return GeyserApi.api().provider(JavaBlockState.Builder.class);
    }

    interface Builder {
        Builder identifier(@NonNull String identifier);

        Builder javaId(@NonNegative int javaId);

        Builder stateGroupId(@NonNegative int stateGroupId);

        Builder blockHardness(@NonNegative float blockHardness);

        Builder waterlogged(boolean waterlogged);

        Builder collision(@NonNull JavaBoundingBox[] collision);

        Builder canBreakWithHand(boolean canBreakWithHand);

        Builder pickItem(@Nullable String pickItem);

        Builder pistonBehavior(@Nullable String pistonBehavior);

        Builder hasBlockEntity(boolean hasBlockEntity);

        JavaBlockState build();
    }
}
