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

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.RecipeUnlockingRequirement;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapedRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTransformRecipeData;
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
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.SmithingRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.CompositeSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.EmptySlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemStackSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.TagSlotDisplay;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRecipeBookAddPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
                    List<List<ItemDescriptorWithCount>> inputs = new ArrayList<>(shapedRecipe.ingredients().size());
                    for (SlotDisplay input : shapedRecipe.ingredients()) {
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
                        continue;
                    }

                    if (complexInputs) {
                        System.out.println(inputs);
                        if (true) continue;
                        List<List<ItemDescriptorWithCount>> processedInputs = Lists.cartesianProduct(inputs);
                        System.out.println(processedInputs.size());
                        if (processedInputs.size() <= 500) { // Do not let us process giant lists.
                            List<String> bedrockRecipeIds = new ArrayList<>();
                            for (int i = 0; i < processedInputs.size(); i++) {
                                List<ItemDescriptorWithCount> possibleInput = processedInputs.get(i);
                                String recipeId = contents.id() + "_" + i;
                                craftingDataPacket.getCraftingData().add(ShapedRecipeData.shaped(recipeId,
                                    shapedRecipe.width(), shapedRecipe.height(), possibleInput,
                                    Collections.singletonList(output), UUID.randomUUID(), "crafting_table", 0, netId++, false, RecipeUnlockingRequirement.INVALID));
                                recipesPacket.getUnlockedRecipes().add(recipeId);
                                bedrockRecipeIds.add(recipeId);
                            }
                            javaToBedrockRecipeIds.put(contents.id(), bedrockRecipeIds);
                            continue;
                        }
                    }
                    String recipeId = Integer.toString(contents.id());
                    craftingDataPacket.getCraftingData().add(ShapedRecipeData.shaped(recipeId,
                        shapedRecipe.width(), shapedRecipe.height(), inputs.stream().map(descriptors -> descriptors.get(0)).toList(),
                        Collections.singletonList(output), UUID.randomUUID(), "crafting_table", 0, netId++, false, RecipeUnlockingRequirement.INVALID));
                    recipesPacket.getUnlockedRecipes().add(recipeId);
                    javaToBedrockRecipeIds.put(contents.id(), Collections.singletonList(recipeId));
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
                case SMITHING -> {
                    if (true) {
                        System.out.println(display);
                        continue;
                    }
                    SmithingRecipeDisplay smithingRecipe = (SmithingRecipeDisplay) display;
                    Pair<Item, ItemData> output = translateToOutput(session, smithingRecipe.result());
                    if (output == null) {
                        continue;
                    }

                    List<ItemDescriptorWithCount> bases = translateToInput(session, smithingRecipe.base());
                    List<ItemDescriptorWithCount> templates = translateToInput(session, smithingRecipe.template());
                    List<ItemDescriptorWithCount> additions = translateToInput(session, smithingRecipe.addition());

                    if (bases == null || templates == null || additions == null) {
                        continue;
                    }

                    int i = 0;
                    List<String> bedrockRecipeIds = new ArrayList<>();
                    for (ItemDescriptorWithCount template : templates) {
                        for (ItemDescriptorWithCount base : bases) {
                            for (ItemDescriptorWithCount addition : additions) {
                                String id = contents.id() + "_" + i++;
                                // Note: vanilla inputs use aux value of Short.MAX_VALUE
                                craftingDataPacket.getCraftingData().add(SmithingTransformRecipeData.of(id,
                                        template, base, addition, output.right(), "smithing_table", netId++));

                                recipesPacket.getUnlockedRecipes().add(id);
                                bedrockRecipeIds.add(id);
                            }
                        }
                    }
                    javaToBedrockRecipeIds.put(contents.id(), bedrockRecipeIds);
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

    private static final ThreadLocal<Map<int[], List<ItemDescriptorWithCount>>> TAG_TO_ITEM_DESCRIPTOR_CACHE = ThreadLocal.withInitial(Object2ObjectOpenHashMap::new);

    private List<ItemDescriptorWithCount> translateToInput(GeyserSession session, SlotDisplay slotDisplay) {
        if (slotDisplay instanceof EmptySlotDisplay) {
            return Collections.singletonList(ItemDescriptorWithCount.EMPTY);
        }
        if (slotDisplay instanceof CompositeSlotDisplay composite) {
            if (composite.contents().size() == 1) {
                return translateToInput(session, composite.contents().get(0));
            }
            return composite.contents().stream()
                .map(subDisplay -> translateToInput(session, subDisplay))
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
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
//                        return Collections.singletonList(new ItemDescriptorWithCount(new MolangDescriptor(molang, 10), 1));
//                    }

                    Set<ItemDescriptorWithCount> itemDescriptors = new HashSet<>();
                    for (int item : key) {
                        itemDescriptors.add(fromItem(session, item));
                    }
                    return new ArrayList<>(itemDescriptors); // This, or a list from the start with contains -> add?
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
}
