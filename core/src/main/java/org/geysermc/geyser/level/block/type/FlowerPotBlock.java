/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level.block.type;

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.level.block.entity.BedrockChunkWantsBlockEntityTag;
import org.geysermc.geyser.translator.level.block.entity.BlockEntityTranslator;
import org.geysermc.geyser.util.BlockEntityUtils;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

public class FlowerPotBlock extends Block implements BedrockChunkWantsBlockEntityTag {
    private final Block flower;

    public FlowerPotBlock(String javaIdentifier, Block flower, Builder builder) {
        super(javaIdentifier, builder);
        this.flower = flower;
    }

    @Override
    public void updateBlock(GeyserSession session, BlockState state, Vector3i position) {
        super.updateBlock(session, state, position);

        NbtMap tag = createTag(session, position, state);
        BlockEntityUtils.updateBlockEntity(session, tag, position);
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setDefinition(session.getBlockMappings().getBedrockBlock(state));
        updateBlockPacket.setBlockPosition(position);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NEIGHBORS);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.NETWORK);
        updateBlockPacket.getFlags().add(UpdateBlockPacket.Flag.PRIORITY);
        session.sendUpstreamPacket(updateBlockPacket);
        BlockEntityUtils.updateBlockEntity(session, tag, position);
    }

    @Override
    public NbtMap createTag(GeyserSession session, Vector3i position, BlockState blockState) {
        NbtMapBuilder tagBuilder = BlockEntityTranslator.getConstantBedrockTag("FlowerPot", position.getX(), position.getY(), position.getZ())
                .putByte("isMovable", (byte) 1);
        // Get the Java name of the plant inside. e.g. minecraft:oak_sapling
        if (this.flower != Blocks.AIR) {
            // Get the Bedrock CompoundTag of the block.
            // This is where we need to store the *Java* name because Bedrock has six minecraft:sapling blocks with different block states.
            // TODO flattening might make this nicer in the future!
            NbtMap plant = session.getBlockMappings().getFlowerPotBlocks().get(this.flower);
            if (plant != null) {
                tagBuilder.putCompound("PlantBlock", plant.toBuilder().build());
            }
        }
        return tagBuilder.build();
    }

    @Override
    public ItemStack pickItem(BlockState state) {
        if (this.flower != Blocks.AIR) {
            return new ItemStack(this.flower.asItem().javaId());
        }
        return super.pickItem(state);
    }

    public Block flower() {
        return flower;
    }
}
