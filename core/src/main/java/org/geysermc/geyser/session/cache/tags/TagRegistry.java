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

package org.geysermc.geyser.session.cache.tags;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.util.MinecraftKey;

/**
 * Lists registries that Geyser stores tags for.
 *
 * When wanting to store tags from a new registry, add the registry here, and register all vanilla tags for it using {@link TagRegistry#registerVanillaTag}. These vanilla tags
 * can be stored in a vanilla tag class, like {@link BlockTag} and {@link ItemTag}. This class can then have an init method that's called in {@link TagRegistry#init}, to ensure
 * that all vanilla tags are registered before any connection is made.
 */
public enum TagRegistry {
    BLOCK("block"),
    ITEM("item"),
    ENCHANTMENT("enchantment");

    private final Key registryKey;

    /**
     * A map mapping vanilla tag keys in this registry to a {@link Tag} instance (this is a {@link VanillaTag}).
     * 
     * Keys should never be manually added to this map. Rather, {@link TagRegistry#registerVanillaTag} should be used, during Geyser init.
     */
    @Getter
    private final Map<Key, Tag> vanillaTags;

    TagRegistry(String registry) {
        this.registryKey = MinecraftKey.key(registry);
        this.vanillaTags = new HashMap<>();
    }

    public Tag registerVanillaTag(Key identifier) {
        if (vanillaTags.containsKey(identifier)) {
            throw new IllegalArgumentException("Vanilla tag " + identifier + " was already registered!");
        }

        Tag tag = new VanillaTag(this, identifier, vanillaTags.size());
        vanillaTags.put(identifier, tag);
        return tag;
    }

    @Nullable
    public static TagRegistry fromKey(Key registryKey) {
        for (TagRegistry registry : TagRegistry.values()) {
            if (registry.registryKey.equals(registryKey)) {
                return registry;
            }
        }
        return null;
    }

    public static void init() {
        BlockTag.init();
        ItemTag.init();
        EnchantmentTag.init();
    }
}
