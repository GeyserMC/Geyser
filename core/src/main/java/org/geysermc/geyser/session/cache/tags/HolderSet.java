/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.session.cache.tags;

import java.util.List;
import java.util.function.Function;
import lombok.Data;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.session.GeyserSession;

@Data
public final class HolderSet {

    private final @Nullable Key tagId;
    private final int @Nullable [] holders;

    public HolderSet(int @NonNull [] holders) {
        this.tagId = null;
        this.holders = holders;
    }

    public HolderSet(@NonNull Key tagId) {
        this.tagId = tagId;
        this.holders = null;
    }

    public int[] resolve(GeyserSession session, TagRegistry registry) {
        if (holders != null) {
            return holders;
        }

        return session.getTagCache().get(Tag.createTag(registry, tagId));
    }

    public static HolderSet readHolderSet(@Nullable Object holderSet, Function<Key, Integer> keyIdMapping) {
        if (holderSet == null) {
            return new HolderSet(new int[]{});
        }

        if (holderSet instanceof String stringTag) {
            // Tag
            if (stringTag.startsWith("#")) {
                return new HolderSet(Key.key(stringTag.substring(1))); // Remove '#' at beginning that indicates tag
            } else if (stringTag.isEmpty()) {
                return new HolderSet(new int[]{});
            }
            return new HolderSet(new int[]{keyIdMapping.apply(Key.key(stringTag))});
        } else if (holderSet instanceof List<?> list) {
            // Assume the list is a list of strings
            return new HolderSet(list.stream().map(o -> (String) o).map(Key::key).map(keyIdMapping).mapToInt(Integer::intValue).toArray());
        }
        throw new IllegalArgumentException("Holder set must either be a tag, a string ID or a list of string IDs");
    }
}
