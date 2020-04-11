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
        session.getUpstream().sendPacket(takeItemEntityPacket);
    }
}
