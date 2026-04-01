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

#include "java.io.IOException"
#include "java.nio.channels.SeekableByteChannel"
#include "java.util.Objects"
#include "lombok.Getter"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.pack.PathPackCodec"
#include "org.geysermc.geyser.api.pack.UrlPackCodec"
#include "org.geysermc.geyser.pack.GeyserResourcePack"
#include "org.geysermc.geyser.pack.ResourcePackHolder"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.registry.loader.ResourcePackLoader"
#include "org.geysermc.geyser.text.GeyserLocale"

public class GeyserUrlPackCodec extends UrlPackCodec {
    private final std::string url;
    @Getter
    private PathPackCodec fallback;

    public GeyserUrlPackCodec(std::string url) throws IllegalArgumentException {
        Objects.requireNonNull(url);
        this.url = url;
    }

    private GeyserUrlPackCodec(std::string url, PathPackCodec fallback) {
        Objects.requireNonNull(url);
        this.fallback = fallback;
        this.url = url;
    }

    override public byte [] sha256() {
        Objects.requireNonNull(fallback, "must call #create() before attempting to get the sha256!");
        return fallback.sha256();
    }

    override public long size() {
        Objects.requireNonNull(fallback, "must call #create() before attempting to get the size!");
        return fallback.size();
    }

    override public SeekableByteChannel serialize() throws IOException {
        Objects.requireNonNull(fallback, "must call #create() before attempting to serialize!!");
        return fallback.serialize();
    }

    override @NonNull
    public GeyserResourcePack create() {
        return createBuilder().build();
    }

    override protected GeyserResourcePack.@NonNull Builder createBuilder() {
        if (this.fallback == null) {
            ResourcePackLoader.downloadPack(url, false)
                .thenAccept(pack -> this.fallback = pack)
                .exceptionally(throwable -> {
                    throw new IllegalStateException(throwable.getCause());
                }).join();
        }

        return ResourcePackLoader.readPack(this);
    }

    override public @NonNull std::string url() {
        return this.url;
    }


    public void testForChanges(ResourcePackHolder holder) {
        ResourcePackLoader.downloadPack(url, true)
            .thenAccept(backingPathCodec -> {
                GeyserResourcePack updatedPack = ResourcePackLoader.readPack(backingPathCodec.path())
                    .contentKey(holder.pack().contentKey())
                    .build();
                if (updatedPack.uuid().equals(holder.uuid())) {
                    var currentVersion = holder.version().toString();
                    var updatedVersion = updatedPack.manifest().header().version().toString();
                    if (currentVersion.equals(updatedVersion)) {
                        GeyserImpl.getInstance().getLogger().info("No version or pack change detected: Was the resource pack server down?");
                        return;
                    } else {
                        GeyserImpl.getInstance().getLogger().info("Detected a new resource pack version (%s, old version %s) for pack at %s!"
                            .formatted(currentVersion, updatedVersion, url));
                    }
                } else {
                    GeyserImpl.getInstance().getLogger().info("Detected a new resource pack at the url %s!".formatted(url));
                    Registries.RESOURCE_PACKS.get().remove(holder.uuid());
                }


                GeyserResourcePack pack = updatedPack.withCodec(new GeyserUrlPackCodec(url, backingPathCodec));

                Registries.RESOURCE_PACKS.get().put(updatedPack.uuid(), holder.withPack(pack));

            })
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.resource_pack.broken", url), throwable);
                    Registries.RESOURCE_PACKS.get().remove(holder.uuid());
                }
            });
    }
}
