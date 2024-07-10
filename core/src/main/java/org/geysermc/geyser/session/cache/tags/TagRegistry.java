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

import java.util.Map;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.util.MinecraftKey;

@Getter
public enum TagRegistry {
    BLOCK("block", BlockTag.ALL_BLOCK_TAGS),
    ITEM("item", ItemTag.ALL_ITEM_TAGS),
    ENCHANTMENT("enchantment", EnchantmentTag.ALL_ENCHANTMENT_TAGS);

    private final Key registryKey;
    private final Map<Key, Tag> vanillaTags;

    TagRegistry(String registry, Map<Key, Tag> vanillaTags) {
        this.registryKey = MinecraftKey.key(registry);
        this.vanillaTags = vanillaTags;
    }

    @Nullable
    public static TagRegistry valueOf(Key registryKey) {
        for (TagRegistry registry : TagRegistry.values()) {
            if (registry.registryKey.equals(registryKey)) {
                return registry;
            }
        }
        return null;
    }
}
