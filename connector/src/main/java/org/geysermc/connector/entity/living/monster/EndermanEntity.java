package org.geysermc.connector.entity.living.monster;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.data.EntityFlag;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.block.BlockTranslator;

public class EndermanEntity extends MonsterEntity {

    public EndermanEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        // Held block
        if (entityMetadata.getId() == 15) {
            metadata.put(EntityData.ENDERMAN_HELD_ITEM_ID, BlockTranslator.getBedrockBlockId((BlockState) entityMetadata.getValue()));
        }
        // 'Angry' - mouth open
        if (entityMetadata.getId() == 16) {
            metadata.getFlags().setFlag(EntityFlag.ANGRY, (boolean) entityMetadata.getValue());
        }
        // TODO: ID 17 is stared at but I don't believe it's used - maybe only for the sound effect. Check after particle merge
        super.updateBedrockMetadata(entityMetadata, session);
    }
}
