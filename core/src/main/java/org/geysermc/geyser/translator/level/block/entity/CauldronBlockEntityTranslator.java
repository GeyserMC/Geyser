package org.geysermc.geyser.translator.level.block.entity;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.session.GeyserSession;

@BlockEntity(type = "Cauldron")
public class CauldronBlockEntityTranslator extends BlockEntityTranslator implements BedrockOnlyBlockEntity {

    @Override
    public void updateBlock(GeyserSession session, int blockState, Vector3i position) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateBlock'");
    }

    public static NbtMap getTag(Vector3i position) {
        return NbtMap.builder()
            .putString("id", "Cauldron")
            .putByte("isMovable", (byte) 0)
            .putShort("PotionId", (short) -1)
            .putShort("PotionType", (short) -1)
            .putList("Items", NbtType.END, NbtList.EMPTY)
            .putInt("x", position.getX())
            .putInt("y", position.getY())
            .putInt("z", position.getZ())
            .build();
    }

    @Override
    public void translateTag(NbtMapBuilder builder, CompoundTag tag, int blockState) {
        builder.putByte("isMovable", (byte) 0)
            .putShort("PotionId", (short) -1)
            .putShort("PotionType", (short) -1)
            .putList("Items", NbtType.END, NbtList.EMPTY);
    }
}
