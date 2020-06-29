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

package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityCollectItemPacket;
import com.nukkitx.protocol.bedrock.packet.TakeItemEntityPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

@Translator(packet = ServerEntityCollectItemPacket.class)
public class JavaCollectItemTranslator extends PacketTranslator<ServerEntityCollectItemPacket> {

    @Override
    public void translate(ServerEntityCollectItemPacket packet, GeyserSession session) {
        // This is the definition of translating - both packets take the same values
        TakeItemEntityPacket takeItemEntityPacket = new TakeItemEntityPacket();
        // Collected entity is the item
        Entity collectedEntity = session.getEntityCache().getEntityByJavaId(packet.getCollectedEntityId());
        // Collector is the entity picking up the item
        Entity collectorEntity;
        if (packet.getCollectorEntityId() == session.getPlayerEntity().getEntityId()) {
            collectorEntity = session.getPlayerEntity();
        } else {
            collectorEntity = session.getEntityCache().getEntityByJavaId(packet.getCollectorEntityId());
        }
        takeItemEntityPacket.setRuntimeEntityId(collectorEntity.getGeyserId());
        takeItemEntityPacket.setItemRuntimeEntityId(collectedEntity.getGeyserId());
        session.sendUpstreamPacket(takeItemEntityPacket);
    }
}
