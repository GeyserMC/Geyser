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

    static {
        /* Load the language mappings */
        InputStream stream = Toolbox.getResource("mappings/locales.json");
        JsonNode locales;
        try {
            locales = Toolbox.JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java locale list", e);
        }

        File localesFolder = new File("locales/");

        if (!localesFolder.exists()) {
            GeyserConnector.getInstance().getLogger().info("Locales not cached, downloading... (this may take some time depending on your internet connection)");
            ObjectMapper mapper = new ObjectMapper();
            try {
                VersionManifest versionManifest = mapper.readValue(WebUtils.getBody("https://launchermeta.mojang.com/mc/game/version_manifest.json"), VersionManifest.class);
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

                VersionInfo versionInfo = mapper.readValue(WebUtils.getBody(latestInfoURL), VersionInfo.class);
                JsonNode assets = mapper.readTree(WebUtils.getBody(versionInfo.getAssetIndex().getUrl())).get("objects");

                localesFolder.mkdir();

                for (JsonNode localeNode : locales.get("locales")) {
                    String currentLocale = localeNode.asText();

                    if (currentLocale.equals("en_us")) { continue; }

                    GeyserConnector.getInstance().getLogger().info("Downloading locale: " + currentLocale);
                    Asset asset = mapper.treeToValue(assets.get("minecraft/lang/" + currentLocale + ".json"), Asset.class);
                    String hash = asset.getHash();
                    FileUtils.writeFile("locales/" + currentLocale + ".json", WebUtils.getBody("http://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash).toCharArray());
                }
            } catch (Exception e) {
                GeyserConnector.getInstance().getLogger().info("Failed to load locales: " + (!e.getMessage().isEmpty() ? e.getMessage() : e.getStackTrace()));
            }
        }

        if (localesFolder.exists()) {
            for (JsonNode localeNode : locales.get("locales")) {
                String currentLocale = localeNode.asText();
                loadLocale(currentLocale);
            }
        }
    }

    public static void downloadAndLoadLocale(String locale) {
        downloadLocale(locale);
        loadLocale(locale);
    }

    private static void downloadLocale(String locale) {

    }

    private static void loadLocale(String locale) {
        File localeFile = new File("locales/" + locale + ".json");

        // Create the en_us locale
        if (!localeFile.exists() && locale.equals("en_us")) {
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
        }

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
            JsonNode locale;
            try {
                locale = Toolbox.JSON_MAPPER.readTree(localeStream);
            } catch (Exception e) {
                throw new AssertionError("Unable to load Java lang map for " + locale, e);
            }

            // Parse all the locale fields
            Iterator<Map.Entry<String, JsonNode>> localeIterator = locale.fields();
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
