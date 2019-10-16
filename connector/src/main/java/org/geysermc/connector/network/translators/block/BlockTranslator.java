package org.geysermc.connector.network.translators.block;

import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import org.geysermc.connector.utils.Toolbox;

public class BlockTranslator {

    public BlockEntry getBedrockBlock(BlockState state) {
        return Toolbox.BLOCK_ENTRIES.get(state.getId());
    }
}
