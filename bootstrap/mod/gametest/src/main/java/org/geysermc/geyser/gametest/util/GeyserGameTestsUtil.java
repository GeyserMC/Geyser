/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.gametest.util;

import net.kyori.adventure.key.Key;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistryKey;
import org.geysermc.geyser.session.cache.tags.Tag;

import java.util.Objects;
import java.util.Optional;

public final class GeyserGameTestsUtil {

    private GeyserGameTestsUtil() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static TagKey<?> geyserTagToMojangTag(Tag<?> tag) {
        return TagKey.create((ResourceKey) geyserKeyToMojangKey(tag.registry()), keyToIdentifier(tag.tag()));
    }

    public static ResourceKey<? extends Registry<?>> geyserKeyToMojangKey(JavaRegistryKey<?> key) {
        return ResourceKey.createRegistryKey(keyToIdentifier(key.registryKey()));
    }

    public static JavaRegistryKey<?> mojangKeyToGeyserKey(ResourceKey<? extends Registry<?>> key) {
        return Objects.requireNonNull(JavaRegistries.fromKey(identifierToKey(key.identifier())), "Geyser does not support registry " + key.identifier());
    }

    public static Identifier keyToIdentifier(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }

    public static Key identifierToKey(Identifier identifier) {
        //noinspection PatternValidation
        return Key.key(identifier.getNamespace(), identifier.getPath());
    }

    public static <T> Optional<RegistryDataLoader.RegistryData<T>> getSyncedRegistryData(ResourceKey<? extends Registry<T>> registry) {
        //noinspection unchecked
        return RegistryDataLoader.SYNCHRONIZED_REGISTRIES.stream()
            .filter(data -> data.key() == registry)
            .map(data -> (RegistryDataLoader.RegistryData<T>) data)
            .findFirst();
    }
}
