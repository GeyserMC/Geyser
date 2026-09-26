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

package org.geysermc.geyser.registry.java;

import it.unimi.dsi.fastutil.ints.IntList;
import org.geysermc.geyser.entity.EntityTypeDefinition;
import org.geysermc.geyser.entity.VanillaEntities;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.registry.java.reader.JavaRegistryReaders;
import org.geysermc.geyser.session.cache.tags.Tag;

public final class BuiltInJavaRegistries {
    private static final MutableJavaRegistry<MutableJavaRegistry<?>> ROOT = new SimpleJavaRegistry<>();
    public static final MutableJavaRegistry<Block> BLOCK = register(JavaRegistries.BLOCK);
    public static final MutableJavaRegistry<Item> ITEM = register(JavaRegistries.ITEM);
    public static final MutableJavaRegistry<EntityTypeDefinition<?>> ENTITY_TYPE = register(JavaRegistries.ENTITY_TYPE);

    public static final JavaRegistryProvider PROVIDER = new JavaRegistryProvider() {
        @Override
        public <T> JavaRegistry<T> registry(JavaRegistryKey<T> registryKey) {
            return ROOT.getByKey(registryKey.registryKey())
                .map(registry -> (JavaRegistry<T>) registry.data())
                .orElseThrow(() -> new IllegalArgumentException("Unknown built-in Java registry: " + registryKey));
        }

        @Override
        public IntList rawTag(Tag<?> tag) {
            throw new IllegalStateException("Unable to provide tags at this stage");
        }
    };

    private BuiltInJavaRegistries() {}

    private static <T> MutableJavaRegistry<T> register(JavaRegistryKey<T> key) {
        MutableJavaRegistry<T> registry = new SimpleJavaRegistry<>();
        return ROOT.register(key.registryKey(), registry);
    }

    public static void bootstrap() {
        Blocks.bootstrap();
        Items.bootstrap();
        VanillaEntities.init();

        JavaRegistryReaders.bootstrap();
    }

    public static void freeze() {
        // We shouldn't need to bind any entries here since all entries are built-in
        ROOT.forEach(registry -> registry.freeze(id -> null));
    }
}
