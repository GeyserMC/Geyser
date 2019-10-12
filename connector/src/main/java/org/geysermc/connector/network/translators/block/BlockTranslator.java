package org.geysermc.connector.network.translators.block;

import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.utils.Toolbox;

public class BlockTranslator {

    public BlockEntry getBedrockBlock(BlockState state) {
        BlockEntry bedrockItem = Toolbox.BLOCK_ENTRIES.get(state.getId());
        if (bedrockItem == null) {
            GeyserLogger.DEFAULT.debug("Missing mapping for java block " + state.getId() + "/nPlease report this to Geyser.");
            return Toolbox.BLOCK_ENTRIES.get(10); // so we can walk and not getting stuck x)
        }

        return bedrockItem;
    }
}
