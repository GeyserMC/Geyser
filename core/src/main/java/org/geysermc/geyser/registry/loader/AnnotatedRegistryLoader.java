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

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.geyser.util.FileUtils;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.function.Function;

/**
 * A mapped registry loader which takes in a {@link String} and returns a transformed
 * {@link Annotation} as the value. The {@link R} represents the final result as mapped
 * by the {@link A}, the annotation. This function exists in this registry loader for
 * the purpose of annotations not often being used as a map key. The {@link V} generic
 * represents the actual map value of what is expected. The function transformation done
 * is used for transforming the key, however the value is not expected to be transformed.
 *
 * <p>
 * Keep in mind that this annotation transforming does NOT need to be done, and can be
 * replaced with a simple <code>Function.identity()</code> if not desired.
 *
 * <p>
 * See {@link BlockEntityRegistryLoader} and {@link SoundTranslatorRegistryLoader} as a
 * good example of these registry loaders in use.
 *
 * @param <R> the final result as transformed by the function
 * @param <A> the raw annotation itself can be transformed
 * @param <V> the value
 */
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
        for (Class<?> clazz : FileUtils.getGeneratedClassesForAnnotation(input)) {
            try {
                entries.put(this.mapper.apply(clazz.getAnnotation(this.annotation)), (V) clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
        return entries;
    }
}