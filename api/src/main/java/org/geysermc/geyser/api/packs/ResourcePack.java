/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.packs;

import java.nio.file.Path;

/**
 * This represents a resource pack and all the data relevant to it
 */

public interface ResourcePack {

    /**
     * Gets the sha256 hash of the resource pack.
     *
     * @return the hash
     */
    byte[] sha256();

    /**
     * Gets the resource pack file.
     *
     * @return the resource pack file
     */
    Path path();

    /**
     * Gets the resource pack file length.
     *
     * @return the resource pack file length
     */
    long length();

    /**
     * Gets the resource pack manifest.
     *
     * @return the resource pack manifest
     */
    ResourcePackManifest manifest();

    /**
     * Gets the version of the resource pack.
     *
     * @return the version of the resource pack
     */
    ResourcePackManifest.Version version();

    /**
     * Gets the name of the command.
     *
     * @return the name of the command
     */
    String contentKey();
}
