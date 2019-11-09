package org.geysermc.connector.entity;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.AddItemEntityPacket;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.console.GeyserLogger;

public class ItemEntity extends Entity {
	
  public ItemEntity(int amount, long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
    super(entityId, geyserId, EntityType.ITEM, position, motion, rotation);
    }
	
   @Override
  public void spawnEntity(GeyserSession session) {
    AddItemEntityPacket AddItemEntity = new AddItemEntityPacket();
    AddItemEntity.setPosition(position);
    AddItemEntity.setRuntimeEntityId(geyserId);
    AddItemEntity.setUniqueEntityId(geyserId);
    AddItemEntity.setMotion(motion);
    session.getUpstream().sendPacket(AddItemEntity);

    valid = true;

    GeyserLogger.DEFAULT.debug("a item droped on " + position);	
	
        }
  }
