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

package org.geysermc.geyser.session.cache.tags;

import java.util.List;
import java.util.function.Function;
import lombok.Data;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.session.cache.TagCache;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;

/**
 * Similar to vanilla Minecraft's HolderSets, stores either a tag or a list of IDs (this list can also be represented as a single ID in vanilla HolderSets).
 *
 * Because HolderSets utilise tags, when loading a HolderSet, Geyser must store tags for the registry the HolderSet is for (it must be listed in {@link org.geysermc.geyser.session.cache.registry.JavaRegistries}).
 *
 * Use the {@link HolderSet#readHolderSet} method to easily read a HolderSet from NBT sent by a server. To turn the HolderSet into a list of network IDs, use the {@link HolderSet#resolve} method.
 */
@Data
public final class HolderSet<T> {

    private final @Nullable Tag<T> tag;
    private final int @Nullable [] holders;

    public HolderSet(int @NonNull [] holders) {
        this.tag = null;
        this.holders = holders;
    }

    public HolderSet(@NonNull Tag<T> tagId) {
        this.tag = tagId;
        this.holders = null;
    }

    /**
     * Resolves the HolderSet. If the HolderSet is a list of IDs, this will be returned. If it is a tag, the tag will be resolved from the tag cache.
     *
     * @return the HolderSet turned into a list of network IDs.
     */
    public int[] resolve(TagCache tagCache) {
        if (holders != null) {
            return holders;
        }

        assert tag != null;
        return tagCache.get(tag);
    }

    /**
     * Reads a HolderSet from an object from NBT.
     *
     * @param registry the registry the HolderSet contains IDs from.
     * @param holderSet the HolderSet as an object from NBT.
     * @param keyIdMapping a function that maps resource location IDs in the HolderSet's registry to their network IDs.
     */
    public static <T> HolderSet<T> readHolderSet(JavaRegistryKey<T> registry, @Nullable Object holderSet, Function<Key, Integer> keyIdMapping) {
        if (holderSet == null) {
            return new HolderSet<>(new int[]{});
        }

        if (holderSet instanceof String stringTag) {
            if (stringTag.startsWith("#")) {
                // Tag
                return new HolderSet<>(new Tag<>(registry, Key.key(stringTag.substring(1)))); // Remove '#' at beginning that indicates tag
            } else if (stringTag.isEmpty()) {
                return new HolderSet<>(new int[]{});
            }
            return new HolderSet<>(new int[]{keyIdMapping.apply(Key.key(stringTag))});
        } else if (holderSet instanceof List<?> list) {
            // Assume the list is a list of strings
            return new HolderSet<>(list.stream().map(o -> (String) o).map(Key::key).map(keyIdMapping).mapToInt(Integer::intValue).toArray());
        }
        throw new IllegalArgumentException("Holder set must either be a tag, a string ID or a list of string IDs");
    }
}
