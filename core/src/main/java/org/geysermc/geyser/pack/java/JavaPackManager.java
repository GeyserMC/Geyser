/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.pack.java;

import com.google.common.hash.HashCode;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.util.FancyHttpClient;
import org.geysermc.mcprotocollib.protocol.data.game.ResourcePackStatus;

import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Pattern;

// TODO do we need our own executor service?
// com.google.common.hash
@SuppressWarnings("UnstableApiUsage")
public final class JavaPackManager {
    // From Mojang: https://mcsrc.dev/1/26.2/net/minecraft/client/resources/server/DownloadedPackSource#L64
    private static final Pattern SHA1 = Pattern.compile("^[a-fA-F0-9]{40}$");
    // From Mojang: https://mcsrc.dev/1/26.2/net/minecraft/client/resources/server/DownloadedPackSource#L192
    private static final int MAX_PACK_SIZE_BYTES = 262144000;
    private static JavaPackManager instance = null;

    private final Map<JavaPack.Id, CompletableFuture<Optional<JavaPack>>> loadedPacks = new Object2ObjectOpenHashMap<>();
    private final Path cacheDirectory;

    private JavaPackManager() {
        cacheDirectory = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache/server_packs");
    }

    // TODO Implement loading from cache
    public CompletableFuture<Optional<JavaPack>> downloadIfAbsent(UUID uuid, String url, String hash,
                                                                  Consumer<ResourcePackStatus> statusConsumer) {
        URI uri;
        try {
            uri = URI.create(url);
            if (!uri.getScheme().equals("http") && !uri.getScheme().equals("https")) {
                throw new IllegalArgumentException("URI scheme must be \"http\" or \"https\"");
            }
        } catch (IllegalArgumentException exception) {
            statusConsumer.accept(ResourcePackStatus.INVALID_URL);
            return CompletableFuture.completedFuture(Optional.empty());
        }

        statusConsumer.accept(ResourcePackStatus.ACCEPTED);

        // If it's not a valid SHA1 hash, then use null and don't verify the download (matches vanilla)
        HashCode parsedHash = hash != null && SHA1.matcher(hash).matches() ? HashCode.fromString(hash.toLowerCase(Locale.ROOT)) : null;
        JavaPack.Id id = new JavaPack.Id(uuid, parsedHash);

        synchronized (this) {
            CompletableFuture<Optional<JavaPack>> current = loadedPacks.get(id);
            // If current has completed and is NOT present, the previous download failed, so we try again now
            if (current != null && (!current.isDone() || current.resultNow().isPresent())) {
                // These futures should never throw as client.downloadFileSafe has an exception handler
                return current.whenComplete((pack, ignored) -> {
                    if (pack.isPresent()) {
                        statusConsumer.accept(ResourcePackStatus.DOWNLOADED);
                        statusConsumer.accept(ResourcePackStatus.SUCCESSFULLY_LOADED);
                    } else {
                        // Can't distinguish between failed download or failed load at this point, but that's okay
                        statusConsumer.accept(ResourcePackStatus.FAILED_DOWNLOAD);
                    }
                });
            }

            current = FancyHttpClient.oneShot(client -> client.downloadFileSafe(uri, cacheDirectory.resolve(uuid.toString()), MAX_PACK_SIZE_BYTES)
                .thenApplyAsync(result -> {
                    statusConsumer.accept(ResourcePackStatus.DOWNLOADED);
                    return result.flatMap(path -> {
                        Optional<JavaPack> opened = JavaPack.open(path, id);
                        opened.ifPresentOrElse(pack -> statusConsumer.accept(ResourcePackStatus.SUCCESSFULLY_LOADED),
                            () -> statusConsumer.accept(ResourcePackStatus.FAILED_RELOAD));
                        return opened;
                    });
                }));
            loadedPacks.put(id, current);
            return current;
        }
    }

    public static JavaPackManager getInstance() {
        if (instance == null) {
            instance = new JavaPackManager();
        }
        return instance;
    }
}
