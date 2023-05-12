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

package org.geysermc.geyser.extension;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.extension.ExtensionDescription;
import org.geysermc.geyser.api.extension.exception.InvalidExtensionException;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class GeyserExtensionClassLoader extends URLClassLoader {
    private final GeyserExtensionLoader loader;
    private final Object2ObjectMap<String, Class<?>> classes = new Object2ObjectOpenHashMap<>();

    public GeyserExtensionClassLoader(GeyserExtensionLoader loader, ClassLoader parent, Path path) throws MalformedURLException {
        super(new URL[] { path.toUri().toURL() }, parent);
        this.loader = loader;
    }

    public Extension load(ExtensionDescription description) throws InvalidExtensionException {
        try {
            Class<?> jarClass;
            try {
                jarClass = Class.forName(description.main(), true, this);
            } catch (ClassNotFoundException ex) {
                throw new InvalidExtensionException("Class " + description.main() + " not found, extension cannot be loaded", ex);
            }

            Class<? extends Extension> extensionClass;
            try {
                extensionClass = jarClass.asSubclass(Extension.class);
            } catch (ClassCastException ex) {
                throw new InvalidExtensionException("Main class " + description.main() + " should implement Extension, but extends " + jarClass.getSuperclass().getSimpleName(), ex);
            }

            return extensionClass.getConstructor().newInstance();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new InvalidExtensionException("No public constructor", ex);
        } catch (InstantiationException ex) {
            throw new InvalidExtensionException("Abnormal extension type", ex);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return this.findClass(name, true);
    }

    protected Class<?> findClass(String name, boolean checkGlobal) throws ClassNotFoundException {
        if (name.startsWith("org.geysermc.geyser.") || name.startsWith("net.minecraft.")) {
            throw new ClassNotFoundException(name);
        }

        Class<?> result = this.classes.get(name);
        if (result == null) {
            result = super.findClass(name);
            if (result == null && checkGlobal) {
                result = this.loader.classByName(name);
            }

            if (result != null) {
                this.loader.setClass(name, result);
            }

            this.classes.put(name, result);
        }
        return result;
    }
}
