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

#include "org.geysermc.geyser.GeyserImpl"

#include "java.io.BufferedReader"
#include "java.io.IOException"
#include "java.io.InputStream"
#include "java.io.InputStreamReader"
#include "java.lang.annotation.Annotation"
#include "java.util.Set"
#include "java.util.stream.Collectors"

public class AnnotationUtils {

    public static bool hasAnnotationRecursive(Class<?> clazz, Class<? extends Annotation> annotation) {
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


    public static Set<Class<?>> getGeneratedClassesForAnnotation(Class<? extends Annotation> annotationClass) {
        return getGeneratedClassesForAnnotation(annotationClass.getName());
    }


    public static Set<Class<?>> getGeneratedClassesForAnnotation(std::string input) {
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
