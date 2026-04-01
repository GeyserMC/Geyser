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
 * Represents the description of an [Extension].
 */
interface ExtensionDescription {
    /**
     * Gets the extension's id.
     * 
     * @return the extension's id
     */
    fun id(): String

    /**
     * Gets the extension's name.
     * 
     * @return the extension's name
     */
    fun name(): String

    /**
     * Gets the extension's main class.
     * 
     * @return the extension's main class
     */
    fun main(): String

    /**
     * Represents the human api version that the extension requires.
     * See the [Geyser version outline](https://github.com/geysermc/api/blob/master/geyser-versioning.md))
     * for more details on the Geyser API version.
     * 
     * @return the extension's requested human api version
     */
    fun humanApiVersion(): Int

    /**
     * Represents the major api version that the extension requires.
     * See the [Geyser version outline](https://github.com/geysermc/api/blob/master/geyser-versioning.md))
     * for more details on the Geyser API version.
     * 
     * @return the extension's requested major api version
     */
    fun majorApiVersion(): Int

    /**
     * Represents the minor api version that the extension requires.
     * See the [Geyser version outline](https://github.com/geysermc/api/blob/master/geyser-versioning.md))
     * for more details on the Geyser API version.
     * 
     * @return the extension's requested minor api version
     */
    fun minorApiVersion(): Int

    /**
     * No longer in use. Geyser is now using an adaption of the romantic versioning scheme.
     * See [here](https://github.com/geysermc/api/blob/master/geyser-versioning.md) for details.
     */
    @Deprecated("")
    fun patchApiVersion(): Int {
        return minorApiVersion()
    }

    /**
     * Returns the extension's requested Geyser Api version.
     */
    fun apiVersion(): String {
        return humanApiVersion().toString() + "." + majorApiVersion() + "." + minorApiVersion()
    }

    /**
     * Gets the extension's description.
     * 
     * @return the extension's description
     */
    fun version(): String

    /**
     * Gets the extension's authors.
     * 
     * @return the extension's authors
     */
    fun authors(): MutableList<String?>
}
