/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.java;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerRespawnPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import com.nukkitx.protocol.bedrock.packet.SetPlayerGameTypePacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.attribute.AttributeType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.DimensionUtils;

@Translator(packet = ServerRespawnPacket.class)
public class JavaRespawnTranslator extends PacketTranslator<ServerRespawnPacket> {

    @Override
    public void translate(ServerRespawnPacket packet, GeyserSession session) {
        Entity entity = session.getPlayerEntity();

        float maxHealth = entity.getAttributes().containsKey(AttributeType.MAX_HEALTH) ? entity.getAttributes().get(AttributeType.MAX_HEALTH).getValue() : 20f;
        // Max health must be divisible by two in bedrock
        entity.getAttributes().put(AttributeType.HEALTH, AttributeType.HEALTH.getAttribute(maxHealth, (maxHealth % 2 == 1 ? maxHealth + 1 : maxHealth)));

        session.getInventoryCache().setOpenInventory(null);

        SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
        playerGameTypePacket.setGamemode(packet.getGamemode().ordinal());
        session.sendUpstreamPacket(playerGameTypePacket);
        session.setGameMode(packet.getGamemode());

        if (session.isRaining()) {
            LevelEventPacket stopRainPacket = new LevelEventPacket();
            stopRainPacket.setType(LevelEventType.STOP_RAINING);
            stopRainPacket.setData(0);
            stopRainPacket.setPosition(Vector3f.ZERO);
            session.sendUpstreamPacket(stopRainPacket);
            session.setRaining(false);
        }

        if (session.isThunder()) {
            LevelEventPacket stopThunderPacket = new LevelEventPacket();
            stopThunderPacket.setType(LevelEventType.STOP_THUNDERSTORM);
            stopThunderPacket.setData(0);
            stopThunderPacket.setPosition(Vector3f.ZERO);
            session.sendUpstreamPacket(stopThunderPacket);
            session.setThunder(false);
        }

        String newDimension = DimensionUtils.getNewDimension(packet.getDimension());
        if (!session.getDimension().equals(newDimension) || !packet.getWorldName().equals(session.getWorldName())) {
            if (!packet.getWorldName().equals(session.getWorldName()) && session.getDimension().equals(newDimension)) {
                // Switching to a new world (based off the world name change); send a fake dimension change
                String fakeDim = DimensionUtils.getTemporaryDimension(session.getDimension(), newDimension);
                DimensionUtils.switchDimension(session, fakeDim);
            }
            session.setWorldName(packet.getWorldName());
            DimensionUtils.switchDimension(session, newDimension);
        }
    }
}
