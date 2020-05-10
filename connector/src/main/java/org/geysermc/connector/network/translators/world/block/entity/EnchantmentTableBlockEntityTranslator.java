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

import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.CompoundTagBuilder;
import com.nukkitx.nbt.tag.CompoundTag;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;
import org.geysermc.connector.utils.BlockEntityUtils;

/**
 * The enchantment table requires a block entity tag in order to show the floating book
 */
public class EnchantmentTableBlockEntityTranslator implements BedrockOnlyBlockEntity, RequiresBlockState {

    @Override
    public boolean isBlock(BlockState blockState) {
        return BlockStateValues.getEnchantmentTableId() != 0 && blockState.getId() == BlockStateValues.getEnchantmentTableId();
    }

    @Override
    public void updateBlock(GeyserSession session, BlockState blockState, Vector3i position) {
        BlockEntityUtils.updateBlockEntity(session, getTag(position), position);
    }

    public static CompoundTag getTag(Vector3i position) {
        CompoundTagBuilder tagBuilder = CompoundTagBuilder.builder()
                .intTag("x", position.getX())
                .intTag("y", position.getY())
                .intTag("z", position.getZ())
                .byteTag("isMovable", (byte) 1)
                .stringTag("id", "EnchantTable")
                .floatTag("rott", 0.0f);
        return tagBuilder.buildRootTag();
    }
}
