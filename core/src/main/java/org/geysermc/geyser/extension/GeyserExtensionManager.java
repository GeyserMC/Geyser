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

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.extension.ExtensionDescription;
import org.geysermc.geyser.api.extension.GeyserExtension;
import org.geysermc.geyser.text.GeyserLocale;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.regex.Pattern;

public class GeyserExtensionManager {
    protected Map<String, GeyserExtension> extensions = new LinkedHashMap<>();
    protected Map<Pattern, GeyserExtensionLoader> fileAssociations = new HashMap<>();

    public void init() {
        GeyserImpl.getInstance().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.extensions.load.loading"));

        this.registerInterface(GeyserExtensionLoader.class);
        this.loadExtensions(new File("extensions"));

        GeyserImpl.getInstance().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.extensions.load.done", this.extensions.size()));
    }

    public GeyserExtension getExtension(String name) {
        if (this.extensions.containsKey(name)) {
            return this.extensions.get(name);
        }
        return null;
    }

    public Map<String, GeyserExtension> getExtensions() {
        return this.extensions;
    }

    public void registerInterface(Class<? extends GeyserExtensionLoader> loader) {
        GeyserExtensionLoader instance;

        if (GeyserExtensionLoader.class.isAssignableFrom(loader)) {
            Constructor<? extends GeyserExtensionLoader> constructor;

            try {
                constructor = loader.getConstructor();
                instance = constructor.newInstance();
            } catch (NoSuchMethodException ex) { // This should never happen
                String className = loader.getName();

                throw new IllegalArgumentException("Class " + className + " does not have a public constructor", ex);
            } catch (Exception ex) { // This should never happen
                throw new IllegalArgumentException("Unexpected exception " + ex.getClass().getName() + " while attempting to construct a new instance of " + loader.getName(), ex);
            }
        } else {
            throw new IllegalArgumentException("Class " + loader.getName() + " does not implement interface ExtensionLoader");
        }

        Pattern[] patterns = instance.extensionFilters();

        synchronized (this) {
            for (Pattern pattern : patterns) {
                fileAssociations.put(pattern, instance);
            }
        }
    }

    public GeyserExtension loadExtension(File file, Map<Pattern, GeyserExtensionLoader> loaders) {
        for (GeyserExtensionLoader loader : (loaders == null ? this.fileAssociations : loaders).values()) {
            for (Pattern pattern : loader.extensionFilters()) {
                if (pattern.matcher(file.getName()).matches()) {
                    try {
                        ExtensionDescription description = loader.extensionDescription(file);
                        if (description != null) {
                            GeyserExtension extension = loader.loadExtension(file);

                            if (extension != null) {
                                this.extensions.put(extension.description().name(), extension);

                                return extension;
                            }
                        }
                    } catch (Exception e) {
                        GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed"), e);
                        return null;
                    }
                }
            }
        }

        return null;
    }

    public Map<String, GeyserExtension> loadExtensions(File dictionary) {
        if (GeyserImpl.VERSION.equalsIgnoreCase("dev")) { // If your IDE says this is always true, ignore it, it isn't.
            GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_dev_environment"));
            return new HashMap<>();
        }
        if (!GeyserImpl.VERSION.contains(".")) {
            GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_version_number"));
            return new HashMap<>();
        }

        String[] apiVersion = GeyserImpl.VERSION.split("\\.");

        if (!dictionary.exists()) {
            dictionary.mkdir();
        }
        if (!dictionary.isDirectory()) {
            return new HashMap<>();
        }

        Map<String, File> extensions = new LinkedHashMap<>();
        Map<String, GeyserExtension> loadedExtensions = new LinkedHashMap<>();

        for (final GeyserExtensionLoader loader : this.fileAssociations.values()) {
            for (File file : dictionary.listFiles((dir, name) -> {
                for (Pattern pattern : loader.extensionFilters()) {
                    if (pattern.matcher(name).matches()) {
                        return true;
                    }
                }
                return false;
            })) {
                if (file.isDirectory()) {
                    continue;
                }

                try {
                    ExtensionDescription description = loader.extensionDescription(file);
                    if (description != null) {
                        String name = description.name();

                        if (extensions.containsKey(name) || this.getExtension(name) != null) {
                            GeyserImpl.getInstance().getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.extensions.load.duplicate", name, file.getName()));
                            continue;
                        }

                        try {
                            //Check the format: majorVersion.minorVersion.patch
                            if (!Pattern.matches("^[0-9]+\\.[0-9]+\\.[0-9]+$", description.apiVersion())) {
                                throw new IllegalArgumentException();
                            }
                        } catch (NullPointerException | IllegalArgumentException e) {
                            GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_api_format", name, apiVersion[0] + "." + apiVersion[1]));
                            continue;
                        }

                        String[] versionArray = description.apiVersion().split("\\.");

                        //Completely different API version
                        if (!Objects.equals(Integer.valueOf(versionArray[0]), Integer.valueOf(apiVersion[0]))) {
                            GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_api_version", name, apiVersion[0] + "." + apiVersion[1]));
                            continue;
                        }

                        //If the extension requires new API features, being backwards compatible
                        if (Integer.parseInt(versionArray[1]) > Integer.parseInt(apiVersion[1])) {
                            GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_api_version", name, apiVersion[0] + "." + apiVersion[1]));
                            continue;
                        }

                        extensions.put(name, file);
                        loadedExtensions.put(name, this.loadExtension(file, this.fileAssociations));
                    }
                } catch (Exception e) {
                    GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.load.failed_with_name", file.getName(), dictionary.getAbsolutePath()), e);
                }
            }
        }

        return loadedExtensions;
    }

    public void enableExtension(GeyserExtension extension) {
        if (!extension.isEnabled()) {
            try {
                extension.extensionLoader().enableExtension(extension);
            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.enable.failed", extension.name()), e);
                this.disableExtension(extension);
            }
        }
    }

    public void disableExtension(GeyserExtension extension) {
        if (extension.isEnabled()) {
            try {
                extension.extensionLoader().disableExtension(extension);
            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.disable.failed", extension.name()), e);
            }
        }
    }

    public void enableExtensions() {
        for (GeyserExtension extension : this.getExtensions().values()) {
            this.enableExtension(extension);
        }
    }

    public void disableExtensions() {
        for (GeyserExtension extension : this.getExtensions().values()) {
            this.disableExtension(extension);
        }
    }
}
