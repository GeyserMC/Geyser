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

package org.geysermc.geyser.session.cache;

import org.geysermc.geyser.text.ChatColor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// https://github.com/GeyserMC/Geyser/issues/6496 - overly long boss bar titles disconnect some Bedrock clients.
public class BossBarTest {

    private static final int MAX_LENGTH = 256;

    @Test
    public void shortTitleIsUnchanged() {
        Assertions.assertEquals("Boss", BossBar.truncateForBedrock("Boss"));
    }

    @Test
    public void longTitleIsTruncated() {
        String title = "a".repeat(300);
        String result = BossBar.truncateForBedrock(title);
        Assertions.assertEquals(MAX_LENGTH, result.length());
        Assertions.assertEquals("a".repeat(MAX_LENGTH), result);
    }

    @Test
    public void escapedOutputNeverExceedsMaxLength() {
        // Every character escapes to 4, so a naive truncate-then-escape would land at 1024 chars.
        String title = "%".repeat(300);
        String result = BossBar.truncateForBedrock(title);
        Assertions.assertTrue(result.length() <= MAX_LENGTH,
            "Escaped title must not exceed " + MAX_LENGTH + " chars, was " + result.length());
        // Only whole "%%%%" groups should be present - never a partial escape sequence.
        Assertions.assertEquals(0, result.length() % 4);
        Assertions.assertEquals("%".repeat(result.length() / 4), result.replace("%%%%", "%"));
    }

    @Test
    public void danglingFormattingCharacterIsDropped() {
        String title = "a".repeat(MAX_LENGTH - 1) + ChatColor.ESCAPE + "r";
        String result = BossBar.truncateForBedrock(title);
        Assertions.assertEquals(MAX_LENGTH - 1, result.length());
        Assertions.assertFalse(result.endsWith(String.valueOf(ChatColor.ESCAPE)));
    }
}
