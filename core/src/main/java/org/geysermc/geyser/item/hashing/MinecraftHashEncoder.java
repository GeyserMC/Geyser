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
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.session.GeyserSession;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encodes primitive Java objects, lists, and maps into a {@link HashCode}, using {@link Hashing#crc32c()} as hash function.
 *
 * <p>Based off the {@code HashOps} class in vanilla Java 1.21.5, and is used by {@link MinecraftHasher}.</p>
 */
@SuppressWarnings("UnstableApiUsage")
public class MinecraftHashEncoder {
    private static final byte TAG_EMPTY = 1;
    private static final byte TAG_MAP_START = 2;
    private static final byte TAG_MAP_END = 3;
    private static final byte TAG_LIST_START = 4;
    private static final byte TAG_LIST_END = 5;
    private static final byte TAG_BYTE = 6;
    private static final byte TAG_SHORT = 7;
    private static final byte TAG_INT = 8;
    private static final byte TAG_LONG = 9;
    private static final byte TAG_FLOAT = 10;
    private static final byte TAG_DOUBLE = 11;
    private static final byte TAG_STRING = 12;
    private static final byte TAG_BOOLEAN = 13;
    private static final byte TAG_BYTE_ARRAY_START = 14;
    private static final byte TAG_BYTE_ARRAY_END = 15;
    private static final byte TAG_INT_ARRAY_START = 16;
    private static final byte TAG_INT_ARRAY_END = 17;
    private static final byte TAG_LONG_ARRAY_START = 18;
    private static final byte TAG_LONG_ARRAY_END = 19;

    private static final Comparator<HashCode> HASH_COMPARATOR = Comparator.comparingLong(HashCode::padToLong);
    private static final Comparator<Map.Entry<HashCode, HashCode>> MAP_ENTRY_ORDER = Map.Entry.<HashCode, HashCode>comparingByKey(HASH_COMPARATOR)
        .thenComparing(Map.Entry.comparingByValue(HASH_COMPARATOR));

    private static final byte[] EMPTY = new byte[]{TAG_EMPTY};
    public static final byte[] EMPTY_MAP = new byte[]{TAG_MAP_START, TAG_MAP_END};
    private static final byte[] FALSE = new byte[]{TAG_BOOLEAN, 0};
    private static final byte[] TRUE = new byte[]{TAG_BOOLEAN, 1};

    private final HashFunction hasher;
    private final GeyserSession session;

    private final HashCode empty;
    private final HashCode emptyMap;
    private final HashCode falseHash;
    private final HashCode trueHash;

    public MinecraftHashEncoder(GeyserSession session) {
        hasher = Hashing.crc32c();
        this.session = session;

        empty = hasher.hashBytes(EMPTY);
        emptyMap = hasher.hashBytes(EMPTY_MAP);
        falseHash = hasher.hashBytes(FALSE);
        trueHash = hasher.hashBytes(TRUE);
    }

    public GeyserSession session() {
        return session;
    }

    public HashCode empty() {
        return empty;
    }

    public HashCode emptyMap() {
        return emptyMap;
    }

    public HashCode number(Number number) {
        if (number instanceof Byte b) {
            return hasher.newHasher(2).putByte(TAG_BYTE).putByte(b).hash();
        } else if (number instanceof Short s) {
            return hasher.newHasher(3).putByte(TAG_SHORT).putShort(s).hash();
        } else if (number instanceof Integer i) {
            return hasher.newHasher(5).putByte(TAG_INT).putInt(i).hash();
        } else if (number instanceof Long l) {
            return hasher.newHasher(9).putByte(TAG_LONG).putLong(l).hash();
        } else if (number instanceof Float f) {
            return hasher.newHasher(5).putByte(TAG_FLOAT).putFloat(f).hash();
        }

        return hasher.newHasher(9).putByte(TAG_DOUBLE).putDouble(number.doubleValue()).hash();
    }

    public HashCode string(String string) {
        return hasher.newHasher().putByte(TAG_STRING).putInt(string.length()).putUnencodedChars(string).hash();
    }

    public HashCode bool(boolean b) {
        return b ? trueHash : falseHash;
    }

    public HashCode map(Map<HashCode, HashCode> map) {
        Hasher mapHasher = hasher.newHasher();
        mapHasher.putByte(TAG_MAP_START);
        map.entrySet().stream()
            .sorted(MAP_ENTRY_ORDER)
            .forEach(entry -> mapHasher.putBytes(entry.getKey().asBytes()).putBytes(entry.getValue().asBytes()));
        mapHasher.putByte(TAG_MAP_END);
        return mapHasher.hash();
    }

    public HashCode nbtMap(NbtMap map) {
        Map<HashCode, HashCode> hashed = new HashMap<>();
        for (String key : map.keySet()) {
            HashCode hashedKey = string(key);
            Object value = map.get(key);
            if (value instanceof NbtList<?> list) {
                hashed.put(hashedKey, nbtList(list));
            } else {
                map.listenForNumber(key, n -> hashed.put(hashedKey, number(n)));
                map.listenForString(key, s -> hashed.put(hashedKey, string(s)));
                map.listenForCompound(key, compound -> hashed.put(hashedKey, nbtMap(compound)));

                map.listenForByteArray(key, bytes -> hashed.put(hashedKey, byteArray(bytes)));
                map.listenForIntArray(key, ints -> hashed.put(hashedKey, intArray(ints)));
                map.listenForLongArray(key, longs -> hashed.put(hashedKey, longArray(longs)));
            }
        }
        return map(hashed);
    }

    public HashCode list(List<HashCode> list) {
        Hasher listHasher = hasher.newHasher();
        listHasher.putByte(TAG_LIST_START);
        list.forEach(hash -> listHasher.putBytes(hash.asBytes()));
        listHasher.putByte(TAG_LIST_END);
        return listHasher.hash();
    }

    // TODO can this be written better?
    @SuppressWarnings("unchecked")
    public HashCode nbtList(NbtList<?> nbtList) {
        NbtType<?> type = nbtList.getType();
        List<HashCode> hashed = new ArrayList<>();

        if (type == NbtType.BYTE) {
            hashed.addAll(((List<Byte>) nbtList).stream().map(this::number).toList());
        } else if (type == NbtType.SHORT) {
            hashed.addAll(((List<Short>) nbtList).stream().map(this::number).toList());
        } else if (type == NbtType.INT) {
            hashed.addAll(((List<Integer>) nbtList).stream().map(this::number).toList());
        } else if (type == NbtType.LONG) {
            hashed.addAll(((List<Long>) nbtList).stream().map(this::number).toList());
        } else if (type == NbtType.FLOAT) {
            hashed.addAll(((List<Float>) nbtList).stream().map(this::number).toList());
        } else if (type == NbtType.DOUBLE) {
            hashed.addAll(((List<Double>) nbtList).stream().map(this::number).toList());
        } else if (type == NbtType.STRING) {
            hashed.addAll(((List<String>) nbtList).stream().map(this::string).toList());
        } else if (type == NbtType.LIST) {
            for (NbtList<?> list : (List<NbtList<?>>) nbtList) {
                hashed.add(nbtList(list));
            }
        } else if (type == NbtType.COMPOUND) {
            for (NbtMap compound : (List<NbtMap>) nbtList) {
                hashed.add(nbtMap(compound));
            }
        }

        return list(hashed);
    }

    public HashCode byteArray(byte[] bytes) {
        Hasher arrayHasher = hasher.newHasher();
        arrayHasher.putByte(TAG_BYTE_ARRAY_START);
        arrayHasher.putBytes(bytes);
        arrayHasher.putByte(TAG_BYTE_ARRAY_END);
        return arrayHasher.hash();
    }

    public HashCode intArray(int[] ints) {
        Hasher arrayHasher = hasher.newHasher();
        arrayHasher.putByte(TAG_INT_ARRAY_START);
        for (int i : ints) {
            arrayHasher.putInt(i);
        }
        arrayHasher.putByte(TAG_INT_ARRAY_END);
        return arrayHasher.hash();
    }

    public HashCode longArray(long[] longs) {
        Hasher arrayHasher = hasher.newHasher();
        arrayHasher.putByte(TAG_LONG_ARRAY_START);
        for (long l : longs) {
            arrayHasher.putLong(l);
        }
        arrayHasher.putByte(TAG_LONG_ARRAY_END);
        return arrayHasher.hash();
    }
}
