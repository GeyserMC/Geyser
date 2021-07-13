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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.utils.FileUtils;
import org.reflections.Reflections;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.function.Function;

public class AnnotatedRegistryLoader<R, A extends Annotation, V> implements RegistryLoader<String, Map<R, V>> {
    private final Class<A> annotation;
    private final Function<A, R> mapper;

    public AnnotatedRegistryLoader(Class<A> annotation, Function<A, R> mapper) {
        this.annotation = annotation;
        this.mapper = mapper;
    }

    @Override
    public Map<R, V> load(String input) {
        Map<R, V> entries = new Object2ObjectOpenHashMap<>();
        Reflections ref = GeyserConnector.getInstance().useXmlReflections() ? FileUtils.getReflections(input) : new Reflections(input);
        for (Class<?> clazz : ref.getTypesAnnotatedWith(this.annotation)) {
            try {
                entries.put(this.mapper.apply(clazz.getAnnotation(this.annotation)), (V) clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
        return entries;
    }
}