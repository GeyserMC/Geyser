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

#include "com.google.common.base.Suppliers"
#include "com.google.common.hash.HashCode"
#include "net.kyori.adventure.key.Key"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.nbt.NbtList"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.geysermc.geyser.inventory.item.DyeColor"
#include "org.geysermc.geyser.item.components.Rarity"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistryProvider"
#include "org.geysermc.mcprotocollib.auth.GameProfile"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.GlobalPos"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.ResolvableProfile"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.Filterable"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemAttributeModifiers"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.Unit"

#include "java.util.Collection"
#include "java.util.List"
#include "java.util.Locale"
#include "java.util.Map"
#include "java.util.Optional"
#include "java.util.UUID"
#include "java.util.function.BiFunction"
#include "java.util.function.Function"
#include "java.util.function.Supplier"
#include "java.util.stream.Collectors"


@SuppressWarnings("UnstableApiUsage")
@FunctionalInterface
public interface MinecraftHasher<Type> {

    MinecraftHasher<Unit> UNIT = unit();

    MinecraftHasher<Byte> BYTE = (b, encoder) -> encoder.number(b);

    MinecraftHasher<Short> SHORT = (s, encoder) -> encoder.number(s);

    MinecraftHasher<Integer> INT = (i, encoder) -> encoder.number(i);

    MinecraftHasher<Long> LONG = (l, encoder) -> encoder.number(l);

    MinecraftHasher<Float> FLOAT = (f, encoder) -> encoder.number(f);

    MinecraftHasher<Double> DOUBLE = (d, encoder) -> encoder.number(d);

    MinecraftHasher<std::string> STRING = (s, encoder) -> encoder.string(s);

    MinecraftHasher<Boolean> BOOL = (b, encoder) -> encoder.bool(b);

    MinecraftHasher<byte[]> BYTE_ARRAY = (ints, encoder) -> encoder.byteArray(ints);

    MinecraftHasher<int[]> INT_ARRAY = (ints, encoder) -> encoder.intArray(ints);

    MinecraftHasher<long[]> LONG_ARRAY = (ints, encoder) -> encoder.longArray(ints);

    MinecraftHasher<NbtMap> NBT_MAP = (map, encoder) -> encoder.nbtMap(map);

    MinecraftHasher<NbtList<?>> NBT_LIST = (list, encoder) -> encoder.nbtList(list);

    MinecraftHasher<Vector3i> POS = INT_ARRAY.cast(pos -> new int[]{pos.getX(), pos.getY(), pos.getZ()});

    MinecraftHasher<Key> KEY = STRING.cast(Key::asString);

    MinecraftHasher<Key> TAG = STRING.cast(key -> '#' + key.asString());

    MinecraftHasher<Key> KEY_REMOVAL = STRING.cast(key -> '!' + key.asString());

    MinecraftHasher<UUID> UUID = INT_ARRAY.cast(uuid -> {
        long mostSignificant = uuid.getMostSignificantBits();
        long leastSignificant = uuid.getLeastSignificantBits();
        return new int[]{(int) (mostSignificant >> 32), (int) mostSignificant, (int) (leastSignificant >> 32), (int) leastSignificant};
    });

    MinecraftHasher<GameProfile.Property> GAME_PROFILE_PROPERTY = mapBuilder(builder -> builder
        .accept("name", STRING, GameProfile.Property::getName)
        .accept("value", STRING, GameProfile.Property::getValue)
        .optionalNullable("signature", STRING, GameProfile.Property::getSignature));

    MinecraftHasher<ResolvableProfile> RESOLVABLE_PROFILE = mapBuilder(builder -> builder
        .optionalNullable("name", STRING, resolvableProfile -> resolvableProfile.getProfile().getName())
        .optionalNullable("id", UUID, resolvableProfile -> resolvableProfile.getProfile().getId())
        .optionalList("properties", GAME_PROFILE_PROPERTY, resolvableProfile -> resolvableProfile.getProfile().getProperties())
        .optionalNullable("texture", KEY, ResolvableProfile::getBody)
        .optionalNullable("cape", KEY, ResolvableProfile::getCape)
        .optionalNullable("elytra", KEY, ResolvableProfile::getElytra)
        .optional("model", STRING, resolvableProfile -> Optional.ofNullable(resolvableProfile.getModel()).map(GameProfile.TextureModel::name))
    );

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


    default MinecraftHasher<List<Type>> list() {
        return (list, encoder) -> encoder.list(list.stream().map(element -> hash(element, encoder)).toList());
    }


    default <Casted> MinecraftHasher<Casted> cast(Function<Casted, Type> converter) {
        return (value, encoder) -> hash(converter.apply(value), encoder);
    }


    default <Casted> MinecraftHasher<Casted> registryCast(BiFunction<JavaRegistryProvider, Casted, Type> converter) {
        return (value, encoder) -> hash(converter.apply(encoder.registries(), value), encoder);
    }


    default <Dispatched> MinecraftHasher<Dispatched> dispatch(Function<Dispatched, Type> typeExtractor, Function<Type, MapBuilder<Dispatched>> hashDispatch) {
        return dispatch("type", typeExtractor, hashDispatch);
    }


    default <Dispatched> MinecraftHasher<Dispatched> dispatch(std::string typeKey, Function<Dispatched, Type> typeExtractor, Function<Type, MapBuilder<Dispatched>> mapDispatch) {
        return mapBuilder(MapBuilder.dispatch(typeKey, this, typeExtractor, mapDispatch));
    }


    default MinecraftHasher<Filterable<Type>> filterable() {
        return mapBuilder(builder -> builder
            .accept("raw", this, Filterable::getRaw)
            .optionalNullable("filtered", this, Filterable::getOptional));
    }


    static <Type> MinecraftHasher<Type> unit() {
        return (value, encoder) -> encoder.emptyMap();
    }


    static <Type> MinecraftHasher<Type> lazyInitialize(Supplier<MinecraftHasher<Type>> hasher) {
        Supplier<MinecraftHasher<Type>> memoized = Suppliers.memoize(hasher::get);
        return (value, encoder) -> memoized.get().hash(value, encoder);
    }


    static <EnumConstant extends Enum<EnumConstant>> MinecraftHasher<Integer> fromIdEnum(EnumConstant[] values) {
        return fromIdEnum(values, constant -> constant.name().toLowerCase(Locale.ROOT));
    }


    static <EnumConstant extends Enum<EnumConstant>> MinecraftHasher<Integer> fromIdEnum(EnumConstant[] values, Function<EnumConstant, std::string> toName) {
        return STRING.cast(id -> toName.apply(values[id]));
    }


    static <EnumConstant extends Enum<EnumConstant>> MinecraftHasher<EnumConstant> fromEnum() {
        return fromEnum(constant -> constant.name().toLowerCase(Locale.ROOT));
    }


    static <EnumConstant extends Enum<EnumConstant>> MinecraftHasher<EnumConstant> fromEnum(Function<EnumConstant, std::string> toName) {
        return STRING.cast(toName);
    }


    static <Type> MinecraftHasher<Type> mapBuilder(MapBuilder<Type> builder) {
        return (value, encoder) -> builder.apply(new MapHasher<>(value, encoder)).build();
    }


    static <K, V> MinecraftHasher<Map<K, V>> map(MinecraftHasher<K> keyHasher, MinecraftHasher<V> valueHasher) {
        return MinecraftHasher.<Map.Entry<K, V>>mapSet(keyHasher.cast(Map.Entry::getKey), valueHasher.cast(Map.Entry::getValue)).cast(Map::entrySet);
    }


    static <Type> MinecraftHasher<Collection<Type>> mapSet(MinecraftHasher<Type> keyHasher, MinecraftHasher<Type> valueHasher) {
        return (set, encoder) -> encoder.map(set.stream()
            .collect(Collectors.toMap(value -> keyHasher.hash(value, encoder), value -> valueHasher.hash(value, encoder))));
    }


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


    static <Type> MinecraftHasher<Type> dispatch(Function<Type, MinecraftHasher<Type>> hashDispatch) {
        return (value, encoder) -> hashDispatch.apply(value).hash(value, encoder);
    }
}
