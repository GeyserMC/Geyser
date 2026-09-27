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
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.TagKey;
import org.geysermc.geyser.gametest.util.CloudburstNbtOps;
import org.geysermc.geyser.gametest.util.GeyserGameTestsUtil;
import org.geysermc.geyser.registry.java.BuiltInJavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistry;
import org.geysermc.geyser.registry.java.JavaRegistryKey;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.geyser.registry.java.MutableJavaRegistry;
import org.geysermc.geyser.registry.java.SimpleJavaRegistry;
import org.geysermc.geyser.registry.java.reader.JavaRegistryReaders;
import org.geysermc.geyser.registry.java.reader.KeyDependentJavaRegistryReader;
import org.geysermc.geyser.registry.java.reader.RegistryEntryContext;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;

import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

// TODO directly use registry/tag cache?
public class GameTestJavaRegistryProvider implements JavaRegistryProvider {
    private final RegistryAccess registries;
    private final Map<JavaRegistryKey<?>, JavaRegistry<?>> registryCache = new Reference2ObjectOpenHashMap<>();
    private final Map<Tag<?>, IntList> tagCache = new Object2ObjectOpenHashMap<>();

    public GameTestJavaRegistryProvider(RegistryAccess registries) {
        this.registries = registries;
    }

    @Override
    public <T> JavaRegistry<T> registry(JavaRegistryKey<T> registryKey) {
        //noinspection unchecked
        return (JavaRegistry<T>) registryCache.computeIfAbsent(registryKey, this::convertRegistryData);
    }

    @Override
    public IntList rawTag(Tag<?> tag) {
        return tagCache.computeIfAbsent(tag, _ -> serializeTag(GeyserGameTestsUtil.geyserTagToMojangTag(tag)));
    }

    private <T> JavaRegistry<T> convertRegistryData(JavaRegistryKey<T> registryKey) {
        try {
            return BuiltInJavaRegistries.PROVIDER.registry(registryKey);
        } catch (IllegalArgumentException ignored) {} // Not built-in.

        Registry<?> mojangRegistry = registries.lookupOrThrow(GeyserGameTestsUtil.geyserKeyToMojangKey(registryKey));
        return buildRegistry(mojangRegistry, registryKey);
    }

    private <Mojang, Geyser> JavaRegistry<Geyser> buildRegistry(Registry<Mojang> mojangRegistry, JavaRegistryKey<Geyser> geyserKey) {
        MutableJavaRegistry<Geyser> geyserRegistry = new SimpleJavaRegistry<>(geyserKey);

        DynamicOps<Object> nbtOps = registries.createSerializationContext(CloudburstNbtOps.INSTANCE);
        Codec<Mojang> codec = GeyserGameTestsUtil.getSyncedRegistryData(mojangRegistry.key()).orElseThrow().elementCodec();
        KeyDependentJavaRegistryReader<Geyser> reader = JavaRegistryReaders.getReader(geyserKey).orElseThrow();

        // listElementIds is sorted by network ID
        mojangRegistry.listElementIds().forEach(key -> geyserRegistry.register(GeyserGameTestsUtil.identifierToKey(key.identifier())));

        geyserRegistry.freeze(id -> {
            Object encoded = codec.encodeStart(nbtOps, mojangRegistry.get(id).orElseThrow().value()).getOrThrow();
            return reader.read(new RegistryEntryContext(this, new RegistryEntry(geyserRegistry.getKeyOrThrow(id), encoded), Optional.empty()));
        });

        return geyserRegistry;
    }

    private <T> IntList serializeTag(TagKey<T> tag) {
        Registry<T> registry = registries.lookupOrThrow(tag.registry());
        return IntList.of(StreamSupport.stream(registry.getTagOrEmpty(tag).spliterator(), false)
            .mapToInt(holder -> registry.getIdOrThrow(holder.value()))
            .toArray());
    }
}
