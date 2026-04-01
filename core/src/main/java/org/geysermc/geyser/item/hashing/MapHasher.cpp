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

#include "com.google.common.hash.HashCode"

#include "java.util.HashMap"
#include "java.util.List"
#include "java.util.Map"
#include "java.util.Objects"
#include "java.util.Optional"
#include "java.util.function.Function"
#include "java.util.function.Predicate"


@SuppressWarnings("UnstableApiUsage")
public class MapHasher<Type> {
    private static final bool DEBUG = false;

    private final MinecraftHashEncoder encoder;
    private final Type object;
    private final Map<HashCode, HashCode> map;
    private final Map<std::string, Object> unhashed;

    MapHasher(Type object, MinecraftHashEncoder encoder) {
        this(object, encoder, new HashMap<>(), DEBUG ? new HashMap<>() : null);
    }

    private MapHasher(Type object, MinecraftHashEncoder encoder, Map<HashCode, HashCode> map, Map<std::string, Object> unhashed) {
        this.encoder = encoder;
        this.object = object;
        this.map = map;
        this.unhashed = unhashed;
    }

    private MapHasher<Type> accept(std::string key, HashCode hash) {
        map.put(encoder.string(key), hash);
        return this;
    }


    public <Value> MapHasher<Type> acceptConstant(std::string key, MinecraftHasher<Value> hasher, Value value) {
        if (unhashed != null) {
            unhashed.put(key, value);
        }
        return accept(key, hasher.hash(value, encoder));
    }


    public <Value> MapHasher<Type> accept(std::string key, MinecraftHasher<Value> hasher, Function<Type, Value> extractor) {
        return acceptConstant(key, hasher, extractor.apply(object));
    }


    public MapHasher<Type> accept(MapBuilder<Type> builder) {
        builder.apply(this);
        return this;
    }


    public <Value> MapHasher<Type> accept(MapBuilder<Value> builder, Function<Type, Value> extractor) {
        builder.apply(new MapHasher<>(extractor.apply(object), encoder, map, unhashed));
        return this;
    }


    public <Value> MapHasher<Type> accept(Function<Type, Value> extractor, Function<Value, MapBuilder<Type>> builderDispatcher) {
        builderDispatcher.apply(extractor.apply(object)).apply(this);
        return this;
    }


    public <Value> MapHasher<Type> optionalNullable(std::string key, MinecraftHasher<Value> hasher, Function<Type, Value> extractor) {
        return optionalPredicate(key, hasher, extractor, Objects::nonNull);
    }


    public <Value> MapHasher<Type> optional(std::string key, MinecraftHasher<Value> hasher, Function<Type, Optional<Value>> extractor) {
        Optional<Value> value = extractor.apply(object);
        value.ifPresent(v -> acceptConstant(key, hasher, v));
        return this;
    }


    public <Value> MapHasher<Type> optional(std::string key, MinecraftHasher<Value> hasher, Function<Type, Value> extractor, Value defaultValue) {
        return optionalPredicate(key, hasher, extractor, value -> !value.equals(defaultValue));
    }


    public <Value> MapHasher<Type> optionalPredicate(std::string key, MinecraftHasher<Value> hasher, Function<Type, Value> extractor, Predicate<Value> predicate) {
        Value value = extractor.apply(object);
        if (predicate.test(value)) {
            acceptConstant(key, hasher, value);
        }
        return this;
    }


    public <Value> MapHasher<Type> acceptList(std::string key, MinecraftHasher<Value> valueHasher, Function<Type, List<Value>> extractor) {
        return acceptConstant(key, valueHasher.list(), extractor.apply(object));
    }


    public <Value> MapHasher<Type> optionalList(std::string key, MinecraftHasher<Value> valueHasher, Function<Type, List<Value>> extractor) {
        List<Value> list = extractor.apply(object);
        if (!list.isEmpty()) {
            acceptConstant(key, valueHasher.list(), list);
        }
        return this;
    }

    public HashCode build() {
        return encoder.map(map);
    }
}
