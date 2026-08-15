/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
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
 */

package org.geysermc.geyser.registry.type;

import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Composite key for {@link org.geysermc.geyser.registry.BlockRegistries#CUSTOM_BLOCK_ITEM_OVERRIDES}.
 * <p>
 * Base overrides use {@code itemModel = null} and match any model.
 * Model-specific overrides carry a non-null {@link Key} and must match exactly.
 */
public record CustomBlockItemOverride(String javaIdentifier, @Nullable Key itemModel) {
    public CustomBlockItemOverride {
        Objects.requireNonNull(javaIdentifier, "javaIdentifier");
    }

    /**
     * Creates a base override that matches all items with the given Java identifier.
     */
    public static CustomBlockItemOverride base(String javaIdentifier) {
        return new CustomBlockItemOverride(javaIdentifier, null);
    }
}
