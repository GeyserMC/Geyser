package org.geysermc.connector.entity.living;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.data.EntityFlag;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class RabbitEntity extends AnimalEntity {
    public RabbitEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        super.updateBedrockMetadata(entityMetadata, session);
        if (entityMetadata.getId() == 15) {
            metadata.put(EntityData.SCALE, .55f);
            boolean isBaby = (boolean) entityMetadata.getValue();
            if(isBaby) {
                metadata.put(EntityData.SCALE, .35f);
                metadata.getFlags().setFlag(EntityFlag.BABY, true);
            }
        }
    }
}