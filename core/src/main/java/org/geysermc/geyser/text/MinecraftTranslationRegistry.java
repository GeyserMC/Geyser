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

import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used for mapping a translation key with the already loaded Java locale data
 * Used in MessageTranslator.java as part of the KyoriPowered/Adventure library
 */
public class MinecraftTranslationRegistry extends TranslatableComponentRenderer<String> {
    private final Pattern stringReplacement = Pattern.compile("%s");
    private final Pattern positionalStringReplacement = Pattern.compile("%([0-9]+)\\$s");

    // Exists to maintain compatibility with Velocity's older Adventure version
    @Override
    public @Nullable MessageFormat translate(@NonNull String key, @NonNull String locale) {
        return this.translate(key, null, locale);
    }

    @Override
    protected @Nullable MessageFormat translate(@NonNull String key, @Nullable String fallback, @NonNull String locale) {
        // Get the locale string
        String localeString = MinecraftLocale.getLocaleStringIfPresent(key, locale);
        if (localeString == null) {
            if (fallback != null) {
                // Fallback strings will still have their params inserted
                localeString = fallback;
            } else {
                // The original translation will be translated
                // Can be tested with 1.19.4: {"translate":"%s","with":[{"text":"weeeeeee"}]}
                localeString = key;
            }
        }

        // replace single quote instances which get lost in MessageFormat otherwise
        localeString = localeString.replace("'", "''");

        // Escape all left curly brackets with single quote - fixes https://github.com/GeyserMC/Geyser/issues/4662
        localeString = escapeBraces(localeString);

        // Replace the `%s` with numbered inserts `{0}`
        Pattern p = stringReplacement;
        Matcher m = p.matcher(localeString);
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (m.find()) {
            m.appendReplacement(sb, "{" + (i++) + "}");
        }
        m.appendTail(sb);

        // Replace the `%x$s` with numbered inserts `{x}`
        p = positionalStringReplacement;
        m = p.matcher(sb.toString());
        sb = new StringBuilder();
        while (m.find()) {
            i = Integer.parseInt(m.group(1)) - 1;
            m.appendReplacement(sb, "{" + i + "}");
        }
        m.appendTail(sb);

        // Locale shouldn't need to be specific - dates for example will not be handled
        return new MessageFormat(sb.toString(), Locale.ROOT);
    }

    private String escapeBraces(String origin) {
        boolean first = true;
        String splited = origin;
        StringBuilder sb = null;
        while (true) {
            // Get the index of the nearest brace
            int braceIndex = splited.indexOf("{"); // Only left braces need to be escaped
            // Append from the content that splited by the braces
            if (braceIndex == -1) {
                // Return the result after the last brace
                return first ? origin : sb.append("'").append(splited).toString();
            }
            if (first) {
                sb = new StringBuilder();
            }
            String appendContent = splited.substring(0, braceIndex);
            // Append single quote on both sides of the braces area or the single quote area that enclosed by braces
            boolean inBracket = first || braceIndex == 0 || Pattern.matches("'+", appendContent);
            if (!inBracket) {
                sb.append("'");
            }
            sb.append(appendContent);
            if (!inBracket || first) {
                sb.append("'");
            }
            sb.append("{");
            // The next one starts after the current braces
            splited = splited.substring(braceIndex + 1);
            first = false;
        }
    }
}
