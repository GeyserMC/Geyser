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

import org.geysermc.geyser.extension.exception.InvalidExtensionException;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ExtensionClassLoader extends URLClassLoader {
    private ExtensionLoader loader;
    private Map<String, Class> classes = new HashMap<>();
    public GeyserExtension extension;

    public ExtensionClassLoader(ExtensionLoader loader, ClassLoader parent, ExtensionDescription description, File file) throws InvalidExtensionException, MalformedURLException {
        super(new URL[] { file.toURI().toURL() }, parent);
        this.loader = loader;

        try {
            Class<?> jarClass;
            try {
                jarClass = Class.forName(description.getMain(), true, this);
            } catch (ClassNotFoundException ex) {
                throw new InvalidExtensionException("Class " + description.getMain() + " not found, extension cannot be loaded", ex);
            }

            Class<? extends GeyserExtension> extensionClass;
            try {
                extensionClass = jarClass.asSubclass(GeyserExtension.class);
            } catch (ClassCastException ex) {
                throw new InvalidExtensionException("Main class " + description.getMain() + " should extends GeyserExtension, but extends " + jarClass.getSuperclass().getSimpleName(), ex);
            }

            extension = extensionClass.newInstance();
        } catch (IllegalAccessException ex) {
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
        if (name.startsWith("org.geysermc.geyser.") || name.startsWith("org.geysermc.connector.") || name.startsWith("net.minecraft.")) {
            throw new ClassNotFoundException(name);
        }
        Class<?> result = classes.get(name);
        if(result == null) {
            if(checkGlobal) {
                result = loader.getClassByName(name);
            }
            if(result == null) {
                result = super.findClass(name);
                if (result != null) {
                    loader.setClass(name, result);
                }
            }
            classes.put(name, result);
        }
        return result;
    }

    Set<String> getClasses() {
        return classes.keySet();
    }
}
