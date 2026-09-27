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
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.java.reader.JavaRegistryReaders;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.Tag;

/**
 * Defines Geyser-wide {@link JavaRegistry}s holding built-in Java content, for example, blocks and items. This content is static and will be the same across all sessions.
 * These registries are initialised and populated early within Geyser's start-up process (see {@link Registries#load()}, which calls {@link BuiltInJavaRegistries#bootstrap()}).
 *
 * <p>Should you need access to these registries, you can access a registry directly using its static field. Note that whilst these registries are exposed as {@link MutableJavaRegistry}s,
 * they can only be registered to early in the start-up process. They will be frozen after {@link BuiltInJavaRegistries#freeze()} is called.
 * <em>You should never call {@link MutableJavaRegistry#clear()} on any of these registries.</em></p>
 *
 * <p>{@link MutableJavaRegistry} extends {@link JavaRegistry} and {@link JavaRegistryLookup}, which will provide plenty of utility methods to access registry content in all sorts of ways.
 * They also implement {@link Iterable}, allowing easy {@code for}-loops over them.</p>
 *
 * <p>In some cases you might need a {@link JavaRegistryProvider}, in which case you can use {@link BuiltInJavaRegistries#PROVIDER}. Note however that this provider will not support tags
 * or networked registries, so you should prefer to use {@link GeyserSession#javaRegistries()} when possible.</p>
 *
 * <p>Built-in registries are registered at a root registry. To register new built-in registries, simply create a new static field and call {@link BuiltInJavaRegistries#register(JavaRegistryKey)}.
 * Please maintain the same order as in {@link JavaRegistries}. Make sure to not forget to add a call to a bootstrap method in {@link BuiltInJavaRegistries#bootstrap()},
 * so that the registry will be populated at the right time.</p>
 *
 * @see JavaRegistryLookup
 * @see JavaRegistry
 * @see MutableJavaRegistry
 * @see JavaRegistries
 */
public final class BuiltInJavaRegistries {
    private static final MutableJavaRegistry<MutableJavaRegistry<?>> ROOT = new SimpleJavaRegistry<>(JavaRegistries.BUILT_IN_ROOT);
    /**
     * Holds all Java blocks. Can be expanded with non-vanilla blocks by API users.
     */
    public static final MutableJavaRegistry<Block> BLOCK = register(JavaRegistries.BLOCK);
    /**
     * Holds all Java items. Can be expanded with non-vanilla items by API users.
     */
    public static final MutableJavaRegistry<Item> ITEM = register(JavaRegistries.ITEM);
    /**
     * Holds all Java entity types, as a {@link EntityTypeDefinition}.
     */
    public static final MutableJavaRegistry<EntityTypeDefinition<?>> ENTITY_TYPE = register(JavaRegistries.ENTITY_TYPE);

    /**
     * This {@link JavaRegistryProvider} will throw when trying to access networked registries or tags.
     */
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

    /**
     * Registers a new built-in registry.
     *
     * @param key the {@link JavaRegistryKey}
     * @param <T> the type of the registry
     * @return the created registry
     */
    private static <T> MutableJavaRegistry<T> register(JavaRegistryKey<T> key) {
        SimpleJavaRegistry<T> registry = new SimpleJavaRegistry<>(key);
        registry.allowUnsafeAccess();
        return ROOT.register(key.registryKey(), registry);
    }

    public static void bootstrap() {
        ROOT.freeze(id -> null);
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
