/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.world.block.entity;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.utils.BlockEntityUtils;

public class FlowerPotBlockEntityTranslator implements BedrockOnlyBlockEntity, RequiresBlockState {

    @Override
    public boolean isBlock(int blockState) {
        return (BlockStateValues.getFlowerPotValues().containsKey(blockState));
    }

    @Override
    public void updateBlock(GeyserSession session, int blockState, Vector3i position) {
        BlockEntityUtils.updateBlockEntity(session, getTag(blockState, position), position);
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setRuntimeId(BlockTranslator.getBedrockBlockId(blockState));
        updateBlockPacket.setBlockPosition(position);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.PRIORITY);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NONE);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        session.sendUpstreamPacket(updateBlockPacket);
    }

    /**
     * Get the Nukkit CompoundTag of the flower pot.
     * @param blockState Java block state of flower pot.
     * @param position Bedrock position of flower pot.
     * @return Bedrock tag of flower pot.
     */
    public static CompoundTag getTag(int blockState, Vector3i position) {
        CompoundTagBuilder tagBuilder = CompoundTagBuilder.builder()
                .intTag("x", position.getX())
                .intTag("y", position.getY())
                .intTag("z", position.getZ())
                .byteTag("isMovable", (byte) 1)
                .stringTag("id", "FlowerPot");
        // Get the Java name of the plant inside. e.g. minecraft:oak_sapling
        String name = BlockStateValues.getFlowerPotValues().get(blockState);
        if (name != null) {
            // Get the Bedrock CompoundTag of the block.
            // This is where we need to store the *Java* name because Bedrock has six minecraft:sapling blocks with different block states.
            CompoundTag plant = BlockStateValues.getFlowerPotBlocks().get(name);
            if (plant != null) {
                tagBuilder.tag(plant.toBuilder().build("PlantBlock"));
            }
        }
        return tagBuilder.buildRootTag();
    }
}
