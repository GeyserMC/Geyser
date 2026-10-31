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

package org.geysermc.geyser.translator.item;

import net.kyori.adventure.key.Key;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * Bedrock has no tooltip styles. When enabled, an item with a Java tooltip style gets a marker at the end of its
 * Bedrock name, which a resource pack can look for to draw that style's tooltip.
 * <p>
 * The marker is U+EF00 followed by three characters made from the CRC32 of the style id (such as
 * {@code minecraft:epic}): its low 18 bits as three 6-bit digits, the first at U+EF40, the second at U+EF80, the third
 * at U+EFC0. A pack can work out each style's marker from the id alone.
 */
public final class TooltipStyleMarkers {
    private static final Pattern MARKER = Pattern.compile("[-][-][-]");

    private TooltipStyleMarkers() {
    }

    public static String marker(Key style) {
        CRC32 crc = new CRC32();
        crc.update(style.asString().getBytes(StandardCharsets.UTF_8));
        long hash = crc.getValue();
        return new String(new char[] {
            '',
            (char) (0xEF40 + ((hash >> 12) & 63)),
            (char) (0xEF80 + ((hash >> 6) & 63)),
            (char) (0xEFC0 + (hash & 63))
        });
    }

    /**
     * Removes markers from text a Bedrock player typed, such as a name in an anvil that started as an item's name.
     */
    public static String strip(String text) {
        return MARKER.matcher(text).replaceAll("");
    }
}
