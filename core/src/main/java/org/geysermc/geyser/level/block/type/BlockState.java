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
        return (T) value;
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
        
        for (int i = keys.length; i-- != 0;) {
            if (keys[i] == property) {
                return this.states[i];
            }
        }
        return null;
    }

    
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
            
            return this;
        }

        

        
        
        
        
        
        
        
        

        
        
        
        
        
        
        int diff = 0;
        for (int i = keys.length - 1; i >= 0; i--) {
            if (keys[i] != property) {
                diff += keys[i].valuesCount();
            } else {
                break;
            }
        }

        
        
        
        int thatOffset = property.indexOf(value);
        int thisOffset = property.indexOf(currentValue);
        if (diff == 0) {
            
            
            
            
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
                        .append(this.states[i].toString().toLowerCase(Locale.ROOT)); 
                if (i < propertyKeys.length - 1) {
                    builder.append(",");
                }
            }
        }
        return builder.toString();
    }

    
    @NonNull
    public static BlockState of(int javaId) {
        return BlockRegistries.BLOCK_STATES.getOrDefault(javaId, BlockRegistries.BLOCK_STATES.get(Block.JAVA_AIR_ID));
    }
}
