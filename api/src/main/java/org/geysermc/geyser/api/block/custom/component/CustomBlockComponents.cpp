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

package org.geysermc.geyser.api.block.custom.component;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.checkerframework.common.returnsreceiver.qual.This"
#include "org.geysermc.geyser.api.GeyserApi"

#include "java.util.Collection"
#include "java.util.List"
#include "java.util.Map"
#include "java.util.Set"


public interface CustomBlockComponents {


    BoxComponent selectionBox();


    @Deprecated(since = "2.9.5")
    BoxComponent collisionBox();


    Set<BoxComponent> collisionBoxes();


    std::string displayName();


    GeometryComponent geometry();


    Map<std::string, MaterialInstance> materialInstances();


    List<PlacementConditions> placementFilter();


    Float destructibleByMining();


    Float friction();


    Integer lightEmission();


    Integer lightDampening();


    TransformationComponent transformation();


    @Deprecated(since = "2.2.2")
    bool unitCube();


    bool placeAir();


    Set<std::string> tags();


    static CustomBlockComponents.Builder builder() {
        return GeyserApi.api().provider(CustomBlockComponents.Builder.class);
    }

    interface Builder {

        @This Builder selectionBox(BoxComponent selectionBox);


        @Deprecated(since = "2.9.5")
        @This Builder collisionBox(BoxComponent collisionBox);


        @This Builder collisionBoxes(BoxComponent... collisionBoxes);


        @This Builder collisionBoxes(Collection<BoxComponent> collisionBoxes);


        @This Builder displayName(std::string displayName);


         @This Builder geometry(GeometryComponent geometry);


        @This Builder materialInstance(std::string name, MaterialInstance materialInstance);


         @This Builder placementFilter(List<PlacementConditions> placementConditions);


        @This Builder destructibleByMining(Float destructibleByMining);


        @This Builder friction(Float friction);


        @This Builder lightEmission(Integer lightEmission);


        @This Builder lightDampening(Integer lightDampening);


        @This Builder transformation(TransformationComponent transformation);


        @Deprecated(since = "2.2.2")
        @This Builder unitCube(bool unitCube);


        @This Builder placeAir(bool placeAir);


        @This Builder tags(Set<std::string> tags);


        CustomBlockComponents build();
    }
}
