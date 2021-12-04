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

package org.geysermc.geyser.translator.protocol.bedrock;

import com.github.steveice10.mc.protocol.data.game.ClientCommand;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;
import com.nukkitx.protocol.bedrock.packet.RespawnPacket;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = RespawnPacket.class)
public class BedrockRespawnTranslator extends PacketTranslator<RespawnPacket> {

    @Override
    public void translate(GeyserSession session, RespawnPacket packet) {
        if (packet.getState() == RespawnPacket.State.CLIENT_READY) {
            // Previously we only sent the respawn packet before the server finished loading
            // The message included was 'Otherwise when immediate respawn is on the client never loads'
            // But I assume the new if statement below fixes that problem
            RespawnPacket respawnPacket = new RespawnPacket();
            respawnPacket.setRuntimeEntityId(0);
            respawnPacket.setPosition(Vector3f.ZERO);
            respawnPacket.setState(RespawnPacket.State.SERVER_READY);
            session.sendUpstreamPacket(respawnPacket);

            if (session.isSpawned()) {
                // Client might be stuck; resend spawn information
                PlayerEntity entity = session.getPlayerEntity();
                entity.updateBedrockMetadata(); // TODO test?

                MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
                movePlayerPacket.setRuntimeEntityId(entity.getGeyserId());
                movePlayerPacket.setPosition(entity.getPosition());
                movePlayerPacket.setRotation(entity.getBedrockRotation());
                movePlayerPacket.setMode(MovePlayerPacket.Mode.RESPAWN);
                session.sendUpstreamPacket(movePlayerPacket);
            }

            ServerboundClientCommandPacket javaRespawnPacket = new ServerboundClientCommandPacket(ClientCommand.RESPAWN);
            session.sendDownstreamPacket(javaRespawnPacket);
        }
    }
}
