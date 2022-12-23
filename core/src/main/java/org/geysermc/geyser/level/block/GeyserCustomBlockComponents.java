/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level.block;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Value;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.block.custom.component.BoxComponent;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.block.custom.component.RotationComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

@Value
public class GeyserCustomBlockComponents implements CustomBlockComponents {
    BoxComponent selectionBox;
    BoxComponent collisionBox;
    String geometry;
    Map<String, MaterialInstance> materialInstances;
    Float destroyTime;
    Float friction;
    Integer lightEmission;
    Integer lightDampening;
    RotationComponent rotation;

    private GeyserCustomBlockComponents(CustomBlockComponentsBuilder builder) {
        this.selectionBox = builder.selectionBox;
        this.collisionBox = builder.collisionBox;
        this.geometry = builder.geometry;
        if (builder.materialInstances.isEmpty()) {
            this.materialInstances = Object2ObjectMaps.emptyMap();
        } else {
            this.materialInstances = Object2ObjectMaps.unmodifiable(new Object2ObjectArrayMap<>(builder.materialInstances));
        }
        this.destroyTime = builder.destroyTime;
        this.friction = builder.friction;
        this.lightEmission = builder.lightEmission;
        this.lightDampening = builder.lightDampening;
        this.rotation = builder.rotation;
    }

    @Override
    public BoxComponent selectionBox() {
        return selectionBox;
    }

    @Override
    public BoxComponent collisionBox() {
        return collisionBox;
    }

    @Override
    public String geometry() {
        return geometry;
    }

    @Override
    public @NonNull Map<String, MaterialInstance> materialInstances() {
        return materialInstances;
    }

    @Override
    public Float destroyTime() {
        return destroyTime;
    }

    @Override
    public Float friction() {
        return friction;
    }

    @Override
    public Integer lightEmission() {
        return lightEmission;
    }

    @Override
    public Integer lightDampening() {
        return lightDampening;
    }

    @Override
    public RotationComponent rotation() {
        return rotation;
    }

    public static class CustomBlockComponentsBuilder implements Builder {
        protected BoxComponent selectionBox;
        protected BoxComponent collisionBox;
        protected String displayName;
        protected String geometry;
        protected final Object2ObjectMap<String, MaterialInstance> materialInstances = new Object2ObjectOpenHashMap<>();
        protected Float destroyTime;
        protected Float friction;
        protected Integer lightEmission;
        protected Integer lightDampening;
        protected RotationComponent rotation;

        private static final Set<String> VALID_MATERIAL_INSTANCE_NAMES = ImmutableSet.of("*", "up", "down", "north", "south", "west", "east");

        private void validateBox(BoxComponent box) {
            if (box == null) {
                return;
            }
            if (box.sizeX() < 0 || box.sizeY() < 0 || box.sizeZ() < 0) {
                throw new IllegalArgumentException("Box size must be non-negative.");
            }
            float minX = box.originX() + 8;
            float minY = box.originY();
            float minZ = box.originZ() + 8;
            float maxX = minX + box.sizeX();
            float maxY = minY + box.sizeY();
            float maxZ = minZ + box.sizeZ();
            if (minX < 0 || minY < 0 || minZ < 0 || maxX > 16 || maxY > 16 || maxZ > 16) {
                throw new IllegalArgumentException("Box bounds must be within (0, 0, 0) and (16, 16, 16)");
            }
        }

        @Override
        public Builder selectionBox(BoxComponent selectionBox) {
            validateBox(selectionBox);
            this.selectionBox = selectionBox;
            return this;
        }

        @Override
        public Builder collisionBox(BoxComponent collisionBox) {
            validateBox(collisionBox);
            this.collisionBox = collisionBox;
            return this;
        }

        @Override
        public Builder geometry(String geometry) {
            this.geometry = geometry;
            return this;
        }

        @Override
        public Builder materialInstance(@NotNull String name, @NotNull MaterialInstance materialInstance) {
            if (!VALID_MATERIAL_INSTANCE_NAMES.contains(name)) {
                throw new IllegalArgumentException("Material instance name must be one of " + VALID_MATERIAL_INSTANCE_NAMES);
            }
            this.materialInstances.put(name, materialInstance);
            return this;
        }

        @Override
        public Builder destroyTime(Float destroyTime) {
            if (destroyTime != null && destroyTime < 0) {
                throw new IllegalArgumentException("Destroy time must be non-negative");
            }
            this.destroyTime = destroyTime;
            return this;
        }

        @Override
        public Builder friction(Float friction) {
            if (friction != null) {
                if (friction < 0 || friction > 1) {
                    throw new IllegalArgumentException("Friction must be in the range 0-1");
                }
            }
            this.friction = friction;
            return this;
        }

        @Override
        public Builder lightEmission(Integer lightEmission) {
            if (lightEmission != null) {
                if (lightEmission < 0 || lightEmission > 15) {
                    throw new IllegalArgumentException("Light emission must be in the range 0-15");
                }
            }
            this.lightEmission = lightEmission;
            return this;
        }

        @Override
        public Builder lightDampening(Integer lightDampening) {
            if (lightDampening != null) {
                if (lightDampening < 0 || lightDampening > 15) {
                    throw new IllegalArgumentException("Light dampening must be in the range 0-15");
                }
            }
            this.lightDampening = lightDampening;
            return this;
        }

        @Override
        public Builder rotation(RotationComponent rotation) {
            if (rotation.x() % 90 != 0 || rotation.y() % 90 != 0 || rotation.z() % 90 != 0) {
                throw new IllegalArgumentException("Rotation must be a multiple of 90 degrees.");
            }
            this.rotation = rotation;
            return this;
        }

        @Override
        public CustomBlockComponents build() {
            return new GeyserCustomBlockComponents(this);
        }
    }
}
