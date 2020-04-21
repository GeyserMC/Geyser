package org.geysermc.connector.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import lombok.Getter;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.utils.ChunkUtils;

/**
 * Item frames are an entity in Java but a block entity in Bedrock.
 */
public class ItemFrameEntity extends Entity {

    /**
     * Used for getting the Bedrock block position.
     * Blocks deal with integers whereas entities deal with floats.
     */
    @Getter
    Vector3i bedrockPosition;

    public ItemFrameEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
        bedrockPosition = Vector3i.from(position.getFloorX(), position.getFloorY(), position.getFloorZ());
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        updateBlock(session, null);
        session.getConnector().getLogger().debug("Spawned item frame at location " + position + " with java id " + entityId);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        System.out.println(entityMetadata.getId() + " " + entityMetadata.getValue());
        if (entityMetadata.getId() == 7 && entityMetadata.getValue() != null) {
            ItemData itemData = new ItemTranslator().translateToBedrock((ItemStack) entityMetadata.getValue());
            CompoundTagBuilder builder = CompoundTag.builder();
            System.out.println(itemData.getTag());
            builder.intTag("Count", itemData.getCount());
            //builder.stringTag("Name", );
            builder.build("Item");
        }
        updateBlock(session, null);
    }

    @Override
    public boolean despawnEntity(GeyserSession session) {
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(getBedrockPosition());
        updateBlockPacket.setRuntimeId(0);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        session.getUpstream().sendPacket(updateBlockPacket);
        return true;
    }

    private CompoundTag getDefaultTag() {
        CompoundTagBuilder builder = CompoundTag.builder();
        builder.intTag("x", bedrockPosition.getX());
        builder.intTag("y", bedrockPosition.getY());
        builder.intTag("z", bedrockPosition.getZ());
        builder.stringTag("id", "ItemFrame");
        return builder.buildRootTag();
    }

    private void updateBlock(GeyserSession session, CompoundTag tag) {
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(getBedrockPosition());
        updateBlockPacket.setRuntimeId(2441);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        session.getUpstream().sendPacket(updateBlockPacket);

        BlockEntityDataPacket blockEntityDataPacket = new BlockEntityDataPacket();
        blockEntityDataPacket.setBlockPosition(getBedrockPosition());
        if (tag != null) {
            blockEntityDataPacket.setData(tag);
        } else {
            blockEntityDataPacket.setData(getDefaultTag());
        }
        session.getUpstream().sendPacket(blockEntityDataPacket);
    }
}
