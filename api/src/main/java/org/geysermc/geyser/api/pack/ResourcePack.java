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

package org.geysermc.geyser.api.pack;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents a resource pack sent to Bedrock clients
 * <p>
 * This representation of a resource pack only contains what
 * Geyser requires to send it to the client.
 */
public interface ResourcePack {

    /**
     * The {@link PackCodec codec} for this pack.
     *
     * @return the codec for this pack
     */
    @NonNull
    PackCodec codec();

    /**
     * Gets the resource pack manifest.
     *
     * @return the resource pack manifest
     */
    @NonNull
    ResourcePackManifest manifest();

    /**
     * Gets the content key of the resource pack. Lack of a content key is represented by an empty String.
     *
     * @return the content key of the resource pack
     */
    @NonNull
    String contentKey();

    /**
     * Creates a resource pack with the given {@link PackCodec}.
     *
     * @param codec the pack codec
     * @return the resource pack
     */
    @NonNull
    static ResourcePack create(@NonNull PackCodec codec) {
        return codec.create();
    }
}
