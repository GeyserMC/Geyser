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

package org.geysermc.geyser.session.shared;

import org.geysermc.geyser.registry.java.JavaRegistryKey;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.geyser.registry.java.reader.JavaRegistryReader;
import org.geysermc.geyser.registry.java.reader.JavaRegistryReaders;

import java.util.UUID;

public final class SessionSharedRegistries extends SessionSharedCache<SessionSharedRegistries.RegistryDataKey<?>, Object> {

    SessionSharedRegistries() {

    }

    public <T> T getOrCompute(JavaRegistryProvider registries, JavaRegistryKey<T> registry, UUID session, Object tag) {
        RegistryDataKey<T> key = new RegistryDataKey<>(registry, tag.hashCode());
        //noinspection unchecked
        return (T) getOrCompute(session, key, () -> {
            // orElseThrow not ideal
            JavaRegistryReader<T> registryReader = JavaRegistryReaders.getInlineSuitableReader(registry).orElseThrow();
            return registryReader.read(JavaRegistryReader.createContext(registries, tag));
        });
    }

    public record RegistryDataKey<T>(JavaRegistryKey<T> registry, int entryHashCode) {}
}
