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

import java.util.List;
import java.util.OptionalInt;

public class CustomItemsTest {
    private ItemMapping testMappingWithDamage;
    private Object2IntMap<CompoundTag> tagToCustomItemWithDamage;
    private ItemMapping testMappingWithNoDamage;
    private Object2IntMap<CompoundTag> tagToCustomItemWithNoDamage;

    @Before
    public void setup() {
        CustomItemOptions a = new GeyserCustomItemOptions(TriState.TRUE, OptionalInt.of(2), OptionalInt.empty());
        CustomItemOptions b = new GeyserCustomItemOptions(TriState.FALSE, OptionalInt.of(5), OptionalInt.empty());
        CustomItemOptions c = new GeyserCustomItemOptions(TriState.FALSE, OptionalInt.empty(), OptionalInt.of(3));
        CustomItemOptions d = new GeyserCustomItemOptions(TriState.TRUE, OptionalInt.empty(), OptionalInt.of(8));
        CustomItemOptions e = new GeyserCustomItemOptions(TriState.FALSE, OptionalInt.empty(), OptionalInt.of(12));
        CustomItemOptions f = new GeyserCustomItemOptions(TriState.FALSE, OptionalInt.of(8), OptionalInt.of(6));
        CustomItemOptions g = new GeyserCustomItemOptions(TriState.NOT_SET, OptionalInt.of(20), OptionalInt.empty());

        Object2IntMap<CustomItemOptions> optionsToId = new Object2IntArrayMap<>();
        // Order here is important, hence why we're using an array map
        optionsToId.put(g, 7);
        optionsToId.put(f, 6);
        optionsToId.put(e, 5);
        optionsToId.put(d, 4);
        optionsToId.put(c, 3);
        optionsToId.put(b, 2);
        optionsToId.put(a, 1);

        tagToCustomItemWithDamage = new Object2IntOpenHashMap<>();

        CompoundTag tag = new CompoundTag("");
        addCustomModelData(6, tag);
        // Test item with no damage should be treated as unbreakable
        tagToCustomItemWithDamage.put(tag, optionsToId.getInt(a));

        tag = new CompoundTag("");
        addCustomModelData(20, tag);
        // Test that an unbreakable item isn't tested for Damaged if there is no damaged predicate
        tagToCustomItemWithDamage.put(tag, optionsToId.getInt(g));

        tag = new CompoundTag("");
        addCustomModelData(3, tag);
        setUnbreakable(true, tag);
        tagToCustomItemWithDamage.put(tag, optionsToId.getInt(a));

        tag = new CompoundTag("");
        addDamage(16, tag);
        setUnbreakable(false, tag);
        tagToCustomItemWithDamage.put(tag, optionsToId.getInt(e));

        tag = new CompoundTag("");
        addCustomModelData(7, tag);
        addDamage(6, tag);
        setUnbreakable(false, tag);
        tagToCustomItemWithDamage.put(tag, optionsToId.getInt(c));

        tag = new CompoundTag("");
        addCustomModelData(9, tag);
        addDamage(6, tag);
        setUnbreakable(true, tag);
        tagToCustomItemWithDamage.put(tag, optionsToId.getInt(a));

        tag = new CompoundTag("");
        addCustomModelData(9, tag);
        addDamage(6, tag);
        setUnbreakable(false, tag);
        tagToCustomItemWithDamage.put(tag, optionsToId.getInt(f));

        List<ObjectIntPair<CustomItemOptions>> customItemOptions = optionsToId.object2IntEntrySet().stream().map(entry -> ObjectIntPair.of(entry.getKey(), entry.getIntValue())).toList();

        testMappingWithDamage = ItemMapping.builder()
                .customItemOptions(customItemOptions)
                .maxDamage(100)
                .build();

        // Test differences with items with no max damage

        tagToCustomItemWithNoDamage = new Object2IntOpenHashMap<>();

        tag = new CompoundTag("");
        addCustomModelData(2, tag);
        // Damage predicates existing mean an item will never match if the item mapping has no max damage
        tagToCustomItemWithNoDamage.put(tag, -1);

        testMappingWithNoDamage = ItemMapping.builder()
                .customItemOptions(customItemOptions)
                .maxDamage(0)
                .build();
    }

    private void addCustomModelData(int value, CompoundTag tag) {
        tag.put(new IntTag("CustomModelData", value));
    }

    private void addDamage(int value, CompoundTag tag) {
        tag.put(new IntTag("Damage", value));
    }

    private void setUnbreakable(boolean value, CompoundTag tag) {
        tag.put(new ByteTag("Unbreakable", (byte) (value ? 1 : 0)));
    }

    @Test
    public void testCustomItems() {
        for (Object2IntMap.Entry<CompoundTag> entry : this.tagToCustomItemWithDamage.object2IntEntrySet()) {
            int id = CustomItemTranslator.getCustomItem(entry.getKey(), this.testMappingWithDamage);
            Assert.assertEquals(entry.getKey() + " did not produce the correct custom item", entry.getIntValue(), id);
        }

        for (Object2IntMap.Entry<CompoundTag> entry : this.tagToCustomItemWithNoDamage.object2IntEntrySet()) {
            int id = CustomItemTranslator.getCustomItem(entry.getKey(), this.testMappingWithNoDamage);
            Assert.assertEquals(entry.getKey() + " did not produce the correct custom item", entry.getIntValue(), id);
        }
    }
}
