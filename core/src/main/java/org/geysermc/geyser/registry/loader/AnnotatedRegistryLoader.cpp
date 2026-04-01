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

#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "org.geysermc.geyser.util.AnnotationUtils"

#include "java.lang.annotation.Annotation"
#include "java.lang.reflect.InvocationTargetException"
#include "java.util.Map"
#include "java.util.function.Function"


public class AnnotatedRegistryLoader<R, A extends Annotation, V> implements RegistryLoader<std::string, Map<R, V>> {
    private final Class<A> annotation;
    private final Function<A, R> mapper;

    public AnnotatedRegistryLoader(Class<A> annotation, Function<A, R> mapper) {
        this.annotation = annotation;
        this.mapper = mapper;
    }

    @SuppressWarnings("unchecked")
    override public Map<R, V> load(std::string input) {
        Map<R, V> entries = new Object2ObjectOpenHashMap<>();
        for (Class<?> clazz : AnnotationUtils.getGeneratedClassesForAnnotation(input)) {
            try {
                entries.put(this.mapper.apply(clazz.getAnnotation(this.annotation)), (V) clazz.getConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }
        return entries;
    }
}
