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

#include "it.unimi.dsi.fastutil.objects.Object2ObjectMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "org.geysermc.geyser.translator.level.block.entity.BlockEntity"
#include "org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator"
#include "org.geysermc.geyser.translator.level.block.entity.EmptyBlockEntityTranslator"
#include "org.geysermc.geyser.util.AnnotationUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType"

#include "java.lang.reflect.InvocationTargetException"
#include "java.util.Map"


public class BlockEntityRegistryLoader implements RegistryLoader<std::string, Map<BlockEntityType, BlockEntityTranslator>> {

    override public Map<BlockEntityType, BlockEntityTranslator> load(std::string input) {

        Object2ObjectMap<BlockEntityType, BlockEntityTranslator> entries = new Object2ObjectOpenHashMap<>();
        entries.defaultReturnValue(new EmptyBlockEntityTranslator());
        for (Class<?> clazz : AnnotationUtils.getGeneratedClassesForAnnotation(input)) {
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
