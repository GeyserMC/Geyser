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
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.item.components.Rarity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Unit;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
@FunctionalInterface
public interface MinecraftHasher<T> {

    MinecraftHasher<Unit> UNIT = (unit, encoder) -> encoder.emptyMap();

    MinecraftHasher<Byte> BYTE = (b, encoder) -> encoder.number(b);

    MinecraftHasher<Short> SHORT = (s, encoder) -> encoder.number(s);

    MinecraftHasher<Integer> INT = (i, encoder) -> encoder.number(i);

    MinecraftHasher<Long> LONG = (l, encoder) -> encoder.number(l);

    MinecraftHasher<Float> FLOAT = (f, encoder) -> encoder.number(f);

    MinecraftHasher<Double> DOUBLE = (d, encoder) -> encoder.number(d);

    MinecraftHasher<String> STRING = (s, encoder) -> encoder.string(s);

    MinecraftHasher<Boolean> BOOL = (b, encoder) -> encoder.bool(b);

    MinecraftHasher<NbtMap> NBT_MAP = (map, encoder) -> encoder.nbtMap(map);

    MinecraftHasher<Key> KEY = STRING.convert(Key::asString);

    MinecraftHasher<Key> TAG = STRING.convert(key -> "#" + key.asString());

    MinecraftHasher<Integer> RARITY = fromIdEnum(Rarity.values(), Rarity::getName);

    MinecraftHasher<Consumable.ItemUseAnimation> ITEM_USE_ANIMATION = fromEnum();

    MinecraftHasher<EquipmentSlot> EQUIPMENT_SLOT = fromEnum(); // FIXME MCPL enum constants aren't right

    HashCode hash(T value, MinecraftHashEncoder encoder);

    default MinecraftHasher<List<T>> list() {
        return (list, encoder) -> encoder.list(list.stream().map(element -> hash(element, encoder)).toList());
    }

    default <F> MinecraftHasher<F> convert(Function<F, T> converter) {
        return (value, encoder) -> hash(converter.apply(value), encoder);
    }

    default <F> MinecraftHasher<F> sessionConvert(BiFunction<GeyserSession, F, T> converter) {
        return (value, encoder) -> hash(converter.apply(encoder.session(), value), encoder);
    }

    static <T> MinecraftHasher<T> recursive(UnaryOperator<MinecraftHasher<T>> delegate) {
        return new Recursive<>(delegate);
    }

    static <T extends Enum<T>> MinecraftHasher<Integer> fromIdEnum(T[] values, Function<T, String> toName) {
        return STRING.convert(id -> toName.apply(values[id]));
    }

    // TODO: note that this only works correctly if enum constants are named appropriately
    static <T extends Enum<T>> MinecraftHasher<T> fromEnum() {
        return STRING.convert(t -> t.name().toLowerCase());
    }

    static <T> MinecraftHasher<T> mapBuilder(UnaryOperator<MapHasher<T>> builder) {
        return (value, encoder) -> builder.apply(new MapHasher<>(value, encoder)).build();
    }

    static <K, V> MinecraftHasher<Map<K, V>> map(MinecraftHasher<K> keyHasher, MinecraftHasher<V> valueHasher) {
        return (map, encoder) -> encoder.map(map.entrySet().stream()
            .map(entry -> Map.entry(keyHasher.hash(entry.getKey(), encoder), valueHasher.hash(entry.getValue(), encoder)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    class Recursive<T> implements MinecraftHasher<T> {
        private final Supplier<MinecraftHasher<T>> delegate;

        public Recursive(UnaryOperator<MinecraftHasher<T>> delegate) {
            this.delegate = Suppliers.memoize(() -> delegate.apply(this));
        }

        @Override
        public HashCode hash(T value, MinecraftHashEncoder encoder) {
            return delegate.get().hash(value, encoder);
        }
    }
}
