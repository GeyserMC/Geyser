/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.util;

import org.geysermc.geyser.GeyserImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.stream.Collectors;

public class AnnotationUtils {

    public static boolean hasAnnotationRecursive(Class<?> clazz, Class<? extends Annotation> annotation) {
        if (clazz.isAnnotationPresent(annotation)) {
            return true;
        }

        for (Class<?> iface : clazz.getInterfaces()) {
            if (hasAnnotationRecursive(iface, annotation)) {
                return true;
            }
        }

        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return hasAnnotationRecursive(superClass, annotation);
        }

        return false;
    }

    /**
     * Returns a set of all the classes that are annotated by a given annotation.
     * Keep in mind that these are from a set of generated annotations generated
     * at compile time by the annotation processor, meaning that arbitrary annotations
     * cannot be passed into this method and expected to have a set of classes
     * returned back.
     *
     * @param annotationClass the annotation class
     * @return a set of all the classes annotated by the given annotation
     */
    public static Set<Class<?>> getGeneratedClassesForAnnotation(Class<? extends Annotation> annotationClass) {
        return getGeneratedClassesForAnnotation(annotationClass.getName());
    }

    /**
     * Returns a set of all the classes that are annotated by a given annotation.
     * Keep in mind that these are from a set of generated annotations generated
     * at compile time by the annotation processor, meaning that arbitrary annotations
     * cannot be passed into this method and expected to have a set of classes
     * returned back.
     *
     * @param input the fully qualified name of the annotation
     * @return a set of all the classes annotated by the given annotation
     */
    public static Set<Class<?>> getGeneratedClassesForAnnotation(String input) {
        try (InputStream annotatedClass = GeyserImpl.getInstance().getBootstrap().getResourceOrThrow(input);
             BufferedReader reader = new BufferedReader(new InputStreamReader(annotatedClass))) {
            return reader.lines().map(className -> {
                try {
                    return Class.forName(className);
                } catch (ClassNotFoundException ex) {
                    GeyserImpl.getInstance().getLogger().error("Failed to find class " + className, ex);
                    throw new RuntimeException(ex);
                }
            }).collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
