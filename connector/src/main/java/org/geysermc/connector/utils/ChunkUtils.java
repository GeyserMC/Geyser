package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.chunk.BlockStorage;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.block.BlockEntry;
import org.geysermc.connector.world.chunk.ChunkSection;

public class ChunkUtils {

    public static ChunkData translateToBedrock(Column column) {
        ChunkData chunkData = new ChunkData();

        Chunk[] chunks = column.getChunks();
        int chunkSectionCount = chunks.length;
        chunkData.sections = new ChunkSection[chunkSectionCount];
        for (int i = 0; i < chunkSectionCount; i++) {
            chunkData.sections[i] = new ChunkSection();
        }

        try {
            for (int chunkY = 0; chunkY < chunkSectionCount; chunkY++) {
                Chunk chunk = chunks[chunkY];

                if (chunk == null || chunk.isEmpty())
                    continue;

                BlockStorage storage = chunk.getBlocks();
                ChunkSection section = chunkData.sections[chunkY];

                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            BlockState blockState = storage.get(x, y, z);
                            BlockEntry block = TranslatorsInit.getBlockTranslator().getBedrockBlock(blockState);

                            section.getBlockStorageArray()[0].setFullBlock(ChunkSection.blockPosition(x, y, z),
                                    block.getBedrockId() << 4 | block.getBedrockData());

                            if (block.getJavaIdentifier().contains("waterlogged=true")) {
                                section.getBlockStorageArray()[1].setFullBlock(ChunkSection.blockPosition(x, y, z),
                                        9 << 4); // water id
                            }
                        }

                        Thread.sleep(0, 3000);
                    }

                    Thread.sleep(0, 15000);
                }

                Thread.sleep(0, 30000);
            }
        } catch (InterruptedException e) {
            //Shouldn't happen
        }
        return chunkData;
    }

    public static final class ChunkData {
        public ChunkSection[] sections;

        public byte[] biomes = new byte[256];
        public byte[] blockEntities = new byte[0];
    }
}
