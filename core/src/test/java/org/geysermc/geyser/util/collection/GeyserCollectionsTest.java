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

package org.geysermc.geyser.util.collection;

import org.junit.Assert;
import org.junit.Test;

public class GeyserCollectionsTest {
    private final byte[] bytes = new byte[] {(byte) 5, (byte) 4, (byte) 3, (byte) 2, (byte) 2, (byte) 1};
    private final boolean[] booleans = new boolean[] {true, false, false, true};
    private final int[] ints = new int[] {76, 3006, 999, 2323, 888, 0, 111, 999};

    private final int[] startBlockRanges = new int[] {0, 70, 600, 450, 787, 1980};

    @Test
    public void testBytes() {
        for (int startRange : startBlockRanges) {
            testBytes(startRange, new FixedInt2ByteMap());
        }
    }

    private void testBytes(final int start, final FixedInt2ByteMap map) {
        int index = start;
        for (byte b : bytes) {
            map.put(index++, b);
        }

        int lastKey = index;

        // Easy, understandable out-of-bounds checks
        Assert.assertFalse("Map contains key bigger by one!", map.containsKey(lastKey));
        Assert.assertTrue("Map doesn't contain final key!", map.containsKey(lastKey - 1));

        // Ensure the first and last values do not throw an exception on get, and test getOrDefault
        map.get(start - 1);
        map.get(lastKey);
        Assert.assertEquals(map.getOrDefault(start - 1, Byte.MAX_VALUE), Byte.MAX_VALUE);
        Assert.assertEquals(map.getOrDefault(lastKey, Byte.MAX_VALUE), Byte.MAX_VALUE);
        Assert.assertEquals(map.getOrDefault(lastKey, Byte.MIN_VALUE), Byte.MIN_VALUE);

        Assert.assertEquals(map.size(), bytes.length);

        for (int i = start; i < bytes.length; i++) {
            Assert.assertTrue(map.containsKey(i));
            Assert.assertEquals(map.get(i), bytes[i - start]);
        }

        for (int i = start - 1; i >= (start - 6); i--) {
            // Lower than expected check
            Assert.assertFalse(i + " is in a map that starts with " + start, map.containsKey(i));
        }

        for (int i = bytes.length + start; i < bytes.length + 5 + start; i++) {
            // Higher than expected check
            Assert.assertFalse(i + " is in a map that ends with " + (start + bytes.length), map.containsKey(i));
        }

        for (byte b : bytes) {
            Assert.assertTrue(map.containsValue(b));
        }
    }

    @Test
    public void testBooleans() {
        for (int startRange : startBlockRanges) {
            testBooleans(startRange, new FixedInt2BooleanMap());
        }
    }

    private void testBooleans(final int start, final FixedInt2BooleanMap map) {
        int index = start;
        for (boolean b : booleans) {
            map.put(index++, b);
        }

        int lastKey = index;

        // Easy, understandable out-of-bounds checks
        Assert.assertFalse("Map contains key bigger by one!", map.containsKey(lastKey));
        Assert.assertTrue("Map doesn't contain final key!", map.containsKey(lastKey - 1));

        // Ensure the first and last values do not throw an exception on get
        map.get(start - 1);
        map.get(lastKey);
        Assert.assertTrue(map.getOrDefault(lastKey, true));

        Assert.assertEquals(map.size(), booleans.length);

        for (int i = start; i < booleans.length; i++) {
            Assert.assertTrue(map.containsKey(i));
            Assert.assertEquals(map.get(i), booleans[i - start]);
        }

        for (int i = start - 1; i >= (start - 6); i--) {
            // Lower than expected check
            Assert.assertFalse(i + " is in a map that starts with " + start, map.containsKey(i));
        }

        for (int i = booleans.length + start; i < booleans.length + start + 5; i++) {
            // Higher than expected check
            Assert.assertFalse(i + " is in a map that ends with " + (start + booleans.length), map.containsKey(i));
        }

        for (boolean b : booleans) {
            Assert.assertTrue(map.containsValue(b));
        }
    }

    @Test
    public void testInts() {
        for (int startRange : startBlockRanges) {
            testInts(startRange, new FixedInt2IntMap());
        }
    }

    private void testInts(final int start, final FixedInt2IntMap map) {
        int index = start;
        for (int i : ints) {
            map.put(index++, i);
        }

        int lastKey = index;

        // Easy, understandable out-of-bounds checks
        Assert.assertFalse("Map contains key bigger by one!", map.containsKey(lastKey));
        Assert.assertTrue("Map doesn't contain final key!", map.containsKey(lastKey - 1));

        // Ensure the first and last values do not throw an exception on get, and test getOrDefault
        map.get(start - 1);
        map.get(lastKey);
        Assert.assertEquals(map.getOrDefault(start - 1, Integer.MAX_VALUE), Integer.MAX_VALUE);
        Assert.assertEquals(map.getOrDefault(lastKey, Integer.MAX_VALUE), Integer.MAX_VALUE);
        Assert.assertEquals(map.getOrDefault(lastKey, Integer.MIN_VALUE), Integer.MIN_VALUE);

        Assert.assertEquals(map.size(), ints.length);

        for (int i = start; i < ints.length; i++) {
            Assert.assertTrue(map.containsKey(i));
            Assert.assertEquals(map.get(i), ints[i - start]);
        }

        for (int i = start - 1; i >= (start - 6); i--) {
            // Lower than expected check
            Assert.assertFalse(i + " is in a map that starts with " + start, map.containsKey(i));
        }

        for (int i = ints.length + start; i < ints.length + 5 + start; i++) {
            // Higher than expected check
            Assert.assertFalse(i + " is in a map that ends with " + (start + ints.length), map.containsKey(i));
        }

        for (int i : ints) {
            Assert.assertTrue(map.containsValue(i));
        }
    }
}
