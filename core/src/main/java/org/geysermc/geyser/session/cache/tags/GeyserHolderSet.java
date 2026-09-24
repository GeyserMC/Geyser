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

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import lombok.Data;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.TagCache;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Similar to vanilla Minecraft's HolderSets, stores either a tag, a list of IDs (this list can also be represented as a single ID in vanilla HolderSets),
 * or a list of inline elements (only supported by some HolderSets, and can also be represented as a single inline element in vanilla HolderSets).
 *
 * <p>Because HolderSets utilise tags, when loading a HolderSet, Geyser must store tags for the registry the HolderSet is for. This is done for all registries registered in
 * {@link org.geysermc.geyser.session.cache.registry.JavaRegistries}.</p>
 *
 * <p>Use the {@link GeyserHolderSet#readHolderSet} method to easily read a HolderSet from NBT sent by a server. To turn the HolderSet into a list of network IDs, use the {@link GeyserHolderSet#resolveRaw} method.
 * To turn the HolderSet into a list of objects, use the {@link GeyserHolderSet#resolve} method.</p>
 *
 * <p>Note that the {@link GeyserHolderSet#resolveRaw(TagCache)} method will fail for inline HolderSets, since inline elements are not registered and as such have no network ID.</p>
 */
@Data
public final class GeyserHolderSet<T> {
    private static final int[] EMPTY = new int[0];

    private final JavaRegistryKey<T> registry;
    private final @Nullable Tag<T> tag;
    private final @Nullable IntList holders;
    private final @Nullable List<T> inline;
    private final @Nullable List<HolderEntry<T>> entries;

    public sealed interface HolderEntry<T> permits HolderEntry.Direct, HolderEntry.Id {
        record Direct<T>(T value) implements HolderEntry<T> {}
        record Id<T>(int id) implements HolderEntry<T> {}
    }

    private GeyserHolderSet(JavaRegistryKey<T> registry) {
        this(registry, IntLists.emptyList());
    }

    public GeyserHolderSet(JavaRegistryKey<T> registry, @Nullable IntList holders) {
        this(registry, null, holders, null, null);
    }

    public GeyserHolderSet(JavaRegistryKey<T> registry, @NonNull Tag<T> tagId) {
        this(registry, tagId, null, null, null);
    }

    public GeyserHolderSet(JavaRegistryKey<T> registry, @NonNull List<T> inline) {
        this(registry, null, null, inline, null);
    }

    private GeyserHolderSet(JavaRegistryKey<T> registry, @Nullable Tag<T> tag, @Nullable IntList holders, @Nullable List<T> inline) {
        this(registry, tag, holders, inline, null);
    }

    private GeyserHolderSet(JavaRegistryKey<T> registry, @Nullable Tag<T> tag, @Nullable IntList holders, @Nullable List<T> inline, @Nullable List<HolderEntry<T>> entries) {
        this.registry = registry;
        this.tag = tag;
        this.holders = holders;
        this.inline = inline;
        this.entries = entries;
    }

    /**
     * Constructs a {@link GeyserHolderSet} from an ordered list of {@link HolderEntry} elements.
     */
    public static <T> GeyserHolderSet<T> ofEntries(JavaRegistryKey<T> registry, @NonNull List<HolderEntry<T>> entries) {
        return new GeyserHolderSet<>(registry, null, null, null, entries);
    }

    /**
     * Constructs an empty {@link GeyserHolderSet}.
     */
    public static <T> GeyserHolderSet<T> empty(JavaRegistryKey<T> registry) {
        return new GeyserHolderSet<>(registry, IntLists.emptyList());
    }

    /**
     * Constructs a {@link GeyserHolderSet} from a MCPL HolderSet.
     */
    public static <T> GeyserHolderSet<T> fromHolderSet(JavaRegistryKey<T> registry, @NonNull HolderSet holderSet) {
        // MCPL HolderSets don't have to support inline elements... for now (TODO CHECK ME)
        Tag<T> tag = holderSet.getLocation() == null ? null : new Tag<>(registry, holderSet.getLocation());
        return new GeyserHolderSet<>(registry, tag, holderSet.getHolders(), null, null);
    }

    public boolean contains(@NonNull GeyserSession session, @Nullable T object) {
        if (object == null) {
            return false;
        }
        if (inline != null) {
            return inline.contains(object);
        }
        if (entries != null) {
            int id = registry.networkId(session, object);
            for (HolderEntry<T> entry : entries) {
                if (entry instanceof HolderEntry.Direct<T> direct) {
                    if (Objects.equals(direct.value(), object)) {
                        return true;
                    }
                } else if (entry instanceof HolderEntry.Id<T> idEntry) {
                    if (id != -1 && idEntry.id() == id) {
                        return true;
                    }
                }
            }
            return false;
        }
        return session.getTagCache().is(this, object);
    }

    /**
     * Resolves the HolderSet, and automatically maps the network IDs to their respective object types.
     * If the HolderSet is a list of IDs, this will be returned. If it is a tag, the tag will be resolved from the tag cache. If it is an inline HolderSet, the list of inline elements will be returned.
     * If it is a heterogeneous HolderSet, the inline elements and resolved registered objects are returned in order.
     *
     * @return the HolderSet turned into a list of objects.
     */
    public List<T> resolve(GeyserSession session) {
        if (inline != null) {
            return inline;
        }
        if (entries != null) {
            List<T> result = new ArrayList<>(entries.size());
            for (HolderEntry<T> entry : entries) {
                if (entry instanceof HolderEntry.Direct<T> direct) {
                    result.add(direct.value());
                } else if (entry instanceof HolderEntry.Id<T> idEntry) {
                    T value = registry.value(session, idEntry.id());
                    if (value != null) {
                        result.add(value);
                    }
                }
            }
            return result;
        }
        return TagCache.mapRawArray(session, resolveRaw(session.getTagCache()), registry);
    }

    /**
     * Resolves the HolderSet into a list of network IDs. If the HolderSet is a list of IDs, this will be returned. If it is a tag, the tag will be resolved from the tag cache.
     *
     * <p>If the HolderSet has inline elements, this method will throw! Inline elements are not registered and as such do not have a network ID.</p>
     *
     * @return the HolderSet turned into a list of network IDs.
     * @throws IllegalStateException when the HolderSet contains inline elements.
     */
    public IntList resolveRaw(TagCache tagCache) {
        if (inline != null || entries != null) {
            throw new IllegalStateException("Tried to resolve network IDs of a GeyserHolderSet(registry=" + registry  + ") with inline elements!");
        } else if (holders != null) {
            return holders;
        }

        return tagCache.getRaw(Objects.requireNonNull(tag, "HolderSet must have a tag if it doesn't have a list of IDs"));
    }

    /**
     * Reads a HolderSet from a NBT object. Does not support reading HolderSets that can hold inline values.
     *
     * <p>Uses {@link JavaRegistryKey#networkId(GeyserSession, Key)} to resolve registry keys to network IDs.</p>
     *
     * @param session the Geyser session.
     * @param registry the registry the HolderSet contains IDs from.
     * @param holderSet the HolderSet as a NBT object.
     */
    public static <T> GeyserHolderSet<T> readHolderSet(GeyserSession session, JavaRegistryKey<T> registry, @Nullable Object holderSet) {
        return readHolderSet(registry, holderSet, key -> registry.networkId(session, key));
    }

    /**
     * Reads a HolderSet from a NBT object. Does not support reading HolderSets that can hold inline values.
     *
     * @param registry the registry the HolderSet contains IDs from.
     * @param holderSet the HolderSet as a NBT object.
     * @param idMapper a function that maps a key in this registry to its respective network ID.
     */
    public static <T> GeyserHolderSet<T> readHolderSet(JavaRegistryKey<T> registry, @Nullable Object holderSet, ToIntFunction<Key> idMapper) {
        return readHolderSet(registry, holderSet, idMapper, null);
    }

    /**
     * Reads a HolderSet from a NBT object. When {@code reader} is not null, this method can read HolderSets with inline registry elements as well, using the passed reader to decode
     * registry elements.
     *
     * @param registry the registry the HolderSet contains IDs from.
     * @param holderSet the HolderSet as a NBT object.
     * @param idMapper a function that maps a key in this registry to its respective network ID.
     * @param reader a function that reads an object in the HolderSet's registry, serialised as NBT. When {@code null}, this method doesn't support reading inline HolderSets.
     */
    public static <T> GeyserHolderSet<T> readHolderSet(JavaRegistryKey<T> registry, @Nullable Object holderSet,
                                                       ToIntFunction<Key> idMapper, @Nullable Function<NbtMap, T> reader) {
        if (holderSet == null) {
            return new GeyserHolderSet<>(registry);
        }

        // This is technically wrong, some registries might not serialise their elements as a map. However, right now this is only used for dialogs,
        // so it works. If this ever changes, we'll have to accommodate for that here
        if (holderSet instanceof NbtMap singleInlineElement && reader != null) {
            return new GeyserHolderSet<>(registry, List.of(reader.apply(singleInlineElement)));
        }
        if (holderSet instanceof String elementOrTag) {
            if (elementOrTag.startsWith("#")) {
                // Tag
                return new GeyserHolderSet<>(registry, new Tag<>(registry, MinecraftKey.key(elementOrTag.substring(1)))); // Remove '#' at beginning that indicates a tag
            } else if (elementOrTag.isEmpty()) {
                return new GeyserHolderSet<>(registry);
            }
            return new GeyserHolderSet<>(registry, IntList.of(idMapper.applyAsInt(MinecraftKey.key(elementOrTag))));
        } else if (holderSet instanceof List<?> list) {
            if (list.isEmpty()) {
                return new GeyserHolderSet<>(registry);
            }

            boolean hasInline = false;
            boolean hasReference = false;
            for (Object element : list) {
                if (element instanceof NbtMap) {
                    hasInline = true;
                } else if (element instanceof String) {
                    hasReference = true;
                }
            }

            if (hasInline && !hasReference) {
                if (reader != null) {
                    return new GeyserHolderSet<>(registry, list.stream().map(o -> (NbtMap) o).map(reader).toList());
                }
            } else if (hasReference && !hasInline) {
                // Assume the list is a list of strings (resource locations)
                return new GeyserHolderSet<>(registry, IntList.of(list.stream().map(o -> (String) o).map(MinecraftKey::key).mapToInt(idMapper).toArray()));
            } else if (hasInline && hasReference) {
                if (reader != null) {
                    List<HolderEntry<T>> entries = new ArrayList<>(list.size());
                    for (Object element : list) {
                        if (element instanceof NbtMap map) {
                            entries.add(new HolderEntry.Direct<>(reader.apply(map)));
                        } else if (element instanceof String str) {
                            entries.add(new HolderEntry.Id<>(idMapper.applyAsInt(MinecraftKey.key(str))));
                        } else {
                            GeyserImpl.getInstance().getLogger().warning("Unexpected element type in mixed HolderSet for registry " + registry + ": " + element);
                        }
                    }
                    return ofEntries(registry, entries);
                }
            }
        }

        String expected = reader == null ? "either a tag, a string ID, or a list of string IDs"
            : "either a tag, a string ID, an inline registry element, a list of string IDs, a list of inline registry elements, or a heterogeneous list of both";
        GeyserImpl.getInstance().getLogger().warning("Failed parsing HolderSet for registry " + registry + "! Expected " + expected + ", found " + holderSet);
        return new GeyserHolderSet<>(registry);
    }
}
