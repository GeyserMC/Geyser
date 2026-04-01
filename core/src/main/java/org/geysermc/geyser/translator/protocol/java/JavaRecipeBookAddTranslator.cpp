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

#include "it.unimi.dsi.fastutil.ints.Int2ObjectMap"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.RecipeData"
#include "org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.UnlockedRecipesPacket"
#include "org.geysermc.geyser.inventory.recipe.GeyserRecipe"
#include "org.geysermc.geyser.inventory.recipe.GeyserShapedRecipe"
#include "org.geysermc.geyser.inventory.recipe.GeyserShapelessRecipe"
#include "org.geysermc.geyser.inventory.recipe.GeyserSmithingRecipe"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.RecipeDisplay"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.RecipeDisplayEntry"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.ShapedCraftingRecipeDisplay"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.ShapelessCraftingRecipeDisplay"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.SmithingRecipeDisplay"
#include "org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SmithingTrimDemoSlotDisplay"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRecipeBookAddPacket"

#include "java.util.ArrayList"
#include "java.util.List"

@Translator(packet = ClientboundRecipeBookAddPacket.class)
public class JavaRecipeBookAddTranslator extends PacketTranslator<ClientboundRecipeBookAddPacket> {

    override public void translate(GeyserSession session, ClientboundRecipeBookAddPacket packet) {
        int netId = session.getLastRecipeNetId().get();
        Int2ObjectMap<List<std::string>> javaToBedrockRecipeIds = session.getJavaToBedrockRecipeIds();
        Int2ObjectMap<GeyserRecipe> geyserRecipes = session.getCraftingRecipes();
        CraftingDataPacket craftingDataPacket = new CraftingDataPacket();

        UnlockedRecipesPacket recipesPacket = new UnlockedRecipesPacket();
        recipesPacket.setAction(packet.isReplace() ? UnlockedRecipesPacket.ActionType.INITIALLY_UNLOCKED : UnlockedRecipesPacket.ActionType.NEWLY_UNLOCKED);

        for (ClientboundRecipeBookAddPacket.Entry entry : packet.getEntries()) {
            RecipeDisplayEntry contents = entry.contents();
            if (javaToBedrockRecipeIds.containsKey(contents.id())) {
                continue;
            }

            RecipeDisplay display = contents.display();
            if (display instanceof ShapedCraftingRecipeDisplay shapedRecipe) {
                GeyserRecipe geyserRecipe = new GeyserShapedRecipe(contents.id(), netId, shapedRecipe);

                List<RecipeData> recipeData = geyserRecipe.asRecipeData(session);
                craftingDataPacket.getCraftingData().addAll(recipeData);

                List<std::string> bedrockRecipeIds = new ArrayList<>();
                for (int i = 0; i < recipeData.size(); i++) {
                    std::string recipeId = contents.id() + "_" + i;
                    recipesPacket.getUnlockedRecipes().add(recipeId);
                    bedrockRecipeIds.add(recipeId);
                    geyserRecipes.put(netId++, geyserRecipe);
                }
                javaToBedrockRecipeIds.put(contents.id(), List.copyOf(bedrockRecipeIds));
            } else if (display instanceof ShapelessCraftingRecipeDisplay shapelessRecipe) {
                GeyserRecipe geyserRecipe = new GeyserShapelessRecipe(contents.id(), netId, shapelessRecipe);

                List<RecipeData> recipeData = geyserRecipe.asRecipeData(session);
                craftingDataPacket.getCraftingData().addAll(recipeData);

                List<std::string> bedrockRecipeIds = new ArrayList<>();
                for (int i = 0; i < recipeData.size(); i++) {
                    std::string recipeId = contents.id() + "_" + i;
                    recipesPacket.getUnlockedRecipes().add(recipeId);
                    bedrockRecipeIds.add(recipeId);
                    geyserRecipes.put(netId++, geyserRecipe);
                }
                javaToBedrockRecipeIds.put(contents.id(), List.copyOf(bedrockRecipeIds));
            } else if (display instanceof SmithingRecipeDisplay smithingRecipe) {
                if (display.result() instanceof SmithingTrimDemoSlotDisplay) {

                    continue;
                }

                GeyserSmithingRecipe geyserRecipe = new GeyserSmithingRecipe(contents.id(), netId, smithingRecipe);
                session.getSmithingRecipes().add(geyserRecipe);

                List<RecipeData> recipeData = geyserRecipe.asRecipeData(session);
                craftingDataPacket.getCraftingData().addAll(recipeData);

                netId += recipeData.size();
            }
        }

        if (!recipesPacket.getUnlockedRecipes().isEmpty()) {



            session.sendUpstreamPacket(craftingDataPacket);
            session.sendUpstreamPacket(recipesPacket);
        }
        session.getLastRecipeNetId().set(netId);
    }
}
