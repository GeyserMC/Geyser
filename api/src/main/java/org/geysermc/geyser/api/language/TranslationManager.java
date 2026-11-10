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

package org.geysermc.geyser.api.language;

/**
 * Geyser's translation manager allows access to Java's locale translations,
 * combined with translations added by extensions or by the user.
 *
 * @since 2.13.0
 */
public interface TranslationManager {
    /**
     * Gets the translation string associated with the key from the locale specified
     * using the parameters specified
     *
     * @param locale the locale to use
     * @param key the key of the translation
     * @param parameters the translation parameters, can be empty
     * @return the translated message, or the key if there is none
     * @since 2.13.0
     */
    default String translationString(String locale, String key, String... parameters) {
        return translationStringOrDefault(locale, key, key, parameters);
    }

    /**
     * Gets the translation string associated with the key from the locale specified
     * using the parameters specified
     *
     * @param locale the locale to use
     * @param key the key of the translation
     * @param defaultValue the fallback value for this translation, parameters are applied to this value
     * @param parameters the translation parameters, can be empty
     * @return the translated message, or the key if there is none
     * @since 2.13.0
     */
    String translationStringOrDefault(String locale, String key, String defaultValue, String... parameters);
}
