package org.geysermc.connector.entity.living.merchant;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.VillagerData;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;

public class VillagerEntity extends AbstractMerchantEntity {

    public static Int2IntMap VILLAGER_VARIANTS = new Int2IntOpenHashMap();

    static {
        VILLAGER_VARIANTS.put(1, 8);
        VILLAGER_VARIANTS.put(2, 11);
        VILLAGER_VARIANTS.put(3, 6);
        VILLAGER_VARIANTS.put(4, 7);
        VILLAGER_VARIANTS.put(5, 1);
        VILLAGER_VARIANTS.put(6, 2);
        VILLAGER_VARIANTS.put(7, 4);
        VILLAGER_VARIANTS.put(8, 12);
        VILLAGER_VARIANTS.put(9, 5);
        VILLAGER_VARIANTS.put(10, 13);
        VILLAGER_VARIANTS.put(11, 14);
        VILLAGER_VARIANTS.put(12, 3);
        VILLAGER_VARIANTS.put(13, 10);
        VILLAGER_VARIANTS.put(14, 9);
    }

    public VillagerEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        System.out.println("ID: " + entityMetadata.getId() + ", " + entityMetadata.getValue());
        if (entityMetadata.getId() == 17) {
            VillagerData villagerData = (VillagerData) entityMetadata.getValue();
            metadata.put(EntityData.VARIANT, VILLAGER_VARIANTS.get(villagerData.getProfession()));
            metadata.put(EntityData.SKIN_ID, villagerData.getType());
            metadata.put(EntityData.TRADE_TIER, villagerData.getLevel() - 1);
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        AddEntityPacket addEntityPacket = new AddEntityPacket();
        // "v2" or else it's the legacy villager
        addEntityPacket.setIdentifier("minecraft:villager_v2");
        addEntityPacket.setRuntimeEntityId(geyserId);
        addEntityPacket.setUniqueEntityId(geyserId);
        addEntityPacket.setPosition(position);
        addEntityPacket.setMotion(motion);
        addEntityPacket.setRotation(getBedrockRotation());
        addEntityPacket.setEntityType(entityType.getType());
        addEntityPacket.getMetadata().putAll(metadata);

        valid = true;
        session.getUpstream().sendPacket(addEntityPacket);

        session.getConnector().getLogger().debug("Spawned entity " + entityType + " at location " + position + " with id " + geyserId + " (java id " + entityId + ")");
    }

}
