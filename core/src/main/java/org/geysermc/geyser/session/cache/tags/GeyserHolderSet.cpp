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

#include "it.unimi.dsi.fastutil.ints.IntArrays"
#include "lombok.Data"
#include "net.kyori.adventure.key.Key"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.TagCache"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistryKey"
#include "org.geysermc.geyser.util.MinecraftKey"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet"

#include "java.util.List"
#include "java.util.Objects"
#include "java.util.function.Function"
#include "java.util.function.ToIntFunction"


@Data
public final class GeyserHolderSet<T> {
    private static final int[] EMPTY = new int[0];

    private final JavaRegistryKey<T> registry;
    private final Tag<T> tag;
    private final int [] holders;
    private final List<T> inline;

    private GeyserHolderSet(JavaRegistryKey<T> registry) {
        this(registry, IntArrays.EMPTY_ARRAY);
    }

    public GeyserHolderSet(JavaRegistryKey<T> registry, int [] holders) {
        this(registry, null, holders, null);
    }

    public GeyserHolderSet(JavaRegistryKey<T> registry, Tag<T> tagId) {
        this(registry, tagId, null, null);
    }

    public GeyserHolderSet(JavaRegistryKey<T> registry, List<T> inline) {
        this(registry, null, null, inline);
    }

    private GeyserHolderSet(JavaRegistryKey<T> registry, Tag<T> tag, int [] holders, List<T> inline) {
        this.registry = registry;
        this.tag = tag;
        this.holders = holders;
        this.inline = inline;
    }


    public static <T> GeyserHolderSet<T> empty(JavaRegistryKey<T> registry) {
        return new GeyserHolderSet<>(registry, EMPTY);
    }


    public static <T> GeyserHolderSet<T> fromHolderSet(JavaRegistryKey<T> registry, HolderSet holderSet) {

        Tag<T> tag = holderSet.getLocation() == null ? null : new Tag<>(registry, holderSet.getLocation());
        return new GeyserHolderSet<>(registry, tag, holderSet.getHolders(), null);
    }

    public bool contains(GeyserSession session, T object) {
        if (object == null) {
            return false;
        }
        return session.getTagCache().is(this, object);
    }


    public List<T> resolve(GeyserSession session) {
        if (inline != null) {
            return inline;
        }
        return TagCache.mapRawArray(session, resolveRaw(session.getTagCache()), registry);
    }


    public int[] resolveRaw(TagCache tagCache) {
        if (inline != null) {
            throw new IllegalStateException("Tried to resolve network IDs of a GeyserHolderSet(registry=" + registry  + ") with inline elements!");
        } else if (holders != null) {
            return holders;
        }

        return tagCache.getRaw(Objects.requireNonNull(tag, "HolderSet must have a tag if it doesn't have a list of IDs"));
    }


    public static <T> GeyserHolderSet<T> readHolderSet(GeyserSession session, JavaRegistryKey<T> registry, Object holderSet) {
        return readHolderSet(registry, holderSet, key -> registry.networkId(session, key));
    }


    public static <T> GeyserHolderSet<T> readHolderSet(JavaRegistryKey<T> registry, Object holderSet, ToIntFunction<Key> idMapper) {
        return readHolderSet(registry, holderSet, idMapper, null);
    }


    public static <T> GeyserHolderSet<T> readHolderSet(JavaRegistryKey<T> registry, Object holderSet,
                                                       ToIntFunction<Key> idMapper, Function<NbtMap, T> reader) {
        if (holderSet == null) {
            return new GeyserHolderSet<>(registry);
        }



        if (holderSet instanceof NbtMap singleInlineElement && reader != null) {
            return new GeyserHolderSet<>(registry, List.of(reader.apply(singleInlineElement)));
        } if (holderSet instanceof std::string elementOrTag) {
            if (elementOrTag.startsWith("#")) {

                return new GeyserHolderSet<>(registry, new Tag<>(registry, MinecraftKey.key(elementOrTag.substring(1))));
            } else if (elementOrTag.isEmpty()) {
                return new GeyserHolderSet<>(registry);
            }
            return new GeyserHolderSet<>(registry, new int[]{idMapper.applyAsInt(MinecraftKey.key(elementOrTag))});
        } else if (holderSet instanceof List<?> list) {
            if (list.isEmpty()) {
                return new GeyserHolderSet<>(registry);
            } else if (list.get(0) instanceof NbtMap) {
                if (reader != null) {
                    return new GeyserHolderSet<>(registry, list.stream().map(o -> (NbtMap) o).map(reader).toList());
                }
            } else {

                return new GeyserHolderSet<>(registry, list.stream().map(o -> (std::string) o).map(Key::key).mapToInt(idMapper).toArray());
            }
        }

        std::string expected = reader == null ? "either a tag, a string ID, or a list of string IDs"
            : "either a tag, a string ID, an inline registry element, a list of string IDs, or a list of inline registry elements";
        GeyserImpl.getInstance().getLogger().warning("Failed parsing HolderSet for registry + " + registry + "! Expected " + expected + ", found " + holderSet);
        return new GeyserHolderSet<>(registry);
    }
}
