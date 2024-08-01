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

import java.util.List;

/**
 * Represents the description of an {@link Extension}.
 */
public interface ExtensionDescription {

    /**
     * Gets the extension's id.
     *
     * @return the extension's id
     */
    @NonNull
    String id();

    /**
     * Gets the extension's name.
     *
     * @return the extension's name
     */
    @NonNull
    String name();

    /**
     * Gets the extension's main class.
     *
     * @return the extension's main class
     */
    @NonNull
    String main();

    /**
     * Represents the human api version that the extension requires.
     * See the <a href="https://github.com/geysermc/api/blob/master/geyser-versioning.md">Geyser version outline</a>)
     * for more details on the Geyser API version.
     *
     * @return the extension's requested human api version
     */
    int humanApiVersion();

    /**
     * Represents the major api version that the extension requires.
     * See the <a href="https://github.com/geysermc/api/blob/master/geyser-versioning.md">Geyser version outline</a>)
     * for more details on the Geyser API version.
     *
     * @return the extension's requested major api version
     */
    int majorApiVersion();

    /**
     * Represents the minor api version that the extension requires.
     * See the <a href="https://github.com/geysermc/api/blob/master/geyser-versioning.md">Geyser version outline</a>)
     * for more details on the Geyser API version.
     *
     * @return the extension's requested minor api version
     */
    int minorApiVersion();

    /**
     * No longer in use. Geyser is now using an adaption of the romantic versioning scheme.
     * See <a href="https://github.com/geysermc/api/blob/master/geyser-versioning.md">here</a> for details.
     */
    @Deprecated(forRemoval = true)
    default int patchApiVersion() {
        return minorApiVersion();
    }

    /**
     * Returns the extension's requested Geyser Api version.
     */
    default String apiVersion() {
        return humanApiVersion() + "." + majorApiVersion() + "." + minorApiVersion();
    }

    /**
     * Gets the extension's description.
     *
     * @return the extension's description
     */
    @NonNull
    String version();

    /**
     * Gets the extension's authors.
     *
     * @return the extension's authors
     */
    @NonNull
    List<String> authors();
}
