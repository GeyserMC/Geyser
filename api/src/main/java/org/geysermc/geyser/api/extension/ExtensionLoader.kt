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
import org.geysermc.geyser.api.event.ExtensionEventBus;

import java.nio.file.Path;

/**
 * The extension loader is responsible for loading, unloading, enabling and disabling extensions
 */
public abstract class ExtensionLoader {
    /**
     * Gets if the given {@link Extension} is enabled.
     *
     * @param extension the extension
     * @return if the extension is enabled
     */
    protected abstract boolean isEnabled(@NonNull Extension extension);

    /**
     * Sets if the given {@link Extension} is enabled.
     *
     * @param extension the extension to enable
     * @param enabled if the extension should be enabled
     */
    protected abstract void setEnabled(@NonNull Extension extension, boolean enabled);

    /**
     * Gets the given {@link Extension}'s data folder.
     *
     * @param extension the extension
     * @return the data folder of the given extension
     */
    @NonNull
    protected abstract Path dataFolder(@NonNull Extension extension);

    /**
     * Gets the given {@link Extension}'s {@link ExtensionDescription}.
     *
     * @param extension the extension
     * @return the description of the given extension
     */
    @NonNull
    protected abstract ExtensionDescription description(@NonNull Extension extension);

    /**
     * Gets the given {@link Extension}'s {@link ExtensionEventBus}.
     *
     * @param extension the extension
     * @return the extension's event bus
     */
    @NonNull
    protected abstract ExtensionEventBus eventBus(@NonNull Extension extension);

    /**
     * Gets the {@link ExtensionLogger} for the given {@link Extension}.
     *
     * @param extension the extension
     * @return the extension logger for the given extension
     */
    @NonNull
    protected abstract ExtensionLogger logger(@NonNull Extension extension);

    /**
     * Loads all extensions.
     *
     * @param extensionManager the extension manager
     */
    protected abstract void loadAllExtensions(@NonNull ExtensionManager extensionManager);

    /**
     * Registers the given {@link Extension} with the given {@link ExtensionManager}.
     *
     * @param extension the extension
     * @param extensionManager the extension manager
     */
    protected void register(@NonNull Extension extension, @NonNull ExtensionManager extensionManager) {
        extensionManager.register(extension);
    }
}