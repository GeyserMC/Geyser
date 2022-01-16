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

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.extension.ExtensionManager;
import org.geysermc.geyser.api.extension.ExtensionLoader;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.text.GeyserLocale;

import java.util.*;
import java.util.stream.Collectors;

public class GeyserExtensionManager extends ExtensionManager {
    private static final Key BASE_EXTENSION_LOADER_KEY = Key.key("geysermc", "base");

    private final Map<String, Extension> extensions = new LinkedHashMap<>();
    private final Map<Extension, ExtensionLoader> extensionsLoaders = new LinkedHashMap<>();

    public void init() {
        GeyserImpl.getInstance().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.extensions.load.loading"));

        Registries.EXTENSION_LOADERS.register(BASE_EXTENSION_LOADER_KEY, new GeyserExtensionLoader());
        for (ExtensionLoader loader : this.extensionLoaders().values()) {
            this.loadAllExtensions(loader);
        }

        GeyserImpl.getInstance().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.extensions.load.done", this.extensions.size()));
    }

    @Override
    public Extension extension(@NonNull String name) {
        if (this.extensions.containsKey(name)) {
            return this.extensions.get(name);
        }

        return null;
    }

    @Override
    public void enable(@NonNull Extension extension) {
        if (!extension.isEnabled()) {
            try {
                this.enableExtension(extension);
            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.enable.failed", extension.name()), e);
                this.disable(extension);
            }
        }
    }

    @Override
    public void disable(@NonNull Extension extension) {
        if (extension.isEnabled()) {
            try {
                this.disableExtension(extension);
            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.disable.failed", extension.name()), e);
            }
        }
    }

    public void enableExtension(Extension extension) {
        if (!extension.isEnabled()) {
            extension.setEnabled(true);
            GeyserImpl.getInstance().eventBus().register(extension, extension);
            GeyserImpl.getInstance().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.extensions.enable.success", extension.description().name()));
        }
    }

    private void disableExtension(@NonNull Extension extension) {
        if (extension.isEnabled()) {
            GeyserImpl.getInstance().eventBus().unregisterAll(extension);

            extension.setEnabled(false);
            GeyserImpl.getInstance().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.extensions.disable.success", extension.description().name()));
        }
    }

    public void enableExtensions() {
        for (Extension extension : this.extensions()) {
            this.enable(extension);
        }
    }

    public void disableExtensions() {
        for (Extension extension : this.extensions()) {
            this.disable(extension);
        }
    }

    @Override
    public ExtensionLoader extensionLoader(@NonNull Extension extension) {
        return this.extensionsLoaders.get(extension);
    }

    @NonNull
    @Override
    public Collection<Extension> extensions() {
        return Collections.unmodifiableCollection(this.extensions.values());
    }

    @Nullable
    @Override
    public ExtensionLoader extensionLoader(@NonNull String identifier) {
        return Registries.EXTENSION_LOADERS.get(Key.key(identifier));
    }

    @Override
    public void registerExtensionLoader(@NonNull String identifier, @NonNull ExtensionLoader extensionLoader) {
        Registries.EXTENSION_LOADERS.register(Key.key(identifier), extensionLoader);
    }

    @NonNull
    @Override
    public Map<String, ExtensionLoader> extensionLoaders() {
        return Registries.EXTENSION_LOADERS.get().entrySet().stream().collect(Collectors.toMap(key -> key.getKey().asString(), Map.Entry::getValue));
    }

    @Override
    public void register(@NonNull Extension extension, @NonNull ExtensionLoader loader) {
        this.extensionsLoaders.put(extension, loader);
        this.extensions.put(extension.name(), extension);
    }
}
