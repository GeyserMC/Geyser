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

package org.geysermc.geyser.pack;

import org.geysermc.geyser.GeyserImpl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

public record ResourcePackMetadata(Path path, String url, long size, String eTag, long lastModified, Path downloadLocation) {
    private static final Path REMOTE_PACK_CACHE = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache").resolve("remote_packs");

    private ResourcePackMetadata(String url, long size, String eTag, long lastModified, Path downloadLocation) {
        this(getMetadataPath(url), url, size, eTag, lastModified, downloadLocation);
    }

    public void save() {
        try {
            Files.write(path, List.of(
                String.valueOf(size),
                eTag,
                String.valueOf(lastModified),
                downloadLocation.getFileName().toString()
            ), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            updateAccessTimes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to write cached pack metadata", exception);
        }
    }

    public void updateAccessTimes() {
        FileTime time = FileTime.from(Instant.now());
        try {
            Files.getFileAttributeView(path, BasicFileAttributeView.class).setTimes(null, time, null);
            Files.getFileAttributeView(downloadLocation, BasicFileAttributeView.class).setTimes(null, time, null);
        } catch (IOException exception) {
            GeyserImpl.getInstance().getLogger().debug("Failed to update access times for resource pack metadata " + this);
        }
    }

    public ResourcePackMetadata withSuffixedDownload(String suffix) {
        Path newDownloadLocation = REMOTE_PACK_CACHE.resolve(url.hashCode() + "_" + System.currentTimeMillis() + suffix + ".zip");
        return new ResourcePackMetadata(path, url, size, eTag, lastModified, newDownloadLocation);
    }

    public boolean equalsIgnoreLocation(ResourcePackMetadata other) {
        return other.size == size && other.eTag.equals(eTag) && other.lastModified == lastModified;
    }

    public static ResourcePackMetadata fromHeaders(String url, HttpHeaders headers) {
        long lastModified = headers.firstValue("last-modified")
            .flatMap(string -> {
                try {
                    return Optional.of(ZonedDateTime.parse(string, DateTimeFormatter.RFC_1123_DATE_TIME));
                } catch (DateTimeParseException exception) {
                    return Optional.empty();
                }
            })
            .map(ChronoZonedDateTime::toInstant)
            .stream().mapToLong(Instant::toEpochMilli)
            .findFirst().orElse(0L);
        return new ResourcePackMetadata(url, headers.firstValueAsLong("content-length").orElse(0L),
            headers.firstValue("etag").orElse(""), lastModified,
            REMOTE_PACK_CACHE.resolve(url.hashCode() + "_" + System.currentTimeMillis() + ".zip"));
    }

    public static Optional<ResourcePackMetadata> fromCache(String url) {
        Path packMetadata = getMetadataPath(url);
        if (Files.exists(packMetadata)) {
            List<String> metadata;
            try {
                metadata = Files.readAllLines(packMetadata, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                GeyserImpl.getInstance().getLogger().error("Failed to read cached pack metadata!", exception);
                try {
                    Files.delete(packMetadata);
                } catch (IOException ignored) {}
                return Optional.empty();
            }

            int cachedSize = Integer.parseInt(metadata.get(0));
            String cachedETag = metadata.get(1);
            long cachedLastModified = Long.parseLong(metadata.get(2));
            Path downloadLocation = REMOTE_PACK_CACHE.resolve(metadata.get(3));
            return Optional.of(new ResourcePackMetadata(url, cachedSize, cachedETag, cachedLastModified, downloadLocation));
        }
        return Optional.empty();
    }

    public static void ensureCacheDirectoryExists() throws IOException {
        Files.createDirectories(REMOTE_PACK_CACHE);
    }

    private static Path getMetadataPath(String url) {
        return REMOTE_PACK_CACHE.resolve(url.hashCode() + ".metadata");
    }
}
