/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.java;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.RecipeUnlockingRequirement;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapedRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.DefaultDescriptor;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.UnlockedRecipesPacket;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.BedrockRequiresTagItem;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.tags.Tag;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.RecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.RecipeDisplayEntry;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.ShapedCraftingRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.ShapelessCraftingRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.CompositeSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.EmptySlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemStackSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.TagSlotDisplay;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRecipeBookAddPacket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Translator(packet = ClientboundRecipeBookAddPacket.class)
public class JavaRecipeBookAddTranslator extends PacketTranslator<ClientboundRecipeBookAddPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundRecipeBookAddPacket packet) {
        System.out.println(packet);
        int netId = session.getLastRecipeNetId().get();
        Int2ObjectMap<List<String>> javaToBedrockRecipeIds = session.getJavaToBedrockRecipeIds();
        CraftingDataPacket craftingDataPacket = new CraftingDataPacket();
        // Check if we should set cleanRecipes here or not.


        UnlockedRecipesPacket recipesPacket = new UnlockedRecipesPacket();
        recipesPacket.setAction(packet.isReplace() ? UnlockedRecipesPacket.ActionType.INITIALLY_UNLOCKED : UnlockedRecipesPacket.ActionType.NEWLY_UNLOCKED);

        for (ClientboundRecipeBookAddPacket.Entry entry : packet.getEntries()) {
            RecipeDisplayEntry contents = entry.contents();
            RecipeDisplay display = contents.display();

            switch (display.getType()) {
                case CRAFTING_SHAPED -> {
                    ShapedCraftingRecipeDisplay shapedRecipe = (ShapedCraftingRecipeDisplay) display;
                    Pair<Item, ItemData> pair = translateToOutput(session, shapedRecipe.result());
                    if (pair == null || !pair.right().isValid()) {
                        // Likely modded item Bedrock will complain about
                        continue;
                    }

                    ItemData output = pair.right();
                    if (!(pair.left() instanceof BedrockRequiresTagItem)) {
                        // Strip NBT - tools won't appear in the recipe book otherwise
                        output = output.toBuilder().tag(null).build();
                    }

                    boolean empty = true;
                    boolean complexInputs = false;
                    List<ItemDescriptorWithCount[]> inputs = new ArrayList<>(shapedRecipe.ingredients().size());
                    for (SlotDisplay input : shapedRecipe.ingredients()) {
                        ItemDescriptorWithCount[] translated = translateToInput(session, input);
                        if (translated == null) {
                            continue;
                        }
                        inputs.add(translated);
                        if (translated.length != 1 || translated[0] != ItemDescriptorWithCount.EMPTY) {
                            empty = false;
                        }
                        complexInputs |= translated.length > 1;
                    }
                    if (empty) {
                        // Crashes Bedrock 1.19.70 otherwise
                        // Fixes https://github.com/GeyserMC/Geyser/issues/3549
                        continue;
                    }

                    if (complexInputs) {

                    } else {
                        String recipeId = Integer.toString(contents.id());
                        craftingDataPacket.getCraftingData().add(ShapedRecipeData.shaped(recipeId,
                                shapedRecipe.width(), shapedRecipe.height(), inputs.stream().map(descriptors -> descriptors[0]).toList(),
                                Collections.singletonList(output), UUID.randomUUID(), "crafting_table", 0, netId++, false, RecipeUnlockingRequirement.INVALID));
                        recipesPacket.getUnlockedRecipes().add(recipeId);
                        javaToBedrockRecipeIds.put(contents.id(), Collections.singletonList(recipeId));
                    }
                }
                case CRAFTING_SHAPELESS -> {
                    ShapelessCraftingRecipeDisplay shapelessRecipe = (ShapelessCraftingRecipeDisplay) display;
                    Pair<Item, ItemData> pair = translateToOutput(session, shapelessRecipe.result());
                    if (pair == null || !pair.right().isValid()) {
                        // Likely modded item Bedrock will complain about
                        continue;
                    }

                    ItemData output = pair.right();
                    if (!(pair.left() instanceof BedrockRequiresTagItem)) {
                        // Strip NBT - tools won't appear in the recipe book otherwise
                        output = output.toBuilder().tag(null).build();
                    }
                }
            }
        }

        System.out.println(craftingDataPacket);
        session.sendUpstreamPacket(craftingDataPacket);
        session.sendUpstreamPacket(recipesPacket);
        session.getLastRecipeNetId().set(netId);

        // Multi-version can mean different Bedrock item IDs
        TAG_TO_ITEM_DESCRIPTOR_CACHE.remove();
    }

    private static final ThreadLocal<Map<int[], ItemDescriptorWithCount[]>> TAG_TO_ITEM_DESCRIPTOR_CACHE = ThreadLocal.withInitial(Object2ObjectOpenHashMap::new);

    private ItemDescriptorWithCount[] translateToInput(GeyserSession session, SlotDisplay slotDisplay) {
        if (slotDisplay instanceof EmptySlotDisplay) {
            return new ItemDescriptorWithCount[] {ItemDescriptorWithCount.EMPTY};
        }
        if (slotDisplay instanceof CompositeSlotDisplay composite) {
            if (composite.contents().size() == 1) {
                return translateToInput(session, composite.contents().get(0));
            }
            return composite.contents().stream()
                .map(subDisplay -> translateToInput(session, subDisplay))
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .toArray(ItemDescriptorWithCount[]::new);
        }
        if (slotDisplay instanceof ItemSlotDisplay itemSlot) {
            return new ItemDescriptorWithCount[] {fromItem(session, itemSlot.item())};
        }
        if (slotDisplay instanceof ItemStackSlotDisplay itemStackSlot) {
            ItemData item = ItemTranslator.translateToBedrock(session, itemStackSlot.itemStack());
            return new ItemDescriptorWithCount[] {ItemDescriptorWithCount.fromItem(item)};
        }
        if (slotDisplay instanceof TagSlotDisplay tagSlot) {
            Key tag = tagSlot.tag();
            int[] items = session.getTagCache().getRaw(new Tag<>(JavaRegistries.ITEM, tag)); // I don't like this...
            if (items == null || items.length == 0) {
                return new ItemDescriptorWithCount[] {ItemDescriptorWithCount.EMPTY};
            } else if (items.length == 1) {
                return new ItemDescriptorWithCount[] {fromItem(session, items[0])};
            } else {
                // Cache is implemented as, presumably, an item tag will be used multiple times in succession
                // (E.G. a chest with planks tags)
                return TAG_TO_ITEM_DESCRIPTOR_CACHE.get().computeIfAbsent(items, key -> {
//                    String molang = "q.is_item_name_any('', "
//                        + Arrays.stream(items).mapToObj(item -> {
//                            ItemMapping mapping = session.getItemMappings().getMapping(item);
//                            return "'" + mapping.getBedrockIdentifier() + "'";
//                        }).collect(Collectors.joining(", "))
//                        + ")";
//                    String molang = Arrays.stream(items).mapToObj(item -> {
//                        ItemMapping mapping = session.getItemMappings().getMapping(item);
//                        return "q.identifier == '" + mapping.getBedrockIdentifier() + "'";
//                    }).collect(Collectors.joining(" || "));
//                    if ("minecraft:planks".equals(tag.toString())) {
//                        String molang = "q.any_tag('minecraft:planks')";
//                        return new ItemDescriptorWithCount[] {new ItemDescriptorWithCount(new MolangDescriptor(molang, 10), 1)};
//                    }
                    return null;
//                    Set<ItemDescriptorWithCount> itemDescriptors = new HashSet<>();
//                    for (int item : key) {
//                        itemDescriptors.add(fromItem(session, item));
//                    }
//                    return itemDescriptors.toArray(ItemDescriptorWithCount[]::new);
                });
            }
        }
        session.getGeyser().getLogger().warning("Unimplemented slot display type for input: " + slotDisplay);
        return null;
    }

    private Pair<Item, ItemData> translateToOutput(GeyserSession session, SlotDisplay slotDisplay) {
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

    private ItemDescriptorWithCount fromItem(GeyserSession session, int item) {
        if (item == Items.AIR_ID) {
            return ItemDescriptorWithCount.EMPTY;
        }
        ItemMapping mapping = session.getItemMappings().getMapping(item);
        return new ItemDescriptorWithCount(new DefaultDescriptor(mapping.getBedrockDefinition(), mapping.getBedrockData()), 1); // Need to check count
    }

//    private static ItemDescriptorWithCount[][] combinations(ItemDescriptorWithCount[] itemDescriptors) {
//        int totalCombinations = 1;
//        for (Set<ItemDescriptorWithCount> optionSet : squashedOptions.keySet()) {
//            totalCombinations *= optionSet.size();
//        }
//        if (totalCombinations > 500) {
//            ItemDescriptorWithCount[] translatedItems = new ItemDescriptorWithCount[ingredients.length];
//            for (int i = 0; i < ingredients.length; i++) {
//                if (ingredients[i].getOptions().length > 0) {
//                    translatedItems[i] = ItemDescriptorWithCount.fromItem(ItemTranslator.translateToBedrock(session, ingredients[i].getOptions()[0]));
//                } else {
//                    translatedItems[i] = ItemDescriptorWithCount.EMPTY;
//                }
//            }
//            return new ItemDescriptorWithCount[][]{translatedItems};
//        }
//        List<Set<ItemDescriptorWithCount>> sortedSets = new ArrayList<>(squashedOptions.keySet());
//        sortedSets.sort(Comparator.comparing(Set::size, Comparator.reverseOrder()));
//        ItemDescriptorWithCount[][] combinations = new ItemDescriptorWithCount[totalCombinations][ingredients.length];
//        int x = 1;
//        for (Set<ItemDescriptorWithCount> set : sortedSets) {
//            IntSet slotSet = squashedOptions.get(set);
//            int i = 0;
//            for (ItemDescriptorWithCount item : set) {
//                for (int j = 0; j < totalCombinations / set.size(); j++) {
//                    final int comboIndex = (i * x) + (j % x) + ((j / x) * set.size() * x);
//                    for (IntIterator it = slotSet.iterator(); it.hasNext(); ) {
//                        combinations[comboIndex][it.nextInt()] = item;
//                    }
//                }
//                i++;
//            }
//            x *= set.size();
//        }
//    }
}
