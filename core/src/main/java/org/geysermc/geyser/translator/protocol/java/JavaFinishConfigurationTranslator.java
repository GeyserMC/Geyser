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

import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.MultiRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.RecipeData;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;

import java.util.List;
import java.util.UUID;

import static org.geysermc.geyser.util.InventoryUtils.LAST_RECIPE_NET_ID;

@Translator(packet = ClientboundFinishConfigurationPacket.class)
public class JavaFinishConfigurationTranslator extends PacketTranslator<ClientboundFinishConfigurationPacket> {
    /**
     * Required to use the specified cartography table recipes
     */
    private static final List<RecipeData> CARTOGRAPHY_RECIPES = List.of(
        MultiRecipeData.of(UUID.fromString("8b36268c-1829-483c-a0f1-993b7156a8f2"), ++LAST_RECIPE_NET_ID), // Map extending
        MultiRecipeData.of(UUID.fromString("442d85ed-8272-4543-a6f1-418f90ded05d"), ++LAST_RECIPE_NET_ID), // Map cloning
        MultiRecipeData.of(UUID.fromString("98c84b38-1085-46bd-b1ce-dd38c159e6cc"), ++LAST_RECIPE_NET_ID), // Map upgrading
        MultiRecipeData.of(UUID.fromString("602234e4-cac1-4353-8bb7-b1ebff70024b"), ++LAST_RECIPE_NET_ID) // Map locking
    );

    @Override
    public void translate(GeyserSession session, ClientboundFinishConfigurationPacket packet) {
        // Clear the player list, as on Java the player list is cleared after transitioning from config to play phase
        PlayerListPacket playerListPacket = new PlayerListPacket();
        playerListPacket.setAction(PlayerListPacket.Action.REMOVE);
        for (PlayerEntity otherEntity : session.getEntityCache().getAllPlayerEntities()) {
            playerListPacket.getEntries().add(new PlayerListPacket.Entry(otherEntity.getTabListUuid()));
        }
        session.sendUpstreamPacket(playerListPacket);
        session.getEntityCache().removeAllPlayerEntities();

        // Potion mixes are registered by default, as they are needed to be able to put ingredients into the brewing stand.
        // (Also add it here so recipes get cleared on configuration - 1.21.3)
        CraftingDataPacket craftingDataPacket = new CraftingDataPacket();
        craftingDataPacket.setCleanRecipes(true);
        craftingDataPacket.getCraftingData().addAll(CARTOGRAPHY_RECIPES);
        craftingDataPacket.getPotionMixData().addAll(Registries.POTION_MIXES.forVersion(session.getUpstream().getProtocolVersion()));
        if (session.isSentSpawnPacket()) {
            session.getUpstream().sendPacket(craftingDataPacket);
            // TODO proper fix to check if we've been online - in online mode (with auth screen),
            //  recipes are not yet known
            if (session.getStonecutterRecipes() != null) {
                session.getCraftingRecipes().clear();
                session.getJavaToBedrockRecipeIds().clear();
                session.getSmithingRecipes().clear();
                session.getStonecutterRecipes().clear();
            }
        } else {
            session.getUpstream().queuePostStartGamePacket(craftingDataPacket);
        }

        // while ClientboundLoginPacket holds the level, it doesn't hold the scoreboard.
        // The ClientboundStartConfigurationPacket indirectly removes the old scoreboard,
        // and this packet indirectly creates the new one.
        // This makes this packet a good place to reset the scoreboard.
        session.getWorldCache().resetScoreboard();
    }
}
