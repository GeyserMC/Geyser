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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.geysermc.connector.GeyserConnector;

import java.io.*;
import java.time.OffsetDateTime;
import java.util.*;

public class LocaleUtils {

    public static final Map<String, Map<String, String>> LOCALE_MAPPINGS = new HashMap<>();

    private static final Map<String, Asset> ASSET_MAP = new HashMap<>();

    static {
        // Create the locales folder
        File localesFolder = new File("locales/");
        localesFolder.mkdir();

        // Download the latest asset list and cache it
        generateAssetCache();
        downloadAndLoadLocale(GeyserConnector.getInstance().getConfig().getDefaultLocale());
    }

    private static void generateAssetCache() {
        try {
            VersionManifest versionManifest = Toolbox.JSON_MAPPER.readValue(WebUtils.getBody("https://launchermeta.mojang.com/mc/game/version_manifest.json"), VersionManifest.class);
            String latestInfoURL = "";
            for (Version version : versionManifest.getVersions()) {
                if (version.getId().equals(versionManifest.getLatestVersion().getRelease())) {
                    latestInfoURL = version.getUrl();
                    break;
                }
            }

            if (latestInfoURL.isEmpty()) {
                throw new Exception("Unable to get latest Minecraft version");
            }

            VersionInfo versionInfo = Toolbox.JSON_MAPPER.readValue(WebUtils.getBody(latestInfoURL), VersionInfo.class);
            JsonNode assets = Toolbox.JSON_MAPPER.readTree(WebUtils.getBody(versionInfo.getAssetIndex().getUrl())).get("objects");

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

    public static void downloadAndLoadLocale(String locale) {
        locale = locale.toLowerCase();
        if (!ASSET_MAP.containsKey("minecraft/lang/" + locale + ".json") && !locale.equals("en_us")) {
            GeyserConnector.getInstance().getLogger().warning("Invalid locale requested to download and load: " + locale);
            return;
        }

        GeyserConnector.getInstance().getLogger().debug("Downloading and loading locale: " + locale);

        downloadLocale(locale);
        loadLocale(locale);
    }

    private static void downloadLocale(String locale) {
        File localeFile = new File("locales/" + locale + ".json");

        if (localeFile.exists()) {
            GeyserConnector.getInstance().getLogger().debug("Locale already downloaded: " + locale);
            return;
        }

        // Create the en_us locale
        if (locale.equals("en_us")) {
            try {
                InputStreamReader isReader = new InputStreamReader(Toolbox.getResource("mappings/lang/en_us.json"));
                BufferedReader reader = new BufferedReader(isReader);
                StringBuffer sb = new StringBuffer();
                String str;
                while((str = reader.readLine())!= null){
                    sb.append(str);
                }

                FileUtils.writeFile(localeFile, sb.toString().toCharArray());
            } catch (Exception e) {
                throw new AssertionError("Unable to load en_us locale!", e);
            }

            return;
        }

        String hash = ASSET_MAP.get("minecraft/lang/" + locale + ".json").getHash();

        try {
            FileUtils.writeFile("locales/" + locale + ".json", WebUtils.getBody("http://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash).toCharArray());
        } catch (Exception e) {
            GeyserConnector.getInstance().getLogger().warning("Failed to download locale " + locale + ": " + (!e.getMessage().isEmpty() ? e.getMessage() : e.getStackTrace()));
        }
    }

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
                throw new AssertionError("Unable to load Java lang map for " + locale, e);
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

    public static String getLocaleString(String messageText, String locale) {
        Map<String, String> localeStrings = LocaleUtils.LOCALE_MAPPINGS.get(locale.toLowerCase());
        if (localeStrings == null)
            localeStrings = LocaleUtils.LOCALE_MAPPINGS.get(GeyserConnector.getInstance().getConfig().getDefaultLocale());

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
