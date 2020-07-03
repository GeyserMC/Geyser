/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
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
        if (!"en_US".equals(cleanLocale(getDefaultLocale()))) {
            loadGeyserLocale("en_US");
        }

        // Load the default locale from config
        loadGeyserLocale(getDefaultLocale());
    }

    /**
     * Loads a Geyser locale from resources, if the file doesn't exist it just logs a warning
     *
     * @param locale Locale to load
     */
    public static void loadGeyserLocale(String locale) {
        locale = cleanLocale(locale);

        InputStream localeStream = GeyserConnector.class.getClassLoader().getResourceAsStream("languages/texts/" + locale + ".properties");

        // Load the locale
        if (localeStream != null) {
            Properties localeProp = new Properties();
            try {
                localeProp.load(new InputStreamReader(localeStream, StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new AssertionError(getLocaleStringLog("geyser.language.load_failed", locale), e);
            }

            // Insert the locale into the mappings
            LOCALE_MAPPINGS.put(locale, localeProp);
        } else {
            if (locale.toLowerCase().equals(getDefaultLocale().toLowerCase())) {
                // The default locale was invalid fallback to en_us
                loadGeyserLocale(getDefaultLocale());
            } else {
                GeyserConnector.getInstance().getLogger().warning(getLocaleStringLog("geyser.language.missing_file", locale));
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
        locale = cleanLocale(locale);

        Properties properties = LOCALE_MAPPINGS.get(locale);
        String formatString = properties.getProperty(key);

        // Try and get the key from the default locale
        if (formatString == null) {
            properties = LOCALE_MAPPINGS.get(cleanLocale(getDefaultLocale()));
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

        return MessageFormat.format(formatString.replace("&", "\u00a7"), values);
    }

    /**
     * Cleans up and formats a locale string
     *
     * @param locale The locale to format
     * @return The formatted locale
     */
    private static String cleanLocale(String locale) {
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
        if (CACHED_LOCALE != null) return CACHED_LOCALE;
        String locale;
        if (GeyserConnector.getInstance() != null &&
                GeyserConnector.getInstance().getConfig() != null &&
                GeyserConnector.getInstance().getConfig().getDefaultLocale() != null) {
            locale = GeyserConnector.getInstance().getConfig().getDefaultLocale();
            CACHED_LOCALE = cleanLocale(locale);
        } else {
            locale = Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry();
            if (GeyserConnector.class.getResource("languages/texts/" + cleanLocale(locale) + ".properties") == null) {
                locale = "en_US";
            }
            if (GeyserConnector.getInstance() != null &&
                    GeyserConnector.getInstance().getConfig() != null && GeyserConnector.getInstance().getConfig().getDefaultLocale() == null) { // Means we should use the system locale
                CACHED_LOCALE = cleanLocale(locale);
            }
        }
        if (!LOCALE_MAPPINGS.containsKey(locale)) {
            loadGeyserLocale(locale);
        }
        return locale;
    }

    public static void init() {
        // no-op
    }
}
