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

package org.geysermc.geyser.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.pack.ResourcePackMetadata;

import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public final class FancyHttpClient implements AutoCloseable {
    private static final Path REMOTE_PACK_CACHE = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache").resolve("remote_packs");

    private final @Nullable ExecutorService executorService;
    private final HttpClient client;
    private final String userAgent;

    private FancyHttpClient(@Nullable ExecutorService executorService) {
        HttpClient.Builder builder = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL);
        if (executorService != null) {
            builder.executor(executorService);
        }

        this.executorService = executorService;
        this.client = builder.build();
        this.userAgent = "Geyser-" + GeyserImpl.getInstance().platformType().platformName() + "/" + GeyserImpl.VERSION;
    }

    public static FancyHttpClient open(Supplier<ExecutorService> executorSupplier) {
        return new FancyHttpClient(executorSupplier.get());
    }

    public static FancyHttpClient open() {
        return new FancyHttpClient(null);
    }

    private <T> CompletableFuture<HttpResponse<T>> fetch(String uri, UnaryOperator<HttpRequest.Builder> builder, HttpResponse.BodyHandler<T> bodyHandler) {
        return client.sendAsync(builder.apply(HttpRequest.newBuilder(URI.create(uri))
            .header("User-Agent", userAgent)
            .timeout(Duration.ofSeconds(10L))).build(), bodyHandler);
    }

    private <T> CompletableFuture<T> fetchOrThrow(String uri, UnaryOperator<HttpRequest.Builder> builder, HttpResponse.BodyHandler<T> bodyHandler) {
        return fetch(uri, builder, bodyHandler)
            .thenApply(response -> {
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    return response.body();
                }
                throw new RuntimeException("Unable to make HTTP request to URL %s (returned status code %d)".formatted(response.request().uri(), response.statusCode()));
            });
    }

    /**
     * Makes a web request to the given URL and returns the body as a string
     *
     * @param reqURL URL to fetch
     * @return body content
     * @throws RuntimeException in returned future when the request failed
     */
    public CompletableFuture<String> getBody(String reqURL) {
        return fetchOrThrow(reqURL, HttpRequest.Builder::GET,
            HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Makes a web request to the given URL and returns the body as a {@link JsonObject}.
     *
     * @param reqURL URL to fetch
     * @return the response as JSON
     * @throws RuntimeException in returned future when the request failed
     */
    public CompletableFuture<JsonObject> getJson(String reqURL) {
        //noinspection deprecation
        return getBody(reqURL).thenApply(string -> new JsonParser().parse(string).getAsJsonObject());
    }

    /**
     * Downloads a file from the given URL and saves it to disk
     *
     * @param reqURL File to fetch
     * @param fileLocation Location to save on disk
     */
    @Deprecated
    public CompletableFuture<?> downloadFile(String reqURL, String fileLocation) {
        return downloadFile(reqURL, Paths.get(fileLocation));
    }

    /**
     * Downloads a file from the given URL and saves it to disk
     *
     * @param reqURL File to fetch
     * @param path Location to save on disk as a path
     */
    public CompletableFuture<?> downloadFile(String reqURL, Path path) {
        return fetchOrThrow(reqURL, HttpRequest.Builder::GET, HttpResponse.BodyHandlers.ofFile(path));
    }

    /**
     * Checks a remote pack URL to see if it is valid
     * If it is, it will download the pack file and return a path to it
     *
     * @param url The URL to check
     * @param force If true, the pack will be downloaded even if it is cached to a separate location.
     * @return Path to the downloaded pack file, or null if it was unable to be loaded
     */
    public CompletableFuture<Path> downloadRemotePack(String url, boolean force) {
        GeyserLogger logger = GeyserImpl.getInstance().getLogger();

        return fetch(url, HttpRequest.Builder::HEAD, HttpResponse.BodyHandlers.discarding())
            .thenComposeAsync(headerResponse -> {
                if (!isSuccess(headerResponse.statusCode())) {
                    throw new IllegalStateException(String.format("Invalid response code from remote pack at URL: %s (code: %d)", url, headerResponse.statusCode()));
                }

                HttpHeaders headers = headerResponse.headers();
                ResourcePackMetadata headerMetadata = ResourcePackMetadata.fromHeaders(url, headers);
                Optional<String> type = headers.firstValue("content-type");

                if (headerMetadata.size() <= 0L) {
                    throw new IllegalArgumentException(String.format("Invalid content length received from remote pack at URL: %s (size: %d)", url, headerMetadata.size()));
                }

                if (type.isEmpty() || !type.get().equals("application/zip")) {
                    throw new IllegalArgumentException(String.format("Url %s tries to provide a resource pack using the %s content type, which is not supported by Bedrock edition! " +
                        "Bedrock Edition only supports the application/zip content type.", url, type.orElse(null)));
                }

                if (!force) {
                    Optional<ResourcePackMetadata> cachedMetadata = ResourcePackMetadata.fromCache(url)
                        .filter(data -> data.equalsIgnoreLocation(headerMetadata));
                    if (cachedMetadata.isPresent()) {
                        logger.debug("Using cached pack (%s) for %s.".formatted(cachedMetadata.get().downloadLocation().getFileName(), url));
                        cachedMetadata.get().updateAccessTimes();
                        return CompletableFuture.completedFuture(cachedMetadata.get().downloadLocation());
                    }
                }

                try {
                    // Ensure remote pack cache dir exists
                    Files.createDirectories(REMOTE_PACK_CACHE);
                } catch (IOException exception) {
                    throw new UncheckedIOException("Failed to create remote pack cache directory!", exception);
                }

                return actuallyDownloadRemotePack(headerMetadata);
            });
    }

    private CompletableFuture<Path> actuallyDownloadRemotePack(ResourcePackMetadata metadata) {
        return fetchOrThrow(metadata.url(), HttpRequest.Builder::GET, HttpResponse.BodyHandlers.ofFile(metadata.downloadLocation()))
            .thenApplyAsync(downloadLocation -> {
                try {
                    // This needs to match as the client fails to download the pack otherwise
                    long downloadSize = Files.size(downloadLocation);
                    if (downloadSize != metadata.size()) {
                        Files.delete(downloadLocation);
                        throw new IllegalStateException("Size mismatch with resource pack at url: %s. Downloaded pack has %s bytes, expected %s bytes!".formatted(metadata.url(), downloadSize, metadata.size()));
                    }

                    boolean shouldDeleteEnclosing = false;
                    ResourcePackMetadata finalMetadata = metadata;

                    try (FileSystem openedPack = FileSystems.newFileSystem(downloadLocation)) {
                        try (Stream<Path> zipStream = Files.list(openedPack.getPath("/"))
                            .filter(path -> path.endsWith(".zip"))) {
                            List<Path> zipsInZip = zipStream.toList();
                            if (zipsInZip.size() == 1) {
                                finalMetadata = metadata.withDownloadLocation(original -> original.getParent().resolve(metadata.url().hashCode() + "_" + System.currentTimeMillis() + "_unzipped.zip"));
                                Files.copy(zipsInZip.getFirst(), finalMetadata.downloadLocation());
                                shouldDeleteEnclosing = true;
                            }
                        }
                    } finally {
                        if (shouldDeleteEnclosing) {
                            // We don't need the original zip anymore
                            Files.delete(downloadLocation);
                        }
                    }

                    // From here on, use finalMetadata#downloadLocation instead
                    finalMetadata.save();
                    GeyserImpl.getInstance().getLogger().debug("Successfully downloaded remote pack! URL: %s (to: %s )".formatted(finalMetadata.url(), finalMetadata.downloadLocation()));
                    return finalMetadata.downloadLocation();
                } catch (IOException exception) {
                    throw new UncheckedIOException("Encountered exception while reading downloaded resource pack at url: %s".formatted(metadata.url()), exception);
                }
            });
    }


    /**
     * Post a string to the given URL
     *
     * @param reqURL URL to post to
     * @param postContent String data to post
     * @return String returned by the server
     * @throws IOException If the request fails
     */
    public CompletableFuture<String> post(String reqURL, String postContent) throws IOException {
        return fetchOrThrow(reqURL, builder -> builder.POST(HttpRequest.BodyPublishers.ofString(postContent)), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Post fields to a URL as a form
     *
     * @param reqURL URL to post to
     * @param fields Form data to post
     * @return String returned by the server
     * @throws IOException If the request fails
     */
    public CompletableFuture<String> postForm(String reqURL, Map<String, String> fields) throws IOException {
        StringBuilder formString = new StringBuilder();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            formString.append(field.getKey()).append("=").append(URLEncoder.encode(field.getValue(), StandardCharsets.UTF_8)).append("&");
        }
        return fetchOrThrow(reqURL, builder -> builder.POST(HttpRequest.BodyPublishers.ofString(formString.toString())), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Get a stream of lines from the given URL
     *
     * @param reqURL URL to fetch
     * @return Stream of lines from the URL or an empty stream if the request fails
     */
    public CompletableFuture<Stream<String>> getLineStream(String reqURL) {
        return getBody(reqURL).thenApply(String::lines);
    }

    @Override
    public void close() throws Exception {
        client.close();
        if (executorService != null) {
            executorService.close();
        }
    }

    private static boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
}
