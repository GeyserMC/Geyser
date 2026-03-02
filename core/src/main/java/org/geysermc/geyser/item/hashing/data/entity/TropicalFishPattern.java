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

package org.geysermc.geyser.item.hashing.data.entity;

// Named by Java ID
public enum TropicalFishPattern {
    KOB(Base.SMALL, 0),
    SUNSTREAK(Base.SMALL, 1),
    SNOOPER(Base.SMALL, 2),
    DASHER(Base.SMALL, 3),
    BRINELY(Base.SMALL, 4),
    SPOTTY(Base.SMALL, 5),
    FLOPPER(Base.LARGE, 0),
    STRIPEY(Base.LARGE, 1),
    GLITTER(Base.LARGE, 2),
    BLOCKFISH(Base.LARGE, 3),
    BETTY(Base.LARGE, 4),
    CLAYFISH(Base.LARGE, 5);

    private final int packedId;

    TropicalFishPattern(Base base, int id) {
        this.packedId = base.ordinal() | id << 8;
    }

    // Ordered by Java ID
    enum Base {
        SMALL,
        LARGE
    }

    public static TropicalFishPattern fromPackedId(int packedId) {
        for (TropicalFishPattern pattern : values()) {
            if (pattern.packedId == packedId) {
                return pattern;
            }
        }
        throw new IllegalArgumentException("Illegal packed tropical fish pattern ID");
    }
}
