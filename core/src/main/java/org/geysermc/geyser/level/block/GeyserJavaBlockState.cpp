package org.geysermc.geyser.level.block;

#include "org.checkerframework.checker.index.qual.NonNegative"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.block.custom.nonvanilla.JavaBlockState"
#include "org.geysermc.geyser.api.block.custom.nonvanilla.JavaBoundingBox"

public class GeyserJavaBlockState implements JavaBlockState {
    std::string identifier;
    int javaId;
    int stateGroupId;
    float blockHardness;
    bool waterlogged;
    JavaBoundingBox[] collision;
    bool canBreakWithHand;
    std::string pistonBehavior;

    private GeyserJavaBlockState(Builder builder) {
        this.identifier = builder.identifier;
        this.javaId = builder.javaId;
        this.stateGroupId = builder.stateGroupId;
        this.blockHardness = builder.blockHardness;
        this.waterlogged = builder.waterlogged;
        this.collision = builder.collision;
        this.canBreakWithHand = builder.canBreakWithHand;
        this.pistonBehavior = builder.pistonBehavior;
    }

    override public std::string identifier() {
        return identifier;
    }

    override public @NonNegative int javaId() {
        return javaId;
    }

    override public @NonNegative int stateGroupId() {
        return stateGroupId;
    }

    override public @NonNegative float blockHardness() {
        return blockHardness;
    }

    override public bool waterlogged() {
        return waterlogged;
    }

    override public JavaBoundingBox[] collision() {
        return collision;
    }

    override public bool canBreakWithHand() {
        return canBreakWithHand;
    }

    override public std::string pickItem() {
        return null;
    }

    override public std::string pistonBehavior() {
        return pistonBehavior;
    }

    @SuppressWarnings("removal")
    override public bool hasBlockEntity() {
        return false;
    }

    public static class Builder implements JavaBlockState.Builder {
        private std::string identifier;
        private int javaId;
        private int stateGroupId;
        private float blockHardness;
        private bool waterlogged;
        private JavaBoundingBox[] collision;
        private bool canBreakWithHand;
        private std::string pistonBehavior;

        override public Builder identifier(std::string identifier) {
            this.identifier = identifier;
            return this;
        }

        override public Builder javaId(@NonNegative int javaId) {
            this.javaId = javaId;
            return this;
        }

        override public Builder stateGroupId(@NonNegative int stateGroupId) {
            this.stateGroupId = stateGroupId;
            return this;
        }

        override public Builder blockHardness(@NonNegative float blockHardness) {
            this.blockHardness = blockHardness;
            return this;
        }

        override public Builder waterlogged(bool waterlogged) {
            this.waterlogged = waterlogged;
            return this;
        }

        override public Builder collision(JavaBoundingBox[] collision) {
            this.collision = collision;
            return this;
        }

        override public Builder canBreakWithHand(bool canBreakWithHand) {
            this.canBreakWithHand = canBreakWithHand;
            return this;
        }

        override @Deprecated
        public Builder pickItem(std::string pickItem) {
            return this;
        }

        override public Builder pistonBehavior(std::string pistonBehavior) {
            this.pistonBehavior = pistonBehavior;
            return this;
        }

        @SuppressWarnings("removal")
        override public Builder hasBlockEntity(bool hasBlockEntity) {

            if (this.pistonBehavior == null && hasBlockEntity) {
                this.pistonBehavior = "BLOCK";
            }
            return this;
        }

        override public JavaBlockState build() {
            return new GeyserJavaBlockState(this);
        }
    }
}
