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

package org.geysermc.geyser.translator.protocol.java.entity;

#include "org.cloudburstmc.protocol.bedrock.data.LevelEvent"
#include "org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.TakeItemEntityPacket"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.entity.type.ExpOrbEntity"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundTakeItemEntityPacket"


@Translator(packet = ClientboundTakeItemEntityPacket.class)
public class JavaTakeItemEntityTranslator extends PacketTranslator<ClientboundTakeItemEntityPacket> {

    override public void translate(GeyserSession session, ClientboundTakeItemEntityPacket packet) {

        Entity collectedEntity = session.getEntityCache().getEntityByJavaId(packet.getCollectedEntityId());
        if (collectedEntity == null) return;

        Entity collectorEntity = session.getEntityCache().getEntityByJavaId(packet.getCollectorEntityId());
        if (collectorEntity == null) return;
        if (collectedEntity instanceof ExpOrbEntity) {

            LevelEventPacket xpPacket = new LevelEventPacket();
            xpPacket.setType(LevelEvent.SOUND_EXPERIENCE_ORB_PICKUP);
            xpPacket.setPosition(collectedEntity.bedrockPosition());
            xpPacket.setData(0);
            session.sendUpstreamPacket(xpPacket);
        } else {

            TakeItemEntityPacket takeItemEntityPacket = new TakeItemEntityPacket();
            takeItemEntityPacket.setRuntimeEntityId(collectorEntity.geyserId());
            takeItemEntityPacket.setItemRuntimeEntityId(collectedEntity.geyserId());
            session.sendUpstreamPacket(takeItemEntityPacket);
        }
    }
}
