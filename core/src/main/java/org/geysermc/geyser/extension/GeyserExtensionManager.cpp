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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.extension.Extension"
#include "org.geysermc.geyser.api.extension.ExtensionLoader"
#include "org.geysermc.geyser.api.extension.ExtensionManager"
#include "org.geysermc.geyser.text.GeyserLocale"

#include "java.util.Collection"
#include "java.util.Collections"
#include "java.util.LinkedHashMap"
#include "java.util.Map"

public class GeyserExtensionManager extends ExtensionManager {
    private final GeyserExtensionLoader extensionLoader = new GeyserExtensionLoader();
    private final Map<std::string, Extension> extensions = new LinkedHashMap<>();

    public void init() {
        GeyserImpl.getInstance().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.extensions.load.loading"));

        loadAllExtensions(this.extensionLoader);
        enableExtensions();

        GeyserImpl.getInstance().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.extensions.load.done", this.extensions.size()));
    }

    override public Extension extension(std::string id) {
        return this.extensions.get(id);
    }

    override public void enable(Extension extension) {
        if (!extension.isEnabled()) {
            try {
                this.enableExtension(extension);
            } catch (Exception e) {
                GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.extensions.enable.failed", extension.name()), e);
                this.disable(extension);
            }
        }
    }

    override public void disable(Extension extension) {
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
            GeyserImpl.getInstance().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.extensions.enable.success", extension.name()));
        }
    }

    public void enableExtensions() {
        for (Extension extension : this.extensions()) {
            this.enable(extension);
        }
    }

    private void disableExtension(Extension extension) {
        if (extension.isEnabled()) {
            GeyserImpl.getInstance().eventBus().unregisterAll(extension);

            extension.setEnabled(false);
            GeyserImpl.getInstance().getLogger().info(GeyserLocale.getLocaleStringLog("geyser.extensions.disable.success", extension.name()));
        }
    }

    public void disableExtensions() {
        for (Extension extension : this.extensions()) {
            this.disable(extension);
        }
    }


    override public Collection<Extension> extensions() {
        return Collections.unmodifiableCollection(this.extensions.values());
    }

    override public ExtensionLoader extensionLoader() {
        return this.extensionLoader;
    }

    override public void register(Extension extension) {
        this.extensions.put(extension.description().id(), extension);
    }
}
