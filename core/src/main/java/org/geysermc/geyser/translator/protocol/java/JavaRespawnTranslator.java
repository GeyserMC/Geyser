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

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetPlayerGameTypePacket;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.geyser.util.DimensionUtils;

@Translator(packet = ClientboundRespawnPacket.class)
public class JavaRespawnTranslator extends PacketTranslator<ClientboundRespawnPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundRespawnPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();

        session.setSpawned(false);

        entity.setHealth(entity.getMaxHealth());
        entity.getAttributes().put(GeyserAttributeType.HEALTH, entity.createHealthAttribute());

        session.setInventoryTranslator(InventoryTranslator.PLAYER_INVENTORY_TRANSLATOR);
        session.setOpenInventory(null);
        session.setClosingInventory(false);

        entity.setLastDeathPosition(packet.getLastDeathPos());
        entity.updateBedrockMetadata();

        SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
        int gamemode = packet.getGamemode().ordinal() == 3 ? 6 : packet.getGamemode().ordinal();
        playerGameTypePacket.setGamemode(packet.getGamemode().ordinal());
        session.sendUpstreamPacket(playerGameTypePacket);
        session.setGameMode(packet.getGamemode());

        if (session.isRaining()) {
            LevelEventPacket stopRainPacket = new LevelEventPacket();
            stopRainPacket.setType(LevelEvent.STOP_RAINING);
            stopRainPacket.setData(0);
            stopRainPacket.setPosition(Vector3f.ZERO);
            session.sendUpstreamPacket(stopRainPacket);
            session.setRaining(false);
        }

        if (session.isThunder()) {
            LevelEventPacket stopThunderPacket = new LevelEventPacket();
            stopThunderPacket.setType(LevelEvent.STOP_THUNDERSTORM);
            stopThunderPacket.setData(0);
            stopThunderPacket.setPosition(Vector3f.ZERO);
            session.sendUpstreamPacket(stopThunderPacket);
            session.setThunder(false);
        }

        String newDimension = packet.getDimension();
        if (!session.getDimension().equals(newDimension) || !packet.getWorldName().equals(session.getWorldName())) {
            // Switching to a new world (based off the world name change or new dimension); send a fake dimension change
            if (DimensionUtils.javaToBedrock(session.getDimension()) == DimensionUtils.javaToBedrock(newDimension)) {
                String fakeDim = DimensionUtils.getTemporaryDimension(session.getDimension(), newDimension);
                DimensionUtils.switchDimension(session, fakeDim);
            }
            session.setWorldName(packet.getWorldName());
            DimensionUtils.switchDimension(session, newDimension);

            ChunkUtils.loadDimension(session);
        }
    }
}
