/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.mappings;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.registry.mappings.util.CustomBlockMapping;
import org.geysermc.geyser.registry.mappings.versions.block.BlockMappingsReader_v1;
import org.geysermc.geyser.registry.mappings.versions.item.ItemMappingsReader_v1;
import org.geysermc.geyser.registry.mappings.versions.item.ItemMappingsReader_v2;

import java.util.function.UnaryOperator;

public record MappingsType<K, V>(String name, Int2ObjectMap<MappingsReader<K, V>> readers) {
    public static final MappingsType<String, CustomBlockMapping> BLOCKS = create("blocks", builder -> builder
        .with(1, new BlockMappingsReader_v1())
        .with(2, new BlockMappingsReader_v1()));
    public static final MappingsType<Identifier, CustomItemDefinition> ITEMS = create("items", builder -> builder
        .with(1, new ItemMappingsReader_v1())
        .with(2, new ItemMappingsReader_v2()));
    
    private static <K, V> MappingsType<K, V> create(String name, UnaryOperator<Builder<K, V>> builder) {
        return new MappingsType<>(name, Int2ObjectMaps.unmodifiable(builder.apply(new Builder<>()).readers));
    }

    private static class Builder<K, V> {
        private final Int2ObjectMap<MappingsReader<K, V>> readers = new Int2ObjectOpenHashMap<>();

        public Builder<K, V> with(int version, MappingsReader<K, V> reader) {
            readers.put(version, reader);
            return this;
        }
    }
}
