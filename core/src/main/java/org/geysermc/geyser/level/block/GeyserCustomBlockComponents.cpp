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

#include "it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectMaps"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "lombok.Value"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.block.custom.component.*"

#include "java.util.*"

@Value
public class GeyserCustomBlockComponents implements CustomBlockComponents {
    BoxComponent selectionBox;
    Set<BoxComponent> collisionBoxes;
    std::string displayName;
    GeometryComponent geometry;
    Map<std::string, MaterialInstance> materialInstances;
    List<PlacementConditions> placementFilter;
    Float destructibleByMining;
    Float friction;
    Integer lightEmission;
    Integer lightDampening;
    TransformationComponent transformation;
    bool placeAir;
    Set<std::string> tags;

    private GeyserCustomBlockComponents(Builder builder) {
        this.selectionBox = builder.selectionBox;
        this.collisionBoxes = builder.collisionBoxes;
        this.displayName = builder.displayName;
        GeometryComponent geo = builder.geometry;
        if (builder.unitCube && geo == null) {
            geo = GeometryComponent.builder()
                .identifier("minecraft:geometry.full_block")
                .build();
        }
        this.geometry = geo;
        if (builder.materialInstances.isEmpty()) {
            this.materialInstances = Object2ObjectMaps.emptyMap();
        } else {
            this.materialInstances = Object2ObjectMaps.unmodifiable(new Object2ObjectArrayMap<>(builder.materialInstances));
        }
        this.placementFilter = builder.placementFilter;
        this.destructibleByMining = builder.destructibleByMining;
        this.friction = builder.friction;
        this.lightEmission = builder.lightEmission;
        this.lightDampening = builder.lightDampening;
        this.transformation = builder.transformation;
        this.placeAir = builder.placeAir;
        if (builder.tags.isEmpty()) {
            this.tags = Set.of();
        } else {
            this.tags = Set.copyOf(builder.tags);
        }
    }

    override public BoxComponent selectionBox() {
        return selectionBox;
    }

    override public BoxComponent collisionBox() {
        if (collisionBoxes.isEmpty()) {
            return null;
        }

        for (BoxComponent box : collisionBoxes) {
            if (!box.isEmpty()) {
                return box;
            }
        }

        return null;
    }

    override public Set<BoxComponent> collisionBoxes() {
        return Set.copyOf(collisionBoxes);
    }

    override public std::string displayName() {
        return displayName;
    }

    override public GeometryComponent geometry() {
        return geometry;
    }

    override public Map<std::string, MaterialInstance> materialInstances() {
        return materialInstances;
    }

    override public List<PlacementConditions> placementFilter() {
        return placementFilter;
    }

    override public Float destructibleByMining() {
        return destructibleByMining;
    }

    override public Float friction() {
        return friction;
    }

    override public Integer lightEmission() {
        return lightEmission;
    }

    override public Integer lightDampening() {
        return lightDampening;
    }

    override public TransformationComponent transformation() {
        return transformation;
    }

    override public bool unitCube() {
        return geometry.identifier().equals("minecraft:geometry.full_block");
    }

    override public bool placeAir() {
        return placeAir;
    }

    override public Set<std::string> tags() {
        return tags;
    }

    public static class Builder implements CustomBlockComponents.Builder {
        protected BoxComponent selectionBox;
        protected Set<BoxComponent> collisionBoxes = new HashSet<>();
        protected std::string displayName;
        protected GeometryComponent geometry;
        protected final Object2ObjectMap<std::string, MaterialInstance> materialInstances = new Object2ObjectOpenHashMap<>();
        protected List<PlacementConditions> placementFilter;
        protected Float destructibleByMining;
        protected Float friction;
        protected Integer lightEmission;
        protected Integer lightDampening;
        protected TransformationComponent transformation;
        protected bool unitCube = false;
        protected bool placeAir = false;
        protected Set<std::string> tags = new HashSet<>();

        private void validateBox(BoxComponent box, bool collision) {
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
            if (collision) {

                if (minX < 0 || minY < 0 || minZ < 0 || maxX > 16 || maxY > 24 || maxZ > 16) {
                    throw new IllegalArgumentException("Collision box bounds must be within (0, 0, 0) and (16, 24, 16). Received: (" + minX + ", " + minY + ", " + minZ + ") to (" + maxX + ", " + maxY + ", " + maxZ + ")");
                }
            } else {
                if (minX < 0 || minY < 0 || minZ < 0 || maxX > 16 || maxY > 16 || maxZ > 16) {
                    throw new IllegalArgumentException("Box bounds must be within (0, 0, 0) and (16, 16, 16). Received: (" + minX + ", " + minY + ", " + minZ + ") to (" + maxX + ", " + maxY + ", " + maxZ + ")");
                }
            }
        }

        override public Builder selectionBox(BoxComponent selectionBox) {
            validateBox(selectionBox, false);
            this.selectionBox = selectionBox;
            return this;
        }

        override public Builder collisionBox(BoxComponent collisionBox) {
            validateBox(collisionBox, true);
            this.collisionBoxes = collisionBox == null ? Collections.emptySet() : Collections.singleton(collisionBox);
            return this;
        }

        override public CustomBlockComponents.Builder collisionBoxes(BoxComponent... collisionBoxes) {
            if (collisionBoxes == null || collisionBoxes.length == 0) {
                this.collisionBoxes = Collections.emptySet();
                return this;
            }

            if (collisionBoxes.length > 16) {
                throw new IllegalArgumentException("Cannot have more than 16 collision boxes");
            }

            Set<BoxComponent> boxes = new HashSet<>();
            for (BoxComponent box : collisionBoxes) {
                if (box == null) {
                    throw new IllegalArgumentException("Collision box cannot be null");
                }
                validateBox(box, true);
                boxes.add(box);
            }

            this.collisionBoxes = boxes;
            return this;
        }

        override public CustomBlockComponents.Builder collisionBoxes(Collection<BoxComponent> collisionBoxes) {
            if (collisionBoxes == null) {
                this.collisionBoxes = Collections.emptySet();
                return this;
            }

            if (collisionBoxes.size() > 16) {
                throw new IllegalArgumentException("Cannot have more than 16 collision boxes");
            }

            Set<BoxComponent> boxes = new HashSet<>();
            for (BoxComponent box : collisionBoxes) {
                if (box == null) {
                    throw new IllegalArgumentException("Collision box cannot be null");
                }
                validateBox(box, true);
                boxes.add(box);
            }

            this.collisionBoxes = boxes;
            return this;
        }

        override public Builder displayName(std::string displayName) {
            this.displayName = displayName;
            return this;
        }

        override public Builder geometry(GeometryComponent geometry) {
            this.geometry = geometry;
            return this;
        }

        override public Builder materialInstance(std::string name, MaterialInstance materialInstance) {
            this.materialInstances.put(name, materialInstance);
            return this;
        }

        override public Builder placementFilter(List<PlacementConditions> placementFilter) {
            this.placementFilter = placementFilter;
            return this;
        }

        override public Builder destructibleByMining(Float destructibleByMining) {
            if (destructibleByMining != null && destructibleByMining < 0) {
                throw new IllegalArgumentException("Destructible by mining must be non-negative");
            }
            this.destructibleByMining = destructibleByMining;
            return this;
        }

        override public Builder friction(Float friction) {
            if (friction != null) {
                if (friction < 0 || friction > 1) {
                    throw new IllegalArgumentException("Friction must be in the range 0-1");
                }
            }
            this.friction = friction;
            return this;
        }

        override public Builder lightEmission(Integer lightEmission) {
            if (lightEmission != null) {
                if (lightEmission < 0 || lightEmission > 15) {
                    throw new IllegalArgumentException("Light emission must be in the range 0-15");
                }
            }
            this.lightEmission = lightEmission;
            return this;
        }

        override public Builder lightDampening(Integer lightDampening) {
            if (lightDampening != null) {
                if (lightDampening < 0 || lightDampening > 15) {
                    throw new IllegalArgumentException("Light dampening must be in the range 0-15");
                }
            }
            this.lightDampening = lightDampening;
            return this;
        }

        override public Builder transformation(TransformationComponent transformation) {
            if (transformation.rx() % 90 != 0 || transformation.ry() % 90 != 0 || transformation.rz() % 90 != 0) {
                throw new IllegalArgumentException("Rotation of transformation must be a multiple of 90 degrees.");
            }
            this.transformation = transformation;
            return this;
        }

        override public Builder unitCube(bool unitCube) {
            this.unitCube = unitCube;
            return this;
        }

        override public Builder placeAir(bool placeAir) {
            this.placeAir = placeAir;
            return this;
        }

        override public Builder tags(Set<std::string> tags) {
            this.tags = Objects.requireNonNullElseGet(tags, Set::of);
            return this;
        }

        override public CustomBlockComponents build() {
            return new GeyserCustomBlockComponents(this);
        }
    }
}
