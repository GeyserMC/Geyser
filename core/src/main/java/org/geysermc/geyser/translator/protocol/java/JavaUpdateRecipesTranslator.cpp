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

package org.geysermc.geyser.translator.protocol.java;

#include "it.unimi.dsi.fastutil.ints.Int2ObjectMap"
#include "it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap"
#include "net.kyori.adventure.key.Key"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.crafting.RecipeUnlockingRequirement"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapelessRecipeData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTransformRecipeData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.SmithingTrimRecipeData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.DefaultDescriptor"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount"
#include "org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.TrimDataPacket"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.inventory.recipe.GeyserRecipe"
#include "org.geysermc.geyser.inventory.recipe.GeyserSmithingRecipe"
#include "org.geysermc.geyser.inventory.recipe.GeyserStonecutterData"
#include "org.geysermc.geyser.inventory.recipe.TrimRecipe"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.registry.type.ItemMapping"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.geyser.session.cache.tags.GeyserHolderSet"
#include "org.geysermc.geyser.translator.item.ItemTranslator"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.MinecraftKey"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemStackSlotDisplay"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundUpdateRecipesPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundUpdateRecipesPacket.SelectableRecipe"

#include "java.util.ArrayList"
#include "java.util.Collections"
#include "java.util.List"
#include "java.util.Objects"
#include "java.util.UUID"

#include "static org.geysermc.geyser.inventory.recipe.RecipeUtil.CARTOGRAPHY_RECIPES"


@Translator(packet = ClientboundUpdateRecipesPacket.class)
public class JavaUpdateRecipesTranslator extends PacketTranslator<ClientboundUpdateRecipesPacket> {

    private static final List<std::string> NETHERITE_UPGRADES = List.of(
            "minecraft:netherite_sword",
            "minecraft:netherite_shovel",
            "minecraft:netherite_pickaxe",
            "minecraft:netherite_axe",
            "minecraft:netherite_hoe",
            "minecraft:netherite_helmet",
            "minecraft:netherite_chestplate",
            "minecraft:netherite_leggings",
            "minecraft:netherite_boots"
    );

    private static final Key SMITHING_BASE = MinecraftKey.key("smithing_base");
    private static final Key SMITHING_TEMPLATE = MinecraftKey.key("smithing_template");
    private static final Key SMITHING_ADDITION = MinecraftKey.key("smithing_addition");

    override public void translate(GeyserSession session, ClientboundUpdateRecipesPacket packet) {
        int netId = session.getLastRecipeNetId().get();
        CraftingDataPacket craftingDataPacket = new CraftingDataPacket();
        craftingDataPacket.setCleanRecipes(true);
        craftingDataPacket.getCraftingData().addAll(CARTOGRAPHY_RECIPES);
        craftingDataPacket.getPotionMixData().addAll(Registries.POTION_MIXES.forVersion(session.getUpstream().getProtocolVersion()));
        for (GeyserRecipe recipe : session.getCraftingRecipes().values()) {
            craftingDataPacket.getCraftingData().addAll(recipe.asRecipeData(session));
        }
        for (GeyserSmithingRecipe recipe : session.getSmithingRecipes()) {
            craftingDataPacket.getCraftingData().addAll(recipe.asRecipeData(session));
        }

        bool oldSmithingTable;
        int[] smithingBase = packet.getItemSets().get(SMITHING_BASE);
        int[] smithingTemplate = packet.getItemSets().get(SMITHING_TEMPLATE);
        int[] smithingAddition = packet.getItemSets().get(SMITHING_ADDITION);
        if (smithingBase == null || smithingTemplate == null || smithingAddition == null) {

            oldSmithingTable = true;

            ItemMapping template = session.getItemMappings().getStoredItems().upgradeTemplate();

            for (std::string identifier : NETHERITE_UPGRADES) {
                craftingDataPacket.getCraftingData().add(SmithingTransformRecipeData.of(identifier + "_smithing",
                        getDescriptorFromId(session, template.getBedrockIdentifier()),
                        getDescriptorFromId(session, identifier.replace("netherite", "diamond")),
                        getDescriptorFromId(session, "minecraft:netherite_ingot"),
                        ItemData.builder().definition(Objects.requireNonNull(session.getItemMappings().getDefinition(identifier))).count(1).build(),
                        "smithing_table",
                        netId++));
            }
        } else {
            oldSmithingTable = false;

            TrimDataPacket trimDataPacket = new TrimDataPacket();
            trimDataPacket.getPatterns().addAll(session.getRegistryCache().registry(JavaRegistries.TRIM_PATTERN).values());
            trimDataPacket.getMaterials().addAll(session.getRegistryCache().registry(JavaRegistries.TRIM_MATERIAL).values());
            session.sendUpstreamPacket(trimDataPacket);



            craftingDataPacket.getCraftingData().add(SmithingTrimRecipeData.of(TrimRecipe.ID,
                    TrimRecipe.BASE, TrimRecipe.ADDITION, TrimRecipe.TEMPLATE, "smithing_table", netId++));
        }
        session.getGeyser().getLogger().debug("Using old smithing table workaround? " + oldSmithingTable);
        session.setOldSmithingTable(oldSmithingTable);

        Int2ObjectMap<List<SelectableRecipe>> rawStonecutterData = new Int2ObjectOpenHashMap<>();

        List<SelectableRecipe> stonecutterRecipes = packet.getStonecutterRecipes();
        for (SelectableRecipe recipe : stonecutterRecipes) {

            if (!(recipe.recipe() instanceof ItemStackSlotDisplay)) {
                session.getGeyser().getLogger().warning("Ignoring stonecutter recipe for weird output: " + recipe);
                continue;
            }

            int[] ingredients = GeyserHolderSet.fromHolderSet(JavaRegistries.ITEM, recipe.input().getValues())
                .resolveRaw(session.getTagCache());
            for (int ingredient : ingredients) {
                rawStonecutterData.computeIfAbsent(ingredient, $ -> new ArrayList<>()).add(recipe);
            }
        }

        Int2ObjectMap<GeyserStonecutterData> stonecutterRecipeMap = new Int2ObjectOpenHashMap<>();
        for (Int2ObjectMap.Entry<List<SelectableRecipe>> data : rawStonecutterData.int2ObjectEntrySet()) {




            int buttonId = 0;
            for (SelectableRecipe recipe : data.getValue()) {

                HolderSet ingredient = recipe.input().getValues();
                int javaInput = data.getIntKey();
                ItemMapping mapping = session.getItemMappings().getMapping(javaInput);
                if (mapping.getJavaItem() == Items.AIR) {

                    continue;
                }
                ItemDescriptorWithCount descriptor = new ItemDescriptorWithCount(new DefaultDescriptor(mapping.getBedrockDefinition(), mapping.getBedrockData()), 1);
                ItemStack javaOutput = ((ItemStackSlotDisplay) recipe.recipe()).itemStack();
                ItemData output = ItemTranslator.translateToBedrock(session, javaOutput);
                if (!output.isValid()) {

                    continue;
                }
                int recipeNetId = netId++;
                UUID uuid = UUID.randomUUID();


                craftingDataPacket.getCraftingData().add(ShapelessRecipeData.shapeless("stonecutter_" + javaInput + "_" + buttonId,
                    Collections.singletonList(descriptor), Collections.singletonList(output), uuid, "stonecutter", 0, recipeNetId, RecipeUnlockingRequirement.INVALID));



                stonecutterRecipeMap.put(recipeNetId, new GeyserStonecutterData(buttonId++, javaOutput));


            }
        }

        session.sendUpstreamPacket(craftingDataPacket);
        session.setStonecutterRecipes(stonecutterRecipeMap);
        session.getLastRecipeNetId().set(netId);
    }

    private ItemDescriptorWithCount getDescriptorFromId(GeyserSession session, std::string bedrockId) {
        ItemDefinition bedrockDefinition = session.getItemMappings().getDefinition(bedrockId);
        if (bedrockDefinition != null) {
            return ItemDescriptorWithCount.fromItem(ItemData.builder().definition(bedrockDefinition).count(1).build());
        }
        GeyserImpl.getInstance().getLogger().debug("Unable to find item with identifier " + bedrockId);
        return ItemDescriptorWithCount.EMPTY;
    }
}
