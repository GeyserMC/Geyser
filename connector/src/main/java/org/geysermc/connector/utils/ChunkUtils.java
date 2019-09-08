package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.chunk.BlockStorage;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.item.BedrockItem;
import org.geysermc.connector.world.chunk.ChunkSection;

public class ChunkUtils {

    public static ChunkData translateToBedrock(Column column) {
        ChunkData chunkData = new ChunkData();
        chunkData.sections = new ChunkSection[16];
        for (int i = 0; i < 16; i++) {
            chunkData.sections[i] = new ChunkSection();
        }

        /*
        for (int y = 0; y < 256; y++) {
            int chunkY = y >> 4;

            Chunk chunk = null;
            try {
                chunk = column.getChunks()[chunkY];
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (chunk == null || chunk.isEmpty())
                continue;

            BlockStorage storage = chunk.getBlocks();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockState block = storage.get(x, chunkY, z);
                    if (block == null)
                        block = new BlockState(0);

                    BedrockItem bedrockBlock = TranslatorsInit.getItemTranslator().getBedrockBlock(block);

                    ChunkSection section = chunkData.sections[chunkY];

                    //org.geysermc.connector.world.chunk.BlockStorage blockStorage = new org.geysermc.connector.world.chunk.BlockStorage();
                    int runtimeId = GlobalBlockPalette.getOrCreateRuntimeId(bedrockBlock.getId(), bedrockBlock.getData());
                    section.setFullBlock(x, y >> 4, z, 0, runtimeId << 2 | bedrockBlock.getData());

                    //section.getBlockStorageArray()[0] = blockStorage;
                    //section.getBlockStorageArray()[1] = blockStorage;
                }
            }
        }

         */

        for (int chunkY = 0; chunkY < 16; chunkY++) {
            Chunk chunk = null;
            try {
                chunk = column.getChunks()[chunkY];
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (chunk == null || chunk.isEmpty())
                continue;

            BlockStorage storage = chunk.getBlocks();
            ChunkSection section = chunkData.sections[chunkY];

            section.getBlockStorageArray()[0] = new org.geysermc.connector.world.chunk.BlockStorage();
            section.getBlockStorageArray()[1] = new org.geysermc.connector.world.chunk.BlockStorage();

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState block = storage.get(x, y, z);
                        BedrockItem bedrockBlock = TranslatorsInit.getItemTranslator().getBedrockBlock(block);

                        section.getBlockStorageArray()[0].setFullBlock(ChunkSection.blockPosition(x, y, z), bedrockBlock.getId() << 4 | bedrockBlock.getData());
                    }
                }
            }
        }
        return chunkData;
    }

    public static final class ChunkData {
        public ChunkSection[] sections;

        public byte[] biomes = new byte[256];
        public byte[] blockEntities = new byte[0];
    }
}
