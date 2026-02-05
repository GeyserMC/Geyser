/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.inventory.recipe;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntObjectMutablePair;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.MultiRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.RecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.DefaultDescriptor;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemTagDescriptor;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.BedrockRequiresTagItem;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.CompositeSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.EmptySlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemStackSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.TagSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.WithRemainderSlotDisplay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.geysermc.geyser.util.InventoryUtils.LAST_RECIPE_NET_ID;

public class RecipeUtil {

    /**
     * Required to use the specified cartography table recipes
     */
    public static final List<RecipeData> CARTOGRAPHY_RECIPES = List.of(
            MultiRecipeData.of(UUID.fromString("8b36268c-1829-483c-a0f1-993b7156a8f2"), ++LAST_RECIPE_NET_ID), // Map extending
            MultiRecipeData.of(UUID.fromString("442d85ed-8272-4543-a6f1-418f90ded05d"), ++LAST_RECIPE_NET_ID), // Map cloning
            MultiRecipeData.of(UUID.fromString("98c84b38-1085-46bd-b1ce-dd38c159e6cc"), ++LAST_RECIPE_NET_ID), // Map upgrading
            MultiRecipeData.of(UUID.fromString("602234e4-cac1-4353-8bb7-b1ebff70024b"), ++LAST_RECIPE_NET_ID) // Map locking
    );

    // Arrays are usually an issue in maps, but because it's referencing the tag array that is unchanged, it actually works out for us.
    private static final ThreadLocal<IntObjectPair<Map<int[], List<ItemDescriptorWithCount>>>> TAG_TO_ITEM_DESCRIPTOR_CACHE = ThreadLocal.withInitial(() -> IntObjectMutablePair.of(0, new Object2ObjectOpenHashMap<>()));

    public static List<ItemDescriptorWithCount> translateToInput(GeyserSession session, SlotDisplay slotDisplay) {
        if (slotDisplay instanceof EmptySlotDisplay) {
            return Collections.singletonList(ItemDescriptorWithCount.EMPTY);
        }
        if (slotDisplay instanceof CompositeSlotDisplay composite) {
            if (composite.contents().size() == 1) {
                return translateToInput(session, composite.contents().get(0));
            }

            // Try and see if the contents match a tag.
            // ViaVersion maps pre-1.21.2 ingredient lists to CompositeSlotDisplays.
            int[] items = new int[composite.contents().size()];
            List<SlotDisplay> contents = composite.contents();
            for (int i = 0; i < contents.size(); i++) {
                SlotDisplay subDisplay = contents.get(i);
                int id;
                if (subDisplay instanceof ItemSlotDisplay item) {
                    id = item.item();
                } else if (!(subDisplay instanceof ItemStackSlotDisplay itemStackSlotDisplay)) {
                    id = -1;
                } else if (itemStackSlotDisplay.itemStack().getAmount() == 1
                        && itemStackSlotDisplay.itemStack().getDataComponentsPatch() == null) {
                    id = itemStackSlotDisplay.itemStack().getId();
                } else {
                    id = -1;
                }
                if (id == -1) {
                    // We couldn't guarantee a "normal" item from this stack.
                    return fallbackCompositeMapping(session, composite);
                }
                items[i] = id;
            }
            // For searching in the tag map.
            Arrays.sort(items);

            List<ItemDescriptorWithCount> tagDescriptor = lookupBedrockTag(session, items);
            if (tagDescriptor != null) {
                return tagDescriptor;
            }

            return fallbackCompositeMapping(session, composite);
        }
        if (slotDisplay instanceof WithRemainderSlotDisplay remainder) {
            // Don't need to worry about what will stay in the crafting table after crafting for the purposes of sending recipes to Bedrock
            return translateToInput(session, remainder.input());
        }
        if (slotDisplay instanceof ItemSlotDisplay itemSlot) {
            return Collections.singletonList(fromItem(session, itemSlot.item()));
        }
        if (slotDisplay instanceof ItemStackSlotDisplay itemStackSlot) {
            ItemData item = ItemTranslator.translateToBedrock(session, itemStackSlot.itemStack());
            return Collections.singletonList(ItemDescriptorWithCount.fromItem(item));
        }
        if (slotDisplay instanceof TagSlotDisplay tagSlot) {
            Key tag = tagSlot.tag();
            int[] items = session.getTagCache().getRaw(new Tag<>(JavaRegistries.ITEM, tag)); // I don't like this...
            if (items == null || items.length == 0) {
                return Collections.singletonList(ItemDescriptorWithCount.EMPTY);
            } else if (items.length == 1) {
                return Collections.singletonList(fromItem(session, items[0]));
            } else {
                // Cache is implemented as, presumably, an item tag will be used multiple times in succession
                // (E.G. a chest with planks tags)
                if (TAG_TO_ITEM_DESCRIPTOR_CACHE.get().firstInt() != session.protocolVersion()) {
                    TAG_TO_ITEM_DESCRIPTOR_CACHE.get().first(session.protocolVersion()).second().clear();
                }
                return TAG_TO_ITEM_DESCRIPTOR_CACHE.get().second().computeIfAbsent(items, key -> {
                    List<ItemDescriptorWithCount> tagDescriptor = lookupBedrockTag(session, key);
                    if (tagDescriptor != null) {
                        return tagDescriptor;
                    }

                    // In the future, we can probably search through and use subsets of tags as well.
                    // I.E. if a Bedrock tag contains [stone stone_brick] and the Java tag uses [stone stone_brick bricks]
                    // we can still use that Bedrock tag alongside plain item descriptors for "bricks".

                    Set<ItemDescriptorWithCount> itemDescriptors = new HashSet<>();
                    for (int item : key) {
                        itemDescriptors.add(fromItem(session, item));
                    }
                    return List.copyOf(itemDescriptors); // This, or a list from the start with contains -> add?
                });
            }
        }
        session.getGeyser().getLogger().warning("Unimplemented slot display type for input: " + slotDisplay);
        return null;
    }

    public static Pair<Item, ItemData> translateToOutput(GeyserSession session, SlotDisplay slotDisplay) {
        if (slotDisplay instanceof EmptySlotDisplay) {
            return null;
        }
        if (slotDisplay instanceof ItemSlotDisplay itemSlot) {
            int item = itemSlot.item();
            return Pair.of(Registries.JAVA_ITEMS.get(item), ItemTranslator.translateToBedrock(session, new ItemStack(item)));
        }
        if (slotDisplay instanceof ItemStackSlotDisplay itemStackSlot) {
            ItemStack stack = itemStackSlot.itemStack();
            return Pair.of(Registries.JAVA_ITEMS.get(stack.getId()), ItemTranslator.translateToBedrock(session, stack));
        }
        session.getGeyser().getLogger().warning("Unimplemented slot display type for output: " + slotDisplay);
        return null;
    }

    private static ItemDescriptorWithCount fromItem(GeyserSession session, int item) {
        if (item == Items.AIR_ID) {
            return ItemDescriptorWithCount.EMPTY;
        }
        ItemMapping mapping = session.getItemMappings().getMapping(item);
        return new ItemDescriptorWithCount(new DefaultDescriptor(mapping.getBedrockDefinition(), mapping.getBedrockData()), 1); // Need to check count
    }

    /**
     * Checks to see if this list of items matches with one of this Bedrock version's tags.
     */
    @Nullable
    private static List<ItemDescriptorWithCount> lookupBedrockTag(GeyserSession session, int[] items) {
        var bedrockTags = Registries.TAGS.forVersion(session.getUpstream().getProtocolVersion());
        String bedrockTag = bedrockTags.get(items);
        if (bedrockTag != null) {
            return Collections.singletonList(
                    new ItemDescriptorWithCount(new ItemTagDescriptor(bedrockTag), 1)
            );
        } else {
            return null;
        }
    }

    /**
     * Converts CompositeSlotDisplay contents to a list of basic ItemDescriptorWithCounts.
     */
    private static List<ItemDescriptorWithCount> fallbackCompositeMapping(GeyserSession session, CompositeSlotDisplay composite) {
        return composite.contents().stream()
                .map(subDisplay -> translateToInput(session, subDisplay))
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
    }

    public static Pair<List<List<ItemDescriptorWithCount>>, ItemData> combinations(GeyserSession session, SlotDisplay result, List<SlotDisplay> ingredients) {
        Pair<Item, ItemData> pair = translateToOutput(session, result);
        if (pair == null || !pair.right().isValid()) {
            // Likely modded item Bedrock will complain about
            // Implementation note: ItemData#isValid() may return true for air because count might be > 0 and the air definition may not be ItemDefinition.AIR
            return null;
        }

        ItemData output = pair.right();
        if (!(pair.left() instanceof BedrockRequiresTagItem)) {
            // Strip NBT - tools won't appear in the recipe book otherwise
            output = output.toBuilder().tag(null).build();
        }

        boolean empty = true;
        boolean complexInputs = false;
        List<List<ItemDescriptorWithCount>> inputs = new ArrayList<>(ingredients.size());
        for (SlotDisplay input : ingredients) {
            List<ItemDescriptorWithCount> translated = translateToInput(session, input);
            if (translated == null) {
                continue;
            }
            inputs.add(translated);
            if (translated.size() != 1 || translated.get(0) != ItemDescriptorWithCount.EMPTY) {
                empty = false;
            }
            complexInputs |= translated.size() > 1;
        }
        if (empty) {
            // Crashes Bedrock 1.19.70 otherwise
            // Fixes https://github.com/GeyserMC/Geyser/issues/3549
            return null;
        }

        if (complexInputs) {
            long size = 1;
            // See how big a cartesian product will get without creating one (Guava throws an error; not really ideal)
            for (List<ItemDescriptorWithCount> list : inputs) {
                size *= list.size();
                if (size > 500) {
                    // Too much. No.
                    complexInputs = false;
                    break;
                }
            }
            if (complexInputs) {
                return Pair.of(Lists.cartesianProduct(inputs), output);
            }
        }

        int totalSimpleRecipes = inputs.stream().mapToInt(List::size).max().orElse(1);

        // Sort inputs to create "uniform" simple recipes, if possible
        inputs = inputs.stream()
                .map(descriptors -> descriptors.stream()
                        .sorted(ItemDescriptorWithCountComparator.INSTANCE)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        List<List<ItemDescriptorWithCount>> finalRecipes = new ArrayList<>(totalSimpleRecipes);
        for (int i = 0; i < totalSimpleRecipes; i++) {
            int current = i;
            finalRecipes.add(inputs.stream().map(descriptors -> {
                if (descriptors.size() > current) {
                    return descriptors.get(current);
                }
                return descriptors.get(0);
            }).toList());
        }

        return Pair.of(finalRecipes, output);
    }

    private static class ItemDescriptorWithCountComparator implements Comparator<ItemDescriptorWithCount> {

        static ItemDescriptorWithCountComparator INSTANCE = new ItemDescriptorWithCountComparator();

        @Override
        public int compare(ItemDescriptorWithCount o1, ItemDescriptorWithCount o2) {
            String tag1 = null, tag2 = null;

            // Collect item tags first
            if (o1.getDescriptor() instanceof ItemTagDescriptor itemTagDescriptor) {
                tag1 = itemTagDescriptor.getItemTag();
            }

            if (o2.getDescriptor() instanceof ItemTagDescriptor itemTagDescriptor) {
                tag2 = itemTagDescriptor.getItemTag();
            }

            if (tag1 != null || tag2 != null) {
                if (tag1 != null && tag2 != null) {
                    return tag1.compareTo(tag2); // Just sort based on their string id
                }

                if (tag1 != null) {
                    return -1;
                }

                return 1; // the second is an item tag; which should be r
            }

            if (o1.getDescriptor() instanceof DefaultDescriptor defaultDescriptor1 && o2.getDescriptor() instanceof DefaultDescriptor defaultDescriptor2) {
                return IntComparators.NATURAL_COMPARATOR.compare(defaultDescriptor1.getItemId().getRuntimeId(), defaultDescriptor2.getItemId().getRuntimeId());
            }

            throw new IllegalStateException("Unable to compare unknown item descriptors: " + o1 + " and " + o2);
        }
    }
}
