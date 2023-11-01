package org.geysermc.geyser.translator.level.block.entity;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.BlockEntityUtils;

@BlockEntity(type = "Cauldron")
public class CauldronBlockEntityTranslator extends BlockEntityTranslator implements SimpleBedrockOnlyBlockEntity {

    @Override
    public void updateBlock(GeyserSession session, int blockState, Vector3i position) {
        NbtMapBuilder builder = getConstantBedrockTag("Cauldron", position.getX(), position.getY(), position.getZ());
        translateTag(session, builder, null, blockState);
        BlockEntityUtils.updateBlockEntity(session, builder.build(), position);
    }

    @Override
    public void translateTag(GeyserSession session, NbtMapBuilder builder, CompoundTag tag, int blockState) {
        builder.putByte("isMovable", (byte) 0)
            .putShort("PotionId", (short) -1)
            .putShort("PotionType", (short) -1)
            .putList("Items", NbtType.END, NbtList.EMPTY);
    }
}
