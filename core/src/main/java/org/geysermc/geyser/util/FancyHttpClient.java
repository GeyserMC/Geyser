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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.pack.bedrock.ResourcePackMetadata;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Wrapper around {@link HttpClient} with helper-methods for making various requests. Adds a {@code User-Agent} header by default, and uses a timeout of 10 seconds.
 *
 * <p>Obtain re-usable instances using {@link FancyHttpClient#open(Executor)} or {@link FancyHttpClient#open()}. Instances for one-time use should be obtained using {@link FancyHttpClient#oneShot(Function)}.</p>
 *
 * <em>Do not use instances with "try-with-resources" statements. This will often lead to the client being closed before the request future completes.</em>
 *
 * @see FancyHttpClient#open(Executor)
 * @see FancyHttpClient#open()
 * @see FancyHttpClient#oneShot(Function)
 */
public final class FancyHttpClient implements AutoCloseable {
    private final HttpClient client;
    private final String userAgent;

    private FancyHttpClient(@Nullable Executor executor) {
        HttpClient.Builder builder = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL);
        if (executor != null) {
            builder.executor(executor);
        }

        this.client = builder.build();
        this.userAgent = "Geyser-" + GeyserImpl.getInstance().platformType().platformName() + "/" + GeyserImpl.VERSION;
    }

    /**
     * Opens a new client using the given {@link Executor} for making requests. The client does not take ownership of the executor: if it is an {@link java.util.concurrent.ExecutorService}, the caller is responsible
     * for closing it.
     *
     * @param executor the {@link Executor} used for making requests
     * @return the new {@link FancyHttpClient}
     */
    public static FancyHttpClient open(Executor executor) {
        return new FancyHttpClient(executor);
    }

    /**
     * Opens a new client using the default {@link Executor} for making requests.
     *
     * @return the new {@link FancyHttpClient}
     */
    public static FancyHttpClient open() {
        return new FancyHttpClient(null);
    }

    /**
     * Opens a new client, and uses the given function for making one or more requests. Once the returned {@link CompletableFuture} completes, the client is automatically closed.
     *
     * @param fetcher the function using the client to make one or more requests
     * @param <T> the type returned at the end of the request pipeline
     * @return the {@link CompletableFuture} the function returned
     */
    public static <T> CompletableFuture<T> oneShot(Function<FancyHttpClient, CompletableFuture<T>> fetcher) {
        // TODO maybe using singleton instance with its own executors?
        FancyHttpClient client = FancyHttpClient.open();
        return fetcher.apply(client).whenComplete(($, $$) -> client.close());
    }

    private <T> CompletableFuture<HttpResponse<T>> fetch(URI uri, UnaryOperator<HttpRequest.Builder> builder, HttpResponse.BodyHandler<T> bodyHandler) {
        return client.sendAsync(builder.apply(HttpRequest.newBuilder(uri)
            .header("User-Agent", userAgent)
            .timeout(Duration.ofSeconds(10L))).build(), bodyHandler);
    }

    private <T> CompletableFuture<T> fetchOrThrow(String uri, UnaryOperator<HttpRequest.Builder> builder, HttpResponse.BodyHandler<T> bodyHandler) {
        URI parsedUri;
        try {
            parsedUri = URI.create(uri);
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
        return fetch(parsedUri, builder, bodyHandler)
            .thenApply(response -> {
                if (isSuccess(response.statusCode())) {
                    return response.body();
                }
                throw new RuntimeException("Unable to make HTTP request to URL %s (returned status code %d)".formatted(response.request().uri(), response.statusCode()));
            });
    }

    /**
     * Makes a GET request to the given URL and parses the response body as a string.
     *
     * <p>If the request failed, the {@link CompletableFuture} completes exceptionally.</p>
     *
     * @param reqURL URL to fetch
     * @return a {@link CompletableFuture} returning the content of the response body, parsed as a string
     */
    public CompletableFuture<String> getBody(String reqURL) {
        return fetchOrThrow(reqURL, HttpRequest.Builder::GET,
            HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Makes a GET request to the given URL, parses the response body as a string, and returns a stream of lines.
     *
     * <p>If the request failed, an empty stream is returned.</p>
     *
     * @param reqURL URL to fetch
     * @return a {@link CompletableFuture} returning the content of the response body, parsed as a string and turned into a stream of lines, or an empty stream if the request fails
     */
    public CompletableFuture<Stream<String>> getLineStream(String reqURL) {
        return getBody(reqURL).thenApply(String::lines).exceptionally(ignored -> Stream.empty());
    }

    /**
     * Makes a GET request to the given URL and parses the response body as a {@link JsonElement}.
     *
     * <p>If the request failed, the {@link CompletableFuture} completes exceptionally.</p>
     *
     * @param reqURL URL to fetch
     * @return a {@link CompletableFuture} returning the content of the response body, parsed as a {@link JsonElement}
     */
    public CompletableFuture<JsonElement> getJson(String reqURL) {
        //noinspection deprecation
        return getBody(reqURL).thenApply(string -> new JsonParser().parse(string));
    }

    /**
     * Makes a GET request to the given URL and parses the response body as a {@link BufferedImage} using {@link ImageIO#read(InputStream)}.
     *
     * <p>If the request failed, the {@link CompletableFuture} completes exceptionally.</p>
     *
     * @param reqURL URL to fetch
     * @return a {@link CompletableFuture} returning the content of the response body, parsed as a {@link BufferedImage}
     */
    public CompletableFuture<BufferedImage> downloadImage(String reqURL) {
        return fetchOrThrow(reqURL, HttpRequest.Builder::GET, HttpResponse.BodyHandlers.ofInputStream())
            .thenApplyAsync(stream -> {
                try {
                    BufferedImage image = ImageIO.read(stream);
                    if (image == null) {
                        throw new IllegalArgumentException("Failed to read image from: %s".formatted(reqURL));
                    }
                    return image;
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
    }

    /**
     * Downloads a file from the given URL and saves it to disk
     *
     * <p>If the request failed, the {@link CompletableFuture} completes exceptionally.</p>
     *
     * @param reqURL file to fetch
     * @param path location to save on disk as a path
     * @return a {@link CompletableFuture} completing when the file is saved
     */
    public CompletableFuture<Path> downloadFile(String reqURL, Path path) {
        return fetchOrThrow(reqURL, HttpRequest.Builder::GET, HttpResponse.BodyHandlers.ofFile(path));
    }

    public CompletableFuture<Optional<Path>> downloadFileSafe(URI uri, Path path, int maxSize) {
        HttpResponse.BodyHandler<Path> downloader = HttpResponse.BodyHandlers.ofFile(path);
        HttpResponse.BodyHandler<Path> handler = response -> {
            if (response.headers().firstValueAsLong("content-length").stream().anyMatch(length -> length < maxSize)) {
                return downloader.apply(response);
            }
            throw new UncheckedIOException(new IOException("content-length header was missing or exceeded maxSize"));
        };
        return fetch(uri, HttpRequest.Builder::GET, handler)
            .thenApply(response -> {
                if (!isSuccess(response.statusCode())) {
                    return Optional.<Path>empty();
                }
                return Optional.of(response.body());
            })
            .exceptionally(throwable -> {
                GeyserImpl.getInstance().getLogger().error("Failed to download file \"" + path + "\" from URL \"" + uri + "\" safely", throwable);
                return Optional.empty();
            });
    }

    /**
     * Checks a remote resourcepack URL to see if it is valid. If it is, it will download the pack file and return a path to it.
     *
     * <p>If the request failed, the {@link CompletableFuture} completes exceptionally.</p>
     *
     * @param url the URL to download from
     * @param force if true, the pack will be downloaded even if it is cached locally
     * @return a {@link CompletableFuture} returning the path to the downloaded pack file
     */
    public CompletableFuture<Path> downloadRemotePack(String url, boolean force) {
        GeyserLogger logger = GeyserImpl.getInstance().getLogger();

        return fetch(URI.create(url), HttpRequest.Builder::HEAD, HttpResponse.BodyHandlers.discarding())
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
                    ResourcePackMetadata.ensureCacheDirectoryExists();
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
                                finalMetadata = metadata.withSuffixedDownload("_unzipped");
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
     * Makes a POST request to the given URL, sending the given string. Parses the response body as a string.
     *
     * <p>If the request failed, the {@link CompletableFuture} completes exceptionally.</p>
     *
     * @param reqURL URL to POST to
     * @param postContent string data to send
     * @return a {@link CompletableFuture} returning the content of the response body, parsed as a string
     */
    public CompletableFuture<String> post(String reqURL, String postContent) {
        return fetchOrThrow(reqURL, builder -> builder.POST(HttpRequest.BodyPublishers.ofString(postContent))
            .header("content-type", "text/plain"), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Makes a POST request to the given URL, sending the given fields formatted as a form. Parses the response body as a string.
     *
     * <p>If the request failed, the {@link CompletableFuture} completes exceptionally.</p>
     *
     * @param reqURL URL to POST to
     * @param fields form data to send
     * @return a {@link CompletableFuture} returning the content of the response body, parsed as a string
     */
    public CompletableFuture<String> postForm(String reqURL, Map<String, String> fields) {
        StringBuilder formString = new StringBuilder();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            formString.append(field.getKey()).append("=").append(URLEncoder.encode(field.getValue(), StandardCharsets.UTF_8)).append("&");
        }
        return fetchOrThrow(reqURL, builder -> builder.POST(HttpRequest.BodyPublishers.ofString(formString.toString()))
            .header("content-type", "application/x-www-form-urlencoded"), HttpResponse.BodyHandlers.ofString());
    }

    @Override
    public void close() {
        client.close();
    }

    private static boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }
}
