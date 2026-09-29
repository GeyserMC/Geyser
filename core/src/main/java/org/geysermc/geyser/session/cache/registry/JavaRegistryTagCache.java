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

package org.geysermc.geyser.session.cache.registry;

import it.unimi.dsi.fastutil.ints.IntList;
import org.geysermc.geyser.registry.java.BuiltInJavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistry;
import org.geysermc.geyser.registry.java.JavaRegistryKey;
import org.geysermc.geyser.registry.java.JavaRegistryProvider;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundUpdateTagsPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;

import java.util.Optional;

/**
 * A {@link JavaRegistryProvider} for a {@link GeyserSession}, accessed through {@link GeyserSession#javaRegistries()}.
 *
 * <p>This class builds on top of {@link BuiltInJavaRegistries#PROVIDER}, and as such works with both built-in and networked registries.</p>
 */
public final class JavaRegistryTagCache implements JavaRegistryProvider {
    private final RegistryCache registryCache;
    private final TagCache tagCache;

    public JavaRegistryTagCache() {
        registryCache = new RegistryCache();
        tagCache = new TagCache();
    }

    @Override
    public <T> JavaRegistry<T> registry(JavaRegistryKey<T> registryKey) {
        return registryCache.registry(registryKey)
            .orElseGet(() -> BuiltInJavaRegistries.PROVIDER.registry(registryKey));
    }

    @Override
    public IntList rawTag(Tag<?> tag) {
        return tagCache.get(tag);
    }

    public void loadRegistryData(Optional<GeyserSession> session, ClientboundRegistryDataPacket registryData) {
        registryCache.load(this, session, registryData);
    }

    public void loadTags(Optional<GeyserSession> session, ClientboundUpdateTagsPacket tagsPacket) {
        tagCache.loadPacket(session, tagsPacket);
    }
}
