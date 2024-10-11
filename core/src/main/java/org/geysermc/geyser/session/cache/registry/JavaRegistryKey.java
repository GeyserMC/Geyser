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

package org.geysermc.geyser.session.cache.registry;

import lombok.Getter;
import net.kyori.adventure.key.Key;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.session.cache.tags.VanillaTag;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.geyser.util.Ordered;

import java.util.HashMap;
import java.util.Map;

public class JavaRegistryKey<T> implements Ordered {
    @Getter
    private static int registered = 0;

    /**
     * A map mapping vanilla tag keys in this registry to a {@link Tag} instance (this is a {@link VanillaTag}).
     *
     * Keys should never be manually added to this map. Rather, {@link JavaRegistryKey#registerVanillaTag} should be used, during Geyser init.
     */
    // TODO should this be changed to VanillaTag ? check
    @Getter
    private final Map<Key, Tag<T>> vanillaTags = new HashMap<>();
    @Getter
    private final Key registryKey;
    private final int geyserId;

    private JavaRegistryKey(Key registryKey, int geyserId) {
        this.registryKey = registryKey;
        this.geyserId = geyserId;
    }

    public static <T> JavaRegistryKey<T> create(String key) {
        JavaRegistryKey<T> registry = new JavaRegistryKey<>(MinecraftKey.key(key), registered);
        registered++;
        return registry;
    }

    public Tag<T> registerVanillaTag(Key identifier) {
        if (vanillaTags.containsKey(identifier)) {
            throw new IllegalArgumentException("Vanilla tag " + identifier + " was already registered!");
        }

        Tag<T> tag = new VanillaTag<>(this, identifier, vanillaTags.size());
        vanillaTags.put(identifier, tag);
        return tag;
    }

    @Override
    public int ordinal() {
        return geyserId;
    }
}
