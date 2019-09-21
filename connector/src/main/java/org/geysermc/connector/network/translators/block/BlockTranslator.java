package org.geysermc.connector.network.translators.block;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import org.geysermc.connector.console.GeyserLogger;
import org.geysermc.connector.network.translators.item.BedrockItem;
import org.geysermc.connector.utils.Remapper;

// Class for future expansion
public class BlockTranslator {

    public BedrockItem getBedrockBlock(BlockState state) {
        BedrockItem bedrockItem = Remapper.BLOCK_REMAPPER.convertToBedrockB(new ItemStack(state.getId()));
        if (bedrockItem == null) {
            GeyserLogger.DEFAULT.debug("Missing mapping for java block " + state.getId() + "/nPlease report this to Geyser.");
            return BedrockItem.DIRT; // so we can walk and not getting stuck x)
        }

        return bedrockItem;
    }
}
