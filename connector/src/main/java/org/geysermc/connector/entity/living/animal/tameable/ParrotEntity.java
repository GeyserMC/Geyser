package org.geysermc.connector.entity.living.animal.tameable;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.EntityData;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class ParrotEntity extends TameableEntity {

    public ParrotEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        // Parrot color
        if (entityMetadata.getId() == 18) {
            metadata.put(EntityData.VARIANT, entityMetadata.getValue());
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }
}
