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

package org.geysermc.geyser.pack.url;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.UrlPackCodec;
import org.geysermc.geyser.pack.path.GeyserPathPackCodec;
import org.geysermc.geyser.registry.loader.ResourcePackLoader;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

public class GeyserUrlPackCodec extends UrlPackCodec {
    private final String url;
    private final String contentKey;
    @Getter
    private final GeyserPathPackCodec fallback;

    public GeyserUrlPackCodec(String url) throws IllegalArgumentException {
        this(url, "");
    }

    public GeyserUrlPackCodec(String url, String contentKey) throws IllegalArgumentException {
        this.url = url;
        this.contentKey = contentKey;
        try {
            this.fallback = new GeyserPathPackCodec(ResourcePackLoader.downloadPack(url).get());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to download pack from " + url, e);
        }
    }

    @Override
    public byte @NonNull [] sha256() {
        return fallback.sha256();
    }

    @Override
    public long size() {
        return fallback.size();
    }

    @Override
    public @NonNull SeekableByteChannel serialize(@NonNull ResourcePack resourcePack) throws IOException {
        return fallback.serialize(resourcePack);
    }

    @Override
    @NonNull
    public ResourcePack create() {
        return ResourcePackLoader.loadDownloadedPack(this);
    }

    @Override
    public @NonNull String url() {
        return this.url;
    }

    @Override
    public @NonNull String contentKey() {
        return this.contentKey;
    }
}
