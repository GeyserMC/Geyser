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

//import com.github.steveice10.opennbt.tag.builtin.ByteTag;
//import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
//import com.github.steveice10.opennbt.tag.builtin.IntTag;
//import it.unimi.dsi.fastutil.Pair;
//import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
//import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
//import org.cloudburstmc.protocol.bedrock.data.definitions.SimpleItemDefinition;
//import org.geysermc.geyser.api.item.custom.CustomItemOptions;
//import org.geysermc.geyser.api.util.TriState;
//import org.geysermc.geyser.item.GeyserCustomItemOptions;
//import org.geysermc.geyser.item.Items;
//import org.geysermc.geyser.registry.type.ItemMapping;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.OptionalInt;

//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//public class CustomItemsTest {
//    private ItemMapping testMappingWithDamage;
//    private Map<CompoundTag, ItemDefinition> tagToCustomItemWithDamage;
//    private ItemMapping testMappingWithNoDamage;
//    private Map<CompoundTag, ItemDefinition> tagToCustomItemWithNoDamage;
//
//    @BeforeAll
//    public void setup() {
//        CustomItemOptions a = new GeyserCustomItemOptions(TriState.TRUE, OptionalInt.of(2), OptionalInt.empty());
//        CustomItemOptions b = new GeyserCustomItemOptions(TriState.FALSE, OptionalInt.of(5), OptionalInt.empty());
//        CustomItemOptions c = new GeyserCustomItemOptions(TriState.FALSE, OptionalInt.empty(), OptionalInt.of(3));
//        CustomItemOptions d = new GeyserCustomItemOptions(TriState.TRUE, OptionalInt.empty(), OptionalInt.of(8));
//        CustomItemOptions e = new GeyserCustomItemOptions(TriState.FALSE, OptionalInt.empty(), OptionalInt.of(12));
//        CustomItemOptions f = new GeyserCustomItemOptions(TriState.FALSE, OptionalInt.of(8), OptionalInt.of(6));
//        CustomItemOptions g = new GeyserCustomItemOptions(TriState.NOT_SET, OptionalInt.of(20), OptionalInt.empty());
//
//        Map<CustomItemOptions, ItemDefinition> optionsToId = new Object2ObjectArrayMap<>();
//        // Order here is important, hence why we're using an array map
//        optionsToId.put(g, new SimpleItemDefinition("geyser:test_item_7", 7, true));
//        optionsToId.put(f, new SimpleItemDefinition("geyser:test_item_6", 6, true));
//        optionsToId.put(e, new SimpleItemDefinition("geyser:test_item_5", 5, true));
//        optionsToId.put(d, new SimpleItemDefinition("geyser:test_item_4", 4, true));
//        optionsToId.put(c, new SimpleItemDefinition("geyser:test_item_3", 3, true));
//        optionsToId.put(b, new SimpleItemDefinition("geyser:test_item_2", 2, true));
//        optionsToId.put(a, new SimpleItemDefinition("geyser:test_item_1", 1, true));
//
//        tagToCustomItemWithDamage = new HashMap<>();
//
//        CompoundTag tag = new CompoundTag("");
//        addCustomModelData(6, tag);
//        // Test item with no damage should be treated as unbreakable
//        tagToCustomItemWithDamage.put(tag, optionsToId.get(a));
//
//        tag = new CompoundTag("");
//        addCustomModelData(20, tag);
//        // Test that an unbreakable item isn't tested for Damaged if there is no damaged predicate
//        tagToCustomItemWithDamage.put(tag, optionsToId.get(g));
//
//        tag = new CompoundTag("");
//        addCustomModelData(3, tag);
//        setUnbreakable(true, tag);
//        tagToCustomItemWithDamage.put(tag, optionsToId.get(a));
//
//        tag = new CompoundTag("");
//        addDamage(16, tag);
//        setUnbreakable(false, tag);
//        tagToCustomItemWithDamage.put(tag, optionsToId.get(e));
//
//        tag = new CompoundTag("");
//        addCustomModelData(7, tag);
//        addDamage(6, tag);
//        setUnbreakable(false, tag);
//        tagToCustomItemWithDamage.put(tag, optionsToId.get(c));
//
//        tag = new CompoundTag("");
//        addCustomModelData(9, tag);
//        addDamage(6, tag);
//        setUnbreakable(true, tag);
//        tagToCustomItemWithDamage.put(tag, optionsToId.get(a));
//
//        tag = new CompoundTag("");
//        addCustomModelData(9, tag);
//        addDamage(6, tag);
//        setUnbreakable(false, tag);
//        tagToCustomItemWithDamage.put(tag, optionsToId.get(f));
//
//        List<Pair<CustomItemOptions, ItemDefinition>> customItemOptions = optionsToId.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())).toList();
//
//        testMappingWithDamage = ItemMapping.builder()
//                .customItemOptions(customItemOptions)
//                .javaItem(Items.WOODEN_PICKAXE)
//                .build();
//
//        // Test differences with items with no max damage
//
//        tagToCustomItemWithNoDamage = new HashMap<>();
//
//        tag = new CompoundTag("");
//        addCustomModelData(2, tag);
//        // Damage predicates existing mean an item will never match if the item mapping has no max damage
//        tagToCustomItemWithNoDamage.put(tag, null);
//
//        testMappingWithNoDamage = ItemMapping.builder()
//                .customItemOptions(customItemOptions)
//                .javaItem(Items.BEDROCK) // Must be defined manually since registries aren't initialized
//                .build();
//    }
//
//    private void addCustomModelData(int value, CompoundTag tag) {
//        tag.put(new IntTag("CustomModelData", value));
//    }
//
//    private void addDamage(int value, CompoundTag tag) {
//        tag.put(new IntTag("Damage", value));
//    }
//
//    private void setUnbreakable(boolean value, CompoundTag tag) {
//        tag.put(new ByteTag("Unbreakable", (byte) (value ? 1 : 0)));
//    }
//
//    @Test
//    public void testCustomItems() {
//        for (Map.Entry<CompoundTag, ItemDefinition> entry : this.tagToCustomItemWithDamage.entrySet()) {
//            ItemDefinition id = CustomItemTranslator.getCustomItem(entry.getKey(), this.testMappingWithDamage);
//            Assertions.assertEquals(entry.getValue(), id, entry.getKey() + " did not produce the correct custom item");
//        }
//
//        for (Map.Entry<CompoundTag, ItemDefinition> entry : this.tagToCustomItemWithNoDamage.entrySet()) {
//            ItemDefinition id = CustomItemTranslator.getCustomItem(entry.getKey(), this.testMappingWithNoDamage);
//            Assertions.assertEquals(entry.getValue(), id, entry.getKey() + " did not produce the correct custom item");
//        }
//    }
//}
