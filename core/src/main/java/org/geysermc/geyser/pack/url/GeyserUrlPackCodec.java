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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.pack.ResourcePack;
import org.geysermc.geyser.api.pack.UrlPackCodec;
import org.geysermc.geyser.pack.path.GeyserPathPackCodec;
import org.geysermc.geyser.registry.loader.ResourcePackLoader;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;

public class GeyserUrlPackCodec extends UrlPackCodec {
    private final String url;
    private final String contentKey;
    @Getter
    private GeyserPathPackCodec fallback;

    public GeyserUrlPackCodec(String url) throws IllegalArgumentException {
        this(url, "");
    }

    public GeyserUrlPackCodec(@NonNull String url, @Nullable String contentKey) throws IllegalArgumentException {
        //noinspection ConstantValue - need to enforce
        if (url == null) {
            throw new IllegalArgumentException("Url cannot be nulL!");
        }
        this.url = url;
        this.contentKey = contentKey == null ? "" : contentKey;
    }

    @Override
    public byte @NonNull [] sha256() {
        if (this.fallback == null) {
            throw new IllegalStateException("Fallback pack not initialized! Needs to be created first.");
        }
        return fallback.sha256();
    }

    @Override
    public long size() {
        if (this.fallback == null) {
            throw new IllegalStateException("Fallback pack not initialized! Needs to be created first.");
        }
        return fallback.size();
    }

    @Override
    public @NonNull SeekableByteChannel serialize(@NonNull ResourcePack resourcePack) throws IOException {
        if (this.fallback == null) {
            throw new IllegalStateException("Fallback pack not initialized! Needs to be created first.");
        }
        return fallback.serialize(resourcePack);
    }

    @Override
    @NonNull
    public ResourcePack create() {
        if (this.fallback == null) {
            try {
                final Path downloadedPack = ResourcePackLoader.downloadPack(url, false).whenComplete((pack, throwable) -> {
                    if (throwable != null) {
                        GeyserImpl.getInstance().getLogger().error("Failed to download pack from " + url, throwable);
                        if (GeyserImpl.getInstance().getConfig().isDebugMode()) {
                            throwable.printStackTrace();
                        }
                    }
                }).join();
                this.fallback = new GeyserPathPackCodec(downloadedPack);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to download pack from " + url, e);
            }
        }
        return ResourcePackLoader.readPack(this);
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
