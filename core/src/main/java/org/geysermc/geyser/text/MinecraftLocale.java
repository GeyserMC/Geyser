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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.util.AssetUtils;
import org.geysermc.geyser.util.FileUtils;
import org.geysermc.geyser.util.JsonUtils;
import org.geysermc.geyser.util.WebUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MinecraftLocale {

    public static final Map<String, Map<String, String>> LOCALE_MAPPINGS = new HashMap<>();

    
    private static final boolean IN_INSTANCE = GeyserImpl.getInstance() != null;

    private static final Path LOCALE_FOLDER = (IN_INSTANCE) ? GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("locales") : null;

    static {
        if (IN_INSTANCE) {
            try {
                
                Files.createDirectories(LOCALE_FOLDER);
                Files.createDirectories(LOCALE_FOLDER.resolve("overrides"));
            } catch (IOException exception) {
                throw new RuntimeException("Unable to create locale folders! " + exception.getMessage());
            }
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

    
    public static void downloadAndLoadLocale(String locale) {
        locale = locale.toLowerCase(Locale.ROOT);

        if (isLocaleLoaded(locale)) {
            GeyserImpl.getInstance().getLogger().debug("Locale already loaded: " + locale);
            return;
        }

        if (locale.equals("nb_no")) {
            
            locale = "no_no";
        }

        
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

    
    private static void downloadLocale(String locale) {
        if (locale.equals("en_us")) {
            return;
        }
        Path localeFile = getPath(locale);

        
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
            
            String hash = AssetUtils.getAsset("minecraft/lang/" + locale + ".json").getHash();
            WebUtils.downloadFile("https://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash, localeFile.toString());
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().error("Unable to download locale file hash", e);
        }
    }

    private static Path getPath(String locale) {
        return LOCALE_FOLDER.resolve(locale + ".json");
    }

    
    private static boolean loadLocale(String locale) {
        String lowercaseLocale = locale.toLowerCase(Locale.ROOT);

        
        Path localeFile = getPath(lowercaseLocale);
        Path localeOverride = getPath("overrides/" + lowercaseLocale);

        if (lowercaseLocale.equals("no_no")) {
            
            lowercaseLocale = "nb_no";
        }

        Map<String, String> langMap = new HashMap<>();
        if (Files.exists(localeFile) && Files.isReadable(localeFile)) {
            langMap.putAll(parseLangFile(localeFile, lowercaseLocale));
        }

        
        if (Files.exists(localeOverride) && Files.isReadable(localeOverride)) {
            langMap.putAll(parseLangFile(localeOverride, lowercaseLocale));
        }

        if (!langMap.isEmpty()) {
            LOCALE_MAPPINGS.put(lowercaseLocale, langMap);
            return true;
        } else {
            return false;
        }
    }

    
    public static Map<String, String> parseLangFile(Path localeFile, String locale) {
        
        try (InputStream localeStream = Files.newInputStream(localeFile, StandardOpenOption.READ)) {
            
            JsonObject localeObj = JsonUtils.fromJson(localeStream);

            
            Map<String, String> langMap = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : localeObj.entrySet()) {
                langMap.put(entry.getKey(), entry.getValue().getAsString());
            }
            return langMap;
        } catch (FileNotFoundException e){
            throw new AssertionError(GeyserLocale.getLocaleStringLog("geyser.locale.fail.file", locale, e.getMessage()));
        } catch (Exception e) {
            throw new AssertionError(GeyserLocale.getLocaleStringLog("geyser.locale.fail.json", locale), e);
        }
    }

    
    public static String getLocaleString(String messageText, String locale) {
        Map<String, String> localeStrings = LOCALE_MAPPINGS.get(locale.toLowerCase(Locale.ROOT));
        if (localeStrings == null) {
            localeStrings = LOCALE_MAPPINGS.get(GeyserLocale.getDefaultLocale());
            if (localeStrings == null) {
                
                GeyserImpl.getInstance().getLogger().debug("MISSING DEFAULT LOCALE: " + GeyserLocale.getDefaultLocale());
                return messageText;
            }
        }

        return localeStrings.getOrDefault(messageText, messageText);
    }

    
    public static @Nullable String getLocaleStringIfPresent(String messageText, String locale) {
        Map<String, String> localeStrings = LOCALE_MAPPINGS.get(locale.toLowerCase(Locale.ROOT));
        if (localeStrings != null) {
            return localeStrings.get(messageText);
        }

        return null;
    }

    
    public static boolean isLocaleLoaded(String locale) {
        return LOCALE_MAPPINGS.containsKey(locale.toLowerCase(Locale.ROOT));
    }

    
    private static String byteArrayToHexString(byte[] b) {
        StringBuilder result = new StringBuilder();
        for (byte value : b) {
            result.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}
