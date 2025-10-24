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

package org.geysermc.geyser.gametest.registries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.session.cache.RegistryCache;
import org.geysermc.geyser.session.cache.registry.JavaRegistry;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.session.cache.registry.RegistryEntryData;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.ToIntFunction;

public class GameTestJavaRegistry<T> implements JavaRegistry<T> {
    private final List<RegistryEntryData<T>> entries;

    public GameTestJavaRegistry(RegistryAccess registries, JavaRegistryKey<T> registryKey) {
        Registry<?> registry = registries.lookupOrThrow(geyserKeyToMojangKey(registryKey));
        entries = convertRegistryData(registryKey, registries, registry);
    }

    @Override
    public List<RegistryEntryData<T>> entries() {
        return entries;
    }

    private static <Mojang, Geyser> List<RegistryEntryData<Geyser>> convertRegistryData(JavaRegistryKey<Geyser> registryKey, RegistryAccess registries, Registry<Mojang> registry) {
        DynamicOps<Object> nbtOps = registries.createSerializationContext(CloudburstNbtOps.INSTANCE);
        Codec<Mojang> codec = getSyncedRegistryData(registry.key()).elementCodec();
        //noinspection unchecked
        RegistryCache.RegistryReader<Geyser> reader = (RegistryCache.RegistryReader<Geyser>) RegistryCache.READERS.get(registryKey);

        ToIntFunction<Key> keyIdFunction = key -> registry.getId(registry.getValue(keyToResourceLocation(key)));
        List<RegistryEntryData<Geyser>> entries = new ArrayList<>();
        for (Mojang entry : registry) {
            int id = registry.getIdOrThrow(entry);
            Key key = resourceLocationToKey(Objects.requireNonNull(registry.getKey(entry)));
            NbtMap encoded = (NbtMap) codec.encodeStart(nbtOps, entry).getOrThrow();
            Geyser mapped = reader.read(new RegistryEntryContext(new RegistryEntry(key, encoded), keyIdFunction, Optional.empty()));
            entries.add(new RegistryEntryData<>(id, key, mapped));
        }
        return List.copyOf(entries);
    }

    private static <T> RegistryDataLoader.RegistryData<T> getSyncedRegistryData(ResourceKey<? extends Registry<T>> registry) {
        //noinspection unchecked
        return RegistryDataLoader.SYNCHRONIZED_REGISTRIES.stream()
            .filter(data -> data.key() == registry)
            .map(data -> (RegistryDataLoader.RegistryData<T>) data)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(registry + " is not a network synced registry"));
    }

    private static ResourceKey<? extends Registry<?>> geyserKeyToMojangKey(JavaRegistryKey<?> key) {
        return ResourceKey.createRegistryKey(keyToResourceLocation(key.registryKey()));
    }

    private static ResourceLocation keyToResourceLocation(Key key) {
        return ResourceLocation.fromNamespaceAndPath(key.namespace(), key.value());
    }

    private static Key resourceLocationToKey(ResourceLocation location) {
        //noinspection PatternValidation
        return Key.key(location.getNamespace(), location.getPath());
    }
}
