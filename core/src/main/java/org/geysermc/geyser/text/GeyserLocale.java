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

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.GeyserBootstrap;
import org.geysermc.geyser.GeyserImpl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class GeyserLocale {
    public static final String SYSTEM_LOCALE = "system";

    
    private static String DEFAULT_LOCALE;
    
    private static boolean SYSTEM_LOCALE_INVALID;

    private static final Map<String, Properties> LOCALE_MAPPINGS = new HashMap<>();

    
    public static void init(GeyserBootstrap bootstrap) {
        String defaultLocale = formatLocale(Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry());
        String loadedLocale = loadGeyserLocale(defaultLocale, bootstrap);
        if (loadedLocale != null) {
            DEFAULT_LOCALE = loadedLocale;
            
            if (!"en_US".equals(loadedLocale)) {
                loadGeyserLocale("en_US", bootstrap);
            }
            SYSTEM_LOCALE_INVALID = false;
        } else {
            DEFAULT_LOCALE = loadGeyserLocale("en_US", bootstrap);
            if (DEFAULT_LOCALE == null) {
                
                throw new IllegalStateException("English locale not found in Geyser. Did you clone the submodules? (git submodule update --init)");
            }
            SYSTEM_LOCALE_INVALID = true;
        }
    }

    
    public static void finalizeDefaultLocale(GeyserImpl geyser) {
        String newDefaultLocale = geyser.config().defaultLocale();
        if (SYSTEM_LOCALE.equals(newDefaultLocale)) {
            
            return;
        }
        String loadedNewLocale = loadGeyserLocale(newDefaultLocale, geyser.getBootstrap());
        if (loadedNewLocale != null) {
            
            DEFAULT_LOCALE = loadedNewLocale;
        } else if (SYSTEM_LOCALE_INVALID) {
            geyser.getLogger().warning(Locale.getDefault().toString() + " is not a valid Bedrock language.");
        }
    }

    public static String getDefaultLocale() {
        return DEFAULT_LOCALE;
    }

    
    public static void loadGeyserLocale(String locale) {
        GeyserImpl geyser = GeyserImpl.getInstance();
        if (geyser == null) {
            throw new IllegalStateException("Geyser instance cannot be null when loading a locale!");
        }
        loadGeyserLocale(locale, geyser.getBootstrap());
    }

    private static @Nullable String loadGeyserLocale(String locale, GeyserBootstrap bootstrap) {
        locale = formatLocale(locale);
        
        if (LOCALE_MAPPINGS.containsKey(locale)) {
            return locale;
        }

        Properties localeProp = new Properties();

        File localLanguage;
        Path localFolder = bootstrap.getConfigFolder().resolve("languages");
        if (Files.exists(localFolder)) {
            localLanguage = localFolder.resolve(locale + ".properties").toFile();
        } else {
            localLanguage = null;
        }
        boolean validLocalLanguage = localLanguage != null && localLanguage.exists();

        InputStream localeStream = bootstrap.getResourceOrNull("languages/texts/" + locale + ".properties");

        
        if (localeStream != null) {
            try {
                try (InputStreamReader reader = new InputStreamReader(localeStream, StandardCharsets.UTF_8)) {
                    localeProp.load(reader);
                } catch (Exception e) {
                    throw new AssertionError(getLocaleStringLog("geyser.language.load_failed", locale), e);
                }

                
                LOCALE_MAPPINGS.put(locale, localeProp);
            } finally {
                try {
                    localeStream.close();
                } catch (IOException ignored) {}
            }
        } else {
            if (!validLocalLanguage) {
                
                bootstrap.getGeyserLogger().debug("Missing locale: " + locale);
            }
        }

        
        
        
        if (validLocalLanguage) {
            try (InputStreamReader stream = new InputStreamReader(new FileInputStream(localLanguage), StandardCharsets.UTF_8)) {
                localeProp.load(stream);
            } catch (IOException e) {
                String message = "Unable to load custom language override!";
                bootstrap.getGeyserLogger().error(message, e);
            }

            LOCALE_MAPPINGS.putIfAbsent(locale, localeProp);
        }
        return localeProp.isEmpty() ? null : locale;
    }

    
    public static String getLocaleStringLog(String key) {
        return getLocaleStringLog(key, ObjectArrays.EMPTY_ARRAY);
    }

    
    public static String getLocaleStringLog(String key, Object... values) {
        return getPlayerLocaleString(key, getDefaultLocale(), values);
    }

    
    public static String getPlayerLocaleString(String key, String locale) {
        return getPlayerLocaleString(key, locale, ObjectArrays.EMPTY_ARRAY);
    }

    
    public static String getPlayerLocaleString(String key, String locale, Object... values) {
        locale = formatLocale(locale);

        Properties properties = LOCALE_MAPPINGS.get(locale);
        String formatString = null;

        if (properties != null) {
            formatString = properties.getProperty(key);
        }

        
        if (formatString == null) {
            properties = LOCALE_MAPPINGS.get(getDefaultLocale());
            formatString = properties.getProperty(key);

            
            if (formatString == null) {
                properties = LOCALE_MAPPINGS.get("en_US");
                formatString = properties.getProperty(key);

                
                if (formatString == null) {
                    return key;
                }
            }
        }

        String message = formatString.replace("&", "\u00a7");
        if (values == null || values.length == 0) {
            
            return message;
        }

        return MessageFormat.format(message.replace("'", "''"), values);
    }

    
    public static String formatLocale(String locale) {
        
        
        if (locale.length() != 5 || locale.indexOf('_') != 2) {
            
            return locale;
        }

        
        String lowerCaseLocale = locale.toLowerCase(Locale.ROOT);
        if (lowerCaseLocale.equals("nn_no") || lowerCaseLocale.equals("no_no")) {
            locale = "nb_NO";
        }

        String language = locale.substring(0, 2);
        String country = locale.substring(3);
        return language.toLowerCase(Locale.ENGLISH) + "_" + country.toUpperCase(Locale.ENGLISH);
    }
}
