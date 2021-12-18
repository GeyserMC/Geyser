/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.util;

public class MathUtils {

    public static final double SQRT_OF_TWO = Math.sqrt(2);

    /**
     * Round the given float to the next whole number
     *
     * @param floatNumber Float to round
     * @return Rounded number
     */
    public static int ceil(float floatNumber) {
        int truncated = (int) floatNumber;
        return floatNumber > truncated ? truncated + 1 : truncated;
    }

    /**
     * If number is greater than the max, set it to max, and if number is lower than low, set it to low.
     *
     * @param num number to calculate
     * @param min the lowest value the number can be
     * @param max the greatest value the number can be
     * @return - min if num is lower than min <br>
     * - max if num is greater than max <br>
     * - num otherwise
     */
    public static double constrain(double num, double min, double max) {
        if (num > max) {
            num = max;
        }

        if (num < min) {
            num = min;
        }

        return num;
    }

    /**
     * If number is greater than the max, set it to max, and if number is lower than low, set it to low.
     *
     * @param num number to calculate
     * @param min the lowest value the number can be
     * @param max the greatest value the number can be
     * @return - min if num is lower than min <br>
     * - max if num is greater than max <br>
     * - num otherwise
     */
    public static int constrain(int num, int min, int max) {
        if (num > max) {
            num = max;
        }

        if (num < min) {
            num = min;
        }

        return num;
    }

    /**
     * Ensures the resulting object is a byte. Java Edition does not care whether a byte is encoded as an integer or not;
     * it converts it into a byte anyway.
     *
     * @param value The value to convert
     * @return The converted byte
     */
    public static byte getNbtByte(Object value) {
        return ((Number) value).byteValue();
    }

    /**
     * Packs a chunk's X and Z coordinates into a single {@code long}.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     * @return the packed coordinates
     */
    public static long chunkPositionToLong(int x, int z) {
        return ((x & 0xFFFFFFFFL) << 32L) | (z & 0xFFFFFFFFL);
    }

    /**
     * @return the bits per entry used when this number is the maximum amount of entries.
     */
    public static int getGlobalPaletteForSize(int size) {
        return 32 - Integer.numberOfLeadingZeros(size - 1);
    }
}
