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

package org.geysermc.geyser.registry.mappings.components;

import com.fasterxml.jackson.databind.JsonNode;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.components.readers.ConsumableReader;
import org.geysermc.geyser.registry.mappings.components.readers.EquippableReader;
import org.geysermc.geyser.registry.mappings.components.readers.FoodReader;
import org.geysermc.geyser.registry.mappings.components.readers.IntComponentReader;
import org.geysermc.geyser.registry.mappings.components.readers.UseCooldownReader;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.HashMap;
import java.util.Map;

public class DataComponentReaders {
    private static final Map<Key, DataComponentReader<?>> READERS = new HashMap<>();

    public static void readDataComponent(DataComponents components, Key key, @NonNull JsonNode node) throws InvalidCustomMappingsFileException {
        DataComponentReader<?> reader = READERS.get(key);
        if (reader == null) {
            throw new InvalidCustomMappingsFileException("Unknown data component " + key);
        }
        reader.read(components, node);
    }

    static {
        READERS.put(MinecraftKey.key("consumable"), new ConsumableReader());
        READERS.put(MinecraftKey.key("equippable"), new EquippableReader());
        READERS.put(MinecraftKey.key("food"), new FoodReader());
        READERS.put(MinecraftKey.key("max_damage"), new IntComponentReader(DataComponentType.MAX_DAMAGE, 0));
        READERS.put(MinecraftKey.key("max_stack_size"), new IntComponentReader(DataComponentType.MAX_STACK_SIZE, 0, 99));
        READERS.put(MinecraftKey.key("use_cooldown"), new UseCooldownReader());
    }
}
