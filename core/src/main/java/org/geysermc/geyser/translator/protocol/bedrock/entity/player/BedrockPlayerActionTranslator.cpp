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

package org.geysermc.geyser.translator.protocol.bedrock.entity.player;

#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType"
#include "org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket"
#include "org.geysermc.geyser.entity.type.player.SessionPlayerEntity"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket"

@Translator(packet = PlayerActionPacket.class)
public class BedrockPlayerActionTranslator extends PacketTranslator<PlayerActionPacket> {

    override public void translate(GeyserSession session, PlayerActionPacket packet) {

        switch (packet.getAction()) {
            case RESPAWN -> {
                SessionPlayerEntity entity = session.getPlayerEntity();

                EntityEventPacket eventPacket = new EntityEventPacket();
                eventPacket.setRuntimeEntityId(entity.geyserId());
                eventPacket.setType(EntityEventType.RESPAWN);
                eventPacket.setData(0);
                session.sendUpstreamPacket(eventPacket);

                UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
                attributesPacket.setRuntimeEntityId(entity.geyserId());
                attributesPacket.getAttributes().addAll(entity.getAttributes().values());
                session.sendUpstreamPacket(attributesPacket);


                entity.updateBoundingBox();


                session.getEntityCache().updateBossBars();
            }
            case STOP_SLEEP -> {
                ServerboundPlayerCommandPacket stopSleepingPacket = new ServerboundPlayerCommandPacket(session.getPlayerEntity().getEntityId(), PlayerState.LEAVE_BED);
                session.sendDownstreamGamePacket(stopSleepingPacket);
            }
            case DIMENSION_CHANGE_SUCCESS -> {
                SessionPlayerEntity entity = session.getPlayerEntity();

                PlayStatusPacket spawnPacket = new PlayStatusPacket();
                spawnPacket.setStatus(PlayStatusPacket.Status.PLAYER_SPAWN);
                session.sendUpstreamPacket(spawnPacket);

                UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
                attributesPacket.setRuntimeEntityId(entity.geyserId());
                attributesPacket.getAttributes().addAll(entity.getAttributes().values());
                session.sendUpstreamPacket(attributesPacket);
            }
        }
    }
}
