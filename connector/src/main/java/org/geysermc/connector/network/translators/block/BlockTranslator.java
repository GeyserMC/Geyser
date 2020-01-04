package org.geysermc.connector.network.translators.block;

import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import org.geysermc.connector.utils.Toolbox;

import java.util.HashMap;
import java.util.Map;

public class BlockTranslator {
    private final Map<String, BlockEntry> javaIdentifierMap = new HashMap<>();

    public BlockEntry getBlockEntry(BlockState state) {
        return Toolbox.BLOCK_ENTRIES.get(state.getId());
    }

    public BlockEntry getBlockEntry(String javaIdentifier) {
        return javaIdentifierMap.computeIfAbsent(javaIdentifier, key -> Toolbox.BLOCK_ENTRIES.values()
                .stream().filter(blockEntry -> blockEntry.getJavaIdentifier().equals(key)).findFirst().orElse(null));
    }
}
