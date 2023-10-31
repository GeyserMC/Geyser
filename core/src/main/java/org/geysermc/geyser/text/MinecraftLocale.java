/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.text;

import com.fasterxml.jackson.databind.JsonNode;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.util.AssetUtils;
import org.geysermc.geyser.util.FileUtils;
import org.geysermc.geyser.util.WebUtils;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class MinecraftLocale {

    public static final Map<String, Map<String, String>> LOCALE_MAPPINGS = new HashMap<>();

    static {
        // Create the locales folder
        File localesFolder = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("locales").toFile();
        //noinspection ResultOfMethodCallIgnored
        localesFolder.mkdir();

        // FIXME TEMPORARY
        try {
            Files.delete(localesFolder.toPath().resolve("en_us.hash"));
        } catch (IOException ignored) {
        }
    }

    public static void ensureEN_US() {
        Path localeFile = getPath("en_us");
        AssetUtils.addTask(!Files.exists(localeFile), new AssetUtils.ClientJarTask("assets/minecraft/lang/en_us.json",
                (stream) -> AssetUtils.saveFile(localeFile, stream),
                () -> {
                    if ("en_us".equals(GeyserLocale.getDefaultLocale())) {
                        loadLocale("en_us");
                    }
                }));
    }

    /**
     * Downloads a locale from Mojang if its not already loaded
     *
     * @param locale Locale to download and load
     */
    public static void downloadAndLoadLocale(String locale) {
        locale = locale.toLowerCase(Locale.ROOT);
        if (locale.equals("nb_no")) {
            // Different locale code - https://minecraft.wiki/w/Language
            locale = "no_no";
        }

        // Check the locale isn't already loaded
        if (!AssetUtils.isAssetKnown("minecraft/lang/" + locale + ".json") && !locale.equals("en_us")) {
            if (loadLocale(locale)) {
                GeyserImpl.getInstance().getLogger().debug("Loaded locale locally while not being in asset map: " + locale);
            } else {
                GeyserImpl.getInstance().getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.locale.fail.invalid", locale));
            }
            return;
        }

        GeyserImpl.getInstance().getLogger().debug("Downloading and loading locale: " + locale);

        downloadLocale(locale);
        if (!loadLocale(locale)) {
            GeyserImpl.getInstance().getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.locale.fail.missing", locale));
        }
    }

    /**
     * Downloads the specified locale if its not already downloaded
     *
     * @param locale Locale to download
     */
    private static void downloadLocale(String locale) {
        if (locale.equals("en_us")) {
            return;
        }
        Path localeFile = getPath(locale);

        // Check if we have already downloaded the locale file
        if (Files.exists(localeFile)) {
            String curHash = byteArrayToHexString(FileUtils.calculateSHA1(localeFile));
            String targetHash = AssetUtils.getAsset("minecraft/lang/" + locale + ".json").getHash();

            if (!curHash.equals(targetHash)) {
                GeyserImpl.getInstance().getLogger().debug("Locale out of date; re-downloading: " + locale);
            } else {
                GeyserImpl.getInstance().getLogger().debug("Locale already downloaded and up-to date: " + locale);
                return;
            }
        }

        try {
            // Get the hash and download the locale
            String hash = AssetUtils.getAsset("minecraft/lang/" + locale + ".json").getHash();
            WebUtils.downloadFile("https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash, localeFile.toString());
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().error("Unable to download locale file hash", e);
        }
    }

    private static Path getPath(String locale) {
        return GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("locales/" + locale + ".json");
    }

    /**
     * Loads a locale already downloaded, if the file doesn't exist it just logs a warning
     *
     * @param locale Locale to load
     */
    private static boolean loadLocale(String locale) {
        File localeFile = GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("locales/" + locale + ".json").toFile();

        // Load the locale
        if (localeFile.exists()) {
            // Read the localefile
            InputStream localeStream;
            try {
                localeStream = new FileInputStream(localeFile);
            } catch (FileNotFoundException e) {
                throw new AssertionError(GeyserLocale.getLocaleStringLog("geyser.locale.fail.file", locale, e.getMessage()));
            }

            // Parse the file as json
            JsonNode localeObj;
            try {
                localeObj = GeyserImpl.JSON_MAPPER.readTree(localeStream);
            } catch (Exception e) {
                throw new AssertionError(GeyserLocale.getLocaleStringLog("geyser.locale.fail.json", locale), e);
            }

            // Parse all the locale fields
            Iterator<Map.Entry<String, JsonNode>> localeIterator = localeObj.fields();
            Map<String, String> langMap = new HashMap<>();
            while (localeIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = localeIterator.next();
                langMap.put(entry.getKey(), entry.getValue().asText());
            }

            String bedrockLocale = locale.toLowerCase(Locale.ROOT);
            if (bedrockLocale.equals("no_no")) {
                // Store this locale under the Bedrock locale so we don't need to do this check over and over
                bedrockLocale = "nb_no";
            }

            // Insert the locale into the mappings
            LOCALE_MAPPINGS.put(bedrockLocale, langMap);

            try {
                localeStream.close();
            } catch (IOException e) {
                throw new AssertionError(GeyserLocale.getLocaleStringLog("geyser.locale.fail.file", locale, e.getMessage()));
            }
            return true;
        } else {
            return false;
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
        Map<String, String> localeStrings = LOCALE_MAPPINGS.get(locale.toLowerCase(Locale.ROOT));
        if (localeStrings == null) {
            localeStrings = LOCALE_MAPPINGS.get(GeyserLocale.getDefaultLocale());
            if (localeStrings == null) {
                // Don't cause a NPE if the locale is STILL missing
                GeyserImpl.getInstance().getLogger().debug("MISSING DEFAULT LOCALE: " + GeyserLocale.getDefaultLocale());
                return messageText;
            }
        }

        return localeStrings.getOrDefault(messageText, messageText);
    }

    /**
     * Translate the given language string into the given locale, or returns null.
     *
     * @param messageText Language string to translate
     * @param locale Locale to translate to
     * @return Translated string or null if it was not found in the given locale
     */
    @Nullable
    public static String getLocaleStringIfPresent(String messageText, String locale) {
        Map<String, String> localeStrings = LOCALE_MAPPINGS.get(locale.toLowerCase(Locale.ROOT));
        if (localeStrings != null) {
            return localeStrings.get(messageText);
        }

        return null;
    }

    /**
     * Convert a byte array into a hex string
     *
     * @param b Byte array to convert
     * @return The hex representation of the given byte array
     */
    private static String byteArrayToHexString(byte[] b) {
        StringBuilder result = new StringBuilder();
        for (byte value : b) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}