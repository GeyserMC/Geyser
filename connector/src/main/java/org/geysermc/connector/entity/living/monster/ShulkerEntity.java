package org.geysermc.connector.entity.living.monster;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.EntityData;
import org.geysermc.connector.entity.living.GolemEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class ShulkerEntity extends GolemEntity {

    public ShulkerEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 15) {
            BlockFace blockFace = (BlockFace) entityMetadata.getValue();
            metadata.put(EntityData.SHULKER_ATTACH_FACE, (byte) blockFace.ordinal());
        }
        if (entityMetadata.getId() == 16) {
            Position position = (Position) entityMetadata.getValue();
            metadata.put(EntityData.SHULKER_ATTACH_POS, Vector3i.from(position.getX(), position.getY(), position.getZ()));
        }
        //TODO Outdated metadata flag SHULKER_PEAK_HEIGHT
//        if (entityMetadata.getId() == 17) {
//            int height = (byte) entityMetadata.getValue();
//            metadata.put(EntityData.SHULKER_PEAK_HEIGHT, height);
//        }
        if (entityMetadata.getId() == 18) {
            int color = Math.abs((byte) entityMetadata.getValue() - 15);
            metadata.put(EntityData.VARIANT, color);
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }
}
