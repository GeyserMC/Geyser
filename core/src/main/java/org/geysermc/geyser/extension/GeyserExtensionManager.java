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
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.extension.ExtensionDescription;
import org.geysermc.geyser.api.extension.GeyserExtension;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.regex.Pattern;

public class GeyserExtensionManager {
    private static GeyserExtensionManager geyserExtensionManager = null;

    protected Map<String, Extension> extensions = new LinkedHashMap<>();
    protected Map<Pattern, GeyserExtensionLoader> fileAssociations = new HashMap<>();

    public static void init() {
        GeyserImpl.getInstance().getLogger().info("Loading extensions...");
        geyserExtensionManager = new GeyserExtensionManager();
        geyserExtensionManager.registerInterface(GeyserExtensionLoader.class);
        geyserExtensionManager.loadExtensions(new File("extensions"));
        GeyserImpl.getInstance().getLogger().info("Loaded " + geyserExtensionManager.extensions.size() + " extensions.");

        for (Extension extension : geyserExtensionManager.getExtensions().values()) {
            if (!extension.isEnabled()) {
                geyserExtensionManager.enableExtension(extension);
            }
        }
    }

    public static GeyserExtensionManager getExtensionManager() {
        return geyserExtensionManager;
    }

    public Extension getExtension(String name) {
        if (this.extensions.containsKey(name)) {
            return this.extensions.get(name);
        }
        return null;
    }

    public Map<String, Extension> getExtensions() {
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
                        GeyserImpl.getInstance().getLogger().error("Could not load extension", e);
                        return null;
                    }
                }
            }
        }

        return null;
    }

    public Map<String, Extension> loadExtensions(File dictionary) {
        if (GeyserImpl.VERSION.equalsIgnoreCase("dev")) {
            GeyserImpl.getInstance().getLogger().error("Cannot load extensions in a development environment, aborting extension loading");
            return new HashMap<>();
        }
        if (!GeyserImpl.VERSION.contains(".")) {
            GeyserImpl.getInstance().getLogger().error("Something went wrong with the Geyser version number, aborting extension loading");
            return new HashMap<>();
        }

        if (!dictionary.exists()) {
            dictionary.mkdir();
        }
        if (!dictionary.isDirectory()) {
            return new HashMap<>();
        }

        Map<String, File> extensions = new LinkedHashMap<>();
        Map<String, Extension> loadedExtensions = new LinkedHashMap<>();

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
                            GeyserImpl.getInstance().getLogger().warning("Found duplicate extension '" + name + "', ignoring '" + file.getName() + "'");
                            continue;
                        }

                        boolean compatible = false;

                        for (String version : description.ApiVersions()) {
                            try {
                                //Check the format: majorVersion.minorVersion.patch
                                if (!Pattern.matches("^[0-9]+\\.[0-9]+\\.[0-9]+$", version)) {
                                    throw new IllegalArgumentException();
                                }
                            } catch (NullPointerException | IllegalArgumentException e) {
                                GeyserImpl.getInstance().getLogger().error("Could't load extension " + name + ": Wrong API format");
                                continue;
                            }

                            String[] versionArray = version.split("\\.");
                            String[] apiVersion = GeyserImpl.VERSION.split("\\.");

                            //Completely different API version
                            if (!Objects.equals(Integer.valueOf(versionArray[0]), Integer.valueOf(apiVersion[0]))) {
                                GeyserImpl.getInstance().getLogger().error("Couldn't load extension " + name + ": Wrong API version, current version: " + apiVersion[0] + "." + apiVersion[1]);
                                continue;
                            }

                            //If the extension requires new API features, being backwards compatible
                            if (Integer.parseInt(versionArray[1]) > Integer.parseInt(apiVersion[1])) {
                                GeyserImpl.getInstance().getLogger().error("Couldn't load extension " + name + ": Wrong API version, current version: " + apiVersion[0] + "." + apiVersion[1]);
                                continue;
                            }

                            compatible = true;
                            break;
                        }

                        if (!compatible) {
                            GeyserImpl.getInstance().getLogger().error("Couldn't load extension " + name +": Incompatible API version");
                        }

                        extensions.put(name, file);
                        loadedExtensions.put(name, this.loadExtension(file, this.fileAssociations));
                    }
                } catch (Exception e) {
                    GeyserImpl.getInstance().getLogger().error("Couldn't load " +file.getName()+ " in folder " + dictionary + ": ", e);
                }
            }
        }

        return loadedExtensions;
    }

    public void enableExtension(Extension extension) {
        if (!extension.isEnabled()) {
            try {
                extension.extensionLoader().enableExtension(extension);
            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().error("Error enabling extension " + extension.name() + ": ", e);
                this.disableExtension(extension);
            }
        }
    }

    public void disableExtension(Extension extension) {
        if (extension.isEnabled()) {
            try {
                extension.extensionLoader().disableExtension(extension);
            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().error("Error disabling extension " + extension.name() + ": ", e);
            }
        }
    }

    public void disableExtensions() {
        for (Extension extension : this.getExtensions().values()) {
            this.disableExtension(extension);
        }
    }
}
