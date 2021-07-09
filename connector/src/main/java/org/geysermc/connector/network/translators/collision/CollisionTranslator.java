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

package org.geysermc.connector.network.translators.collision;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.BiMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.collision.translators.BlockCollision;
import org.geysermc.connector.network.translators.collision.translators.EmptyCollision;
import org.geysermc.connector.network.translators.collision.translators.OtherCollision;
import org.geysermc.connector.network.translators.collision.translators.SolidCollision;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.utils.FileUtils;
import org.reflections.Reflections;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Pattern;

public class CollisionTranslator {
    private static final Int2ObjectMap<BlockCollision> COLLISION_MAP = new Int2ObjectOpenHashMap<>();

    public static void init() {
        List<Class<?>> collisionTypes = new ArrayList<>();

        Map<Class<?>, CollisionRemapper> annotationMap = new HashMap<>();

        Reflections ref = GeyserConnector.getInstance().useXmlReflections() ? FileUtils.getReflections("org.geysermc.connector.network.translators.collision.translators") : new Reflections("org.geysermc.connector.network.translators.collision.translators");
        for (Class<?> clazz : ref.getTypesAnnotatedWith(CollisionRemapper.class)) {
            GeyserConnector.getInstance().getLogger().debug("Found annotated collision translator: " + clazz.getCanonicalName());

            collisionTypes.add(clazz);
            annotationMap.put(clazz, clazz.getAnnotation(CollisionRemapper.class));
        }

        // Load collision mappings file
        InputStream stream = FileUtils.getResource("mappings/collision.json");

        ArrayNode collisionList;
        try {
            collisionList = (ArrayNode) GeyserConnector.JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load collision data", e);
        }

        BiMap<String, Integer> javaIdBlockMap = BlockTranslator.getJavaIdBlockMap();

        // Map of classes that don't change based on parameters that have already been created
        Map<Class<?>, BlockCollision> instantiatedCollision = new HashMap<>();

        for (Map.Entry<String, Integer> entry : javaIdBlockMap.entrySet()) {
            BlockCollision newCollision = instantiateCollision(entry.getKey(), entry.getValue(), collisionTypes, annotationMap, instantiatedCollision, collisionList);
            if (newCollision != null) {
                instantiatedCollision.put(newCollision.getClass(), newCollision);
            }
            COLLISION_MAP.put(entry.getValue().intValue(), newCollision);
        }
    }

    private static BlockCollision instantiateCollision(String blockID, int numericBlockID, List<Class<?>> collisionTypes, Map<Class<?>, CollisionRemapper> annotationMap, Map<Class<?>, BlockCollision> instantiatedCollision, ArrayNode collisionList) {

        String blockName = blockID.split("\\[")[0].replace("minecraft:", "");
        String params = "";
        if (blockID.contains("[")) {
            params = "[" + blockID.split("\\[")[1];
        }
        int collisionIndex = BlockTranslator.getBlockMapping(numericBlockID).getCollisionIndex();

        for (Class<?> type : collisionTypes) {
            CollisionRemapper annotation = annotationMap.get(type);

            Pattern pattern = Pattern.compile(annotation.regex());
            Pattern paramsPattern = Pattern.compile(annotation.paramRegex());

            if (pattern.matcher(blockName).find() && paramsPattern.matcher(params).find()) {
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

    // Note: these reuse classes, so don't try to store more than once instance or coordinates will get overwritten

    public static BlockCollision getCollision(int blockID, int x, int y, int z) {
        BlockCollision collision = COLLISION_MAP.get(blockID);
        if (collision != null) {
            collision.setPosition(x, y, z);
        }
        return collision;
    }


    public static BlockCollision getCollisionAt(GeyserSession session, int x, int y, int z) {
        try {
            return getCollision(session.getConnector().getWorldManager().getBlockAt(session, x, y, z), x, y, z);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Block out of world
            return null;
        }
    }

}