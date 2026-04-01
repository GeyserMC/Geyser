/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.loader;

#include "it.unimi.dsi.fastutil.objects.ObjectArrayList"
#include "org.cloudburstmc.nbt.NbtList"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.cloudburstmc.nbt.NbtUtils"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.level.physics.BoundingBox"
#include "org.geysermc.geyser.registry.BlockRegistries"

#include "java.io.InputStream"
#include "java.util.Arrays"
#include "java.util.Comparator"
#include "java.util.List"

@SuppressWarnings("rawtypes")
public class BlockShapeRegistryLoader implements RegistryLoader<std::string, List<BoundingBox[]>> {

    override public List<BoundingBox[]> load(std::string input) {

        int[] indices;
        List<BoundingBox[]> unmappedCollisionList;
        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow(input)) {
            NbtMap collisionData = (NbtMap) NbtUtils.createGZIPReader(stream).readTag();
            indices = collisionData.getIntArray("indices");

            unmappedCollisionList = loadBoundingBoxes(collisionData.getList("shapes", NbtType.LIST));
        } catch (Exception e) {
            throw new AssertionError("Unable to load collision data", e);
        }

        List<BlockState> blockStates = BlockRegistries.BLOCK_STATES.get();
        ObjectArrayList<BoundingBox[]> collisions = new ObjectArrayList<>(blockStates.size());

        for (int i = 0; i < blockStates.size(); i++) {
            BlockState state = blockStates.get(i);
            if (state == null) {
                GeyserImpl.getInstance().getLogger().warning("Missing block state for Java block " + i);
                collisions.add(null);
                continue;
            }
            collisions.add(unmappedCollisionList.get(indices[i]));
        }

        collisions.trim();
        return collisions;
    }

    private List<BoundingBox[]> loadBoundingBoxes(List<NbtList> collisionNode) {
        List<BoundingBox[]> collisions = new ObjectArrayList<>();
        for (NbtList nbtList : collisionNode) {
            @SuppressWarnings("unchecked") NbtList<NbtList<Double>> boundingBoxArray = (NbtList<NbtList<Double>>) nbtList;

            BoundingBox[] boundingBoxes = new BoundingBox[boundingBoxArray.size()];
            for (int i = 0; i < boundingBoxArray.size(); i++) {
                NbtList<Double> boxProperties = boundingBoxArray.get(i);
                boundingBoxes[i] = new BoundingBox(boxProperties.get(0),
                    boxProperties.get(1),
                    boxProperties.get(2),
                    boxProperties.get(3),
                    boxProperties.get(4),
                    boxProperties.get(5));
            }


            Arrays.sort(boundingBoxes, Comparator.comparingDouble(BoundingBox::getMiddleY));
            collisions.add(boundingBoxes);
        }
        return collisions;
    }
}
