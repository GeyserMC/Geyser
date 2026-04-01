/*
 * Copyright (c) 2024-2025 GeyserMC. http://geysermc.org
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

import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.geyser.util.PlayerListUtils;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;

import java.util.ArrayList;
import java.util.List;

import static org.geysermc.geyser.inventory.recipe.RecipeUtil.CARTOGRAPHY_RECIPES;

@Translator(packet = ClientboundFinishConfigurationPacket.class)
public class JavaFinishConfigurationTranslator extends PacketTranslator<ClientboundFinishConfigurationPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundFinishConfigurationPacket packet) {
        
        List<PlayerListPacket.Entry> entries = new ArrayList<>();
        session.getEntityCache().forEachPlayerEntity(otherPlayer -> {
            entries.add(new PlayerListPacket.Entry(otherPlayer.getTabListUuid()));
        });
        if (!entries.isEmpty()) {
            PlayerListUtils.batchSendPlayerList(session, entries, PlayerListPacket.Action.REMOVE);
        }
        session.getEntityCache().removeAllPlayerEntities();

        
        
        CraftingDataPacket craftingDataPacket = new CraftingDataPacket();
        craftingDataPacket.setCleanRecipes(true);
        craftingDataPacket.getCraftingData().addAll(CARTOGRAPHY_RECIPES);
        craftingDataPacket.getPotionMixData().addAll(Registries.POTION_MIXES.forVersion(session.getUpstream().getProtocolVersion()));
        if (session.isSentSpawnPacket()) {
            session.getUpstream().sendPacket(craftingDataPacket);
            
            
            if (session.getStonecutterRecipes() != null) {
                session.getLastRecipeNetId().set(InventoryUtils.LAST_RECIPE_NET_ID + 1);
                session.getCraftingRecipes().clear();
                session.getJavaToBedrockRecipeIds().clear();
                session.getSmithingRecipes().clear();
                session.getStonecutterRecipes().clear();
            }
        } else {
            session.getUpstream().queuePostStartGamePacket(craftingDataPacket);
        }

        
        
        
        
        session.getWorldCache().resetScoreboard();

        
        session.getComponentCache().resolveComponents();
    }
}
