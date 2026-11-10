/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.event.lifecycle;

import org.geysermc.event.Event;

import java.util.Map;

/**
 * Called when a locale is loaded in Geyser, translations with
 * parameters follow Java's parameter system. This event can be
 * used to register custom translations, which can be used by extensions
 * for logging or localisation purposes, or to include translations from datapacks, plugins
 * or mods.
 *
 * @since 2.13.0
 */
public interface GeyserLoadLocaleEvent extends Event {
    /**
     * Returns the locale this event loads, formatted as {@code language_region}, all lowercase. For example:
     *
     * <ul>
     *  <li>{@code en_gb} for British English.</li>
     *  <li>{@code en_us} for American English.</li>
     * </ul>
     *
     * <p>Geyser will only ever load locales Java Edition supports.</p>
     *
     * @return the locale this event loads
     * @since 2.13.0
     */
    String locale();

    /**
     * Registers a single translation into this locale.
     * 
     * @param key the translation key
     * @param value the translated value
     * @since 2.13.0
     */
    void registerTranslation(String key, String value);

    /**
     * Registers multiple translations into this locale.
     * 
     * @param translations a map of translation keys and translated values
     * @since 2.13.0
     */
    void registerTranslations(Map<String, String> translations);

    /**
     * Removes a translation from this locale.
     * 
     * @param key the translation key
     * @since 2.13.0
     */
    void unregisterTranslation(String key);

    /**
     * Returns all translations currently in this locale.
     * 
     * @return a read-only view of all translations currently in this locale
     * @since 2.13.0
     */
    Map<String, String> getTranslations();
}
