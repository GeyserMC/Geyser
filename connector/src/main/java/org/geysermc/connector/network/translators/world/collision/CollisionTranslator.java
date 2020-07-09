/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.world.collision;

import com.google.common.collect.BiMap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.network.translators.world.collision.translators.BlockCollision;
import org.geysermc.connector.network.translators.world.collision.translators.EmptyCollision;
import org.geysermc.connector.network.translators.world.collision.translators.SolidCollision;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Pattern;

public class CollisionTranslator {
    private static Map<Integer, BlockCollision> collisionMap = new HashMap<>();

    public static void init() {
        // If chunk caching is off then don't initialize
        if (!GeyserConnector.getInstance().getConfig().isCacheChunks()) {
            return;
        }

        List<Class> collisionTypes = new ArrayList<Class>();

        Map<Class, CollisionRemapper> annotationMap = new HashMap<>();
        // Map<Class, Pattern> paramRegexMap = new HashMap<>();

        Reflections ref = new Reflections("org.geysermc.connector.network.translators.world.collision.translators");
        for (Class<?> clazz : ref.getTypesAnnotatedWith(CollisionRemapper.class)) {
            // String regex = clazz.getAnnotation(CollisionRemapper.class).regex();
            // String paramRegex = clazz.getAnnotation(CollisionRemapper.class).paramRegex();

            GeyserConnector.getInstance().getLogger().debug("Found annotated collision translator: " + clazz.getCanonicalName());

            collisionTypes.add(clazz);
            // regexMap.put(clazz, Pattern.compile(regex));
            // paramRegexMap.put(clazz, Pattern.compile(paramRegex));
            annotationMap.put(clazz, clazz.getAnnotation(CollisionRemapper.class));
        }

        System.out.println(collisionTypes);

        BiMap<String, Integer> javaIdBlockMap = BlockTranslator.getJavaIdBlockMap();
        // Map of classes that don't change based on parameters that have already been created
        // BiMap<Class, BlockCollision> instantiatedCollision = HashBiMap.create();
        Map<Class, BlockCollision> instantiatedCollision = new HashMap<>();
        for (Map.Entry<String, Integer> entry : javaIdBlockMap.entrySet()) {
            BlockCollision newCollision = instantiateCollision(entry.getKey(), collisionTypes, annotationMap, instantiatedCollision);
            if (newCollision != null) {
                instantiatedCollision.put(newCollision.getClass(), newCollision);
            }
            collisionMap.put(entry.getValue(), newCollision);
        }
    }

    private static BlockCollision instantiateCollision(String blockID, List<Class> collisionTypes, Map<Class, CollisionRemapper> annotationMap, Map<Class, BlockCollision> instantiatedCollision) {

        String blockName = blockID.split("\\[")[0].replace("minecraft:", "");
        String params = "";
        if (blockID.contains("[")) {
            params = "[" + blockID.split("\\[")[1];
        }

        Iterator i = collisionTypes.iterator();
        while (i.hasNext()) {
            Class collisionType = (Class) i.next();
            CollisionRemapper annotation = annotationMap.get(collisionType);

            Pattern pattern = Pattern.compile(annotation.regex());
            Pattern paramsPattern = Pattern.compile(annotation.paramRegex());

            if (pattern.matcher(blockName).find() && paramsPattern.matcher(params).find()) {
                try {
                    // System.out.println("************** TYPE: " + collisionType + " BLOCK: " + blockID + " **************");

                    /* if (instantiatedCollision.keySet().size() > 1) {
                        System.out.println("Two!");
                        System.out.println(instantiatedCollision.keySet().toArray()[1]);
                        System.out.println(collisionType);
                        System.out.println(!annotation.usesParams());
                        System.out.println(instantiatedCollision.containsKey(collisionType));
                    } */
                    if (!annotation.usesParams() && instantiatedCollision.containsKey(collisionType)) {
                        // System.out.println("Early return (found): " + collisionType);
                        return instantiatedCollision.get(collisionType);
                    }
                    // Return null when empty to save unnecessary checks
                    if (collisionType == EmptyCollision.class) {
                        // System.out.println("null!!!");
                        return null;
                    }
                    BlockCollision collision = (BlockCollision) collisionType.getDeclaredConstructor(String.class).newInstance(params);
                    // If there's an existing instance equal to this one, use that instead
                    // if (instantiatedCollision.containsValue(collision)) {
                        // System.out.println("Can delete " + collision);
                        for (Map.Entry<Class, BlockCollision> entry : instantiatedCollision.entrySet()) {
                            if (entry.getValue().equals(collision)) {
                                // System.out.println("Deleting " + collision);
                                collision = entry.getValue();
                                break;
                            }
                        }
                    // }
                    return collision;
                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        if (instantiatedCollision.containsKey(SolidCollision.class)) {
            return instantiatedCollision.get(SolidCollision.class);
        } else {
            return new SolidCollision(params);
        }
    }

    public static BlockCollision getCollision(Integer blockID, int x, int y, int z) {
        BlockCollision collision = collisionMap.get(blockID);
        if (collision != null) {
            collision.setPosition(x, y, z);
        }
        return collision;
    }
    public static BlockCollision getCollisionAt(int x, int y, int z, GeyserSession session) {
        try {
            return getCollision(
                    session.getConnector().getWorldManager().getBlockAt(session, x, y, z),
                    x, y, z
            );
        } catch (ArrayIndexOutOfBoundsException e) {
            // Block out of world
            return null;
        }
    }

}