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

package org.geysermc.geyser.item.hashing;

import com.google.common.hash.HashCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * {@link MapHasher}s are used to encode a {@link Type} to a map-like structure, which is then hashed using a {@link MinecraftHashEncoder}.
 *
 * <p>{@link MapHasher}s store the {@link Type} they are encoding, but it isn't directly accessible. Instead, extractor functions are used to extract specific properties of the {@link Type}.</p>
 *
 * @param <Type> the type this {@link MapHasher} encodes.
 */
@SuppressWarnings("UnstableApiUsage")
public class MapHasher<Type> {
    private static final boolean DEBUG = false;

    private final MinecraftHashEncoder encoder;
    private final Type object;
    private final Map<HashCode, HashCode> map;
    private final Map<String, Object> unhashed;

    MapHasher(Type object, MinecraftHashEncoder encoder) {
        this(object, encoder, new HashMap<>(), DEBUG ? new HashMap<>() : null);
    }

    private MapHasher(Type object, MinecraftHashEncoder encoder, Map<HashCode, HashCode> map, Map<String, Object> unhashed) {
        this.encoder = encoder;
        this.object = object;
        this.map = map;
        this.unhashed = unhashed;
    }

    private MapHasher<Type> accept(String key, HashCode hash) {
        map.put(encoder.string(key), hash);
        return this;
    }

    /**
     * Adds a constant {@link Value} to the map.
     *
     * @param key the key to put the constant in.
     * @param hasher the hasher used to hash a {@link Value}.
     * @param value the {@link Value}.
     * @param <Value> the type of the value.
     */
    public <Value> MapHasher<Type> acceptConstant(String key, MinecraftHasher<Value> hasher, Value value) {
        if (unhashed != null) {
            unhashed.put(key, value);
        }
        return accept(key, hasher.hash(value, encoder));
    }

    /**
     * Extracts a {@link Value} from a {@link Type} using the {@code extractor}, and adds it to the map.
     *
     * @param key the key to put the {@link Value} in.
     * @param hasher the hasher used to hash a {@link Value}.
     * @param extractor the function that extracts a {@link Value} from a {@link Type}.
     * @param <Value> the type of the value.
     */
    public <Value> MapHasher<Type> accept(String key, MinecraftHasher<Value> hasher, Function<Type, Value> extractor) {
        return acceptConstant(key, hasher, extractor.apply(object));
    }

    /**
     * Applies the {@link MapBuilder} to this {@link MapHasher}, essentially adding all the keys it defines here.
     */
    public MapHasher<Type> accept(MapBuilder<Type> builder) {
        builder.apply(this);
        return this;
    }

    /**
     * Extracts a {@link Value} from a {@link Type} using the {@code extractor}, and applies the given {@link MapBuilder} for it to this {@link MapHasher},
     * essentially adding the keys it defines here.
     *
     * @param builder the {@link MapBuilder} that encodes a {@link Value}.
     * @param extractor the function that extracts a {@link Value} from a {@link Type}.
     * @param <Value> the type of the value.
     */
    public <Value> MapHasher<Type> accept(MapBuilder<Value> builder, Function<Type, Value> extractor) {
        builder.apply(new MapHasher<>(extractor.apply(object), encoder, map, unhashed));
        return this;
    }

    /**
     * Extracts a {@link Value} from a {@link Type} using the {@code extractor}, then dispatches a {@link MapBuilder} from the {@link Value} using the {@code builderDispatcher},
     * and applies it to this {@link MapHasher}, essentially adding the keys it defines here.
     *
     * @param extractor the function that extracts a {@link Value} from a {@link Type}.
     * @param builderDispatcher the function that dispatches a {@link MapBuilder} from a {@link Value}.
     * @param <Value> the type of the value.
     */
    public <Value> MapHasher<Type> accept(Function<Type, Value> extractor, Function<Value, MapBuilder<Type>> builderDispatcher) {
        builderDispatcher.apply(extractor.apply(object)).apply(this);
        return this;
    }

    /**
     * Extracts a {@link Value} from a {@link Type} using the {@code extractor}, and adds it to the map if it is not null.
     *
     * @param key the key to put the {@link Value} in.
     * @param hasher the hasher used to hash a {@link Value}.
     * @param extractor the function that extracts a {@link Value} from a {@link Type}.
     * @param <Value> the type of the value.
     */
    public <Value> MapHasher<Type> optionalNullable(String key, MinecraftHasher<Value> hasher, Function<Type, Value> extractor) {
        Value value = extractor.apply(object);
        if (value != null) {
            acceptConstant(key, hasher, value);
        }
        return this;
    }

    /**
     * Extracts an {@link Optional} of a {@link Value} from a {@link Type} using the {@code extractor}, and adds it to the map if it is present.
     *
     * @param key the key to put the {@link Value} in.
     * @param hasher the hasher used to hash a {@link Value}.
     * @param extractor the function that extracts a {@link Value} from a {@link Type}.
     * @param <Value> the type of the value.
     */
    public <Value> MapHasher<Type> optional(String key, MinecraftHasher<Value> hasher, Function<Type, Optional<Value>> extractor) {
        Optional<Value> value = extractor.apply(object);
        value.ifPresent(v -> acceptConstant(key, hasher, v));
        return this;
    }

    /**
     * Extracts a {@link Value} from a {@link Type} using the {@code extractor}, and adds it to the map if it's not equal to {@code defaultValue}.
     *
     * @param key the key to put the {@link Value} in.
     * @param hasher the hasher used to hash a {@link Value}.
     * @param extractor the function that extracts a {@link Value} from a {@link Type}.
     * @param defaultValue the default {@link Value}. The {@link Value} won't be added to the map if it equals the default.
     * @param <Value> the type of the value.
     */
    public <Value> MapHasher<Type> optional(String key, MinecraftHasher<Value> hasher, Function<Type, Value> extractor, Value defaultValue) {
        Value value = extractor.apply(object);
        if (!value.equals(defaultValue)) {
            acceptConstant(key, hasher, value);
        }
        return this;
    }

    /**
     * Extracts a list of {@link Value}s from a {@link Type}, and adds it to the map.
     *
     * @param key the key to put the list of {@link Value}s in.
     * @param valueHasher the hasher used to hash a single {@link Value}.
     * @param extractor the function that extracts a list of {@link Value}s from a {@link Type}.
     * @param <Value> the type of the value.
     */
    public <Value> MapHasher<Type> acceptList(String key, MinecraftHasher<Value> valueHasher, Function<Type, List<Value>> extractor) {
        return acceptConstant(key, valueHasher.list(), extractor.apply(object));
    }

    /**
     * Extracts a list of {@link Value}s from a {@link Type}, and adds it to the map if it is not empty.
     *
     * @param key the key to put the list of {@link Value}s in.
     * @param valueHasher the hasher used to hash a single {@link Value}.
     * @param extractor the function that extracts a list of {@link Value}s from a {@link Type}.
     * @param <Value> the type of the value.
     */
    public <Value> MapHasher<Type> optionalList(String key, MinecraftHasher<Value> valueHasher, Function<Type, List<Value>> extractor) {
        List<Value> list = extractor.apply(object);
        if (!list.isEmpty()) {
            acceptConstant(key, valueHasher.list(), list);
        }
        return this;
    }

    public HashCode build() {
        if (unhashed != null) {
            System.out.println(unhashed);
        }
        return encoder.map(map);
    }
}
