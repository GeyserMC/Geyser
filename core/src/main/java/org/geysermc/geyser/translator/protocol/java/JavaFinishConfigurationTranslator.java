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

import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.geyser.util.PlayerListUtils;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;

import java.util.ArrayList;
import java.util.List;

@Translator(packet = ClientboundFinishConfigurationPacket.class)
public class JavaFinishConfigurationTranslator extends PacketTranslator<ClientboundFinishConfigurationPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundFinishConfigurationPacket packet) {
        // Clear the player list, as on Java the player list is cleared after transitioning from config to play phase
        List<PlayerListPacket.Entry> entries = new ArrayList<>();
        session.getEntityCache().forEachPlayerEntity(otherPlayer -> {
            entries.add(new PlayerListPacket.Entry(otherPlayer.getTabListUuid()));
        });
        if (!entries.isEmpty()) {
            PlayerListUtils.batchSendPlayerList(session, entries, PlayerListPacket.Action.REMOVE);
        }
        session.getEntityCache().removeAllPlayerEntities();

        if (session.isSentSpawnPacket()) {
            // TODO proper fix to check if we've been online - in online mode (with auth screen),
            //  recipes are not yet known
            if (session.getStonecutterRecipes() != null) {
                session.getLastRecipeNetId().set(InventoryUtils.LAST_RECIPE_NET_ID + 1);
                session.getCraftingRecipes().clear();
                session.getJavaToBedrockRecipeIds().clear();
                session.getSmithingRecipes().clear();
                session.getStonecutterRecipes().clear();
            }
            session.getUpstream().sendPacket(session.getCraftingDataPacket());
        } else {
            session.getUpstream().queuePostStartGamePacket(session.getCraftingDataPacket());
        }

        // while ClientboundLoginPacket holds the level, it doesn't hold the scoreboard.
        // The ClientboundStartConfigurationPacket indirectly removes the old scoreboard,
        // and this packet indirectly creates the new one.
        // This makes this packet a good place to reset the scoreboard.
        session.getWorldCache().resetScoreboard();

        // Resolve API components from non-vanilla registered items that required registry data to map to MCPL components
        session.getComponentCache().resolveComponents();
    }
}
