/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.language;

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.language.LanguageProvider;
import org.geysermc.geyser.api.language.LocaleManager;

import java.util.HashSet;
import java.util.Set;

public final class LanguageManager {
    private final Set<LanguageProvider> languageProviders = new HashSet<>();

    /**
     * Registers a {@link LanguageProvider}, should only be done before Minecraft locales are loaded!
     * @param provider the {@link LanguageProvider} to register
     * @return if the {@link LanguageProvider} was registered successfully
     */
    public boolean registerLanguageProvider(LanguageProvider provider) {
        return languageProviders.add(provider);
    }

    /**
     * Load locales into the given {@link LocaleManager}
     * @param localeManager the {@link LocaleManager}
     */
    public void registerTranslationStrings(LocaleManager localeManager) {
        languageProviders.forEach(provider -> provider.loadLocale(localeManager));
    }
}
