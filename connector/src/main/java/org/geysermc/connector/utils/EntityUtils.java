package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.entity.type.MobType;
import com.github.steveice10.mc.protocol.data.game.entity.type.object.ObjectType;
import org.geysermc.connector.entity.type.EntityType;

public class EntityUtils {

    public static MobType toJavaEntity(EntityType type) {
        try {
            return MobType.valueOf(type.name());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static EntityType toBedrockEntity(MobType type) {
        try {
            return EntityType.valueOf(type.name());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static EntityType toBedrockEntity(ObjectType type) {
        try {
            return EntityType.valueOf(type.name());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
