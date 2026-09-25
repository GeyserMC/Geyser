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
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtList;
import org.geysermc.adventure.text.serializer.nbt.HeterogeneousNbtList;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Similar to vanilla Minecraft's HolderSets, stores either:
 *
 * <ul>
 *     <li>A single reference to a tag, which holds the list of IDs,</li>
 *     <li>A list of IDs (can also be represented as a single ID),</li>
 *     <li>A list of inline elements (only supported by some sets, can also be represented as a single inline element), or,</li>
 *     <li>A list with both IDs and inline elements (only supported by some sets).</li>
 * </ul>
 *
 * <p>Because HolderSets may utilise tags, when loading a HolderSet, Geyser must store tags for the registry the HolderSet is for. This is done for all registries registered in
 * {@link JavaRegistries}.</p>
 *
 * {@link GeyserHolderSet}s can be constructed using the {@link GeyserHolderSet#empty(JavaRegistryKey)}, {@link GeyserHolderSet#ofTag(JavaRegistryKey, Tag)},
 * and {@link GeyserHolderSet#of(JavaRegistryKey, List)} methods.
 */
@Accessors(fluent = true)
public final class GeyserHolderSet<T> {
    @Getter
    private final JavaRegistryKey<T> registry;
    private final @Nullable Tag<T> tag;
    private final @Nullable List<Holder<T>> holders;
    @Getter
    private final boolean empty;

    private GeyserHolderSet(JavaRegistryKey<T> registry, @Nullable Tag<T> tag, @Nullable List<Holder<T>> inline) {
        this.registry = registry;
        this.tag = tag;
        this.holders = inline;
        empty = tag == null && (inline == null || inline.isEmpty());
    }

    /**
     * Constructs an empty {@link GeyserHolderSet}.
     *
     * @param registry the registry the set is bound to
     * @param <T> the type of the set
     * @return a new empty {@link GeyserHolderSet} for the given registry
     */
    public static <T> GeyserHolderSet<T> empty(JavaRegistryKey<T> registry) {
        return new GeyserHolderSet<>(registry, null, null);
    }

    /**
     * Constructs a {@link GeyserHolderSet} for the given {@code tag}.
     *
     * @param registry the registry the set is bound to
     * @param tag the tag of the set
     * @param <T> the type of the set
     * @return the constructed {@link GeyserHolderSet}
     */
    public static <T> GeyserHolderSet<T> ofTag(JavaRegistryKey<T> registry, Key tag) {
        return ofTag(registry, new Tag<>(registry, tag));
    }

    /**
     * Constructs a {@link GeyserHolderSet} for the given {@code tag}.
     *
     * @param registry the registry the set is bound to
     * @param tag the tag of the set
     * @param <T> the type of the set
     * @return the constructed {@link GeyserHolderSet}
     */
    public static <T> GeyserHolderSet<T> ofTag(JavaRegistryKey<T> registry, Tag<T> tag) {
        return new GeyserHolderSet<>(registry, tag, null);
    }

    /**
     * Constructs a {@link GeyserHolderSet} holding a single registry reference (ID holder).
     *
     * @param registry the registry the set is bound to
     * @param id the single ID
     * @param <T> the type of the set
     * @return the constructed {@link GeyserHolderSet}
     */
    public static <T> GeyserHolderSet<T> of(JavaRegistryKey<T> registry, int id) {
        return new GeyserHolderSet<>(registry, null, List.of(Holder.ofId(id)));
    }

    /**
     * Constructs a {@link GeyserHolderSet} holding a single inline element (custom holder).
     *
     * @param registry the registry the set is bound to
     * @param element the single inline element
     * @param <T> the type of the set
     * @return the constructed {@link GeyserHolderSet}
     */
    public static <T> GeyserHolderSet<T> of(JavaRegistryKey<T> registry, T element) {
        return new GeyserHolderSet<>(registry, null, List.of(Holder.ofCustom(element)));
    }

    /**
     * Constructs a {@link GeyserHolderSet} holding a list of registry reference (ID holder).
     *
     * @param registry the registry the set is bound to
     * @param ids the list of references
     * @param <T> the type of the set
     * @return the constructed {@link GeyserHolderSet}
     */
    public static <T> GeyserHolderSet<T> of(JavaRegistryKey<T> registry, IntList ids) {
        return new GeyserHolderSet<>(registry, null, ids.intStream().mapToObj(Holder::<T>ofId).toList());
    }

    /**
     * Constructs a {@link GeyserHolderSet} holding a list of holders (either ID or custom holders).
     *
     * @param registry the registry the set is bound to
     * @param holders the list of holders
     * @param <T> the type of the set
     * @return the constructed {@link GeyserHolderSet}
     */
    public static <T> GeyserHolderSet<T> of(JavaRegistryKey<T> registry, List<Holder<T>> holders) {
        return new GeyserHolderSet<>(registry, null, List.copyOf(holders));
    }

    /**
     * Constructs a {@link GeyserHolderSet} from a MCPL {@link HolderSet}.
     *
     * @param registry the registry the set is bound to
     * @param holderSet the MCPL {@link HolderSet}
     * @param <T> the type of the set
     * @return the constructed {@link GeyserHolderSet}
     */
    public static <T> GeyserHolderSet<T> fromMCPL(JavaRegistryKey<T> registry, HolderSet holderSet) {
        // MCPL HolderSets don't have to support inline elements... for now (last checked: Java 26.3)
        Key tag = holderSet.getLocation();
        if (tag == null) {
            return GeyserHolderSet.of(registry, Objects.requireNonNull(holderSet.getHolders()));
        }
        return GeyserHolderSet.ofTag(registry, tag);
    }

    /**
     * Resolves this set, and checks if the given {@code object} is in it.
     *
     * @param session the {@link GeyserSession}
     * @param object the {@code object} to check for
     * @return true if the given {@code object} was in this set, false otherwise
     */
    public boolean contains(GeyserSession session, @Nullable T object) {
        if (object == null || empty) {
            return false;
        }
        return resolve(session).contains(object);
    }

    /**
     * Resolves this set to a list of {@link Holder}s, which may either be an {@link Holder.IdHolder} or a {@link Holder.CustomHolder}
     * (depending on the registry, both can be present in the list at once).
     *
     * @param session the {@link GeyserSession}
     * @return the resolved set as a list of {@link Holder}s
     */
    public List<Holder<T>> resolveHolders(GeyserSession session) {
        if (empty) {
            return List.of();
        } else if (holders != null) {
            return holders;
        }
        assert tag != null;
        // TODO maybe cache this, but how realise that tags are updated?
        return session.getTagCache().getRaw(tag).intStream().mapToObj(Holder::<T>ofId).toList();
    }

    /**
     * Resolves this set to a raw int-array of network IDs. <em>This will not work for sets that have any inline holders.</em>
     *
     * <p>This method is deprecated: prefer using {@link GeyserHolderSet#resolveHolders(GeyserSession)} or {@link GeyserHolderSet#resolve(GeyserSession)} as much as possible,
     * which also won't fail on inline holders.</p>
     *
     * @param session the {@link GeyserSession}
     * @return the resolved set as a raw int-array of network IDs
     */
    @Deprecated
    public int[] resolveRawHolders(GeyserSession session) {
        return resolveHolders(session).stream().mapToInt(Holder::id).toArray();
    }

    /**
     * Resolves this set to a list of {@link T}s. This calls {@link GeyserHolderSet#resolveHolders(GeyserSession)}, and maps the {@link Holder.IdHolder}s
     * to a {@link T} using {@link JavaRegistryKey#value(GeyserSession, int)}.
     *
     * @param session the {@link GeyserSession}
     * @return the resolved set as a list of {@link Holder}s
     */
    public List<T> resolve(GeyserSession session) {
        if (empty) {
            return List.of();
        }
        // TODO same as above
        return resolveHolders(session).stream().map(holder -> holder.getOrCompute(id -> registry.value(session, id))).toList();
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
                                                       ToIntFunction<Key> idMapper, @Nullable Function<Object, T> reader) {
        boolean canReadInline = reader != null;

        return switch (holderSet) {
            case null -> GeyserHolderSet.empty(registry);
            // Technically wrong if a string can be an inline element, but this never happens (as of Java 26.3)
            case String singleElementOrTag -> {
                if (singleElementOrTag.startsWith("#")) {
                    // Tag
                    // Remove '#' at beginning that indicates a tag
                    yield GeyserHolderSet.ofTag(registry, new Tag<>(registry, MinecraftKey.key(singleElementOrTag.substring(1))));
                } else if (singleElementOrTag.isEmpty()) {
                    // Technically illegal, we accept it anyway
                    yield GeyserHolderSet.empty(registry);
                }
                yield GeyserHolderSet.of(registry, idMapper.applyAsInt(MinecraftKey.key(singleElementOrTag)));
            }
            case NbtList<?> list -> {
                if (list.isEmpty()) {
                    yield GeyserHolderSet.empty(registry);
                } else {
                    // List can hold both reference strings and inline elements, and we have to parse them all
                    List<Object> unwrapped = HeterogeneousNbtList.tryUnwrap(list);
                    List<Holder<T>> holders = new ObjectArrayList<>();

                    for (Object tag : unwrapped) {
                        if (tag instanceof String reference) {
                            try {
                                holders.add(Holder.ofId(idMapper.applyAsInt(MinecraftKey.key(reference))));
                                continue;
                            } catch (InvalidKeyException ignored) {}
                        }
                        // Try to read the inline tag, if we fail just return an empty set
                        if (canReadInline) {
                            holders.add(Holder.ofCustom(reader.apply(tag)));
                        } else {
                            GeyserImpl.getInstance().getLogger().warning("Failed parsing HolderSet for registry " + registry + ", don't know how to parse inline element!");
                            yield GeyserHolderSet.empty(registry);
                        }
                    }

                    yield GeyserHolderSet.of(registry, holders);
                }
            }
            case Object singleInlineElement -> {
                if (canReadInline) {
                    yield GeyserHolderSet.of(registry, reader.apply(singleInlineElement));
                } else {
                    GeyserImpl.getInstance().getLogger().warning("Failed parsing HolderSet for registry " + registry + ", don't know how to parse inline element!");
                    yield GeyserHolderSet.empty(registry);
                }
            }
        };
    }
}
