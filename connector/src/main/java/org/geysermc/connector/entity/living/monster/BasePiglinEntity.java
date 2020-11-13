package org.geysermc.connector.entity.living.monster;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.utils.DimensionUtils;

public class BasePiglinEntity extends MonsterEntity {

    public BasePiglinEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 15) {
            // Immune to zombification?
            // Apply shaking effect if not in the nether and zombification is possible
            metadata.getFlags().setFlag(EntityFlag.SHAKING, !((boolean) entityMetadata.getValue()) && !session.getDimension().equals(DimensionUtils.NETHER));
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }
}
