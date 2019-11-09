package org.geysermc.connector.entity;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.packet.AddItemEntityPacket;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;


public class Item extends Entity {
	
     
    public Item(int amount, long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }
	
   @Override
    public void spawnEntity(GeyserSession session) {
        AddItemEntityPacket AddItemEntity = new AddItemEntityPacket();
        AddItemEntity.setPosition(position);
        AddItemEntity.setRuntimeEntityId(entityId);
        AddItemEntity.setUniqueEntityId(entityId);
        AddItemEntity.setMotion(motion);
		
       //todo work on those underneed
		
       //AddItemEntity.setfromFishing();
       //AddItemEntity.setItemData();
       //AddItemEntity.setmetadata();

        session.getUpstream().sendPacket(AddItemEntity);
    }
}
