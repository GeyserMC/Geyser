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

package org.geysermc.geyser.registry.loader;

import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityType;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.geyser.translator.level.block.entity.BlockEntity;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.geyser.translator.level.block.entity.EmptyBlockEntityTranslator;
import org.geysermc.geyser.util.FileUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Loads block entities from the given classpath.
 */
public class BlockEntityRegistryLoader implements RegistryLoader<String, Map<BlockEntityType, BlockEntityTranslator>> {

    @Override
    public Map<BlockEntityType, BlockEntityTranslator> load(String input) {
        // Overridden so one translator can be applied to multiple block entity types
        Object2ObjectMap<BlockEntityType, BlockEntityTranslator> entries = new Object2ObjectOpenHashMap<>();
        entries.defaultReturnValue(new EmptyBlockEntityTranslator());
        for (Class<?> clazz : FileUtils.getGeneratedClassesForAnnotation(input)) {
            try {
                BlockEntity annotation = clazz.getAnnotation(BlockEntity.class);
                BlockEntityTranslator translator = (BlockEntityTranslator) clazz.getConstructor().newInstance();
                for (BlockEntityType type : annotation.type()) {
                    entries.put(type, translator);
                }
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
        }
        return entries;
    }
}