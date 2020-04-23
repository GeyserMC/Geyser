package org.geysermc.connector.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.type.object.HangingDirection;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.utils.Toolbox;

import java.util.concurrent.TimeUnit;

/**
 * Item frames are an entity in Java but a block entity in Bedrock.
 */
public class ItemFrameEntity extends Entity {

    /**
     * A map of Vector3i positions to Java entity IDs.
     * Used for translating Bedrock block actions to Java entity actions.
     */
    private static final Object2LongMap<Vector3i> POSITION_TO_ENTITY_ID = new Object2LongOpenHashMap<>();

    /**
     * Used for getting the Bedrock block position.
     * Blocks deal with integers whereas entities deal with floats.
     */
    Vector3i bedrockPosition;
    /**
     * Specific block 'state' we are emulating in Bedrock.
     */
    int bedrockRuntimeId;
    /**
     * Rotation of item in frame
     */
    float rotation = 0.0f;
    CompoundTag cachedTag;

    public ItemFrameEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation, HangingDirection direction) {
        super(entityId, geyserId, entityType, position, motion, rotation);
        CompoundTagBuilder builder = CompoundTag.builder();
        builder.tag(CompoundTag.builder()
                .stringTag("name", "minecraft:frame")
                .intTag("version", BlockTranslator.getBlockStateVersion())
                .tag(CompoundTag.builder()
                        .intTag("facing_direction", direction.ordinal())
                        .byteTag("item_frame_map_bit", (byte) 0)
                        .build("states"))
                .build("block"));
        builder.shortTag("id", (short) 199);
        bedrockRuntimeId = BlockTranslator.getItemFrame(builder.buildRootTag());
        bedrockPosition = Vector3i.from(position.getFloorX(), position.getFloorY(), position.getFloorZ());
        POSITION_TO_ENTITY_ID.put(bedrockPosition, entityId);
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
            ItemData itemData = ItemTranslator.translateToBedrock(session, (ItemStack) entityMetadata.getValue());
            ItemEntry itemEntry = new ItemTranslator().getItem((ItemStack) entityMetadata.getValue());
            CompoundTagBuilder builder = CompoundTag.builder();

            String blockName = "";
            for (StartGamePacket.ItemEntry startGamePacketItemEntry: Toolbox.ITEMS) {
                if (startGamePacketItemEntry.getId() == (short) itemEntry.getBedrockId()) {
                    blockName = startGamePacketItemEntry.getIdentifier();
                    break;
                }
            }

            builder.byteTag("Count", (byte) itemData.getCount());
            builder.shortTag("Damage", itemData.getDamage());
            builder.stringTag("Name", blockName);
            CompoundTagBuilder tag = getDefaultTag().toBuilder();
            tag.tag(builder.build("Item"));
            tag.floatTag("ItemDropChance", 1.0f);
            tag.floatTag("ItemRotation", rotation);
            cachedTag = tag.buildRootTag();
            updateBlock(session, tag.buildRootTag());
        } else if (entityMetadata.getId() == 7 && entityMetadata.getValue() == null && cachedTag != null) {
            cachedTag = getDefaultTag();
            updateBlock(session, null);
        } else if (entityMetadata.getId() == 8) {
            rotation = ((int) entityMetadata.getValue()) * 45;
            if (cachedTag == null) {
                session.getConnector().getLogger().warning("Cached item frame tag is null at " + bedrockPosition.toString());
                return;
            }
            CompoundTagBuilder builder = cachedTag.toBuilder();
            builder.floatTag("ItemRotation", rotation);
            updateBlock(session, builder.buildRootTag());
        } else {
            updateBlock(session, null);
        }
    }

    @Override
    public boolean despawnEntity(GeyserSession session) {
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(bedrockPosition);
        updateBlockPacket.setRuntimeId(0);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.PRIORITY);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NONE);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        session.getUpstream().sendPacket(updateBlockPacket);
        POSITION_TO_ENTITY_ID.remove(position, entityId);
        return true;
    }

    private CompoundTag getDefaultTag() {
        CompoundTagBuilder builder = CompoundTag.builder();
        builder.intTag("x", bedrockPosition.getX());
        builder.intTag("y", bedrockPosition.getY());
        builder.intTag("z", bedrockPosition.getZ());
        builder.byteTag("isMovable", (byte) 1);
        builder.stringTag("id", "ItemFrame");
        return builder.buildRootTag();
    }

    private void updateBlock(GeyserSession session, CompoundTag tag) {
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(bedrockPosition);
        updateBlockPacket.setRuntimeId(bedrockRuntimeId);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.PRIORITY);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NONE);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        if (session.isSpawned()) {
            session.getUpstream().sendPacket(updateBlockPacket);
        } else {
            session.getConnector().getGeneralThreadPool().schedule(() ->
                    session.getUpstream().sendPacket(updateBlockPacket),
                    5,
                    TimeUnit.SECONDS);
        }

        BlockEntityDataPacket blockEntityDataPacket = new BlockEntityDataPacket();
        blockEntityDataPacket.setBlockPosition(bedrockPosition);
        if (tag != null) {
            blockEntityDataPacket.setData(tag);
        } else if (cachedTag != null) {
            blockEntityDataPacket.setData(cachedTag);
        } else {
            blockEntityDataPacket.setData(getDefaultTag());
        }
        System.out.println(blockEntityDataPacket);
        session.getUpstream().sendPacket(blockEntityDataPacket);
    }

    /**
     * Finds the Java entity ID of an item frame from its Bedrock position.
     * @param position position of item frame in Bedrock
     * @return Java entity ID or -1 if not found
     */
    public static long getItemFrameEntityId(Vector3i position) {
        return POSITION_TO_ENTITY_ID.getOrDefault(position, -1);
    }
}
