package org.geysermc.connector.entity.living.monster;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.Attribute;
import com.nukkitx.protocol.bedrock.data.EntityEventType;
import com.nukkitx.protocol.bedrock.data.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import org.geysermc.connector.entity.living.InsentientEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class EnderDragonEntity extends InsentientEntity {

    public EnderDragonEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 15) {
            metadata.getFlags().setFlag(EntityFlag.FIRE_IMMUNE, true);
            switch ((int) entityMetadata.getValue()) {
                // Performing breath attack
                case 5:
                    EntityEventPacket entityEventPacket = new EntityEventPacket();
                    entityEventPacket.setType(EntityEventType.DRAGON_FLAMING);
                    entityEventPacket.setRuntimeEntityId(geyserId);
                    entityEventPacket.setData(0);
                    session.getUpstream().sendPacket(entityEventPacket);
                case 6:
                case 7:
                    metadata.getFlags().setFlag(EntityFlag.SITTING, true);
                    break;
            }
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        AddEntityPacket addEntityPacket = new AddEntityPacket();
        addEntityPacket.setIdentifier("minecraft:" + entityType.name().toLowerCase());
        addEntityPacket.setRuntimeEntityId(geyserId);
        addEntityPacket.setUniqueEntityId(geyserId);
        addEntityPacket.setPosition(position);
        addEntityPacket.setMotion(motion);
        addEntityPacket.setRotation(getBedrockRotation());
        addEntityPacket.setEntityType(entityType.getType());
        addEntityPacket.getMetadata().putAll(metadata);

        // Otherwise dragon is always 'dying'
        addEntityPacket.getAttributes().add(new Attribute("minecraft:health", 0.0f, 200f, 200f, 200f));

        valid = true;
        session.getUpstream().sendPacket(addEntityPacket);

        session.getConnector().getLogger().debug("Spawned entity " + entityType + " at location " + position + " with id " + geyserId + " (java id " + entityId + ")");
    }
}
