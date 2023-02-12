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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;

/**
 * Manages Geyser {@link Extension}s
 */
public abstract class ExtensionManager {

    /**
     * Gets an extension with the given name.
     *
     * @param name the name of the extension
     * @return an extension with the given name
     */
    @Nullable
    public abstract Extension extension(@NonNull String name);

    /**
     * Enables the given {@link Extension}.
     *
     * @param extension the extension to enable
     */
    public abstract void enable(@NonNull Extension extension);

    /**
     * Disables the given {@link Extension}.
     *
     * @param extension the extension to disable
     */
    public abstract void disable(@NonNull Extension extension);

    /**
     * Gets all the {@link Extension}s currently loaded.
     *
     * @return all the extensions currently loaded
     */
    @NonNull
    public abstract Collection<Extension> extensions();

    /**
     * Gets the {@link ExtensionLoader}.
     *
     * @return the extension loader
     */
    @Nullable
    public abstract ExtensionLoader extensionLoader();

    /**
     * Registers an {@link Extension} with the given {@link ExtensionLoader}.
     *
     * @param extension the extension
     */
    public abstract void register(@NonNull Extension extension);

    /**
     * Loads all extensions from the given {@link ExtensionLoader}.
     */
    protected final void loadAllExtensions(@NonNull ExtensionLoader extensionLoader) {
        extensionLoader.loadAllExtensions(this);
    }
}
