package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.block.BlockEntry;
import org.geysermc.connector.world.BiomePalette;
import org.geysermc.connector.world.chunk.ChunkSection;

public class ChunkUtils {

    public static ChunkData translateToBedrock(Column column) {
        ChunkData chunkData = new ChunkData();

        Chunk[] chunks = column.getChunks();

        int chunkSectionCount = chunks.length;

        chunkData.sections = new ChunkSection[chunkSectionCount];

        int[] biomesConverted = new int[256];

        try {
            for (int biomeX = 0; biomeX < 16; biomeX++) {
                for (int biomeZ = 0; biomeZ < 16; biomeZ++) {
                    biomesConverted[(biomeX << 4) | biomeZ] = column.getBiomeData()[(biomeZ * 4) | biomeX];
                }
            }

            BiomePalette palette = new BiomePalette(biomesConverted);

            for (int biomeX = 0; biomeX < 16; biomeX++) {
                for (int biomeZ = 0; biomeZ < 16; biomeZ++) {
                    chunkData.biomes[(biomeX << 4) | biomeZ] = (byte) (palette.get(biomeX, biomeZ));
                }
            }
        } catch (Exception e) {}

        for(CompoundTag tag : column.getTileEntities()) {
            //TODO: Tiles
            //System.out.println(tag.toString());
        }

        for (int chunkY = 0; chunkY < chunkSectionCount; chunkY++) {
            chunkData.sections[chunkY] = new ChunkSection();
            Chunk chunk = chunks[chunkY];

            if (chunk == null || chunk.isEmpty())
                continue;

            ChunkSection section = chunkData.sections[chunkY];

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState blockState = chunk.get(x, y, z);
                        BlockEntry block = TranslatorsInit.getBlockTranslator().getBedrockBlock(blockState);

                        section.getBlockStorageArray()[0].setFullBlock(ChunkSection.blockPosition(x, y, z),
                                block.getBedrockId() << 4 | block.getBedrockData());

                        if (block.getJavaIdentifier().contains("waterlogged=true")) {
                            section.getBlockStorageArray()[1].setFullBlock(ChunkSection.blockPosition(x, y, z),
                                    9 << 4); // water id
                        }
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
