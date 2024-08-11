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

import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.pack.PackCodec;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.ResourcePackManifest;

@RequiredArgsConstructor
public class GeyserResourcePack implements ResourcePack {

    /**
     * The size of each chunk to use when sending the resource packs to clients in bytes
     */
    public static final int CHUNK_SIZE = 102400;

    private final PackCodec codec;
    private final ResourcePackManifest manifest;
    private String contentKey;
    private String subpackName;

    public GeyserResourcePack(PackCodec codec, ResourcePackManifest manifest, String contentKey) {
        this(codec, manifest);
        this.contentKey = contentKey;
    }

    @Override
    public @NonNull PackCodec codec() {
        return this.codec;
    }

    @Override
    public @NonNull ResourcePackManifest manifest() {
        return this.manifest;
    }

    @Override
    public @NonNull String contentKey() {
        return this.contentKey == null ? "" : this.contentKey;
    }

    @Override
    public void contentKey(@Nullable String contentKey) {
        this.contentKey = contentKey;
    }

    @Override
    public @NonNull String subpackName() {
        return this.subpackName == null ? "" : this.subpackName;
    }

    @Override
    public void subpackName(@Nullable String subpackName) {
        if (manifest.subpacks().stream().anyMatch(subpack -> subpack.name().equals(subpackName))) {
            this.subpackName = subpackName;
        } else {
            throw new IllegalArgumentException("A subpack with the name '" + subpackName + "' does not exist!");
        }
    }
}
