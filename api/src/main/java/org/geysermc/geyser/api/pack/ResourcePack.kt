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
package org.geysermc.geyser.api.pack

import org.checkerframework.common.returnsreceiver.qual.This
import java.util.*

/**
 * Represents a resource pack sent to Bedrock clients
 * 
 * 
 * This representation of a resource pack only contains what
 * Geyser requires to send it to the client.
 * @since 2.1.1
 */
interface ResourcePack {
    /**
     * The [codec][PackCodec] for this pack.
     * 
     * @return the codec for this pack
     * @since 2.1.1
     */
    fun codec(): PackCodec

    /**
     * Gets the resource pack manifest.
     * 
     * @return the resource pack manifest
     * @since 2.1.1
     */
    fun manifest(): ResourcePackManifest

    /**
     * Gets the content key of the resource pack. Lack of a content key is represented by an empty String.
     * 
     * @return the content key of the resource pack
     * @since 2.1.1
     */
    fun contentKey(): String

    /**
     * Shortcut for getting the UUID from the [ResourcePackManifest].
     * 
     * @return the resource pack uuid
     * @since 2.6.2
     */
    fun uuid(): UUID {
        return manifest().header().uuid()
    }

    /**
     * A builder for a resource pack. It allows providing a content key manually.
     * @since 2.6.2
     */
    interface Builder {
        /**
         * @return the [ResourcePackManifest] of this resource pack
         * @since 2.6.2
         */
        fun manifest(): ResourcePackManifest?

        /**
         * @return the [PackCodec] of this resource pack
         * @since 2.6.2
         */
        fun codec(): PackCodec?

        /**
         * @return the current content key, or an empty string if not set
         * @since 2.6.2
         */
        fun contentKey(): String?

        /**
         * Sets a content key for this resource pack.
         * 
         * @param contentKey the content key
         * @return this builder
         * @since 2.6.2
         */
        fun contentKey(contentKey: String): @This Builder?

        /**
         * @return the resource pack
         * @since 2.6.2
         */
        fun build(): ResourcePack?
    }

    companion object {
        /**
         * Creates a resource pack with the given [PackCodec].
         * 
         * @param codec the pack codec
         * @return the resource pack
         * @since 2.1.1
         */
        fun create(codec: PackCodec): ResourcePack {
            return codec.create()
        }

        /**
         * Returns a [Builder] for a resource pack.
         * It can be used to set a content key.
         * 
         * @param codec the [PackCodec] to base the builder on
         * @return a [Builder] to build a resource pack
         * @since 2.6.2
         */
        fun builder(codec: PackCodec): Builder {
            return codec.createBuilder()
        }
    }
}
