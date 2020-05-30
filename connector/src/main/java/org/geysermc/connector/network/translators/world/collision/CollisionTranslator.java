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

import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.google.common.collect.BiMap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.translators.world.collision.translators.*;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Pattern;

public class CollisionTranslator {
    private static Map<BlockState, BlockCollision> collisionMap = new HashMap<>();

    public static void init() {
        // If chunk caching is off then don't initialize
        if (!GeyserConnector.getInstance().getConfig().isCacheChunks()) {
            return;
        }

        List collisionTypes = new ArrayList<Class>();

        Map<Class, Pattern> regexMap = new HashMap<>();
        Map<Class, Pattern> paramRegexMap = new HashMap<>();

        Reflections ref = new Reflections("org.geysermc.connector.network.translators.world.collision.translators");
        for (Class<?> clazz : ref.getTypesAnnotatedWith(CollisionRemapper.class)) {
            String regex = clazz.getAnnotation(CollisionRemapper.class).regex();
            String paramRegex = clazz.getAnnotation(CollisionRemapper.class).paramRegex();

            GeyserConnector.getInstance().getLogger().debug("Found annotated collision translator: " + clazz.getCanonicalName());

            collisionTypes.add(clazz);
            regexMap.put(clazz, Pattern.compile(regex));
            paramRegexMap.put(clazz, Pattern.compile(paramRegex));

            BiMap<String, BlockState> javaIdBlockMap = BlockTranslator.getJavaIdBlockMap();

            for (Map.Entry<String, BlockState> entry : javaIdBlockMap.entrySet()) {
                BlockCollision newCollision = instantiateCollision(entry.getKey(), collisionTypes, regexMap, paramRegexMap);
                collisionMap.put(entry.getValue(), newCollision);
            }
        }
    }

    private static BlockCollision instantiateCollision(String blockID, List collisionTypes, Map<Class, Pattern> regexMap, Map<Class, Pattern> paramRegexMap) {

        String blockName = blockID.split("\\[")[0].replace("minecraft:", "");
        String params = "";
        if (blockID.contains("[")) {
            params = "[" + blockID.split("\\[")[1];
        }

        Iterator i = collisionTypes.iterator();
        while (i.hasNext()) {
            Class collisionType = (Class) i.next();
            if (regexMap.get(collisionType).matcher(blockName).find() &&
                    paramRegexMap.get(collisionType).matcher(params).find()) {
                try {
                    BlockCollision collision = (BlockCollision) collisionType.getDeclaredConstructor(String.class).newInstance(params);
                    // Return null when empty to save unnecessary checks
                    if (collision.getClass().isInstance(EmptyCollision.class)) {
                        return null;
                    }
                    return collision;
                } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        return new SolidCollision(params);
    }

    public static BlockCollision getCollision(BlockState block, int x, int y, int z) {
        BlockCollision collision = collisionMap.get(block);
        collision.setPosition(x, y, z);
        return collision;
    }
}