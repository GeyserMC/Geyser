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
import org.geysermc.geyser.api.GeyserApi;

import java.util.UUID;

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
     * @return the resource pack uuid. Shortcut for getting the UUID from the header.
     */
    @NonNull
    default UUID uuid() {
        return manifest().header().uuid();
    }

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

    /**
     * Returns a {@link Builder} for a resource pack.
     * It can be used to set a content key, or a default subpack.
     *
     * @param codec the {@link PackCodec} to base the builder on
     * @return a {@link Builder} to build a resource pack.
     */
    static Builder builder(@NonNull PackCodec codec) {
        return GeyserApi.api().provider(Builder.class, codec);
    }

    /**
     * A builder for a resource pack. It allows providing a content key manually, or
     * setting a default subpack.
     */
    interface Builder {

        /**
         * @return the {@link ResourcePackManifest} of this resource pack
         */
        ResourcePackManifest manifest();

        /**
         * @return the {@link PackCodec} of this resource pack
         */
        PackCodec codec();

        /**
         * @return the current content key, or an empty string if not set
         */
        String contentKey();

        /**
         * Sets a content key for this resource pack.
         *
         * @param contentKey the content key
         * @return this builder
         */
        Builder contentKey(@NonNull String contentKey);

        /**
         * @return the resource pack
         */
        ResourcePack build();
    }
}
