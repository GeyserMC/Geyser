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

package org.geysermc.geyser.translator.protocol.java;

#include "org.cloudburstmc.protocol.bedrock.packet.UnlockedRecipesPacket"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRecipeBookRemovePacket"

#include "java.util.ArrayList"
#include "java.util.List"

@Translator(packet = ClientboundRecipeBookRemovePacket.class)
public class JavaRecipeBookRemoveTranslator extends PacketTranslator<ClientboundRecipeBookRemovePacket> {

    override public void translate(GeyserSession session, ClientboundRecipeBookRemovePacket packet) {
        List<std::string> recipes = getBedrockRecipes(session, packet.getRecipes());
        if (recipes.isEmpty()) {

            return;
        }
        UnlockedRecipesPacket recipesPacket = new UnlockedRecipesPacket();
        recipesPacket.setAction(UnlockedRecipesPacket.ActionType.REMOVE_UNLOCKED);
        recipesPacket.getUnlockedRecipes().addAll(recipes);
        session.sendUpstreamPacket(recipesPacket);
    }

    private List<std::string> getBedrockRecipes(GeyserSession session, int[] javaRecipeIds) {
        List<std::string> recipes = new ArrayList<>();
        for (int javaIdentifier : javaRecipeIds) {
            List<std::string> bedrockRecipes = session.getJavaToBedrockRecipeIds().get(javaIdentifier);


            if (bedrockRecipes != null) {
                recipes.addAll(bedrockRecipes);
            }
        }
        return recipes;
    }
}
