package org.geysermc.connector.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.data.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class EnderCrystalEntity extends Entity {

    public EnderCrystalEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);

    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        // Show beam
        // Usually performed client-side on Bedrock except for Ender Dragon respawn event
        if (entityMetadata.getId() == 7) {
            if (entityMetadata.getValue() instanceof Position) {
                Position pos = (Position) entityMetadata.getValue();
                metadata.put(EntityData.BLOCK_TARGET, Vector3i.from(pos.getX(), pos.getY(), pos.getZ()));
            } else {
                metadata.put(EntityData.BLOCK_TARGET, Vector3i.ZERO);
            }
        }
        // There is a base located on the ender crystal
        if (entityMetadata.getId() == 8) {
            metadata.getFlags().setFlag(EntityFlag.SHOW_BOTTOM, (boolean) entityMetadata.getValue());
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        AddEntityPacket addEntityPacket = new AddEntityPacket();
        // Not end crystal but ender crystal
        addEntityPacket.setIdentifier("minecraft:ender_crystal");
        addEntityPacket.setRuntimeEntityId(geyserId);
        addEntityPacket.setUniqueEntityId(geyserId);
        addEntityPacket.setPosition(position);
        addEntityPacket.setMotion(motion);
        addEntityPacket.setRotation(getBedrockRotation());
        addEntityPacket.setEntityType(entityType.getType());
        addEntityPacket.getMetadata().putAll(metadata);

        valid = true;
        session.getUpstream().sendPacket(addEntityPacket);

        session.getConnector().getLogger().debug("Spawned entity " + entityType + " at location " + position + " with id " + geyserId + " (java id " + entityId + ")");
    }
}
