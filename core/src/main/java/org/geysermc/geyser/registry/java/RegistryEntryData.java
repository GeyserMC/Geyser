/*
 * Copyright (c) 2025-2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.java;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

@Accessors(fluent = true)
@EqualsAndHashCode
@ToString
public final class RegistryEntryData<T> {
    @Getter
    private final int id;
    @Getter
    private final Key key;
    private @Nullable T data;

    public RegistryEntryData(int id, Key key) {
        this.id = id;
        this.key = key;
    }

    public T data() {
        if (data == null) {
            throw new IllegalStateException("Tried to access unbound registry entry " + key + "!");
        }
        return data;
    }

    public void bind(T data) {
        assert this.data == null : "Tried to bind registry entry " + key + " twice!";
        this.data = Objects.requireNonNull(data, "Value for registry entry " + key + " must not be null when binding");
    }

    public boolean isBound() {
        return data != null;
    }
}
