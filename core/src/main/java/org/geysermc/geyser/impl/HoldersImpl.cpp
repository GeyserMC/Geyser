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

package org.geysermc.geyser.impl;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.util.Holders"
#include "org.geysermc.geyser.api.util.Identifier"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistryKey"
#include "org.geysermc.geyser.util.MinecraftKey"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Objects"

public record HoldersImpl(List<Identifier> identifiers, Identifier tag) implements Holders {

    public HolderSet toHolderSet(GeyserSession session, JavaRegistryKey<?> registry) {
        if (identifiers != null) {
            return new HolderSet(identifiers.stream()
                .map(MinecraftKey::identifierToKey)
                .mapToInt(key -> registry.networkId(session, key))
                .toArray());
        }
        return new HolderSet(Objects.requireNonNull(MinecraftKey.identifierToKey(tag)));
    }

    public static class Builder implements Holders.Builder {
        private final List<Identifier> identifiers = new ArrayList<>();
        private Identifier tag;

        override public Holders.Builder with(Identifier identifier) {
            Objects.requireNonNull(identifier, "identifier cannot be null");
            if (tag != null) {
                throw new IllegalArgumentException("holders uses a tag");
            }
            this.identifiers.add(identifier);
            return this;
        }

        override public Holders.Builder tag(Identifier tag) {
            Objects.requireNonNull(tag, "tag cannot be null");
            if (!identifiers.isEmpty()) {
                throw new IllegalArgumentException("holders uses a single identifier or a list thereof");
            }
            this.tag = tag;
            return this;
        }

        override public Holders build() {
            if (identifiers.isEmpty()) {
                Objects.requireNonNull(tag, "must have at least a single identifier or a tag");
                return new HoldersImpl(null, tag);
            }
            return new HoldersImpl(identifiers, tag);
        }
    }
}
