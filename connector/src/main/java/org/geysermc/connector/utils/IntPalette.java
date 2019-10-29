package org.geysermc.connector.utils;

import java.util.Arrays;

/**
 * This file property the NukkitX project
 * https://github.com/NukkitX/Nukkit
 * @author https://github.com/boy0001/
 */
public class IntPalette {
    private static int[] INT0 = new int[0];
    private int[] keys = INT0;
    private int lastIndex = Integer.MIN_VALUE;

    public void add(int key) {
        keys = insert(key);
        lastIndex = Integer.MIN_VALUE;
    }

    protected void set(int[] keys) {
        this.keys = keys;
        lastIndex = Integer.MIN_VALUE;
    }

    private int[] insert(int val) {
        lastIndex = Integer.MIN_VALUE;
        if (keys.length == 0) {
            return new int[] { val };
        }
        else if (val < keys[0]) {
            int[] s = new int[keys.length + 1];
            System.arraycopy(keys, 0, s, 1, keys.length);
            s[0] = val;
            return s;
        } else if (val > keys[keys.length - 1]) {
            int[] s = Arrays.copyOf(keys, keys.length + 1);
            s[keys.length] = val;
            return s;
        }
        int[] s = Arrays.copyOf(keys, keys.length + 1);
        for (int i = 0; i < s.length; i++) {
            if (keys[i] < val) {
                continue;
            }
            System.arraycopy(keys, i, s, i + 1, s.length - i - 1);
            s[i] = val;
            break;
        }
        return s;
    }

    public int getKey(int index) {
        return keys[index];
    }

    public int getValue(int key) {
        int lastTmp = lastIndex;
        boolean hasLast = lastTmp != Integer.MIN_VALUE;
        int index;
        if (hasLast) {
            int lastKey = keys[lastTmp];
            if (lastKey == key) return lastTmp;
            if (lastKey > key) {
                index = binarySearch0(0, lastTmp, key);
            } else {
                index = binarySearch0(lastTmp + 1, keys.length, key);
            }
        } else {
            index = binarySearch0(0, keys.length, key);
        }
        if (index >= keys.length || index < 0) {
            return lastIndex = Integer.MIN_VALUE;
        } else {
            return lastIndex = index;
        }
    }

    private int binarySearch0(int fromIndex, int toIndex, int key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = keys[mid];

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    public int length() {
        return keys.length;
    }

    public IntPalette clone() {
        IntPalette p = new IntPalette();
        p.keys = this.keys != INT0 ? this.keys.clone() : INT0;
        p.lastIndex = this.lastIndex;
        return p;
    }
}
