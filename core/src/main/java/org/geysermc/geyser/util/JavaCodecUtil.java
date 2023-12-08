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

package org.geysermc.geyser.util;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Iterator;

public final class JavaCodecUtil {

    /**
     * Iterate over a Java Edition codec and return each entry as a CompoundTag
     */
    public static Iterable<CompoundTag> iterateAsTag(CompoundTag tag) {
        ListTag value = tag.get("value");
        Iterator<Tag> originalIterator = value.iterator();
        return new Iterable<>() {
            @NonNull
            @Override
            public Iterator<CompoundTag> iterator() {
                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return originalIterator.hasNext();
                    }

                    @Override
                    public CompoundTag next() {
                        return (CompoundTag) originalIterator.next();
                    }
                };
            }
        };
    }

    private JavaCodecUtil() {
    }
}
