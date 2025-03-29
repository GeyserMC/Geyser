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

import com.google.common.base.Suppliers;
import com.google.common.hash.HashCode;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.inventory.item.DyeColor;
import org.geysermc.geyser.item.components.Rarity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.GlobalPos;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Filterable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemAttributeModifiers;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Unit;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Encodes an object into a {@link HashCode} using a {@link MinecraftHashEncoder}.
 *
 * <p>Hashers have been implemented for common types, such as all Java primitives, units (encodes an empty map), NBT maps and lists,
 * {@link Vector3i} positions, {@link Key} resource locations and {@code #}-prefixed tags, UUIDs, and more types. Furthermore, in {@link RegistryHasher}
 * more hashers can be found for more specific for Minecraft types, and {@link ComponentHasher#COMPONENT} hashes {@link net.kyori.adventure.text.Component}s.</p>
 *
 * <p>Most hashers are not created by implementing them directly, rather, they are built on top of other hashers, using various methods to manipulate and map them.
 * Here are some commonly used ones:</p>
 *
 * <ul>
 *     <li>{@link MinecraftHasher#list()} creates a hasher that hashes a list of the objects.</li>
 *     <li>{@link MinecraftHasher#cast(Function)} and {@link MinecraftHasher#sessionCast(BiFunction)} create a new hasher that delegates to this hasher with a converter function.</li>
 *     <li>{@link MinecraftHasher#filterable()} creates a hasher that hashes a {@link Filterable} instance of the object.</li>
 * </ul>
 *
 * <p>On top of these there are more methods, as well as static helper methods in this class, to create hashers.
 * One worth pointing out is {@link MinecraftHasher#mapBuilder(MapBuilder)}, which hashes an object into a map-like structure. Read the documentation there, on {@link MapBuilder}, and on {@link MapHasher} on how to use it.</p>
 *
 * <p>As of right now, hashers are used to create hash codes for data components, which Minecraft requires to be sent in inventory transactions. You'll find all the hashers for data components in
 * {@link DataComponentHashers}. In the future, hashers may be used elsewhere as well. If necessary, this system can even be refactored to write to different data structures, such as NBT or JSON files, as well.</p>
 *
 * <p>When creating new hashers, please be sure to put them in the proper place:</p>
 *
 * <ul>
 *     <li>Hashers that hash very generic types, or types that are used broadly across Minecraft (like key, UUID, game profile, etc.) belong here in {@link MinecraftHasher}.</li>
 *     <li>Hashers that hash more specific types, are more complicated, or depend on a hasher in {@link RegistryHasher}, belong in {@link RegistryHasher}.</li>
 *     <li>Hashers that hash components, and are used nowhere else, belong in {@link DataComponentHashers}.</li>
 * </ul>
 *
 * @param <Type> the type this hasher hashes.
 */
@SuppressWarnings("UnstableApiUsage")
@FunctionalInterface
public interface MinecraftHasher<Type> {

    MinecraftHasher<Unit> UNIT = (unit, encoder) -> encoder.emptyMap();

    MinecraftHasher<Byte> BYTE = (b, encoder) -> encoder.number(b);

    MinecraftHasher<Short> SHORT = (s, encoder) -> encoder.number(s);

    MinecraftHasher<Integer> INT = (i, encoder) -> encoder.number(i);

    MinecraftHasher<Long> LONG = (l, encoder) -> encoder.number(l);

    MinecraftHasher<Float> FLOAT = (f, encoder) -> encoder.number(f);

    MinecraftHasher<Double> DOUBLE = (d, encoder) -> encoder.number(d);

    MinecraftHasher<String> STRING = (s, encoder) -> encoder.string(s);

    MinecraftHasher<Boolean> BOOL = (b, encoder) -> encoder.bool(b);

    MinecraftHasher<IntStream> INT_ARRAY = (ints, encoder) -> encoder.intArray(ints.toArray());

    MinecraftHasher<NbtMap> NBT_MAP = (map, encoder) -> encoder.nbtMap(map);

    MinecraftHasher<NbtList<?>> NBT_LIST = (list, encoder) -> encoder.nbtList(list);

    MinecraftHasher<Vector3i> POS = INT_ARRAY.cast(pos -> IntStream.of(pos.getX(), pos.getY(), pos.getZ()));

    MinecraftHasher<Key> KEY = STRING.cast(Key::asString);

    MinecraftHasher<Key> TAG = STRING.cast(key -> '#' + key.asString());

    MinecraftHasher<Key> KEY_REMOVAL = STRING.cast(key -> '!' + key.asString());

    MinecraftHasher<UUID> UUID = INT_ARRAY.cast(uuid -> {
        long mostSignificant = uuid.getMostSignificantBits();
        long leastSignificant = uuid.getLeastSignificantBits();
        return IntStream.of((int) (mostSignificant >> 32), (int) mostSignificant, (int) (leastSignificant >> 32), (int) leastSignificant);
    }); // TODO test

    MinecraftHasher<GameProfile.Property> GAME_PROFILE_PROPERTY = mapBuilder(builder -> builder
        .accept("name", STRING, GameProfile.Property::getName)
        .accept("value", STRING, GameProfile.Property::getValue)
        .optionalNullable("signature", STRING, GameProfile.Property::getSignature));

    MinecraftHasher<GameProfile> GAME_PROFILE = mapBuilder(builder -> builder
        .optionalNullable("name", STRING, GameProfile::getName)
        .optionalNullable("id", UUID, GameProfile::getId)
        .optionalList("properties", GAME_PROFILE_PROPERTY, GameProfile::getProperties));

    MinecraftHasher<Integer> RARITY = fromIdEnum(Rarity.values(), Rarity::getName);

    MinecraftHasher<Integer> DYE_COLOR = fromIdEnum(DyeColor.values(), DyeColor::getJavaIdentifier);

    MinecraftHasher<EquipmentSlot> EQUIPMENT_SLOT = fromEnum(slot -> switch (slot) {
        case MAIN_HAND -> "mainhand";
        case OFF_HAND -> "offhand";
        case BOOTS -> "feet";
        case LEGGINGS -> "legs";
        case CHESTPLATE -> "chest";
        case HELMET -> "head";
        case BODY -> "body";
        case SADDLE -> "saddle";
    });

    MinecraftHasher<ItemAttributeModifiers.EquipmentSlotGroup> EQUIPMENT_SLOT_GROUP = fromEnum(group -> switch (group) {
        case ANY -> "any";
        case MAIN_HAND -> "mainhand";
        case OFF_HAND -> "offhand";
        case HAND -> "hand";
        case FEET -> "feet";
        case LEGS -> "legs";
        case CHEST -> "chest";
        case HEAD -> "head";
        case ARMOR -> "armor";
        case BODY -> "body";
        case SADDLE -> "saddle";
    });

    MinecraftHasher<GlobalPos> GLOBAL_POS = mapBuilder(builder -> builder
        .accept("dimension", KEY, GlobalPos::getDimension)
        .accept("pos", POS, GlobalPos::getPosition));

    HashCode hash(Type value, MinecraftHashEncoder encoder);

    /**
     * Creates a hasher that hashes a list of objects this hasher hashes.
     */
    default MinecraftHasher<List<Type>> list() {
        return (list, encoder) -> encoder.list(list.stream().map(element -> hash(element, encoder)).toList());
    }

    /**
     * "Casts" this hasher to another hash a different object, with a converter method. The returned hasher takes a {@link Casted}, converts it to a {@link Type} using the {@code converter}, and then hashes it.
     *
     * <p>If a {@link GeyserSession} object is needed for conversion, use {@link MinecraftHasher#sessionCast(BiFunction)}.</p>
     *
     * @param converter the converter function that converts a {@link Casted} into a {@link Type}.
     * @param <Casted> the type of the new hasher.
     * @see MinecraftHasher#sessionCast(BiFunction)
     */
    default <Casted> MinecraftHasher<Casted> cast(Function<Casted, Type> converter) {
        return (value, encoder) -> hash(converter.apply(value), encoder);
    }

    /**
     * Like {@link MinecraftHasher#cast(Function)}, but has access to {@link GeyserSession}.
     *
     * @param converter the converter function.
     * @param <Casted> the type of the new hasher.
     * @see MinecraftHasher#cast(Function)
     */
    default <Casted> MinecraftHasher<Casted> sessionCast(BiFunction<GeyserSession, Casted, Type> converter) {
        return (value, encoder) -> hash(converter.apply(encoder.session(), value), encoder);
    }

    /**
     * Delegates to {@link MinecraftHasher#dispatch(String, Function, Function)}, uses {@code "type"} as the {@code typeKey}.
     *
     * @see MinecraftHasher#dispatch(String, Function, Function)
     */
    default <Dispatched> MinecraftHasher<Dispatched> dispatch(Function<Dispatched, Type> typeExtractor, Function<Type, MapBuilder<Dispatched>> hashDispatch) {
        return dispatch("type", typeExtractor, hashDispatch);
    }

    /**
     * Creates a hasher that dispatches a {@link Type} from a {@link Dispatched} using {@code typeExtractor}, puts this in the {@code typeKey} key in a map, and uses a
     * {@link MapBuilder} provided by {@code mapDispatch} to build the rest of the map.
     *
     * <p>This can be used to create hashers that hash an abstract type or interface into a map with different keys depending on the type.</p>
     *
     * @param typeKey the key to store the {@link Type} in.
     * @param typeExtractor the function that extracts a {@link Type} from a {@link Dispatched}.
     * @param mapDispatch the function that provides a {@link MapBuilder} based on a {@link Type}.
     * @param <Dispatched> the type of the new hasher.
     */
    default <Dispatched> MinecraftHasher<Dispatched> dispatch(String typeKey, Function<Dispatched, Type> typeExtractor, Function<Type, MapBuilder<Dispatched>> mapDispatch) {
        return mapBuilder(builder -> builder
            .accept(typeKey, this, typeExtractor)
            .accept(typeExtractor, mapDispatch));
    }

    /**
     * Creates a hasher that wraps the {@link Type} in a {@link Filterable}.
     */
    default MinecraftHasher<Filterable<Type>> filterable() {
        return mapBuilder(builder -> builder
            .accept("raw", this, Filterable::getRaw)
            .optionalNullable("filtered", this, Filterable::getOptional));
    }

    /**
     * Lazily-initialises the given hasher using {@link Suppliers#memoize(com.google.common.base.Supplier)}.
     */
    static <Type> MinecraftHasher<Type> lazyInitialize(Supplier<MinecraftHasher<Type>> hasher) {
        Supplier<MinecraftHasher<Type>> memoized = Suppliers.memoize(hasher::get);
        return (value, encoder) -> memoized.get().hash(value, encoder);
    }

    /**
     * Uses {@link Enum#name()} (lowercased) as {@code toName} function in {@link MinecraftHasher#fromIdEnum(Enum[], Function)}.
     *
     * <p>Please be aware that you are using literal enum constants as string values here, meaning that if there is a typo in a constant, or a constant changes name, things
     * may break. Use cautiously.</p>
     *
     * @param values the array of {@link EnumConstant}s.
     * @param <EnumConstant> the enum.
     * @see MinecraftHasher#fromIdEnum(Enum[], Function)
     */
    static <EnumConstant extends Enum<EnumConstant>> MinecraftHasher<Integer> fromIdEnum(EnumConstant[] values) {
        return fromIdEnum(values, constant -> constant.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Creates a hasher that looks up an int in the array of {@link EnumConstant}s, and uses {@code toName} to turn the constant into a string, which it then hashes.
     *
     * @param values the array of {@link EnumConstant}s.
     * @param toName the function that turns a {@link EnumConstant} into a string.
     * @param <EnumConstant> the enum.
     * @see MinecraftHasher#fromIdEnum(Enum[])
     */
    static <EnumConstant extends Enum<EnumConstant>> MinecraftHasher<Integer> fromIdEnum(EnumConstant[] values, Function<EnumConstant, String> toName) {
        return STRING.cast(id -> toName.apply(values[id]));
    }

    /**
     * Uses {@link Enum#name()} (lowercased) as {@code toName} function in {@link MinecraftHasher#fromEnum(Function)}.
     *
     * <p>Please be aware that you are using literal enum constants as string values here, meaning that if there is a typo in a constant, or a constant changes name, things
     * may break. Use cautiously.</p>
     *
     * @param <EnumConstant> the enum.
     * @see MinecraftHasher#fromEnum(Function)
     */
    static <EnumConstant extends Enum<EnumConstant>> MinecraftHasher<EnumConstant> fromEnum() {
        return fromEnum(constant -> constant.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Creates a hasher for {@link EnumConstant}s that uses {@code toName} to turn a {@link EnumConstant} into a string, which it then hashes.
     *
     * @param toName the function that turns a {@link EnumConstant} into a string.
     * @param <EnumConstant> the enum.
     * @see MinecraftHasher#fromEnum()
     */
    static <EnumConstant extends Enum<EnumConstant>> MinecraftHasher<EnumConstant> fromEnum(Function<EnumConstant, String> toName) {
        return STRING.cast(toName);
    }

    /**
     * Creates a hasher that uses a {@link MapBuilder}, which uses a {@link MapHasher} to hash a {@link Type} into a map.
     *
     * @param builder the builder that creates a map from a {@link Type}.
     * @param <Type> the type to hash.
     */
    static <Type> MinecraftHasher<Type> mapBuilder(MapBuilder<Type> builder) {
        return (value, encoder) -> builder.apply(new MapHasher<>(value, encoder)).build();
    }

    /**
     * Creates a hasher that uses {@link MinecraftHasher#mapSet(MinecraftHasher, MinecraftHasher)} to hash {@link K} keys and {@link V} values into a map,
     * using the {@code keyHasher} and {@code valueHasher} respectively.
     *
     * @param keyHasher the hasher that hashes objects of type {@link K}.
     * @param valueHasher the hasher that hashes objects of type {@link V}.
     * @param <K> the key type.
     * @param <V> the value type.
     * @see MinecraftHasher#mapSet(MinecraftHasher, MinecraftHasher)
     */
    static <K, V> MinecraftHasher<Map<K, V>> map(MinecraftHasher<K> keyHasher, MinecraftHasher<V> valueHasher) {
        return MinecraftHasher.<Map.Entry<K, V>>mapSet(keyHasher.cast(Map.Entry::getKey), valueHasher.cast(Map.Entry::getValue)).cast(Map::entrySet);
    }

    /**
     * Creates a hasher that hashes a set of {@link Type} entries into a map of keys and values, using {@code keyHasher} and {@code valueHasher} respectively.
     *
     * <p>Note that the key hasher is usually expected to hash into a string, but this is not necessarily a requirement. It may be once different encoders are used to encode to NBT or JSON,
     * but this is not the case yet.</p>
     *
     * @param keyHasher the hasher that hashes a {@link Type} into a key to be used in the map.
     * @param valueHasher the hasher that hashes a {@link Type} into a value to be used in the map.
     * @param <Type> the entry type.
     * @see MinecraftHasher#map(MinecraftHasher, MinecraftHasher)
     */
    static <Type> MinecraftHasher<Collection<Type>> mapSet(MinecraftHasher<Type> keyHasher, MinecraftHasher<Type> valueHasher) {
        return (set, encoder) -> encoder.map(set.stream()
            .collect(Collectors.toMap(value -> keyHasher.hash(value, encoder), value -> valueHasher.hash(value, encoder))));
    }

    /**
     * Creates a hasher that hashes {@link Type} using either {@code firstHasher} to hash {@link First} (obtained using {@code firstExtractor}), or uses {@code secondHasher}
     * to hash {@link Second} (obtained via {@code secondExtractor}).
     *
     * <p>Specifically, the hasher first tries to use {@code firstExtractor} to obtain a {@link First} from {@link Type}. If this returns a not-null value, {@code firstHasher} is used to hash the value.</p>
     *
     * <p>If the returned value is null, then {@code secondExtractor} is used to obtain a {@link Second} from {@link Type}. This must never be null. {@code secondHasher} is then used to hash the value.</p>
     *
     * <p>Note: {@code secondExtractor} must never return null if {@code firstExtractor} does!</p>
     *
     * @param firstHasher the hasher used to hash {@link First}.
     * @param firstExtractor the function used to obtain a {@link First} from a {@link Type}.
     * @param secondHasher the hasher used to hash {@link Second}.
     * @param secondExtractor the function used to obtain a {@link Second} from a {@link Type}.
     * @param <Type> the type to hash.
     * @param <First> the first either type to hash.
     * @param <Second> the second either type to hash.
     */
    static <Type, First, Second> MinecraftHasher<Type> either(MinecraftHasher<First> firstHasher, Function<Type, First> firstExtractor,
                                                              MinecraftHasher<Second> secondHasher, Function<Type, Second> secondExtractor) {
        return (value, encoder) -> {
            First first = firstExtractor.apply(value);
            if (first != null) {
                return firstHasher.hash(first, encoder);
            }
            return secondHasher.hash(secondExtractor.apply(value), encoder);
        };
    }

    /**
     * Creates a hasher that dispatches a {@link MinecraftHasher} from a {@link Type}, and then uses that hasher to encode {@link Type}.
     *
     * @param hashDispatch the function that returns a {@link MinecraftHasher} from a {@link Type}.
     * @param <Type> the type to hash.
     */
    static <Type> MinecraftHasher<Type> dispatch(Function<Type, MinecraftHasher<Type>> hashDispatch) {
        return (value, encoder) -> hashDispatch.apply(value).hash(value, encoder);
    }
}
