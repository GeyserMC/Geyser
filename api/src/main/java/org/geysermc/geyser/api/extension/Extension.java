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

package org.geysermc.geyser.api.extension;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.api.GeyserApiBase;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.ExtensionEventBus;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents an extension within Geyser.
 */
public interface Extension extends EventRegistrar {

    /**
     * Gets if the extension is enabled
     *
     * @return true if the extension is enabled
     */
    default boolean isEnabled() {
        return this.extensionLoader().isEnabled(this);
    }

    /**
     * Enables or disables the extension
     *
     * @param enabled if the extension should be enabled
     */
    default void setEnabled(boolean enabled) {
        this.extensionLoader().setEnabled(this, enabled);
    }

    /**
     * Gets the extension's data folder
     *
     * @return the extension's data folder
     */
    @NonNull
    default Path dataFolder() {
        return this.extensionLoader().dataFolder(this);
    }

    /**
     * Gets the {@link ExtensionEventBus}.
     *
     * @return the extension event bus
     */
    @NonNull
    default ExtensionEventBus eventBus() {
        return this.extensionLoader().eventBus(this);
    }

    /**
     * Gets the {@link ExtensionManager}.
     *
     * @return the extension manager
     */
    @NonNull
    default ExtensionManager extensionManager() {
        return this.geyserApi().extensionManager();
    }

    /**
     * Gets the extension's name
     *
     * @return the extension's name
     */
    @NonNull
    default String name() {
        return this.description().name();
    }

    /**
     * Gets this extension's {@link ExtensionDescription}.
     *
     * @return the extension's description
     */
    @NonNull
    default ExtensionDescription description() {
        return this.extensionLoader().description(this);
    }

    /**
     * Gets the extension's logger
     *
     * @return the extension's logger
     */
    @NonNull
    default ExtensionLogger logger() {
        return this.extensionLoader().logger(this);
    }

    /**
     * Gets the {@link ExtensionLoader}.
     *
     * @return the extension loader
     */
    @NonNull
    default ExtensionLoader extensionLoader() {
        return Objects.requireNonNull(this.extensionManager().extensionLoader());
    }

    /**
     * Gets the {@link GeyserApiBase} instance
     *
     * @return the geyser api instance
     */
    @NonNull
    default GeyserApi geyserApi() {
        return GeyserApi.api();
    }

    /**
     * Disable the extension.
     */
    default void disable() {
        this.setEnabled(false);
        this.eventBus().unregisterAll();
    }
}
