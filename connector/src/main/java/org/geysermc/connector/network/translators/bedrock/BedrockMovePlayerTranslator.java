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

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionRotationPacket;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.item.BedrockItem;

public class BedrockMovePlayerTranslator extends PacketTranslator<MovePlayerPacket> {

    @Override
    public void translate(MovePlayerPacket packet, GeyserSession session) {
        Entity entity = session.getPlayerEntity();
        if (entity == null)
            return;

        ClientPlayerPositionRotationPacket playerPositionRotationPacket = new ClientPlayerPositionRotationPacket(
                packet.isOnGround(), packet.getPosition().getX(), Math.ceil((packet.getPosition().getY() - EntityType.PLAYER.getOffset()) * 2) / 2,
                packet.getPosition().getZ(), packet.getRotation().getY(), packet.getRotation().getX());

        entity.moveAbsolute(packet.getPosition(), packet.getRotation());

        boolean colliding = false;
        Position position = new Position((int) packet.getPosition().getX(),
                (int) Math.ceil((packet.getPosition().getY() - EntityType.PLAYER.getOffset()) * 2) / 2, (int) packet.getPosition().getZ());

        BedrockItem block = session.getChunkCache().getBlockAt(position);
        if (!block.getIdentifier().contains("air"))
            colliding = true;

        if (!colliding)
            session.getDownstream().getSession().send(playerPositionRotationPacket);
    }
}
