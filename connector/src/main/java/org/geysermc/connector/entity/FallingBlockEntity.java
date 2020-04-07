package org.geysermc.connector.entity;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.EntityData;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.translators.block.BlockTranslator;

public class FallingBlockEntity extends Entity {

    public FallingBlockEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation, int javaId) {
        super(entityId, geyserId, entityType, position, motion, rotation);

        int bedrockId = BlockTranslator.getBedrockBlockId(javaId);

        this.metadata.put(EntityData.VARIANT, bedrockId);
    }
}
