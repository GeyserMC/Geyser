package org.geysermc.connector.network.translators.block.entity;

import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;

public interface RequiresBlockState {

    boolean isBlock(BlockState blockState);

}
