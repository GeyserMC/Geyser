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
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TranslationUtils {

    private static final Map<String, Properties> LOCALE_MAPPINGS = new HashMap<>();

    private static final String DEFAULT_LOCALE = (GeyserConnector.getInstance().getConfig().getDefaultLocale() != null && !GeyserConnector.getInstance().getConfig().getDefaultLocale().toLowerCase().equals("en_gb") ? GeyserConnector.getInstance().getConfig().getDefaultLocale() : "en_us");

    static {
        loadGeyserLocale(DEFAULT_LOCALE);
    }

    /**
     * Loads a Geyser locale from resources, if the file doesn't exist it just logs a warning
     *
     * @param locale Locale to load
     */
    public static void loadGeyserLocale(String locale) {
        locale = locale.toLowerCase();

        // No point having 2 separate files
        if (locale.equals("en_gb")) {
            locale = "en_us";
        }

        InputStream localeStream = LocaleUtils.class.getClassLoader().getResourceAsStream("languages/" + locale + ".properties");

        // Load the locale
        if (localeStream != null) {
            Properties localeProp = new Properties();
            try {
                localeProp.load(localeStream);
            } catch (Exception e) {
                throw new AssertionError("Unable to load Geyser locale map for " + locale, e);
            }

            // Insert the locale into the mappings
            LOCALE_MAPPINGS.put(locale, localeProp);
        } else {
            GeyserConnector.getInstance().getLogger().warning("Missing Geyser locale file: " + locale);
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
        Properties properties = LOCALE_MAPPINGS.get(DEFAULT_LOCALE.toLowerCase());
        return MessageFormat.format(properties.getProperty(key, key), values);
    }

    /**
     * Get a formatted language string with the given locale for Geyser
     *
     * @param key Language string to translate
     * @param locale Locale to translate to
     * @param values Values to put into the string
     * @return Translated string or the original message if it was not found in the given locale
     */
    public static String getLocaleStringPly(String key, String locale, Object... values) {
        locale = locale.toLowerCase();

        // No point having 2 separate files
        if (locale.equals("en_gb")) {
            locale = "en_us";
        }

        Properties properties = LOCALE_MAPPINGS.get(locale);
        return MessageFormat.format(properties.getProperty(key, key), values);
    }

    public static void init() {
        // no-op
    }
}
