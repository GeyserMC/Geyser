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

import com.google.gson.JsonElement;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaItemDataComponents;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.item.custom.impl.JavaPiercingWeaponImpl;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.components.readers.AttackRangeReader;
import org.geysermc.geyser.registry.mappings.components.readers.BooleanComponentReader;
import org.geysermc.geyser.registry.mappings.components.readers.ConsumableReader;
import org.geysermc.geyser.registry.mappings.components.readers.EnchantableReader;
import org.geysermc.geyser.registry.mappings.components.readers.EquippableReader;
import org.geysermc.geyser.registry.mappings.components.readers.FoodPropertiesReader;
import org.geysermc.geyser.registry.mappings.components.readers.IntComponentReader;
import org.geysermc.geyser.registry.mappings.components.readers.KineticWeaponReader;
import org.geysermc.geyser.registry.mappings.components.readers.RepairableReader;
import org.geysermc.geyser.registry.mappings.components.readers.SwingAnimationReader;
import org.geysermc.geyser.registry.mappings.components.readers.ToolReader;
import org.geysermc.geyser.registry.mappings.components.readers.UnitReader;
import org.geysermc.geyser.registry.mappings.components.readers.UseCooldownReader;
import org.geysermc.geyser.registry.mappings.components.readers.UseEffectsReader;
import org.geysermc.geyser.util.MinecraftKey;

import java.util.HashMap;
import java.util.Map;

public class DataComponentReaders {
    private static final Map<Key, DataComponentReader<?>> READERS = new HashMap<>();

    public static void readDataComponent(CustomItemDefinition.Builder builder, String key, @NonNull JsonElement element, String baseContext) throws InvalidCustomMappingsFileException {
        // Component removal
        if (key.startsWith("!")) {
            builder.removeComponent(Identifier.of(key.substring(1)));
            return;
        }

        DataComponentReader<?> reader = READERS.get(MinecraftKey.key(key));
        if (reader == null) {
            throw new InvalidCustomMappingsFileException("reading data components", "unknown data component " + key, baseContext);
        }
        reader.read(builder, element, "component " + key, baseContext);
    }

    private static void register(DataComponentReader<?> reader) {
        Key key = MinecraftKey.identifierToKey(reader.type().identifier());
        if (READERS.containsKey(key)) {
            throw new IllegalStateException("Duplicate component reader for component: " + reader.type().identifier());
        }
        READERS.put(key, reader);
    }

    static {
        register(new ConsumableReader());
        register(new EquippableReader());
        register(new FoodPropertiesReader());
        register(new IntComponentReader(JavaItemDataComponents.MAX_DAMAGE, 0));
        register(new IntComponentReader(JavaItemDataComponents.MAX_STACK_SIZE, 1, 99));
        register(new UseCooldownReader());
        register(new EnchantableReader());
        register(new ToolReader());
        register(new RepairableReader());
        register(new BooleanComponentReader(JavaItemDataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        register(new AttackRangeReader());
        register(new KineticWeaponReader());
        register(new UnitReader<>(JavaItemDataComponents.PIERCING_WEAPON, JavaPiercingWeaponImpl.INSTANCE));
        register(new SwingAnimationReader());
        register(new UseEffectsReader());
    }
}
