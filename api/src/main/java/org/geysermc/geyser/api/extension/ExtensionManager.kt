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
package org.geysermc.geyser.api.extension

/**
 * Manages Geyser [Extension]s
 */
abstract class ExtensionManager {
    /**
     * Gets an extension by the given ID.
     * 
     * @param id the ID of the extension
     * @return an extension with the given ID
     */
    abstract fun extension(id: String): Extension?

    /**
     * Enables the given [Extension].
     * 
     * @param extension the extension to enable
     */
    abstract fun enable(extension: Extension)

    /**
     * Disables the given [Extension].
     * 
     * @param extension the extension to disable
     */
    abstract fun disable(extension: Extension)

    /**
     * Gets all the [Extension]s currently loaded.
     * 
     * @return all the extensions currently loaded
     */
    abstract fun extensions(): MutableCollection<Extension?>

    /**
     * Gets the [ExtensionLoader].
     * 
     * @return the extension loader
     */
    abstract fun extensionLoader(): ExtensionLoader?

    /**
     * Registers an [Extension] with the given [ExtensionLoader].
     * 
     * @param extension the extension
     */
    abstract fun register(extension: Extension)

    /**
     * Loads all extensions from the given [ExtensionLoader].
     */
    protected fun loadAllExtensions(extensionLoader: ExtensionLoader) {
        extensionLoader.loadAllExtensions(this)
    }
}
