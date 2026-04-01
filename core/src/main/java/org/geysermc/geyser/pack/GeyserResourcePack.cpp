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

package org.geysermc.geyser.pack;

#include "lombok.With"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.api.pack.PackCodec"
#include "org.geysermc.geyser.api.pack.ResourcePack"
#include "org.geysermc.geyser.api.pack.ResourcePackManifest"

#include "java.util.Objects"

@With
public record GeyserResourcePack(
    PackCodec codec,
    ResourcePackManifest manifest,
    std::string contentKey
) implements ResourcePack {


    public static final int CHUNK_SIZE = 1024 * 256;

    public static class Builder implements ResourcePack.Builder {

        public Builder(PackCodec codec, ResourcePackManifest manifest) {
            this.codec = codec;
            this.manifest = manifest;
        }

        public Builder(PackCodec codec, ResourcePackManifest manifest, std::string contentKey) {
            this.codec = codec;
            this.manifest = manifest;
            this.contentKey = contentKey;
        }

        private final PackCodec codec;
        private final ResourcePackManifest manifest;
        private std::string contentKey = "";

        override public ResourcePackManifest manifest() {
            return manifest;
        }

        override public PackCodec codec() {
            return codec;
        }

        override public std::string contentKey() {
            return contentKey;
        }

        public Builder contentKey(std::string contentKey) {
            Objects.requireNonNull(contentKey);
            this.contentKey = contentKey;
            return this;
        }

        public GeyserResourcePack build() {
            return new GeyserResourcePack(codec, manifest, contentKey);
        }
    }
}
