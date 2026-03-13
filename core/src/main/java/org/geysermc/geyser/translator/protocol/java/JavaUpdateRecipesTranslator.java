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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.TrimDataPacket;
import org.geysermc.geyser.inventory.recipe.GeyserStonecutterData;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.tags.GeyserHolderSet;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemStackSlotDisplay;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundUpdateRecipesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundUpdateRecipesPacket.SelectableRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to send all valid recipes from Java to Bedrock.
 * <p>
 * Bedrock REQUIRES a CraftingDataPacket to be sent in order to craft anything.
 */
@Translator(packet = ClientboundUpdateRecipesPacket.class)
public class JavaUpdateRecipesTranslator extends PacketTranslator<ClientboundUpdateRecipesPacket> {

    private static final Key SMITHING_BASE = MinecraftKey.key("smithing_base");
    private static final Key SMITHING_TEMPLATE = MinecraftKey.key("smithing_template");
    private static final Key SMITHING_ADDITION = MinecraftKey.key("smithing_addition");

    @Override
    public void translate(GeyserSession session, ClientboundUpdateRecipesPacket packet) {
        int netId = session.getLastRecipeNetId().get();

        boolean oldSmithingTable;
        int[] smithingBase = packet.getItemSets().get(SMITHING_BASE);
        int[] smithingTemplate = packet.getItemSets().get(SMITHING_TEMPLATE);
        int[] smithingAddition = packet.getItemSets().get(SMITHING_ADDITION);
        if (smithingBase == null || smithingTemplate == null || smithingAddition == null) {
            // We're probably on a version before the smithing table got expanded functionality.
            oldSmithingTable = true;
        } else {
            oldSmithingTable = false;
            // BDS sends armor trim templates and materials before the CraftingDataPacket
            TrimDataPacket trimDataPacket = new TrimDataPacket();
            trimDataPacket.getPatterns().addAll(session.getRegistryCache().registry(JavaRegistries.TRIM_PATTERN).values()); // TODO this is wrong!! See the TODOs in the registry readers
            trimDataPacket.getMaterials().addAll(session.getRegistryCache().registry(JavaRegistries.TRIM_MATERIAL).values());
            session.sendUpstreamPacket(trimDataPacket);
        }
        session.getGeyser().getLogger().debug("Using old smithing table workaround? " + oldSmithingTable);
        session.setOldSmithingTable(oldSmithingTable);

        Int2ObjectMap<List<SelectableRecipe>> rawStonecutterData = new Int2ObjectOpenHashMap<>();

        List<SelectableRecipe> stonecutterRecipes = packet.getStonecutterRecipes();
        for (SelectableRecipe recipe : stonecutterRecipes) {
            // Hardcoding the heck out of this until we see different examples of how this works.
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
            // Implementation note: data used to have to be sorted according to the item translation key.
            // This is no longer necessary as of 1.21.2, and is instead presented in the order the server sends us.
            // (Recipes are ordered differently between Paper and vanilla)
            // See #5150.
            int buttonId = 0;
            for (SelectableRecipe recipe : data.getValue()) {
                int javaInput = data.getIntKey();
                ItemMapping mapping = session.getItemMappings().getMapping(javaInput);
                if (mapping.getJavaItem() == Items.AIR) {
                    // Modded ?
                    continue;
                }
                ItemStack javaOutput = ((ItemStackSlotDisplay) recipe.recipe()).itemStack();
                ItemData output = ItemTranslator.translateToBedrock(session, javaOutput);
                if (!output.isValid()) {
                    // Probably modded items
                    continue;
                }
                int recipeNetId = netId++;

                // Save the recipe list for reference when crafting
                // Add the net ID as the key and the button required + output for the value
                stonecutterRecipeMap.put(recipeNetId, new GeyserStonecutterData(buttonId++, javaInput, javaOutput));

                // Currently, stone cutter recipes are not locked/unlocked on Bedrock; so no need to cache their identifiers.
            }
        }

        session.sendUpstreamPacket(session.getCraftingDataPacket());
        session.setStonecutterRecipes(stonecutterRecipeMap);
        session.getLastRecipeNetId().set(netId);
    }
}
