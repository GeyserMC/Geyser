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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.RegistryAccess;
import org.geysermc.geyser.session.cache.registry.JavaRegistry;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.session.cache.registry.JavaRegistryProvider;

import java.util.Map;

public class GameTestJavaRegistryProvider implements JavaRegistryProvider {
    private final RegistryAccess registries;
    private final Map<JavaRegistryKey<?>, GameTestJavaRegistry<?>> registryCache = new Object2ObjectOpenHashMap<>();

    public GameTestJavaRegistryProvider(RegistryAccess registries) {
        this.registries = registries;
    }

    @Override
    public <T> JavaRegistry<T> registry(JavaRegistryKey<T> registryKey) {
        //noinspection unchecked
        return (JavaRegistry<T>) registryCache.computeIfAbsent(registryKey, key -> new GameTestJavaRegistry<>(registries, key));
    }
}
