/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

import org.checkerframework.checker.nullness.qual.NonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;

import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

public class WebUtils {

    private static final Path REMOTE_PACK_CACHE = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("cache").resolve("remote_packs");

    /**
     * Makes a web request to the given URL and returns the body as a string
     *
     * @param reqURL URL to fetch
     * @return body content or
     * @throws IOException / a wrapped UnknownHostException for nicer errors.
     */
    public static String getBody(String reqURL) throws IOException {
        try {
            URL url = new URL(reqURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", getUserAgent()); // Otherwise Java 8 fails on checking updates
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            checkResponseCode(con);
            return connectionToString(con);
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Unable to resolve requested url (%s)! Are you offline?".formatted(reqURL), e);
        }
    }

    /**
     * Makes a web request to the given URL and returns the body as a {@link JsonObject}.
     *
     * @param reqURL URL to fetch
     * @return the response as JSON
     */
    public static JsonObject getJson(String reqURL) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(reqURL).openConnection();
        con.setRequestProperty("User-Agent", getUserAgent());
        con.setConnectTimeout(10000);
        con.setReadTimeout(10000);
        checkResponseCode(con);
        try (InputStreamReader isr = new InputStreamReader(con.getInputStream());
             JsonReader reader = GeyserImpl.GSON.newJsonReader(isr)) {
            //noinspection deprecation
            return new JsonParser().parse(reader).getAsJsonObject();
        }
    }

    /**
     * Downloads a file from the given URL and saves it to disk
     *
     * @param reqURL File to fetch
     * @param fileLocation Location to save on disk
     */
    public static void downloadFile(String reqURL, String fileLocation) {
        downloadFile(reqURL, Paths.get(fileLocation));
    }

    /**
     * Downloads a file from the given URL and saves it to disk
     *
     * @param reqURL File to fetch
     * @param path Location to save on disk as a path
     */
    public static void downloadFile(String reqURL, Path path) {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(reqURL).openConnection();
            con.setRequestProperty("User-Agent", getUserAgent());
            checkResponseCode(con);
            InputStream in = con.getInputStream();
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException("Unable to download and save file: " + path.toAbsolutePath() + " (" + reqURL + ")", e);
        }
    }

    /**
     * Checks a remote pack URL to see if it is valid
     * If it is, it will download the pack file and return a path to it
     *
     * @param url The URL to check
     * @param force If true, the pack will be downloaded even if it is cached to a separate location.
     * @return Path to the downloaded pack file, or null if it was unable to be loaded
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static @NonNull Path downloadRemotePack(String url, boolean force) throws IOException {
        GeyserLogger logger = GeyserImpl.getInstance().getLogger();
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();

            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);
            con.setRequestProperty("User-Agent", "Geyser-" + GeyserImpl.getInstance().platformType().platformName() + "/" + GeyserImpl.VERSION);
            con.setInstanceFollowRedirects(true);

            int responseCode = con.getResponseCode();
            if (responseCode >= 400) {
                throw new IllegalStateException(String.format("Invalid response code from remote pack at URL: %s (code: %d)", url, responseCode));
            }

            int size = con.getContentLength();
            String type = con.getContentType();

            if (size <= 0) {
                throw new IllegalArgumentException(String.format("Invalid content length received from remote pack at URL: %s (size: %d)", url, size));
            }

            if (type == null || !type.equals("application/zip")) {
                throw new IllegalArgumentException(String.format("Url %s tries to provide a resource pack using the %s content type, which is not supported by Bedrock edition! " +
                    "Bedrock Edition only supports the application/zip content type.", url, type));
            }

            // Ensure remote pack cache dir exists
            Files.createDirectories(REMOTE_PACK_CACHE);

            Path packMetadata = REMOTE_PACK_CACHE.resolve(url.hashCode() + ".metadata");
            Path downloadLocation;

            // If we downloaded this pack before, reuse it if the ETag matches.
            if (Files.exists(packMetadata) && !force) {
                try {
                    List<String> metadata = Files.readAllLines(packMetadata, StandardCharsets.UTF_8);
                    int cachedSize = Integer.parseInt(metadata.get(0));
                    String cachedEtag = metadata.get(1);
                    long cachedLastModified = Long.parseLong(metadata.get(2));
                    downloadLocation = REMOTE_PACK_CACHE.resolve(metadata.get(3));

                    if (cachedSize == size &&
                            cachedEtag.equals(con.getHeaderField("ETag")) &&
                            cachedLastModified == con.getLastModified() &&
                            downloadLocation.toFile().exists()) {
                        logger.debug("Using cached pack (%s) for %s.".formatted(downloadLocation.getFileName(), url));
                        downloadLocation.toFile().setLastModified(System.currentTimeMillis());
                        packMetadata.toFile().setLastModified(System.currentTimeMillis());
                        return downloadLocation;
                    } else {
                        logger.debug("Deleting cached pack/metadata (%s) as it appears to have changed!".formatted(url));
                        Files.deleteIfExists(packMetadata);
                        Files.deleteIfExists(downloadLocation);
                    }
                } catch (IOException e) {
                    GeyserImpl.getInstance().getLogger().error("Failed to read cached pack metadata! " + e);
                    packMetadata.toFile().deleteOnExit();
                }
            }

            downloadLocation = REMOTE_PACK_CACHE.resolve(url.hashCode() + "_" + System.currentTimeMillis() + ".zip");
            Files.copy(con.getInputStream(), downloadLocation, StandardCopyOption.REPLACE_EXISTING);

            // This needs to match as the client fails to download the pack otherwise
            long downloadSize = Files.size(downloadLocation);
            if (downloadSize != size) {
                Files.delete(downloadLocation);
                throw new IllegalStateException("Size mismatch with resource pack at url: %s. Downloaded pack has %s bytes, expected %s bytes!"
                        .formatted(url, downloadSize, size));
            }

            try {
                boolean shouldDeleteEnclosing = false;
                var originalZip = downloadLocation;
                try (ZipFile zip = new ZipFile(downloadLocation.toFile())) {
                    // This can (or should???) contain a zip
                    if (zip.stream().allMatch(name -> name.getName().endsWith(".zip"))) {
                        // Unzip the pack, as that's what we're after
                        downloadLocation = REMOTE_PACK_CACHE.resolve(url.hashCode() + "_" + System.currentTimeMillis() + "_unzipped.zip");
                        Files.copy(zip.getInputStream(zip.entries().nextElement()), downloadLocation, StandardCopyOption.REPLACE_EXISTING);
                        shouldDeleteEnclosing = true;
                    }
                } finally {
                    if (shouldDeleteEnclosing) {
                        // We don't need the original zip anymore
                        Files.delete(originalZip);
                    }
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Encountered exception while reading downloaded resource pack at url: %s".formatted(url), e);
            }

            try {
                Files.write(
                        packMetadata,
                        Arrays.asList(
                                String.valueOf(size),
                                con.getHeaderField("ETag"),
                                String.valueOf(con.getLastModified()),
                                downloadLocation.getFileName().toString()
                        ),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
                packMetadata.toFile().setLastModified(System.currentTimeMillis());
            } catch (IOException e) {
                Files.delete(packMetadata);
                Files.delete(downloadLocation);
                throw new IllegalStateException("Failed to write cached pack metadata: " + e.getMessage());
            }

            downloadLocation.toFile().setLastModified(System.currentTimeMillis());
            logger.debug("Successfully downloaded remote pack! URL: %s (to: %s )".formatted(url, downloadLocation));
            return downloadLocation;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Unable to download resource pack from malformed URL %s".formatted(url));
        } catch (SocketTimeoutException | ConnectException e) {
            logger.debug(e);
            throw new IllegalArgumentException("Unable to download pack from url %s due to network error ( %s )".formatted(url, e.toString()));
        }
    }


    /**
     * Post a string to the given URL
     *
     * @param reqURL URL to post to
     * @param postContent String data to post
     * @return String returned by the server
     * @throws IOException If the request fails
     */
    public static String post(String reqURL, String postContent) throws IOException {
        URL url = new URL(reqURL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "text/plain");
        con.setRequestProperty("User-Agent", getUserAgent());
        con.setDoOutput(true);

        OutputStream out = con.getOutputStream();
        out.write(postContent.getBytes(StandardCharsets.UTF_8));
        out.close();

        return connectionToString(con);
    }

    /**
     * Gets the string output from the passed {@link HttpURLConnection},
     * or logs the error message.
     */
    private static String connectionToString(HttpURLConnection con) throws IOException {
        checkResponseCode(con);
        return inputStreamToString(con.getInputStream(), con::disconnect);
    }

    /**
     * Throws an exception if there is an error stream to avoid further issues
     */
    private static void checkResponseCode(HttpURLConnection con) throws IOException {
        // Send the request (we dont use this but its required for getErrorStream() to work)
        con.getResponseCode();

        // Read the error message if there is one if not just read normally
        InputStream errorStream = con.getErrorStream();
        if (errorStream != null) {
            throw new IOException(inputStreamToString(errorStream, null));
        }
    }

    /**
     * Get the string output from the passed {@link InputStream}
     *
     * @param stream The input stream to get the string from
     * @return The body of the returned page
     * @throws IOException If the request fails
     */
    private static String inputStreamToString(InputStream stream, @Nullable Runnable onFinish) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream))) {
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
                content.append("\n");
            }

            if (onFinish != null) {
                onFinish.run();
            }
        }

        return content.toString();
    }

    /**
     * Post fields to a URL as a form
     *
     * @param reqURL URL to post to
     * @param fields Form data to post
     * @return String returned by the server
     * @throws IOException If the request fails
     */
    public static String postForm(String reqURL, Map<String, String> fields) throws IOException {
        URL url = new URL(reqURL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("User-Agent", getUserAgent());
        con.setDoOutput(true);

        try (OutputStream out = con.getOutputStream()) {
            // Write the form data to the output
            for (Map.Entry<String, String> field : fields.entrySet()) {
                out.write((field.getKey() + "=" + URLEncoder.encode(field.getValue(), StandardCharsets.UTF_8) + "&").getBytes(StandardCharsets.UTF_8));
            }
        }

        return connectionToString(con);
    }

    /**
     * Find a SRV record for the given address
     *
     * @param geyser Geyser instance
     * @param remoteAddress Address to find the SRV record for
     * @return The SRV record or null if not found
     */
    public static String @Nullable [] findSrvRecord(GeyserImpl geyser, String remoteAddress) {
        try {
            // Searches for a server address and a port from a SRV record of the specified host name
            InitialDirContext ctx = new InitialDirContext();
            Attribute attr = ctx.getAttributes("dns:///_minecraft._tcp." + remoteAddress, new String[]{"SRV"}).get("SRV");
            // size > 0 = SRV entry found
            if (attr != null && attr.size() > 0) {
                return ((String) attr.get(0)).split(" ");
            }
        } catch (Exception | NoClassDefFoundError ex) { // Check for a NoClassDefFoundError to prevent Android crashes
            if (geyser.config().debugMode()) {
                geyser.getLogger().debug("Exception while trying to find an SRV record for the remote host.");
                ex.printStackTrace(); // Otherwise we can get a stack trace for any domain that doesn't have an SRV record
            }
        }
        return null;
    }

    /**
     * Get a stream of lines from the given URL
     *
     * @param reqURL URL to fetch
     * @return Stream of lines from the URL or an empty stream if the request fails
     */
    public static Stream<String> getLineStream(String reqURL) {
        try {
            URL url = new URL(reqURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", getUserAgent()); // Otherwise Java 8 fails on checking updates
            con.setConnectTimeout(10000);
            con.setReadTimeout(10000);

            return connectionToString(con).lines();
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().error("Error while trying to get a stream from " + reqURL, e);
            return Stream.empty();
        }
    }

    public static String getUserAgent() {
        return "Geyser-" + GeyserImpl.getInstance().platformType().platformName() + "/" + GeyserImpl.VERSION;
    }

    public static String toHttps(String url) {
        if (url != null && url.startsWith("http://")) {
            return "https://" + url.substring(7);
        }
        return url;
    }
}
