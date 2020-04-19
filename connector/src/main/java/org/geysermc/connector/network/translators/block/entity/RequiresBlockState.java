package org.geysermc.connector.network.translators.block.entity;

import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;

/**
 * Implemented in block entities if their Java block state is required for additional values in Bedrock
 */
public interface RequiresBlockState {

    /**
     * Determines if block is part of class
     * @param blockState BlockState to be compared
     * @return true if part of the class
     */
    boolean isBlock(BlockState blockState);

}
