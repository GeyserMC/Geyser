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

package org.geysermc.connector.network.translators.java.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityCollectItemPacket;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import com.nukkitx.protocol.bedrock.packet.TakeItemEntityPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.ExpOrbEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

/**
 * This packet is called whenever a player picks up an item.
 * In Java, this is called for item entities, experience orbs and arrows
 * Bedrock uses it for arrows and item entities, but not experience orbs.
 */
@Translator(packet = ServerEntityCollectItemPacket.class)
public class JavaEntityCollectItemTranslator extends PacketTranslator<ServerEntityCollectItemPacket> {

    @Override
    public void translate(ServerEntityCollectItemPacket packet, GeyserSession session) {
        // Collected entity is the other entity
        Entity collectedEntity = session.getEntityCache().getEntityByJavaId(packet.getCollectedEntityId());
        if (collectedEntity == null) return;
        // Collector is the entity 'picking up' the item
        Entity collectorEntity;
        if (packet.getCollectorEntityId() == session.getPlayerEntity().getEntityId()) {
            collectorEntity = session.getPlayerEntity();
        } else {
            collectorEntity = session.getEntityCache().getEntityByJavaId(packet.getCollectorEntityId());
        }
        if (collectorEntity == null) return;
        if (collectedEntity instanceof ExpOrbEntity) {
            // Player just picked up an experience orb
            LevelEventPacket xpPacket = new LevelEventPacket();
            xpPacket.setType(LevelEventType.SOUND_EXPERIENCE_ORB_PICKUP);
            xpPacket.setPosition(collectedEntity.getPosition());
            xpPacket.setData(0);
            session.sendUpstreamPacket(xpPacket);
        } else {
            // Item is being picked up (visual only)
            TakeItemEntityPacket takeItemEntityPacket = new TakeItemEntityPacket();
            takeItemEntityPacket.setRuntimeEntityId(collectorEntity.getGeyserId());
            takeItemEntityPacket.setItemRuntimeEntityId(collectedEntity.getGeyserId());
            session.sendUpstreamPacket(takeItemEntityPacket);
        }
    }
}
