/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

import java.util.concurrent.ThreadLocalRandom;

@Translator(packet = ServerRespawnPacket.class)
public class JavaRespawnTranslator extends PacketTranslator<ServerRespawnPacket> {

    @Override
    public void translate(ServerRespawnPacket packet, GeyserSession session) {
        Entity entity = session.getPlayerEntity();
        if (entity == null)
            return;

        float maxHealth = entity.getAttributes().containsKey(AttributeType.MAX_HEALTH) ? entity.getAttributes().get(AttributeType.MAX_HEALTH).getValue() : 20f;
        // Max health must be divisible by two in bedrock
        entity.getAttributes().put(AttributeType.HEALTH, AttributeType.HEALTH.getAttribute(maxHealth, (maxHealth % 2 == 1 ? maxHealth + 1 : maxHealth)));

        session.getInventoryCache().setOpenInventory(null);

        SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
        playerGameTypePacket.setGamemode(packet.getGamemode().ordinal());
        session.sendUpstreamPacket(playerGameTypePacket);
        session.setGameMode(packet.getGamemode());

        LevelEventPacket stopRainPacket = new LevelEventPacket();
        stopRainPacket.setType(LevelEventType.STOP_RAINING);
        stopRainPacket.setData(ThreadLocalRandom.current().nextInt(50000) + 10000);
        stopRainPacket.setPosition(Vector3f.ZERO);
        session.sendUpstreamPacket(stopRainPacket);

        if (!entity.getDimension().equals(packet.getDimension())) {
            DimensionUtils.switchDimension(session, packet.getDimension());
        } else {
            if (session.isManyDimPackets()) { //reloading world
                String fakeDim = entity.getDimension().equals(DimensionUtils.OVERWORLD) ? DimensionUtils.NETHER : DimensionUtils.OVERWORLD;
                DimensionUtils.switchDimension(session, fakeDim);
                DimensionUtils.switchDimension(session, packet.getDimension());
            } else {
                // Handled in JavaPlayerPositionRotationTranslator
                session.setSpawned(false);
            }
        }
    }
}
