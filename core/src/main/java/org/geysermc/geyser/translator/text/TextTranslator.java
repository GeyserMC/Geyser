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

package org.geysermc.geyser.translator.text;

import net.kyori.adventure.text.Component;
import org.geysermc.geyser.session.GeyserSession;

/**
 * Converts a Java {@link Component} to a Bedrock legacy-formatted {@link String}.
 *
 * <p>Implementations usually do this using the methods available in {@link MessageTranslator}, possibly using a player's locale.
 * This interface essentially <a href="https://en.wikipedia.org/wiki/Currying">curries</a> the player's locale (and as such, in many cases, the {@link GeyserSession})
 * outside of the {@code convertMessage} method, leading to a simpler signature.</p>
 *
 * <p>It is planned to expand the usage of this interface in the future, reducing the need to pass a {@link GeyserSession} around.</p>
 */
@FunctionalInterface
public interface TextTranslator {

    String convertText(Component component);

    static TextTranslator forLocale(String locale) {
        return component -> MessageTranslator.convertMessage(component, locale);
    }
}
