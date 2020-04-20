/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import org.geysermc.connector.GeyserConnector;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipFile;

public class LocaleUtils {

    public static final Map<String, Map<String, String>> LOCALE_MAPPINGS = new HashMap<>();

    private static final Map<String, Asset> ASSET_MAP = new HashMap<>();

    private static final String DEFAULT_LOCALE = (GeyserConnector.getInstance().getConfig().getDefaultLocale() != null ? GeyserConnector.getInstance().getConfig().getDefaultLocale() : "en_us");

    private static String smallestURL = "";

    static {
        // Create the locales folder
        File localesFolder = new File("locales/");
        localesFolder.mkdir();

        // Download the latest asset list and cache it
        generateAssetCache();
        downloadAndLoadLocale(DEFAULT_LOCALE);
    }

    /**
     * Fetch the latest versions asset cache from Mojang so we can grab the locale files later
     */
    private static void generateAssetCache() {
        try {
            // Get the version manifest from Mojang
            VersionManifest versionManifest = Toolbox.JSON_MAPPER.readValue(WebUtils.getBody("https://launchermeta.mojang.com/mc/game/version_manifest.json"), VersionManifest.class);

            // Get the url for the latest version of the games manifest
            String latestInfoURL = "";
            for (Version version : versionManifest.getVersions()) {
                if (version.getId().equals(versionManifest.getLatestVersion().getRelease())) {
                    latestInfoURL = version.getUrl();
                    break;
                }
            }

            // Make sure we definitely got a version
            if (latestInfoURL.isEmpty()) {
                throw new Exception("Unable to get latest Minecraft version");
            }

            // Get the individual version manifest
            VersionInfo versionInfo = Toolbox.JSON_MAPPER.readValue(WebUtils.getBody(latestInfoURL), VersionInfo.class);

            // Get the smallest jar for use when downloading the en_us locale, will be either the server or client
            int currentSize = Integer.MAX_VALUE;
            for (VersionDownload download : versionInfo.getDownloads().values()) {
                if (download.getUrl().endsWith(".jar") && download.getSize() < currentSize) {
                    smallestURL = download.getUrl();
                    currentSize = download.getSize();
                }
            }

            // Get the assets list
            JsonNode assets = Toolbox.JSON_MAPPER.readTree(WebUtils.getBody(versionInfo.getAssetIndex().getUrl())).get("objects");

            // Put each asset into an array for use later
            Iterator<Map.Entry<String, JsonNode>> assetIterator = assets.fields();
            while (assetIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = assetIterator.next();
                Asset asset = Toolbox.JSON_MAPPER.treeToValue(entry.getValue(), Asset.class);
                ASSET_MAP.put(entry.getKey(), asset);
            }
        } catch (Exception e) {
            GeyserConnector.getInstance().getLogger().info("Failed to load locale asset cache: " + (!e.getMessage().isEmpty() ? e.getMessage() : e.getStackTrace()));
        }
    }

    /**
     * Downloads a locale from Mojang if its not already loaded
     *
     * @param locale Locale to download and load
     */
    public static void downloadAndLoadLocale(String locale) {
        locale = locale.toLowerCase();

        // Check the locale isn't already loaded
        if (!ASSET_MAP.containsKey("minecraft/lang/" + locale + ".json") && !locale.equals("en_us")) {
            GeyserConnector.getInstance().getLogger().warning("Invalid locale requested to download and load: " + locale);
            return;
        }

        GeyserConnector.getInstance().getLogger().debug("Downloading and loading locale: " + locale);

        downloadLocale(locale);
        loadLocale(locale);
    }

    /**
     * Downloads the specified locale if its not already downloaded
     *
     * @param locale Locale to download
     */
    private static void downloadLocale(String locale) {
        File localeFile = new File("locales/" + locale + ".json");

        // Check if we have already downloaded the locale file
        if (localeFile.exists()) {
            GeyserConnector.getInstance().getLogger().debug("Locale already downloaded: " + locale);
            return;
        }

        // Create the en_us locale
        if (locale.equals("en_us")) {
            downloadEN_US(localeFile);

            return;
        }

        // Get the hash and download the locale
        String hash = ASSET_MAP.get("minecraft/lang/" + locale + ".json").getHash();
        WebUtils.downloadFile("http://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash, "locales/" + locale + ".json");
    }

    /**
     * Loads a locale already downloaded, if the file doesn't exist it just logs a warning
     *
     * @param locale Locale to load
     */
    private static void loadLocale(String locale) {
        File localeFile = new File("locales/" + locale + ".json");

        // Load the locale
        if (localeFile.exists()) {
            // Read the localefile
            InputStream localeStream;
            try {
                localeStream = new FileInputStream(localeFile);
            } catch (FileNotFoundException e) {
                throw new AssertionError("Unable to load locale: " + locale + " (" + e.getMessage() + ")");
            }

            // Parse the file as json
            JsonNode localeObj;
            try {
                localeObj = Toolbox.JSON_MAPPER.readTree(localeStream);
            } catch (Exception e) {
                throw new AssertionError("Unable to load Java edition lang map for " + locale, e);
            }

            // Parse all the locale fields
            Iterator<Map.Entry<String, JsonNode>> localeIterator = localeObj.fields();
            Map<String, String> langMap = new HashMap<>();
            while (localeIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = localeIterator.next();
                langMap.put(entry.getKey(), entry.getValue().asText());
            }

            // Insert the locale into the mappings
            LOCALE_MAPPINGS.put(locale.toLowerCase(), langMap);
        } else {
            GeyserConnector.getInstance().getLogger().warning("Missing locale file: " + locale);
        }
    }

    /**
     * Download then en_us locale by downloading the server jar and extracting it from there.
     *
     * @param localeFile File to save the locale to
     */
    private static void downloadEN_US(File localeFile) {
        try {
            // Let the user know we are downloading the JAR
            GeyserConnector.getInstance().getLogger().info("Downloading Minecraft JAR to extract en_us locale, please wait... (this may take some time depending on the speed of your internet connection)");
            GeyserConnector.getInstance().getLogger().debug("Download URL: " + smallestURL);

            // Download the smallest JAR (client or server)
            WebUtils.downloadFile(smallestURL, "tmp_locale.jar");

            // Load in the JAR as a zip and extract the file
            ZipFile localeJar = new ZipFile("tmp_locale.jar");
            InputStream inputStream = localeJar.getInputStream(localeJar.getEntry("assets/minecraft/lang/en_us.json"));
            FileOutputStream outputStream = new FileOutputStream(localeFile);

            // Write the file to the locale dir
            int data = inputStream.read();
            while(data != -1){
                outputStream.write(data);
                data = inputStream.read();
            }

            // Flush all changes to disk and cleanup
            outputStream.flush();
            outputStream.close();

            inputStream.close();
            localeJar.close();

            // Delete the nolonger needed client/server jar
            Files.delete(Paths.get("tmp_locale.jar"));
        } catch (Exception e) {
            throw new AssertionError("Unable to download and extract en_us locale!", e);
        }
    }

    /**
     * Translate the given language string into the given locale, or falls back to the default locale
     *
     * @param messageText Language string to translate
     * @param locale Locale to translate to
     * @return Translated string or the original message if it was not found in the given locale
     */
    public static String getLocaleString(String messageText, String locale) {
        Map<String, String> localeStrings = LocaleUtils.LOCALE_MAPPINGS.get(locale.toLowerCase());
        if (localeStrings == null)
            localeStrings = LocaleUtils.LOCALE_MAPPINGS.get(DEFAULT_LOCALE);

        return localeStrings.getOrDefault(messageText, messageText);
    }

    public static void init() {
        // no-op
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
class VersionManifest {
    @JsonProperty("latest")
    private LatestVersion latestVersion;

    @JsonProperty("versions")
    private List<Version> versions;
}

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
class LatestVersion {
    @JsonProperty("release")
    private String release;

    @JsonProperty("snapshot")
    private String snapshot;
}

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
class Version {
    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("url")
    private String url;

    @JsonProperty("time")
    private String time;

    @JsonProperty("releaseTime")
    private String releaseTime;
}

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
class VersionInfo {
    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("time")
    private String time;

    @JsonProperty("releaseTime")
    private String releaseTime;

    @JsonProperty("assetIndex")
    private AssetIndex assetIndex;

    @JsonProperty("downloads")
    private Map<String, VersionDownload> downloads;
}

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
class VersionDownload {
    @JsonProperty("sha1")
    private String sha1;

    @JsonProperty("size")
    private int size;

    @JsonProperty("url")
    private String url;
}

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
class AssetIndex {
    @JsonProperty("id")
    private String id;

    @JsonProperty("sha1")
    private String sha1;

    @JsonProperty("size")
    private int size;

    @JsonProperty("totalSize")
    private int totalSize;

    @JsonProperty("url")
    private String url;
}

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
class Asset {
    @JsonProperty("hash")
    private String hash;

    @JsonProperty("size")
    private int size;
}
