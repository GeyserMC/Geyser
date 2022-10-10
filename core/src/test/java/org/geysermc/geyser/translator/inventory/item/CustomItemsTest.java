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

package org.geysermc.geyser.translator.inventory.item;

import com.github.steveice10.opennbt.tag.builtin.ByteTag;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.item.GeyserCustomItemOptions;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.OptionalInt;

public class CustomItemsTest {
    private ItemMapping testMappingWithDamage;
    private Object2IntMap<CompoundTag> tagToCustomItemWithDamage;

    @Before
    public void setup() {
        CustomItemOptions a = new GeyserCustomItemOptions(TriState.TRUE, OptionalInt.of(2), OptionalInt.empty());
        CustomItemOptions b = new GeyserCustomItemOptions(TriState.FALSE, OptionalInt.of(5), OptionalInt.empty());
        CustomItemOptions c = new GeyserCustomItemOptions(TriState.FALSE, OptionalInt.empty(), OptionalInt.of(3));
        CustomItemOptions d = new GeyserCustomItemOptions(TriState.TRUE, OptionalInt.empty(), OptionalInt.of(8));
        CustomItemOptions e = new GeyserCustomItemOptions(TriState.FALSE, OptionalInt.empty(), OptionalInt.of(12));
        CustomItemOptions f = new GeyserCustomItemOptions(TriState.FALSE, OptionalInt.of(8), OptionalInt.of(6));

        Object2IntMap<CustomItemOptions> optionsToId = new Object2IntArrayMap<>();
        // Order here is important, hence why we're using an array map
        optionsToId.put(f, 6);
        optionsToId.put(e, 5);
        optionsToId.put(d, 4);
        optionsToId.put(c, 3);
        optionsToId.put(b, 2);
        optionsToId.put(a, 1);

        tagToCustomItemWithDamage = new Object2IntOpenHashMap<>();

        CompoundTag tag = new CompoundTag("");
        tag.put(new IntTag("CustomModelData", 6));
        // Test item with no damage should be treated as unbreakable
        tagToCustomItemWithDamage.put(tag, optionsToId.getInt(a));

        tag = new CompoundTag("");
        tag.put(new IntTag("CustomModelData", 3));
        tag.put(new ByteTag("Unbreakable", (byte) 1));
        tagToCustomItemWithDamage.put(tag, optionsToId.getInt(a));

        tag = new CompoundTag("");
        tag.put(new IntTag("Damage", 16));
        tag.put(new ByteTag("Unbreakable", (byte) 0));
        tagToCustomItemWithDamage.put(tag, optionsToId.getInt(e));

        tag = new CompoundTag("");
        tag.put(new IntTag("CustomModelData", 7));
        tag.put(new IntTag("Damage", 6));
        tag.put(new ByteTag("Unbreakable", (byte) 0));
        tagToCustomItemWithDamage.put(tag, optionsToId.getInt(c));

        tag = new CompoundTag("");
        tag.put(new IntTag("CustomModelData", 8));
        tag.put(new IntTag("Damage", 6));
        tag.put(new ByteTag("Unbreakable", (byte) 1));
        tagToCustomItemWithDamage.put(tag, optionsToId.getInt(a));

        tag = new CompoundTag("");
        tag.put(new IntTag("CustomModelData", 9));
        tag.put(new IntTag("Damage", 6));
        tag.put(new ByteTag("Unbreakable", (byte) 0));
        tagToCustomItemWithDamage.put(tag, optionsToId.getInt(f));

        testMappingWithDamage = ItemMapping.builder()
                .customItemOptions(optionsToId.object2IntEntrySet().stream().map(entry -> ObjectIntPair.of(entry.getKey(), entry.getIntValue())).toList())
                .maxDamage(100)
                .build();
        // Later, possibly add a condition with a mapping with no damage
    }

    @Test
    public void testCustomItems() {
        for (Object2IntMap.Entry<CompoundTag> entry : this.tagToCustomItemWithDamage.object2IntEntrySet()) {
            int id = CustomItemTranslator.getCustomItem(entry.getKey(), this.testMappingWithDamage);
            Assert.assertEquals(entry.getKey() + " did not produce the correct custom item", entry.getIntValue(), id);
        }
    }
}
