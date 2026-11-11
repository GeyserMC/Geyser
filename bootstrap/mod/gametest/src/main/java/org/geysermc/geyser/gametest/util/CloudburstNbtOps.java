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

package org.geysermc.geyser.gametest.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.adventure.text.serializer.nbt.HeterogeneousNbtList;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

// Has some cursed modifications made to support non-null empty tags (represented as NbtType.END),
// and heterogeneous lists
public class CloudburstNbtOps implements DynamicOps<Object> {
    private static final NbtType<?> NULL_REPRESENTATIVE = NbtType.END;
    public static final CloudburstNbtOps INSTANCE = new CloudburstNbtOps();

    @Override
    public Object empty() {
        return NULL_REPRESENTATIVE;
    }

    @Override
    public Object emptyMap() {
        return NbtMap.EMPTY;
    }

    @Override
    public Object emptyList() {
        return NbtList.EMPTY;
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, Object input) {
        if (input == NULL_REPRESENTATIVE) {
            return outOps.empty();
        }
        NbtType<?> type = NbtType.byClass(input.getClass());
        return switch (type.getEnum()) {
            case END -> outOps.empty();
            case BYTE -> outOps.createByte((Byte) input);
            case SHORT -> outOps.createShort((Short) input);
            case INT -> outOps.createInt((Integer) input);
            case LONG -> outOps.createLong((Long) input);
            case FLOAT -> outOps.createFloat((Float) input);
            case DOUBLE -> outOps.createDouble((Double) input);
            case BYTE_ARRAY -> outOps.createByteList(ByteBuffer.wrap((byte[]) input));
            case STRING -> outOps.createString((String) input);
            case LIST -> this.convertList(outOps, input);
            case COMPOUND -> this.convertMap(outOps, input);
            case INT_ARRAY -> outOps.createIntList(Arrays.stream((int[]) input));
            case LONG_ARRAY -> outOps.createLongList(Arrays.stream((long[]) input));
        };
    }

    @Override
    public DataResult<Number> getNumberValue(Object input) {
        if (input instanceof Number) {
            return DataResult.success((Number) input);
        }
        return DataResult.error(() -> "Input is not a number: " + input);
    }

    @Override
    public Number getNumberValue(Object input, Number defaultValue) {
        return input instanceof Number ? (Number) input : defaultValue;
    }

    @Override
    public Object createNumeric(Number i) {
        return i;
    }

    @Override
    public DataResult<Boolean> getBooleanValue(Object input) {
        return getNumberValue(input).map(value -> value.doubleValue() != 0.0);
    }

    @Override
    public Object createBoolean(boolean value) {
        return value;
    }

    @Override
    public DataResult<String> getStringValue(Object input) {
        if (input instanceof String) {
            return DataResult.success((String) input);
        }
        return DataResult.error(() -> "Input is not a string: " + input);
    }

    @Override
    public Object createString(String value) {
        return value;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public DataResult<Object> mergeToList(Object list, Object value) {
        value = replaceNullRepresentativeWithNull(value);
        if (list == empty()) {
            NbtType<?> type = NbtType.byClass(value.getClass());
            return DataResult.success(new NbtList(type, value));
        }
        if (list instanceof NbtList<?> nbtList) {
            List<Object> unwrapped = HeterogeneousNbtList.tryUnwrap(nbtList);
            HeterogeneousNbtList newList = new HeterogeneousNbtList();
            unwrapped.forEach(newList::add);
            newList.add(value);
            return DataResult.success(newList.build());
        }
        return DataResult.error(() -> "mergeToList was not called with a list: " + list);
    }

    @Override
    public DataResult<Object> mergeToList(Object list, List<Object> values) {
        values = values.stream()
            .map(CloudburstNbtOps::replaceNullRepresentativeWithNull)
            .toList();
        if (list == empty()) {
            if (values.isEmpty()) {
                return DataResult.success(emptyList());
            }
            return DataResult.success(HeterogeneousNbtList.of(values.toArray(Object[]::new)));
        }
        if (list instanceof NbtList<?> nbtList) {
            if (values.isEmpty()) {
                return DataResult.success(nbtList);
            }
            if (nbtList.isEmpty()) {
                return DataResult.success(new NbtList(NbtType.byClass(values.getFirst().getClass()), values));
            }
            List<Object> unwrapped = HeterogeneousNbtList.tryUnwrap(nbtList);
            HeterogeneousNbtList newList = new HeterogeneousNbtList();
            unwrapped.forEach(newList::add);
            values.forEach(newList::add);
            return DataResult.success(newList.build());
        }
        return DataResult.error(() -> "mergeToList was not called with a list: " + list);
    }

    @Override
    public DataResult<Object> mergeToMap(Object map, Object key, Object value) {
        Object checkedMap = replaceNullRepresentativeWithNull(map);
        Object checkedKey = replaceNullRepresentativeWithNull(key);
        value = replaceNullRepresentativeWithNull(value);
        if (value == null) {
            return DataResult.success(map);
        }
        if (!(checkedMap instanceof NbtMap) && checkedMap != null) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + checkedMap, checkedMap);
        } else if (!(checkedKey instanceof String)) {
            return DataResult.error(() -> "key is not a string: " + checkedKey, checkedMap);
        } else {
            NbtMapBuilder builder;
            if (checkedMap instanceof NbtMap nbtMap) {
                builder = nbtMap.toBuilder();
            } else {
                builder = NbtMap.builder();
            }

            builder.put((String) checkedKey, value);
            return DataResult.success(builder.build());
        }
    }

    @Override
    public DataResult<Stream<Pair<Object, Object>>> getMapValues(Object input) {
        if (input instanceof NbtMap nbt) {
            return DataResult.success(nbt.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())));
        } else {
            return DataResult.error(() -> "Input was not NbtMap");
        }
    }

    @Override
    public Object createMap(Stream<Pair<Object, Object>> map) {
        NbtMapBuilder builder = NbtMap.builder();
        map.forEach(pair -> builder.put((String) pair.getFirst(), replaceNullRepresentativeWithNull(pair.getSecond())));
        return builder.build();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public DataResult<Stream<Object>> getStream(Object input) {
        if (input instanceof NbtList<?> list) {
            return DataResult.success(HeterogeneousNbtList.tryUnwrap(list).stream());
        }
        if (input instanceof int[] ints) {
            return DataResult.success(Arrays.stream(ints).mapToObj(Integer::valueOf));
        }
        if (input instanceof long[] longs) {
            return DataResult.success(Arrays.stream(longs).mapToObj(Long::valueOf));
        }
        if (input instanceof byte[] bytes) {
            ByteList byteList = new ByteArrayList(bytes);
            return DataResult.success((Stream) byteList.stream());
        }
        return DataResult.error(() -> "Was not a list");
    }

    @Override
    public DataResult<IntStream> getIntStream(Object input) {
        if (input instanceof int[] ints) {
            return DataResult.success(Arrays.stream(ints));
        } else {
            return DynamicOps.super.getIntStream(input);
        }
    }

    @Override
    public Object createIntList(IntStream input) {
        return input.toArray();
    }

    @Override
    public DataResult<LongStream> getLongStream(Object input) {
        if (input instanceof long[] longs) {
            return DataResult.success(Arrays.stream(longs));
        } else {
            return DynamicOps.super.getLongStream(input);
        }
    }

    @Override
    public Object createLongList(LongStream input) {
        return input.toArray();
    }

    @Override
    public Object createList(Stream<Object> input) {
        return input.map(CloudburstNbtOps::replaceNullRepresentativeWithNull)
            .collect(HeterogeneousNbtList.collector());
    }

    @Override
    public Object remove(Object input, String key) {
        if (input instanceof NbtMap map) {
            NbtMapBuilder builder = map.toBuilder();
            builder.remove(key);
            return builder.build();
        } else {
            return input;
        }
    }

    private static @Nullable Object replaceNullRepresentativeWithNull(Object object) {
        if (object == NbtType.END) {
            return null;
        }
        return object;
    }
}
