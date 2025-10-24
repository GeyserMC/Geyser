package org.geysermc.geyser.gametest.registries;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

// Stolen from mappings-gen: is there a better way to do this?
public class CloudburstNbtOps implements DynamicOps<Object> {
    public static final CloudburstNbtOps INSTANCE = new CloudburstNbtOps();

    @Override
    public Object empty() {
        return null;
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
        if (input == empty()) {
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
        if (list == empty()) {
            NbtType<?> type = NbtType.byClass(value.getClass());
            return DataResult.success(new NbtList(type, value));
        }
        if (list instanceof NbtList<?> nbtList) {
            List listBuilder = new ArrayList<>(nbtList);
            listBuilder.add(value);
            return DataResult.success(new NbtList(nbtList.getType(), listBuilder));
        }
        return DataResult.error(() -> "mergeToList was not called with a list: " + list);
    }

    @Override
    public DataResult<Object> mergeToList(Object list, List<Object> values) {
        if (list == empty()) {
            if (values.isEmpty()) {
                return DataResult.success(emptyList());
            }
            NbtType<?> type = NbtType.byClass(values.get(0).getClass());
            return DataResult.success(new NbtList(type, values));
        }
        if (list instanceof NbtList<?> nbtList) {
            if (values.isEmpty()) {
                return DataResult.success(nbtList);
            }
            if (nbtList.isEmpty()) {
                return DataResult.success(new NbtList(NbtType.byClass(values.get(0).getClass()), values));
            }
            List listBuilder = new ArrayList<>(nbtList);
            listBuilder.addAll(values);
            return DataResult.success(new NbtList(nbtList.getType(), listBuilder));
        }
        return DataResult.error(() -> "mergeToList was not called with a list: " + list);
    }

    @Override
    public DataResult<Object> mergeToMap(Object map, Object key, Object value) {
        if (!(map instanceof NbtMap) && map != null) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
        } else if (!(key instanceof String)) {
            return DataResult.error(() -> "key is not a string: " + key, map);
        } else {
            NbtMapBuilder builder;
            if (map instanceof NbtMap nbtMap) {
                builder = nbtMap.toBuilder();
            } else {
                builder = NbtMap.builder();
            }

            builder.put((String) key, value);
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
        map.forEach(pair -> builder.put((String) pair.getFirst(), pair.getSecond()));
        return builder.build();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public DataResult<Stream<Object>> getStream(Object input) {
        if (input instanceof NbtList<?> list) {
            return DataResult.success((Stream<Object>) list.stream());
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
        final List<?> list = input.toList();
        if (list.isEmpty()) {
            return emptyList();
        }
        NbtType<?> type = NbtType.byClass(list.getFirst().getClass());
        return new NbtList(type, list);
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
}
