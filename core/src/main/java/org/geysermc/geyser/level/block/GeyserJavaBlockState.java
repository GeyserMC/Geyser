/*
 * Copyright (c) 2023-2024 GeyserMC. http://geysermc.org
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
package org.geysermc.geyser.level.block;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.block.custom.nonvanilla.JavaBlockState;
import org.geysermc.geyser.api.block.custom.nonvanilla.JavaBoundingBox;

public class GeyserJavaBlockState implements JavaBlockState {
    String identifier;
    int javaId;
    int stateGroupId;
    float blockHardness;
    boolean waterlogged;
    JavaBoundingBox[] collision;
    boolean canBreakWithHand;
    String pickItem;
    String pistonBehavior;
    boolean hasBlockEntity;

    private GeyserJavaBlockState(Builder builder) {
        this.identifier = builder.identifier;
        this.javaId = builder.javaId;
        this.stateGroupId = builder.stateGroupId;
        this.blockHardness = builder.blockHardness;
        this.waterlogged = builder.waterlogged;
        this.collision = builder.collision;
        this.canBreakWithHand = builder.canBreakWithHand;
        this.pickItem = builder.pickItem;
        this.pistonBehavior = builder.pistonBehavior;
        this.hasBlockEntity = builder.hasBlockEntity;
    }

    @Override
    public @NonNull String identifier() {
        return identifier;
    }

    @Override
    public @NonNegative int javaId() {
        return javaId;
    }

    @Override
    public @NonNegative int stateGroupId() {
        return stateGroupId;
    }

    @Override
    public @NonNegative float blockHardness() {
        return blockHardness;
    }

    @Override
    public boolean waterlogged() {
        return waterlogged;
    }

    @Override
    public @NonNull JavaBoundingBox[] collision() {
        return collision;
    }

    @Override
    public boolean canBreakWithHand() {
        return canBreakWithHand;
    }

    @Override
    public @Nullable String pickItem() {
        return pickItem;
    }

    @Override
    public @Nullable String pistonBehavior() {
        return pistonBehavior;
    }

    @Override
    public boolean hasBlockEntity() {
        return hasBlockEntity;
    }

    public static class Builder implements JavaBlockState.Builder {
        private String identifier;
        private int javaId;
        private int stateGroupId;
        private float blockHardness;
        private boolean waterlogged;
        private JavaBoundingBox[] collision;
        private boolean canBreakWithHand;
        private String pickItem;
        private String pistonBehavior;
        private boolean hasBlockEntity;

        @Override
        public Builder identifier(@NonNull String identifier) {
            this.identifier = identifier;
            return this;
        }

        @Override
        public Builder javaId(@NonNegative int javaId) {
            this.javaId = javaId;
            return this;
        }

        @Override
        public Builder stateGroupId(@NonNegative int stateGroupId) {
            this.stateGroupId = stateGroupId;
            return this;
        }

        @Override
        public Builder blockHardness(@NonNegative float blockHardness) {
            this.blockHardness = blockHardness;
            return this;
        }

        @Override
        public Builder waterlogged(boolean waterlogged) {
            this.waterlogged = waterlogged;
            return this;
        }

        @Override
        public Builder collision(@NonNull JavaBoundingBox[] collision) {
            this.collision = collision;
            return this;
        }

        @Override
        public Builder canBreakWithHand(boolean canBreakWithHand) {
            this.canBreakWithHand = canBreakWithHand;
            return this;
        }

        @Override
        public Builder pickItem(@Nullable String pickItem) {
            this.pickItem = pickItem;
            return this;
        }

        @Override
        public Builder pistonBehavior(@Nullable String pistonBehavior) {
            this.pistonBehavior = pistonBehavior;
            return this;
        }

        @Override
        public Builder hasBlockEntity(boolean hasBlockEntity) {
            this.hasBlockEntity = hasBlockEntity;
            return this;
        }

        @Override
        public JavaBlockState build() {
            return new GeyserJavaBlockState(this);
        }
    }
}
