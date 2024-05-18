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

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import org.geysermc.geyser.level.block.property.Property;
import org.geysermc.geyser.registry.BlockRegistries;

import java.util.Locale;

public final class BlockState {
    private final Block block;
    private final int javaId;
    private final Reference2ObjectMap<Property<?>, Comparable<?>> states;

    BlockState(Block block, int javaId) {
        this(block, javaId, Reference2ObjectMaps.emptyMap());
    }

    BlockState(Block block, int javaId, Reference2ObjectMap<Property<?>, Comparable<?>> states) {
        this.block = block;
        this.javaId = javaId;
        this.states = states;
    }

    public <T extends Comparable<T>> T getValue(Property<T> property) {
        //noinspection unchecked
        return (T) states.get(property);
    }

    public Block block() {
        return block;
    }

    public int javaId() {
        return javaId;
    }

    public boolean is(Block block) {
        return this.block == block;
    }

    @Override
    public String toString() {
        if (this.states.isEmpty()) {
            return this.block.javaIdentifier().toString();
        }
        return this.block.javaIdentifier().toString() + "[" + paramsToString() + "]";
    }

    private String paramsToString() {
        StringBuilder builder = new StringBuilder();
        var it = this.states.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            builder.append(entry.getKey().name())
                    .append("=")
                    .append(entry.getValue().toString().toLowerCase(Locale.ROOT)); // lowercase covers enums
            if (it.hasNext()) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    public static BlockState of(int javaId) {
        return BlockRegistries.BLOCK_STATES.get(javaId);
    }
}
