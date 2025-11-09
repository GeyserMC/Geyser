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

package org.geysermc.geyser.api.network.message;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents the priority of a message when being processed.
 * @since 2.9.1
 */
public enum MessagePriority {
    FIRST(100),
    EARLY(50),
    NORMAL(0),
    LATE(-50),
    LAST(-100);

    private final int value;

    MessagePriority(int value) {
        this.value = value;
    }

    /**
     * Gets the numeric value associated with this priority. Higher means earlier.
     *
     * @return the priority value
     */
    public int value() {
        return value;
    }

    /**
     * Creates a custom priority in the range [-100, 100].
     *
     * @param value the priority value
     * @return the priority
     * @throws IllegalArgumentException if outside allowed range
     */
    @NonNull
    public static MessagePriority of(int value) {
        if (value >= 75) return LAST;
        if (value >= 25) return LATE;
        if (value <= -75) return FIRST;
        if (value <= -25) return EARLY;
        return NORMAL;
    }
}
