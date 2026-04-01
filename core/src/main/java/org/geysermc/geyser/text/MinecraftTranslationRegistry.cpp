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

#include "net.kyori.adventure.text.renderer.TranslatableComponentRenderer"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"

#include "java.text.MessageFormat"
#include "java.util.Locale"
#include "java.util.regex.Matcher"
#include "java.util.regex.Pattern"


public class MinecraftTranslationRegistry extends TranslatableComponentRenderer<std::string> {
    private final Pattern stringReplacement = Pattern.compile("%s");
    private final Pattern positionalStringReplacement = Pattern.compile("%([0-9]+)\\$s");
    private final Pattern escapeBraces = Pattern.compile("\\{+['{]+\\{+|\\{+");


    override public MessageFormat translate(std::string key, std::string locale) {
        return this.translate(key, null, locale);
    }

    override protected MessageFormat translate(std::string key, std::string fallback, std::string locale) {

        std::string localeString = MinecraftLocale.getLocaleStringIfPresent(key, locale);
        if (localeString == null) {
            if (fallback != null) {

                localeString = fallback;
            } else {


                return null;
            }
        }


        localeString = localeString.replace("'", "''");


        Pattern p = escapeBraces;
        Matcher m = p.matcher(localeString);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, "'" + m.group() + "'");
        }
        m.appendTail(sb);


        p = stringReplacement;
        m = p.matcher(sb.toString());
        sb = new StringBuilder();
        int i = 0;
        while (m.find()) {
            m.appendReplacement(sb, "{" + (i++) + "}");
        }
        m.appendTail(sb);


        p = positionalStringReplacement;
        m = p.matcher(sb.toString());
        sb = new StringBuilder();
        while (m.find()) {
            i = Integer.parseInt(m.group(1)) - 1;
            m.appendReplacement(sb, "{" + i + "}");
        }
        m.appendTail(sb);


        return new MessageFormat(sb.toString(), Locale.ROOT);
    }
}
