/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;

public class JavaJoinGameTranslator extends PacketTranslator<ServerJoinGamePacket> {

    @Override
    public void translate(ServerJoinGamePacket packet, GeyserSession session) {
        AdventureSettingsPacket bedrockPacket = new AdventureSettingsPacket();
        bedrockPacket.setUniqueEntityId(session.getPlayerEntity().getGeyserId());
        bedrockPacket.setPlayerPermission(1);
        session.getUpstream().sendPacketImmediately(bedrockPacket);

        PlayStatusPacket playStatus = new PlayStatusPacket();
        playStatus.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
        session.getUpstream().sendPacketImmediately(playStatus);

        PlayerEntity entity = session.getPlayerEntity();
        if (entity == null) return;

        session.getPlayerEntity().setEntityId(packet.getEntityId());

        SetPlayerGameTypePacket playerGameTypePacket = new SetPlayerGameTypePacket();
        playerGameTypePacket.setGamemode(packet.getGameMode().ordinal());
        session.getUpstream().sendPacket(playerGameTypePacket);
        session.setGameMode(packet.getGameMode());

        SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
        entityDataPacket.setRuntimeEntityId(entity.getGeyserId());
        entityDataPacket.getMetadata().putAll(entity.getMetadata());
        session.getUpstream().sendPacket(entityDataPacket);

        session.setRenderDistance(packet.getViewDistance() + 1); // +1 to be sure it includes every chunk
        if (session.getRenderDistance() > 32) session.setRenderDistance(32); // <3 u ViaVersion but I don't like crashing clients x)

        ChunkRadiusUpdatedPacket packet1 = new ChunkRadiusUpdatedPacket();
        packet1.setRadius(session.getRenderDistance());
        session.getUpstream().sendPacket(packet1);

        session.setSpawned(true);
    }
}
