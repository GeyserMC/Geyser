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

package org.geysermc.geyser.registry.loader;

import com.fasterxml.jackson.databind.node.ArrayNode;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.AllArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.BlockMapping;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.translator.collision.CollisionRemapper;
import org.geysermc.geyser.translator.collision.OtherCollision;
import org.geysermc.geyser.translator.collision.SolidCollision;
import org.geysermc.geyser.util.FileUtils;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Loads collision data from the given resource path.
 */
public class CollisionRegistryLoader extends MultiResourceRegistryLoader<String, Int2ObjectMap<BlockCollision>> {

    @Override
    public Int2ObjectMap<BlockCollision> load(Pair<String, String> input) {
        Int2ObjectMap<BlockCollision> collisions = new Int2ObjectOpenHashMap<>();

        Map<Class<?>, CollisionInfo> annotationMap = new IdentityHashMap<>();
        for (Class<?> clazz : FileUtils.getGeneratedClassesForAnnotation(CollisionRemapper.class.getName())) {
            GeyserImpl.getInstance().getLogger().debug("Found annotated collision translator: " + clazz.getCanonicalName());

            CollisionRemapper collisionRemapper = clazz.getAnnotation(CollisionRemapper.class);
            annotationMap.put(clazz, new CollisionInfo(collisionRemapper, Pattern.compile(collisionRemapper.regex()), Pattern.compile(collisionRemapper.paramRegex())));
        }

        // Load collision mappings file
        List<BoundingBox[]> collisionList;
        try (InputStream stream = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow(input.value())) {
            ArrayNode collisionNode = (ArrayNode) GeyserImpl.JSON_MAPPER.readTree(stream);
            collisionList = loadBoundingBoxes(collisionNode);
        } catch (Exception e) {
            throw new AssertionError("Unable to load collision data", e);
        }

        BlockMapping[] blockMappings = BlockRegistries.JAVA_BLOCKS.get();

        // Map of unique collisions to its instance
        Map<BlockCollision, BlockCollision> collisionInstances = new Object2ObjectOpenHashMap<>();
        for (int i = 0; i < blockMappings.length; i++) {
            BlockMapping blockMapping = blockMappings[i];
            if (blockMapping == null) {
                GeyserImpl.getInstance().getLogger().warning("Missing block mapping for Java block " + i);
                continue;
            }

            BlockCollision newCollision = instantiateCollision(blockMapping, annotationMap, collisionList);

            if (newCollision != null) {
                // If there's an existing instance equal to this one, use that instead
                BlockCollision existingInstance = collisionInstances.get(newCollision);
                if (existingInstance != null) {
                    newCollision = existingInstance;
                } else {
                    collisionInstances.put(newCollision, newCollision);
                }
            }

            collisions.put(i, newCollision);
        }
        return collisions;
    }

    private @Nullable BlockCollision instantiateCollision(BlockMapping mapping, Map<Class<?>, CollisionInfo> annotationMap, List<BoundingBox[]> collisionList) {
        String[] blockIdParts = mapping.getJavaIdentifier().split("\\[");
        String blockName = blockIdParts[0].replace("minecraft:", "");
        String params = "";
        if (blockIdParts.length == 2) {
            params = "[" + blockIdParts[1];
        }
        int collisionIndex = mapping.getCollisionIndex();

        for (Map.Entry<Class<?>, CollisionInfo> collisionRemappers : annotationMap.entrySet()) {
            Class<?> type = collisionRemappers.getKey();
            CollisionInfo collisionInfo = collisionRemappers.getValue();
            CollisionRemapper annotation = collisionInfo.collisionRemapper;

            if (collisionInfo.pattern.matcher(blockName).find() && collisionInfo.paramsPattern.matcher(params).find()) {
                try {
                    if (annotation.passDefaultBoxes()) {
                        // Create an OtherCollision instance and get the bounding boxes
                        BoundingBox[] defaultBoxes = collisionList.get(collisionIndex);
                        return (BlockCollision) type.getDeclaredConstructor(String.class, BoundingBox[].class).newInstance(params, defaultBoxes);
                    } else {
                        return (BlockCollision) type.getDeclaredConstructor(String.class).newInstance(params);
                    }
                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // Unless some of the low IDs are changed, which is unlikely, the first item should always be empty collision
        if (collisionIndex == 0) {
            return null;
        }

        // Unless some of the low IDs are changed, which is unlikely, the second item should always be full collision
        if (collisionIndex == 1) {
            return new SolidCollision(params);
        }
        return new OtherCollision(collisionList.get(collisionIndex));
    }

    private List<BoundingBox[]> loadBoundingBoxes(ArrayNode collisionNode) {
        List<BoundingBox[]> collisions = new ObjectArrayList<>();
        for (int collisionIndex = 0; collisionIndex < collisionNode.size(); collisionIndex++) {
            ArrayNode boundingBoxArray = (ArrayNode) collisionNode.get(collisionIndex);

            BoundingBox[] boundingBoxes = new BoundingBox[boundingBoxArray.size()];
            for (int i = 0; i < boundingBoxArray.size(); i++) {
                ArrayNode boxProperties = (ArrayNode) boundingBoxArray.get(i);
                boundingBoxes[i] = new BoundingBox(boxProperties.get(0).asDouble(),
                        boxProperties.get(1).asDouble(),
                        boxProperties.get(2).asDouble(),
                        boxProperties.get(3).asDouble(),
                        boxProperties.get(4).asDouble(),
                        boxProperties.get(5).asDouble());
            }

            // Sorting by lowest Y first fixes some bugs
            Arrays.sort(boundingBoxes, Comparator.comparingDouble(BoundingBox::getMiddleY));
            collisions.add(boundingBoxes);
        }
        return collisions;
    }

    /**
     * Used to prevent patterns from being compiled more than needed
     */
    @AllArgsConstructor
    public static class CollisionInfo {
        private final CollisionRemapper collisionRemapper;
        private final Pattern pattern;
        private final Pattern paramsPattern;
    }
}