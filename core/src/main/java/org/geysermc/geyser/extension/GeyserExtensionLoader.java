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

import org.geysermc.api.Geyser;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.extension.ExtensionLoader;
import org.geysermc.geyser.api.extension.GeyserExtension;
import org.geysermc.geyser.api.extension.exception.InvalidDescriptionException;
import org.geysermc.geyser.api.extension.exception.InvalidExtensionException;
import org.geysermc.geyser.text.GeyserLocale;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

public class GeyserExtensionLoader implements ExtensionLoader {
    private final Map<String, Class> classes = new HashMap<>();
    private final Map<String, GeyserExtensionClassLoader> classLoaders = new HashMap<>();

    @Override
    public GeyserExtension loadExtension(File file) throws InvalidExtensionException {
        if (file == null) {
            throw new InvalidExtensionException("File is null");
        }

        if (!file.exists()) {
            throw new InvalidExtensionException(new FileNotFoundException(file.getPath()) + " does not exist");
        }

        final GeyserExtensionDescription description;
        try {
            description = extensionDescription(file);
        } catch (InvalidDescriptionException e) {
            throw new InvalidExtensionException(e);
        }

        final File parentFile = file.getParentFile();
        final File dataFolder = new File(parentFile, description.name());
        if (dataFolder.exists() && !dataFolder.isDirectory()) {
            throw new InvalidExtensionException("The folder " + dataFolder.getPath() + " is not a directory and is the data folder for the extension " + description.name() + "!");
        }

        final GeyserExtensionClassLoader loader;
        try {
            loader = new GeyserExtensionClassLoader(this, getClass().getClassLoader(), description, file);
        } catch (Throwable e) {
            throw new InvalidExtensionException(e);
        }
        classLoaders.put(description.name(), loader);

        setup(loader.extension, description, dataFolder, file);
        return loader.extension;
    }

    private void setup(GeyserExtension extension, GeyserExtensionDescription description, File dataFolder, File file) {
        GeyserExtensionLogger logger = new GeyserExtensionLogger(GeyserImpl.getInstance().getLogger(), description.name());
        extension.init(Geyser.api(), this, logger, description, dataFolder, file);
        extension.onLoad();
    }

    @Override
    public GeyserExtensionDescription extensionDescription(File file) throws InvalidDescriptionException {
        JarFile jarFile = null;
        InputStream stream = null;

        try {
            jarFile = new JarFile(file);

            JarEntry descriptionEntry = jarFile.getJarEntry("extension.yml");
            if (descriptionEntry == null) {
                throw new InvalidDescriptionException(new FileNotFoundException("extension.yml") + " does not exist in the jar file!");
            }

            stream = jarFile.getInputStream(descriptionEntry);
            return new GeyserExtensionDescription(stream);
        } catch (IOException e) {
            throw new InvalidDescriptionException(e);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public Pattern[] extensionFilters() {
        return new Pattern[] { Pattern.compile("^.+\\.jar$") };
    }

    @Override
    public Class<?> classByName(final String name) throws ClassNotFoundException{
        Class<?> clazz = classes.get(name);
        try {
            for(GeyserExtensionClassLoader loader : classLoaders.values()) {
                try {
                    clazz = loader.findClass(name,false);
                } catch(NullPointerException e) {
                }
            }
            return clazz;
        } catch(NullPointerException s) {
            return null;
        }
    }

    void setClass(String name, final Class<?> clazz) {
        if (!classes.containsKey(name)) {
            classes.put(name,clazz);
        }
    }

    void removeClass(String name) {
        classes.remove(name);
    }

    @Override
    public void enableExtension(GeyserExtension extension) {
        if (!extension.isEnabled()) {
            GeyserImpl.getInstance().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.extensions.enable.success", extension.description().name()));
            extension.setEnabled(true);
        }
    }

    @Override
    public void disableExtension(GeyserExtension extension) {
        if (extension.isEnabled()) {
            GeyserImpl.getInstance().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.extensions.disable.success", extension.description().name()));
            extension.setEnabled(false);
        }
    }
}
