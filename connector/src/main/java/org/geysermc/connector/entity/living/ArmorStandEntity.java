package org.geysermc.connector.entity.living;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.data.EntityFlag;
import org.geysermc.connector.entity.LivingEntity;
import org.geysermc.connector.entity.living.MonsterEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class ArmorStandEntity extends LivingEntity {

    public ArmorStandEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getType() == MetadataType.BYTE) {
            byte xd = (byte) entityMetadata.getValue();
            if((xd & 0x01) == 0x01) {
                metadata.put(EntityData.SCALE, .55f);
            }
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }
}
