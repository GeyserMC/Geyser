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

package org.geysermc.connector.network.translators.block.entity;

import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.block.BlockStateValues;
import org.geysermc.connector.network.translators.block.BlockTranslator;

import java.util.concurrent.TimeUnit;

public class FlowerPotBlockEntityTranslator implements RequiresBlockState {

    @Override
    public boolean isBlock(BlockState blockState) {
        return BlockStateValues.getFlowerPotValue(blockState) != null;
    }

    public void updateBlock(GeyserSession session, BlockState blockState, Vector3i position) {
        BlockEntityDataPacket packet = new BlockEntityDataPacket();
        packet.setBlockPosition(position);
        packet.setData(
                CompoundTagBuilder.builder()
                    .intTag("x", position.getX())
                    .intTag("y", position.getY())
                    .intTag("z", position.getZ())
                    .byteTag("isMovable", (byte) 1)
                    .stringTag("id", "FlowerPot")
                    .tag(
                            CompoundTagBuilder.builder()
                                .stringTag("name", "minecraft:red_flower")
                                .intTag("version", 17760256)
                                .tag(
                                        CompoundTagBuilder.builder()
                                        .stringTag("flower_type", "cornflower")
                                        .build("states")
                                )
                                .build("PlantBlock")
                    ).buildRootTag()
        );
        System.out.println(packet.getData().toString());
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setBlockPosition(position);
        updateBlockPacket.setRuntimeId(BlockTranslator.getBedrockBlockId(blockState));
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NONE);
        updateBlockPacket.setDataLayer(0);
        session.getUpstream().sendPacket(packet);
        session.getConnector().getGeneralThreadPool().schedule(() -> session.getUpstream().sendPacket(updateBlockPacket), 25, TimeUnit.MILLISECONDS);
    }
}
