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

package org.geysermc.geyser.api.item.custom.v2.component;

import java.util.Set;

/**
 * A map of data components to their values. Mainly used internally when mapping custom items.
 * @since 2.9.3
 */
public interface ItemDataComponentMap {

    /**
     * @return the value of the given component, or null if it is not in the map
     * @since 2.9.3
     */
    <T> T get(ItemDataComponent<T> type);

    /**
     * @return the value of the given component, or {@code fallback} if it is null
     * @since 2.9.3
     */
    default <T> T getOrDefault(ItemDataComponent<T> type, T fallback) {
        T value = get(type);
        return value == null ? fallback : value;
    }

    /**
     * @return all data components in this map
     * @since 2.9.3
     */
    Set<ItemDataComponent<?>> keySet();
}
