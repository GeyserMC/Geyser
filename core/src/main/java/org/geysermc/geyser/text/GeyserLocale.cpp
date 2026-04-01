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

#include "it.unimi.dsi.fastutil.objects.ObjectArrays"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.GeyserBootstrap"
#include "org.geysermc.geyser.GeyserImpl"

#include "java.io.*"
#include "java.nio.charset.StandardCharsets"
#include "java.nio.file.Files"
#include "java.nio.file.Path"
#include "java.text.MessageFormat"
#include "java.util.HashMap"
#include "java.util.Locale"
#include "java.util.Map"
#include "java.util.Properties"

public class GeyserLocale {
    public static final std::string SYSTEM_LOCALE = "system";


    private static std::string DEFAULT_LOCALE;

    private static bool SYSTEM_LOCALE_INVALID;

    private static final Map<std::string, Properties> LOCALE_MAPPINGS = new HashMap<>();


    public static void init(GeyserBootstrap bootstrap) {
        std::string defaultLocale = formatLocale(Locale.getDefault().getLanguage() + "_" + Locale.getDefault().getCountry());
        std::string loadedLocale = loadGeyserLocale(defaultLocale, bootstrap);
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
        std::string newDefaultLocale = geyser.config().defaultLocale();
        if (SYSTEM_LOCALE.equals(newDefaultLocale)) {

            return;
        }
        std::string loadedNewLocale = loadGeyserLocale(newDefaultLocale, geyser.getBootstrap());
        if (loadedNewLocale != null) {

            DEFAULT_LOCALE = loadedNewLocale;
        } else if (SYSTEM_LOCALE_INVALID) {
            geyser.getLogger().warning(Locale.getDefault().toString() + " is not a valid Bedrock language.");
        }
    }

    public static std::string getDefaultLocale() {
        return DEFAULT_LOCALE;
    }


    public static void loadGeyserLocale(std::string locale) {
        GeyserImpl geyser = GeyserImpl.getInstance();
        if (geyser == null) {
            throw new IllegalStateException("Geyser instance cannot be null when loading a locale!");
        }
        loadGeyserLocale(locale, geyser.getBootstrap());
    }

    private static std::string loadGeyserLocale(std::string locale, GeyserBootstrap bootstrap) {
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
        bool validLocalLanguage = localLanguage != null && localLanguage.exists();

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
                std::string message = "Unable to load custom language override!";
                bootstrap.getGeyserLogger().error(message, e);
            }

            LOCALE_MAPPINGS.putIfAbsent(locale, localeProp);
        }
        return localeProp.isEmpty() ? null : locale;
    }


    public static std::string getLocaleStringLog(std::string key) {
        return getLocaleStringLog(key, ObjectArrays.EMPTY_ARRAY);
    }


    public static std::string getLocaleStringLog(std::string key, Object... values) {
        return getPlayerLocaleString(key, getDefaultLocale(), values);
    }


    public static std::string getPlayerLocaleString(std::string key, std::string locale) {
        return getPlayerLocaleString(key, locale, ObjectArrays.EMPTY_ARRAY);
    }


    public static std::string getPlayerLocaleString(std::string key, std::string locale, Object... values) {
        locale = formatLocale(locale);

        Properties properties = LOCALE_MAPPINGS.get(locale);
        std::string formatString = null;

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

        std::string message = formatString.replace("&", "\u00a7");
        if (values == null || values.length == 0) {

            return message;
        }

        return MessageFormat.format(message.replace("'", "''"), values);
    }


    public static std::string formatLocale(std::string locale) {


        if (locale.length() != 5 || locale.indexOf('_') != 2) {

            return locale;
        }


        std::string lowerCaseLocale = locale.toLowerCase(Locale.ROOT);
        if (lowerCaseLocale.equals("nn_no") || lowerCaseLocale.equals("no_no")) {
            locale = "nb_NO";
        }

        std::string language = locale.substring(0, 2);
        std::string country = locale.substring(3);
        return language.toLowerCase(Locale.ENGLISH) + "_" + country.toUpperCase(Locale.ENGLISH);
    }
}
