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
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.extension.ExtensionDescription;
import org.geysermc.geyser.api.extension.exception.InvalidExtensionException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class GeyserExtensionClassLoader extends URLClassLoader {
    /**
     * Class name prefixes that load child first. Geyser ships these packages
     * unrelocated, but as diverged forks of the upstream libraries. Parent
     * first delegation would silently replace the copy an extension bundles
     * with the fork, and the two are not behavior compatible. An extension
     * that bundles these packages gets its own copy. An extension that does
     * not bundle them still falls through to the Geyser copy.
     */
    private static final String[] CHILD_FIRST_CLASS_PREFIXES = {
        "dev.kastle.",
        "io.github.sendablemetatype.netty.",
        "io.github.sendablemetatype.webrtc."
    };

    /**
     * Resource name prefixes that load child first, for the same reason. The
     * webrtc-java native binaries sit at the jar root and are found through a
     * classloader resource lookup, so without this an extension that bundles
     * its own natives would extract the Geyser copy instead.
     */
    private static final String[] CHILD_FIRST_RESOURCE_PREFIXES = {
        "dev/kastle/",
        "io/github/sendablemetatype/netty/",
        "io/github/sendablemetatype/webrtc/",
        "libwebrtc-java-",
        "webrtc-java-"
    };

    private final GeyserExtensionLoader loader;
    private final GeyserExtensionDescription description;
    private final Object2ObjectMap<String, Class<?>> classes = new Object2ObjectOpenHashMap<>();
    private boolean warnedForExternalClassAccess;

    public GeyserExtensionClassLoader(GeyserExtensionLoader loader, ClassLoader parent, Path path, GeyserExtensionDescription description) throws MalformedURLException {
        super(new URL[] { path.toUri().toURL() }, parent);
        this.loader = loader;
        this.description = description;
    }

    public Extension load() throws InvalidExtensionException {
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
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (childFirst(name, CHILD_FIRST_CLASS_PREFIXES)) {
            synchronized (getClassLoadingLock(name)) {
                Class<?> result = findLoadedClass(name);
                if (result == null) {
                    try {
                        // Search only the extension jar, not the parent
                        result = super.findClass(name);
                        // Cache so that a later findClass call for the same
                        // name does not define the class a second time
                        this.classes.put(name, result);
                    } catch (ClassNotFoundException ignored) {
                        // The extension does not bundle this class, so the
                        // normal parent first path below serves it
                    }
                }
                if (result != null) {
                    if (resolve) {
                        resolveClass(result);
                    }
                    return result;
                }
            }
        }
        return super.loadClass(name, resolve);
    }

    @Override
    public URL getResource(String name) {
        if (childFirst(name, CHILD_FIRST_RESOURCE_PREFIXES)) {
            URL url = findResource(name);
            if (url != null) {
                return url;
            }
        }
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (!childFirst(name, CHILD_FIRST_RESOURCE_PREFIXES)) {
            return super.getResources(name);
        }
        // Keep all copies visible but put the extension's own copy first
        List<URL> urls = new ArrayList<>();
        Enumeration<URL> local = findResources(name);
        while (local.hasMoreElements()) {
            urls.add(local.nextElement());
        }
        Enumeration<URL> all = super.getResources(name);
        while (all.hasMoreElements()) {
            URL url = all.nextElement();
            if (!urls.contains(url)) {
                urls.add(url);
            }
        }
        return Collections.enumeration(urls);
    }

    private static boolean childFirst(String name, String[] prefixes) {
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return this.findClass(name, true);
    }

    protected Class<?> findClass(String name, boolean checkGlobal) throws ClassNotFoundException {
        Class<?> result = this.classes.get(name);
        if (result == null) {
            // Try to find class in current extension
            try {
                result = super.findClass(name);
            } catch (ClassNotFoundException ignored) {
                // If class is not found in current extension, check in the global class loader
                // This is used for classes that are not in the extension, but are in other extensions
                if (checkGlobal) {
                    if (!warnedForExternalClassAccess && this.description.dependencies().isEmpty()) { // Don't warn when the extension has dependencies, it is probably using it's dependencies!
                        GeyserImpl.getInstance().getLogger().warning("Extension " + this.description.name() + " loads class " + name + " from an external source. " +
                                "This can change at any time and break the extension, additionally to potentially causing unexpected behaviour!");
                        warnedForExternalClassAccess = true;
                    }
                    result = this.loader.classByName(name);
                }
            }

            if (result != null) {
                // If class is found, cache it
                this.loader.setClass(name, result);
                this.classes.put(name, result);
            } else {
                // If class is not found, throw exception
                throw new ClassNotFoundException(name);
            }
        }
        return result;
    }
}
