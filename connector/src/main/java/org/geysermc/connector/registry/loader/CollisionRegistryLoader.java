/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.registry.loader;

import com.fasterxml.jackson.databind.node.ArrayNode;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.AllArgsConstructor;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.translators.collision.BoundingBox;
import org.geysermc.connector.network.translators.collision.CollisionRemapper;
import org.geysermc.connector.network.translators.collision.translators.BlockCollision;
import org.geysermc.connector.network.translators.collision.translators.EmptyCollision;
import org.geysermc.connector.network.translators.collision.translators.OtherCollision;
import org.geysermc.connector.network.translators.collision.translators.SolidCollision;
import org.geysermc.connector.registry.BlockRegistries;
import org.geysermc.connector.utils.FileUtils;
import org.geysermc.connector.utils.Object2IntBiMap;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class CollisionRegistryLoader extends MultiResourceRegistryLoader<String, Map<Integer, BlockCollision>> {

    @Override
    public Map<Integer, BlockCollision> load(Pair<String, String> input) {
        Int2ObjectMap<BlockCollision> collisions = new Int2ObjectOpenHashMap<>();

        Map<Class<?>, CollisionInfo> annotationMap = new HashMap<>();
        for (Class<?> clazz : FileUtils.getGeneratedClassesForAnnotation(CollisionRemapper.class.getName())) {
            GeyserConnector.getInstance().getLogger().debug("Found annotated collision translator: " + clazz.getCanonicalName());

            CollisionRemapper collisionRemapper = clazz.getAnnotation(CollisionRemapper.class);
            annotationMap.put(clazz, new CollisionInfo(collisionRemapper, Pattern.compile(collisionRemapper.regex()), Pattern.compile(collisionRemapper.paramRegex())));
        }

        // Load collision mappings file
        InputStream stream = FileUtils.getResource(input.value());

        ArrayNode collisionList;
        try {
            collisionList = (ArrayNode) GeyserConnector.JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load collision data", e);
        }

        Object2IntBiMap<String> javaIdBlockMap = BlockRegistries.JAVA_IDENTIFIERS.get();

        // Map of classes that don't change based on parameters that have already been created
        Map<Class<?>, BlockCollision> instantiatedCollision = new HashMap<>();
        for (Object2IntMap.Entry<String> entry : javaIdBlockMap.object2IntEntrySet()) {
            BlockCollision newCollision = instantiateCollision(entry.getKey(), entry.getIntValue(), annotationMap, instantiatedCollision, collisionList);
            if (newCollision != null) {
                instantiatedCollision.put(newCollision.getClass(), newCollision);
            }
            collisions.put(entry.getIntValue(), newCollision);
        }
        return collisions;
    }

    private BlockCollision instantiateCollision(String blockID, int numericBlockID, Map<Class<?>, CollisionInfo> annotationMap, Map<Class<?>, BlockCollision> instantiatedCollision, ArrayNode collisionList) {
        String[] blockIdParts = blockID.split("\\[");
        String blockName = blockIdParts[0].replace("minecraft:", "");
        String params = "";
        if (blockID.contains("[")) {
            params = "[" + blockIdParts[1];
        }
        int collisionIndex = BlockRegistries.JAVA_BLOCKS.get(numericBlockID).getCollisionIndex();

        for (Map.Entry<Class<?>, CollisionInfo> collisionRemappers : annotationMap.entrySet()) {
            Class<?> type = collisionRemappers.getKey();
            CollisionInfo collisionInfo = collisionRemappers.getValue();
            CollisionRemapper annotation = collisionInfo.collisionRemapper;

            if (collisionInfo.pattern.matcher(blockName).find() && collisionInfo.paramsPattern.matcher(params).find()) {
                try {
                    if (!annotation.usesParams() && instantiatedCollision.containsKey(type)) {
                        return instantiatedCollision.get(type);
                    }

                    // Return null when empty to save unnecessary checks
                    if (type == EmptyCollision.class) {
                        return null;
                    }

                    BlockCollision collision;
                    if (annotation.passDefaultBoxes()) {
                        // Create an OtherCollision instance and get the bounding boxes
                        BoundingBox[] defaultBoxes = new OtherCollision((ArrayNode) collisionList.get(collisionIndex)).getBoundingBoxes();
                        collision = (BlockCollision) type.getDeclaredConstructor(String.class, BoundingBox[].class).newInstance(params, defaultBoxes);
                    } else {
                        collision = (BlockCollision) type.getDeclaredConstructor(String.class).newInstance(params);
                    }

                    // If there's an existing instance equal to this one, use that instead
                    for (Map.Entry<Class<?>, BlockCollision> entry : instantiatedCollision.entrySet()) {
                        if (entry.getValue().equals(collision)) {
                            collision = entry.getValue();
                            break;
                        }
                    }
                    return collision;
                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        // Unless some of the low IDs are changed, which is unlikely, the first item should always be empty collision
        if (collisionIndex == 0) {
            if (instantiatedCollision.containsKey(EmptyCollision.class)) {
                return instantiatedCollision.get(EmptyCollision.class);
            } else {
                return new EmptyCollision(params);
            }
        }

        // Unless some of the low IDs are changed, which is unlikely, the second item should always be full collision
        if (collisionIndex == 1) {
            if (instantiatedCollision.containsKey(SolidCollision.class)) {
                return instantiatedCollision.get(SolidCollision.class);
            } else {
                return new SolidCollision(params);
            }
        }

        BlockCollision collision = new OtherCollision((ArrayNode) collisionList.get(collisionIndex));
        // If there's an existing instance equal to this one, use that instead
        for (Map.Entry<Class<?>, BlockCollision> entry : instantiatedCollision.entrySet()) {
            if (entry.getValue().equals(collision)) {
                collision = entry.getValue();
                break;
            }
        }

        return collision;
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