/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session.cache.registry;

import it.unimi.dsi.fastutil.ints.IntList;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;

import java.util.List;

// TODO javadocs
public interface JavaRegistryProvider {

    <T> JavaRegistry<T> registry(JavaRegistryKey<T> registryKey);

    IntList rawTag(Tag<?> tag);

    default <T> List<T> tag(Tag<T> tag) {
        return mapRawArray(this, tag.registry(), rawTag(tag));
    }

    /**
     * Should only be used when the network ID of an element is already known. If not, prefer using the {@link JavaRegistryProvider#is(Tag, Object)} shorthand method.
     */
    default boolean is(Tag<?> tag, int id) {
        return rawTag(tag).contains(id);
    }

    default <T> boolean is(Tag<T> tag, T object) {
        return rawTag(tag).contains(tag.registry().getId(this, object));
    }

    /**
     * @return true if the specified network ID is in the given {@link HolderSet} set.
     */
    default <T> boolean is(JavaRegistryKey<T> registry, @Nullable HolderSet holderSet, int id) {
        if (holderSet == null) {
            return false;
        }

        IntList entries = holderSet.resolve(key -> {
            // This should never happen, since a key in a HolderSet is always a tag
            // We check for it anyway
            if (key.value().startsWith("#")) {
                key = Key.key(key.namespace(), key.value().substring(1));
            }
            return rawTag(new Tag<>(registry, key));
        });

        return entries.contains(id);
    }

    /**
     * Maps a raw array of network IDs to their respective objects.
     */
    private static <T> List<T> mapRawArray(JavaRegistryProvider registries, JavaRegistryKey<T> registry, IntList array) {
        return array.intStream().mapToObj(i -> registry.get(registries, i)).toList();
    }
}
