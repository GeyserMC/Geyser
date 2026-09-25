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
import lombok.Getter;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.RegistryCache;
import org.geysermc.geyser.session.cache.TagCache;
import org.geysermc.geyser.session.cache.tags.Tag;

public final class JavaRegistryTagCache implements JavaRegistryProvider {
    @Getter
    private final RegistryCache registryCache;
    @Getter
    private final TagCache tagCache;

    public JavaRegistryTagCache(GeyserSession session) {
        registryCache = new RegistryCache(session);
        tagCache = new TagCache(session);
    }

    @Override
    public <T> JavaRegistry<T> registry(JavaRegistryKey<T> registryKey) {
        // TODO hardcoded registries
        return registryCache.registry(registryKey);
    }

    @Override
    public IntList rawTag(Tag<?> tag) {
        return tagCache.get(tag);
    }
}
