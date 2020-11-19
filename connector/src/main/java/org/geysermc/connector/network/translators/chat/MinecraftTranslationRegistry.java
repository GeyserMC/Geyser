/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.chat;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.Translator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.connector.utils.LocaleUtils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used for mapping a translation key with the already loaded Java locale data
 * Used in MessageTranslator.java as part of the KyoriPowered/Adventure library
 */
public class MinecraftTranslationRegistry implements Translator {
    @Override
    public @NonNull Key name() {
        return Key.key("geyser", "minecraft_translations");
    }

    @Override
    public @Nullable MessageFormat translate(@NonNull String key, @NonNull Locale locale) {
        // Get the locale string
        String localeString = LocaleUtils.getLocaleString(key, locale.toString());

        // Replace the `%s` with numbered inserts `{0}`
        Pattern p = Pattern.compile("%s");
        Matcher m = p.matcher(localeString);
        StringBuffer sb = new StringBuffer();
        int i = 0;
        while (m.find()) {
            m.appendReplacement(sb, "{" + (i++) + "}");
        }
        m.appendTail(sb);

        // Replace the `%x$s` with numbered inserts `{x}`
        p = Pattern.compile("%([0-9]+)\\$s");
        m = p.matcher(sb.toString());
        sb = new StringBuffer();
        while (m.find()) {
            i = Integer.parseInt(m.group(1)) - 1;
            m.appendReplacement(sb, "{" + i + "}");
        }
        m.appendTail(sb);

        return new MessageFormat(sb.toString(), locale);
    }
}
