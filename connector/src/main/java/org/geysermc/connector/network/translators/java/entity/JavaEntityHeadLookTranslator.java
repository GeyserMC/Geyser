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

package org.geysermc.connector.network.translators.java.entity;

import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityHeadLookPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;

@Translator(packet = ServerEntityHeadLookPacket.class)
public class JavaEntityHeadLookTranslator extends PacketTranslator<ServerEntityHeadLookPacket> {

    @Override
    public void translate(ServerEntityHeadLookPacket packet, GeyserSession session) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            entity = session.getPlayerEntity();
        }

        if (entity == null) return;

        entity.setRotation(Vector3f.from(entity.getRotation().getX(), entity.getRotation().getY(), packet.getHeadYaw()));

        if (entity.getEntityType() != EntityType.PLAYER) {
            MoveEntityAbsolutePacket moveEntityAbsolutePacket = new MoveEntityAbsolutePacket();
            moveEntityAbsolutePacket.setRuntimeEntityId(entity.getGeyserId());
            moveEntityAbsolutePacket.setPosition(entity.getPosition());
            moveEntityAbsolutePacket.setRotation(entity.getBedrockRotation());
            session.getUpstream().sendPacket(moveEntityAbsolutePacket);
        } else {
            MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
            movePlayerPacket.setRuntimeEntityId(entity.getGeyserId());
            movePlayerPacket.setPosition(entity.getPosition());
            movePlayerPacket.setRotation(entity.getBedrockRotation());
            movePlayerPacket.setMode(MovePlayerPacket.Mode.ROTATION);
            session.getUpstream().sendPacket(movePlayerPacket);
        }
    }
}
