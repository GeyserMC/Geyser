/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.geyser.extension;

import javassist.ClassPool;
import javassist.CtClass;
import lombok.Getter;
import org.geysermc.geyser.extension.annotations.Extension;
import org.geysermc.geyser.extension.relocator.JavaRelocator;
import org.geysermc.geyser.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * ClassLoader for Extensions
 *
 * If a extension is marked as shared then its libraries will be available to other extensions.
 */
public class ExtensionClassLoader extends URLClassLoader {
    private final ExtensionManager extensionManager;
    private final JarFile jar;
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();

    @Getter
    private final Class<? extends GeyserExtension> extensionClass;

    private final JavaRelocator relocator;

    ExtensionClassLoader(ExtensionManager extensionManager, ClassLoader parent, File extensionFile) throws IOException, InvalidExtensionClassLoaderException {
        super(new URL[] {extensionFile.toURI().toURL()}, parent);

        JavaRelocator relocator;
        try {
            relocator = new JavaRelocator(FileUtils.getResource("relocations.json"));
        } catch (AssertionError e) {
            relocator = new JavaRelocator();
        }
        this.relocator = relocator;

        this.jar = new JarFile(extensionFile);
        this.extensionManager = extensionManager;
        this.extensionClass = findExtension();

        if (this.extensionClass == null) {
            throw new InvalidExtensionClassLoaderException("Unable to find class annotated by @Extension");
        }
    }

    /**
     * Find first class annotated by @Extension
     */
    @SuppressWarnings("unchecked")
    private Class<? extends GeyserExtension> findExtension() {
        for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements();) {
            JarEntry entry = entries.nextElement();
            if (!entry.getName().endsWith(".class")) {
                continue;
            }
            try (InputStream is = jar.getInputStream(entry)) {
                ClassPool cp = ClassPool.getDefault();
                CtClass ct = cp.makeClass(is);

                if (ct.getAnnotation(Extension.class) != null) {
                    Class<?> cls = loadFromJar(entry);
                    cacheClass(cls, true);
                    return (Class<? extends GeyserExtension>) cls;
                }
            } catch (IOException | ClassNotFoundException ignored) {
            }
        }
        return null;
    }

    /**
     * Cache class locally and if required, globally as well
     */
    private void cacheClass(Class<?> cls, boolean global) {
        classes.put(cls.getName(), cls);

        if (global) {
            extensionManager.getGlobalClasses().put(cls.getName(), cls);
        }
    }

    /**
     * Load a classfile from the jar
     */
    private Class<?> loadFromJar(String classPath) throws ClassNotFoundException {
        JarEntry entry = jar.getJarEntry(classPath);

        if (entry == null) {
            throw new ClassNotFoundException(classPath);
        }

        return loadFromJar(entry);
    }

    /**
     * Load a classfile from the jar
     */
    private Class<?> loadFromJar(JarEntry entry) throws ClassNotFoundException {
        byte[] classBytes;

        try {
            try (InputStream is = jar.getInputStream(entry)) {
                classBytes = relocator.load(entry.getName(), is);
//                classBytes = ByteStreams.toByteArray(is);
            }
        } catch (IOException e) {
            throw new ClassNotFoundException(entry.getName(), e);
        }

        String packageName = getPackageName(entry.getName());
        if (packageName != null && getPackageName(packageName) == null) {
            definePackage(packageName, null, null, null, null, null, null, null);
        }

        return defineClass(
                entry.getName().replace('/', '.').substring(0,entry.getName().length()-6),
                classBytes,
                0,
                classBytes.length
        );
    }

    private String getPackageName(String className) {
        int i = className.lastIndexOf('.');
        if (i != -1) {
            return className.substring(0, i);
        }
        return null;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // First try load it from our cache
        if (classes.containsKey(name)) {
            return classes.get(name);
        }

        boolean global = extensionClass != null && extensionClass.getAnnotation(Extension.class).global();

        // Now try load from global if permitted
        if (global) {
            Class<?> cls = extensionManager.getGlobalClasses().get(name);
            if (cls != null) {
                return cls;
            }
        }

        // Try load from our jar
        try {
            String classPath = name.replace('.','/').concat(".class");
            Class<?> cls = loadFromJar(classPath);
            cacheClass(cls, global);
            return cls;
        } catch (ClassNotFoundException ignored) {
        }

        // Try load from parent
        try {
            Class<?> cls = getParent().loadClass(name);
            resolveClass(cls);
            cacheClass(cls, global);
            return cls;
        } catch (ClassNotFoundException ignored) {
        }

        throw new ClassNotFoundException(name);
    }

    @Override
    public URL getResource(String name) {
        return findResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return findResources(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return super.getResourceAsStream(name);
    }

    public static class InvalidExtensionClassLoaderException extends Exception {
        InvalidExtensionClassLoaderException(String msg) {
            super(msg);
        }
    }
}
