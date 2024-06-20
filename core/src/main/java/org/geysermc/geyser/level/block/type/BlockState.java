/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level.block.type;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.level.block.property.Property;
import org.geysermc.geyser.registry.BlockRegistries;

import java.util.Locale;

public final class BlockState {
    private final Block block;
    private final int javaId;
    /**
     * The values of each property of this block state. These should be treated as keys to {@link Block#propertyKeys()}.
     * Of note - the comparable part probably doesn't do anything because we occasionally use strings in place of enums.
     * Will be null if there's only one block state for a block.
     */
    private final Comparable<?>[] states;

    public BlockState(Block block, int javaId) {
        this(block, javaId, null);
    }

    BlockState(Block block, int javaId, Comparable<?>[] states) {
        this.block = block;
        this.javaId = javaId;
        this.states = states;
    }

    public <T extends Comparable<T>> T getValue(Property<T> property) {
        //noinspection unchecked
        return (T) get(property);
    }

    public <T extends Comparable<T>> T getValueNullable(Property<T> property) {
        var value = get(property);
        if (value == null) {
            return null;
        }
        //noinspection unchecked
        return (T) get(property);
    }

    public <T extends Comparable<T>> T getValue(Property<T> property, T def) {
        var value = get(property);
        if (value == null) {
            return def;
        }
        //noinspection unchecked
        return (T) value;
    }

    @Nullable
    private Comparable<?> get(Property<?> property) {
        Property<?>[] keys = this.block.propertyKeys();
        if (keys == null) {
            return null;
        }
        // We're copying the behavior Reference2ObjectArrayMap uses
        for (int i = keys.length; i-- != 0;) {
            if (keys[i] == property) {
                return this.states[i];
            }
        }
        return null;
    }

    /**
     * @return the {@link BlockState} instance with the given value.
     */
    public <T extends Comparable<T>> BlockState withValue(Property<T> property, T value) {
        Property<?>[] keys = this.block.propertyKeys();
        if (keys == null) {
            throw new IllegalStateException(this + " does not have any different states!");
        }

        T currentValue = getValue(property);
        if (currentValue == null) {
            throw new IllegalArgumentException("This BlockState does not have the property " + property);
        }
        if (currentValue.equals(value)) {
            // No action required. This block state is the state we're looking for.
            return this;
        }

        // Diff is how much we will have to traverse as a sort of offset

        // Block states are calculated in a predictable structure:
        // minecraft:cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=true,west=none]
        // minecraft:cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=true,west=low]
        // minecraft:cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=true,west=tall]
        // minecraft:cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=none]
        // minecraft:cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=low]
        // minecraft:cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=false,west=tall]
        // minecraft:cobblestone_wall[east=none,north=none,south=none,up=false,waterlogged=true,west=none]

        // The last value goes through all its iterations, then the next state goes through all its iterations.
        // West goes none -> low -> tall, then waterlogged is toggled as west cycles again.
        // Then when waterlogged goes through all its properties, up is toggled, and west goes through again
        // If we want to find the "up" property in order, then we need to find how many iterations each property
        // after it goes in. West goes for 3, waterlogged goes for 2. Adding those together, we find that we need to
        // add five to get to the next toggle of the up property
        int diff = 0;
        for (int i = keys.length - 1; i >= 0; i--) {
            if (keys[i] != property) {
                diff += keys[i].valuesCount();
            } else {
                break;
            }
        }

        // How many times do we have to jump by diff? This depends on how far away each value is from each other.
        // piston_head[facing=north] might be right next to piston_head[facing=south], which just one diff'd hop.
        // But piston_head[facing=west] is further away, requiring more hops.
        int thatOffset = property.indexOf(value);
        int thisOffset = property.indexOf(currentValue);
        if (diff == 0) {
            // This can happen if the property is at the tail end of the block and there are no other properties to look through
            // If we have minecraft:cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=true,west=none]
            // And want minecraft:cobblestone_wall[east=none,north=none,south=none,up=true,waterlogged=true,west=low]
            // The above for loop will always stop at the first break because the last property has already been found
            diff = 1;
        }
        return of(this.javaId + ((thatOffset - thisOffset) * diff));
    }

    public Block block() {
        return this.block;
    }

    public int javaId() {
        return this.javaId;
    }

    public boolean is(Block block) {
        return this.block == block;
    }

    @Override
    public String toString() {
        if (this.states == null) {
            return this.block.javaIdentifier().toString();
        }
        return this.block.javaIdentifier().toString() + "[" + paramsToString() + "]";
    }

    private String paramsToString() {
        StringBuilder builder = new StringBuilder();
        Property<?>[] propertyKeys = this.block.propertyKeys();
        if (propertyKeys != null) {
            for (int i = 0; i < propertyKeys.length; i++) {
                builder.append(propertyKeys[i].name())
                        .append("=")
                        .append(this.states[i].toString().toLowerCase(Locale.ROOT)); // lowercase covers enums
                if (i < propertyKeys.length - 1) {
                    builder.append(",");
                }
            }
        }
        return builder.toString();
    }

    /**
     * Null-safe method that looks up a Java block state ID in the BLOCK_STATES registry, and defaults to air if not found.
     *
     * @param javaId the Java block state ID to look up.
     * @return the corresponding block state, or air if the given ID wasn't registered and returned null.
     */
    @NonNull
    public static BlockState of(int javaId) {
        return BlockRegistries.BLOCK_STATES.getOrDefault(javaId, BlockRegistries.BLOCK_STATES.get(Block.JAVA_AIR_ID));
    }
}
