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

package org.geysermc.geyser.impl.entity;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.api.entity.data.types.Hitbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record HitboxImpl(
    Vector3f min,
    Vector3f max,
    Vector3f pivot
) implements Hitbox {

    public static List<Hitbox> fromMetaData(@Nullable NbtMap metaDataMap) {
        if (metaDataMap == null) {
            return List.of();
        }

        List<Hitbox> boxes = new ArrayList<>();
        List<NbtMap> hitboxes = metaDataMap.getList("Hitboxes", NbtType.COMPOUND);
        for (NbtMap hitbox : hitboxes) {
            boxes.add(new HitboxImpl(
               Vector3f.from(hitbox.getFloat("MinX"), hitbox.getFloat("MinY"), hitbox.getFloat("MinZ")),
               Vector3f.from(hitbox.getFloat("MaxX"), hitbox.getFloat("MaxY"), hitbox.getFloat("MaxZ")),
               Vector3f.from(hitbox.getFloat("PivotX"),hitbox.getFloat("PivotY"),hitbox.getFloat("PivotZ"))
            ));
        }
        return boxes;
    }

    public NbtMap toNbtMap() {
        return NbtMap.builder()
            .putFloat("MinX", min.getX())
            .putFloat("MinY", min.getY())
            .putFloat("MinZ", min.getZ())
            .putFloat("MaxX", max.getX())
            .putFloat("MaxY", max.getY())
            .putFloat("MaxZ", max.getZ())
            .putFloat("PivotX", pivot.getX())
            .putFloat("PivotY", pivot.getY())
            .putFloat("PivotZ", pivot.getZ())
            .build();
    }

    public static NbtMap toNbtMap(List<Hitbox> hitboxes) {
        List<NbtMap> list = new ArrayList<>();
        for (Hitbox hitbox : hitboxes) {
            if (hitbox instanceof HitboxImpl impl) {
                list.add(impl.toNbtMap());
            } else {
                throw new IllegalArgumentException("Unknown hitbox class implementation: " + hitbox.getClass().getSimpleName());
            }
        }
        return NbtMap.builder().putList("Hitboxes", NbtType.COMPOUND, list).build();
    }

    public static class Builder implements Hitbox.Builder {
        Vector3f min, max, pivot;

        @Override
        public Hitbox.Builder min(@NonNull Vector3f min) {
            Objects.requireNonNull(min, "min");
            this.min = min;
            return this;
        }

        @Override
        public Hitbox.Builder max(@NonNull Vector3f max) {
            Objects.requireNonNull(max, "max");
            this.max = max;
            return this;
        }

        @Override
        public Hitbox.Builder pivot(@NonNull Vector3f pivot) {
            Objects.requireNonNull(pivot, "pivot");
            this.pivot = pivot;
            return this;
        }

        @Override
        public Hitbox build() {
            return new HitboxImpl(min == null ? Vector3f.ZERO : min, max == null ? Vector3f.ZERO : max, pivot == null ? Vector3f.ZERO : pivot);
        }
    }
}
