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

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundRecipePacket;
import org.cloudburstmc.protocol.bedrock.packet.UnlockedRecipesPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

import java.util.Arrays;
import java.util.List;

@Translator(packet = ClientboundRecipePacket.class)
public class JavaClientboundRecipesTranslator extends PacketTranslator<ClientboundRecipePacket> {

    @Override
    public void translate(GeyserSession session, ClientboundRecipePacket packet) {
        UnlockedRecipesPacket recipesPacket = new UnlockedRecipesPacket();
        //GeyserImpl.getInstance().getLogger().info("DEBUG: init packet" + packet);

        // current state: recipes do show up; but are not taken away/properly unlocked. Need to do more BDS digging
        switch (packet.getAction()) {
            case INIT -> {
                GeyserImpl.getInstance().getLogger().info("Sending all recipes to client");
                recipesPacket.setAction(UnlockedRecipesPacket.ActionType.INITIALLY_UNLOCKED);
                recipesPacket.getUnlockedRecipes().addAll(List.of(packet.getAlreadyKnownRecipes()));
            }
            case ADD -> {
                recipesPacket.setAction(UnlockedRecipesPacket.ActionType.NEWLY_UNLOCKED);
                recipesPacket.getUnlockedRecipes().addAll(List.of(packet.getRecipes()));
            }
            case REMOVE -> {
                // TODO: doesnt fully work yet.
                recipesPacket.setAction(UnlockedRecipesPacket.ActionType.REMOVE_UNLOCKED);
                recipesPacket.getUnlockedRecipes().removeAll(Arrays.asList(packet.getRecipes()));
            }
        }

        // not yet lol
        session.sendUpstreamPacket(recipesPacket);
    }
}

