/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import org.geysermc.connector.GeyserConnector;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class LanguageUtils {

    /**
     * If we determine the locale that the user wishes to use, use that locale
     */
    private static String CACHED_LOCALE;

    private static final Map<String, Properties> LOCALE_MAPPINGS = new HashMap<>();

    static {
        // Load it as a backup in case something goes really wrong
        if (!"en_US".equals(formatLocale(getDefaultLocale()))) { // getDefaultLocale() loads the locale automatically
            loadGeyserLocale("en_US");
        }
    }

    /**
     * Loads a Geyser locale from resources, if the file doesn't exist it just logs a warning
     *
     * @param locale Locale to load
     */
    public static void loadGeyserLocale(String locale) {
        locale = formatLocale(locale);
        // Don't load the locale if it's already loaded.
        if (LOCALE_MAPPINGS.containsKey(locale)) return;

        InputStream localeStream = GeyserConnector.class.getClassLoader().getResourceAsStream("languages/texts/" + locale + ".properties");

        // Load the locale
        if (localeStream != null) {
            Properties localeProp = new Properties();
            try (InputStreamReader reader = new InputStreamReader(localeStream, StandardCharsets.UTF_8)) {
                localeProp.load(reader);
            } catch (Exception e) {
                throw new AssertionError(getLocaleStringLog("geyser.language.load_failed", locale), e);
            }

            // Insert the locale into the mappings
            LOCALE_MAPPINGS.put(locale, localeProp);
        } else {
            if (GeyserConnector.getInstance() != null && GeyserConnector.getInstance().getLogger() != null) {
                GeyserConnector.getInstance().getLogger().warning("Missing locale: " + locale);
            }
        }
    }

    /**
     * Get a formatted language string with the default locale for Geyser
     *
     * @param key Language string to translate
     * @param values Values to put into the string
     * @return Translated string or the original message if it was not found in the given locale
     */
    public static String getLocaleStringLog(String key, Object... values) {
        return getPlayerLocaleString(key, getDefaultLocale(), values);
    }

    /**
     * Get a formatted language string with the given locale for Geyser
     *
     * @param key Language string to translate
     * @param locale Locale to translate to
     * @param values Values to put into the string
     * @return Translated string or the original message if it was not found in the given locale
     */
    public static String getPlayerLocaleString(String key, String locale, Object... values) {
        locale = formatLocale(locale);

        Properties properties = LOCALE_MAPPINGS.get(locale);
        String formatString = null;

        if (properties != null) {
            formatString = properties.getProperty(key);
        }

        // Try and get the key from the default locale
        if (formatString == null) {
            properties = LOCALE_MAPPINGS.get(formatLocale(getDefaultLocale()));
            formatString = properties.getProperty(key);
        }

        // Try and get the key from en_US (this should only ever happen in development)
        if (formatString == null) {
            properties = LOCALE_MAPPINGS.get("en_US");
            formatString = properties.getProperty(key);
        }

        // Final fallback
        if (formatString == null) {
            formatString = key;
        }

        return MessageFormat.format(formatString.replace("'", "''").replace("&", "\u00a7"), values);
    }

    /**
     * Cleans up and formats a locale string
     *
     * @param locale The locale to format
     * @return The formatted locale
     */
    public static String formatLocale(String locale) {
        try {
            String[] parts = locale.toLowerCase().split("_");
            return parts[0] + "_" + parts[1].toUpperCase();
        } catch (Exception e) {
            return locale;
        }
    }

    /**
     * Get the default locale that Geyser should use
     * @return the current default locale
     */
    public static String getDefaultLocale() {
        if (CACHED_LOCALE != null) return CACHED_LOCALE; // We definitely know the locale the user is using
        String locale;
        boolean isValid = true;
        if (GeyserConnector.getInstance() != null &&
                GeyserConnector.getInstance().getConfig() != null &&
                GeyserConnector.getInstance().getConfig().getDefaultLocale() != null) { // If the config option for getDefaultLocale does not equal null, use that
            locale = formatLocale(GeyserConnector.getInstance().getConfig().getDefaultLocale());
            if (isValidLanguage(locale)) {
                CACHED_LOCALE = locale;
                return locale;
            } else {
                isValid = false;
            }
        }
        locale = formatLocale(Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry());
        if (!isValidLanguage(locale)) { // Bedrock does not support this language
            locale = "en_US";
            loadGeyserLocale(locale);
        }
        if (GeyserConnector.getInstance() != null &&
                GeyserConnector.getInstance().getConfig() != null && (GeyserConnector.getInstance().getConfig().getDefaultLocale() == null || !isValid)) { // Means we should use the system locale for sure
            CACHED_LOCALE = locale;
        }
        return locale;
    }

    /**
     * Ensures that the given locale is supported by Bedrock
     * @param locale the locale to validate
     * @return true if the given locale is supported by Bedrock and by extension Geyser
     */
    private static boolean isValidLanguage(String locale) {
        boolean result = true;
        if (FileUtils.class.getResource("/languages/texts/" + locale + ".properties") == null) {
            result = false;
            if (GeyserConnector.getInstance() != null && GeyserConnector.getInstance().getLogger() != null) { // Could be too early for these to be initialized
                if (locale.equals("en_US")) {
                    GeyserConnector.getInstance().getLogger().error("English locale not found in Geyser. Did you clone the submodules? (git submodule update --init)");
                } else {
                    GeyserConnector.getInstance().getLogger().warning(locale + " is not a valid Bedrock language."); // We can't translate this since we just loaded an invalid language
                }
            }
        } else {
            if (!LOCALE_MAPPINGS.containsKey(locale)) {
                loadGeyserLocale(locale);
            }
        }
        return result;
    }

    public static void init() {
        // no-op
    }
}
