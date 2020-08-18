package org.geysermc.connector.entity.living.monster;

import com.nukkitx.math.vector.Vector3f;
import org.geysermc.connector.entity.type.EntityType;

public class BasePiglinEntity extends MonsterEntity {

    public BasePiglinEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }
}