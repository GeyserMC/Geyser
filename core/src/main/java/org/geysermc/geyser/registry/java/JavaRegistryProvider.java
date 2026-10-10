/*
 * Copyright (c) 2025-2026 GeyserMC. http://geysermc.org
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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;

import java.util.List;

/**
 * Provides access to a {@link JavaRegistry} for each registry defined in {@link JavaRegistries}, a well as access to tags for each registry.
 *
 * <p>This interface also has utility methods for checking if a registry tag contains an object. Alternatively, you can use one of the following shorthand methods for blocks and items:</p>
 *
 * <ul>
 *     <li>{@link Block#is(GeyserSession, Tag)}</li>
 *     <li>{@link Block#is(GeyserSession, HolderSet)}</li>
 *     <li>{@link GeyserItemStack#is(GeyserSession, Tag)}</li>
 *     <li>{@link GeyserItemStack#is(GeyserSession, HolderSet)}</li>
 *     <li>{@link Item#is(GeyserSession, Tag)}</li>
 *     <li>{@link Item#is(GeyserSession, HolderSet)}</li>
 * </ul>
 *
 * @see JavaRegistry
 * @see JavaRegistries
 * @see Tag
 * @see Provider
 */
public interface JavaRegistryProvider {

    /**
     * Provides a {@link JavaRegistry} for the given {@code registryKey}.
     *
     * @param registryKey the registry key to get a {@link JavaRegistry} for
     * @param <T> the type of the registry
     * @return the {@link JavaRegistry} for the {@code registryKey}
     */
    <T> JavaRegistry<T> registry(JavaRegistryKey<T> registryKey);

    /**
     * Provides a list of raw network IDs for the given {@code tag}. This can be an empty list.
     *
     * <p>Generally, prefer using {@link JavaRegistryProvider#tag(Tag)} as it maps the network IDs into a list of objects.</p>
     *
     * @param tag the tag to look up
     * @return the list of raw network IDs the tag holds
     */
    IntList rawTag(Tag<?> tag);

    /**
     * Provides a list of objects for the given {@code tag}. This can be an empty list.
     *
     * @param tag the tag to look up
     * @param <T> the type of the registry of the tag
     * @return the list of objects the tag holds
     */
    default <T> List<T> tag(Tag<T> tag) {
        return mapRawArray(this, tag.registry(), rawTag(tag));
    }

    /**
     * Checks and returns true if the given {@code tag} contains the given {@code id}.
     *
     * <p>If the network ID of the object is unknown, prefer using the {@link JavaRegistryProvider#is(Tag, Object)} shorthand method.</p>
     *
     * @param tag the tag to look up
     * @param id the id to check for
     * @return true if the {@code id} was present in the {@code tag}
     * @see JavaRegistryProvider#is(Tag, Object)
     */
    default boolean is(Tag<?> tag, int id) {
        return rawTag(tag).contains(id);
    }

    /**
     * Checks and returns true if the given {@code tag} contains the given {@code object}.
     *
     * @param tag the tag to look up
     * @param object the object to check for
     * @param <T> the type of the registry of the tag
     * @return true if the {@code object} was present in the {@code tag}
     * @see JavaRegistryProvider#is(Tag, int)
     */
    default <T> boolean is(Tag<T> tag, T object) {
        return rawTag(tag).contains(registry(tag.registry()).getId(object).orElse(-1));
    }

    /**
     * Checks and returns true if the given {@code holderSet} contains the given {@code id}.
     *
     * @param registry the registry the {@code holderSet} belongs to
     * @param holderSet the set. If null, this method will always return false
     * @param id the id to check for
     * @param <T> the type of the registry
     * @return true if the specified network ID is in the given {@link HolderSet}
     */
    default <T> boolean is(JavaRegistryKey<T> registry, @Nullable HolderSet holderSet, int id) {
        if (holderSet == null) {
            return false;
        }

        IntList entries = holderSet.resolve(key -> rawTag(new Tag<>(registry, key)));
        return entries.contains(id);
    }

    /**
     * Provides a {@link JavaRegistryProvider}.
     */
    @FunctionalInterface
    interface Provider {

        JavaRegistryProvider registries();
    }

    private static <T> List<T> mapRawArray(JavaRegistryProvider registries, JavaRegistryKey<T> registry, IntList array) {
        return array.intStream().mapToObj(i -> registry.getOrThrow(registries, i)).toList();
    }
}
