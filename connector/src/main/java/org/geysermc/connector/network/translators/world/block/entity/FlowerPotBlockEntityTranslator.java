/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.network.translators.world.block.entity;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;
import org.geysermc.connector.utils.BlockEntityUtils;

public class FlowerPotBlockEntityTranslator implements BedrockOnlyBlockEntity {
    /**
     * @param blockState the Java block state of a potential flower pot block
     * @return true if the block is a flower pot
     */
    public static boolean isFlowerBlock(int blockState) {
        return BlockStateValues.getFlowerPotValues().containsKey(blockState);
    }

    /**
     * Get the Nukkit CompoundTag of the flower pot.
     *
     * @param blockState Java block state of flower pot.
     * @param position   Bedrock position of flower pot.
     * @return Bedrock tag of flower pot.
     */
    public static NbtMap getTag(GeyserSession session, int blockState, Vector3i position) {
        NbtMapBuilder tagBuilder = NbtMap.builder()
                .putInt("x", position.getX())
                .putInt("y", position.getY())
                .putInt("z", position.getZ())
                .putByte("isMovable", (byte) 1)
                .putString("id", "FlowerPot");
        // Get the Java name of the plant inside. e.g. minecraft:oak_sapling
        String name = BlockStateValues.getFlowerPotValues().get(blockState);
        if (name != null) {
            // Get the Bedrock CompoundTag of the block.
            // This is where we need to store the *Java* name because Bedrock has six minecraft:sapling blocks with different block states.
            NbtMap plant = session.getBlockTranslator().getFlowerPotBlocks().get(name);
            if (plant != null) {
                tagBuilder.put("PlantBlock", plant.toBuilder().build());
            }
        }
        return tagBuilder.build();
    }

    @Override
    public boolean isBlock(int blockState) {
        return isFlowerBlock(blockState);
    }

    @Override
    public void updateBlock(GeyserSession session, int blockState, Vector3i position) {
        NbtMap tag = getTag(session, blockState, position);
        BlockEntityUtils.updateBlockEntity(session, tag, position);
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setRuntimeId(session.getBlockTranslator().getBedrockBlockId(blockState));
        updateBlockPacket.setBlockPosition(position);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.PRIORITY);
        session.sendUpstreamPacket(updateBlockPacket);
        BlockEntityUtils.updateBlockEntity(session, tag, position);
    }
}
