package org.geysermc.connector.network.translators.java;

import com.flowpowered.math.vector.Vector3f;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerBossBarPacket;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.packet.BossEventPacket;
import com.nukkitx.protocol.bedrock.packet.RemoveEntityPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.MessageUtils;

public class JavaBossBarTranslator extends PacketTranslator<ServerBossBarPacket> {
    @Override
    public void translate(ServerBossBarPacket packet, GeyserSession session) {
        BossEventPacket bossEventPacket = new BossEventPacket();
        bossEventPacket.setBossUniqueEntityId(session.getEntityCache().getBossBar(packet.getUUID()));

        switch (packet.getAction()) {
            case ADD:
                long entityId = session.getEntityCache().addBossBar(packet.getUUID());
                addBossEntity(session, entityId);

                bossEventPacket.setType(BossEventPacket.Type.SHOW);
                bossEventPacket.setBossUniqueEntityId(entityId);
                bossEventPacket.setTitle(MessageUtils.getBedrockMessage(packet.getTitle()));
                bossEventPacket.setHealthPercentage(packet.getHealth());
                bossEventPacket.setColor(0); //ignored by client
                bossEventPacket.setOverlay(1);
                bossEventPacket.setDarkenSky(0);
                break;
            case UPDATE_TITLE:
                bossEventPacket.setType(BossEventPacket.Type.TITLE);
                bossEventPacket.setTitle(MessageUtils.getBedrockMessage(packet.getTitle()));
                break;
            case UPDATE_HEALTH:
                bossEventPacket.setType(BossEventPacket.Type.HEALTH_PERCENTAGE);
                bossEventPacket.setHealthPercentage(packet.getHealth());
                break;
            case REMOVE:
                bossEventPacket.setType(BossEventPacket.Type.HIDE);
                removeBossEntity(session, session.getEntityCache().removeBossBar(packet.getUUID()));
                break;
            case UPDATE_STYLE:
            case UPDATE_FLAGS:
                //todo
                return;
        }

        session.getUpstream().sendPacket(bossEventPacket);
    }

    /**
     * Bedrock still needs an entity to display the BossBar.<br>
     * Just like 1.8 but it doesn't care about which entity
     */
    private void addBossEntity(GeyserSession session, long entityId) {
        AddEntityPacket addEntityPacket = new AddEntityPacket();
        addEntityPacket.setUniqueEntityId(entityId);
        addEntityPacket.setRuntimeEntityId(entityId);
        addEntityPacket.setIdentifier("minecraft:creeper");
        addEntityPacket.setEntityType(33);
        addEntityPacket.setPosition(session.getPlayerEntity().getPosition());
        addEntityPacket.setRotation(Vector3f.ZERO);
        addEntityPacket.setMotion(Vector3f.ZERO);
        addEntityPacket.getMetadata().put(EntityData.SCALE, 0.01F); // scale = 0 doesn't work?

        session.getUpstream().sendPacket(addEntityPacket);
    }

    private void removeBossEntity(GeyserSession session, long entityId) {
        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.setUniqueEntityId(entityId);

        session.getUpstream().sendPacket(removeEntityPacket);
    }
}
