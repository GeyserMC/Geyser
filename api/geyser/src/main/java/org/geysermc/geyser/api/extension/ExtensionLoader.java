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

import org.geysermc.geyser.api.extension.exception.InvalidDescriptionException;
import org.geysermc.geyser.api.extension.exception.InvalidExtensionException;
import java.io.File;

/**
 * The extension loader is responsible for loading, unloading, enabling and disabling extensions
 */
public interface ExtensionLoader {
    /**
     * Loads an extension from a given file
     *
     * @param file the file to load the extension from
     * @return the loaded extension
     * @throws InvalidExtensionException
     */
    GeyserExtension loadExtension(File file) throws InvalidExtensionException;

    /**
     * Gets an extension's description from a given file
     *
     * @param file the file to get the description from
     * @return the extension's description
     * @throws InvalidDescriptionException
     */
    ExtensionDescription extensionDescription(File file) throws InvalidDescriptionException;

    /**
     * Gets a class by its name from the extension's classloader
     *
     * @param name the name of the class
     * @return the class
     * @throws ClassNotFoundException
     */
    Class<?> classByName(final String name) throws ClassNotFoundException;

    /**
     * Enables an extension
     *
     * @param extension the extension to enable
     */
    void enableExtension(GeyserExtension extension);

    /**
     * Disables an extension
     *
     * @param extension the extension to disable
     */
    void disableExtension(GeyserExtension extension);
}
