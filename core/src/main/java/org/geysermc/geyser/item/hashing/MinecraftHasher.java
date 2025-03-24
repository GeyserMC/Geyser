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
import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.item.components.Rarity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectInstance;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Unit;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
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

    MinecraftHasher<ToolData.Rule> TOOL_RULE = mapBuilder(builder -> builder
        .accept("blocks", RegistryHasher.BLOCK.holderSet(), ToolData.Rule::getBlocks)
        .optionalNullable("speed", MinecraftHasher.FLOAT, ToolData.Rule::getSpeed)
        .optionalNullable("correct_for_drops", MinecraftHasher.BOOL, ToolData.Rule::getCorrectForDrops));

    MinecraftHasher<EquipmentSlot> EQUIPMENT_SLOT = fromEnum();

    MinecraftHasher<MobEffectInstance> MOB_EFFECT_INSTANCE = mapBuilder(builder -> builder
        .accept("id", RegistryHasher.EFFECT, MobEffectInstance::getEffect)
        .optional("amplifier", BYTE, instance -> (byte) instance.getDetails().getAmplifier(), (byte) 0)
        .optional("duration", INT, instance -> instance.getDetails().getDuration(), 0)
        .optional("ambient", BOOL, instance -> instance.getDetails().isAmbient(), false)
        .optional("show_particles", BOOL, instance -> instance.getDetails().isShowParticles(), true)
        .accept("show_icon", BOOL, instance -> instance.getDetails().isShowIcon())); // TODO check this, also hidden effect but is recursive

    HashCode hash(T value, MinecraftHashEncoder encoder);

    default MinecraftHasher<List<T>> list() {
        return (list, encoder) -> encoder.list(list.stream().map(element -> hash(element, encoder)).toList());
    }

    default <F> MinecraftHasher<F> convert(Function<F, T> converter) {
        return (object, encoder) -> hash(converter.apply(object), encoder);
    }

    default <F> MinecraftHasher<F> sessionConvert(BiFunction<GeyserSession, F, T> converter) {
        return (object, encoder) -> hash(converter.apply(encoder.session(), object), encoder);
    }

    static <T extends Enum<T>> MinecraftHasher<Integer> fromIdEnum(T[] values, Function<T, String> toName) {
        return STRING.convert(id -> toName.apply(values[id]));
    }

    // TODO: note that this only works correctly if enum constants are named appropriately
    static <T extends Enum<T>> MinecraftHasher<T> fromEnum() {
        return STRING.convert(t -> t.name().toLowerCase());
    }

    static <T> MinecraftHasher<T> mapBuilder(UnaryOperator<MapHasher<T>> builder) {
        return (object, encoder) -> builder.apply(new MapHasher<>(object, encoder)).build();
    }

    static <K, V> MinecraftHasher<Map<K, V>> map(MinecraftHasher<K> keyHasher, MinecraftHasher<V> valueHasher) {
        return (map, encoder) -> encoder.map(map.entrySet().stream()
            .map(entry -> Map.entry(keyHasher.hash(entry.getKey(), encoder), valueHasher.hash(entry.getValue(), encoder)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
}
