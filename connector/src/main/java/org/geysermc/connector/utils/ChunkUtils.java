package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.chunk.BlockStorage;
import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.item.BedrockItem;
import org.geysermc.connector.world.chunk.ChunkSection;

import java.util.Arrays;

public class ChunkUtils {

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    public static ChunkData translateToBedrock(Column column) {
        ChunkData chunkData = new ChunkData();
        chunkData.sections = new ChunkSection[16];
        for (int i = 0; i < 16; i++) {
            chunkData.sections[i] = new ChunkSection();
        }

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
                    BlockState block = storage.get(x, y & 0xF, z);
                    BedrockItem bedrockBlock = TranslatorsInit.getItemTranslator().getBedrockBlock(block);

                    ChunkSection section = chunkData.sections[chunkY];

                    org.geysermc.connector.world.chunk.BlockStorage blockStorage = new org.geysermc.connector.world.chunk.BlockStorage();
                    blockStorage.setFullBlock(ChunkSection.blockPosition(x, y, z), bedrockBlock.getId());

                    section.getBlockStorageArray()[0] = blockStorage;
                    section.getBlockStorageArray()[1] = blockStorage;
                }
            }
        }

        return chunkData;
    }

    public static final class ChunkData {
        public ChunkSection[] sections;
    }

    public static void putBytes(int count, byte[] buffer, byte[] bytes) {
        if (bytes == null) {
            return;
        }

        int minCapacity = count + bytes.length;
        if ((minCapacity) - buffer.length > 0) {
            int oldCapacity = buffer.length;
            int newCapacity = oldCapacity << 1;

            if (newCapacity - minCapacity < 0) {
                newCapacity = minCapacity;
            }

            if (newCapacity - MAX_ARRAY_SIZE > 0) {
                newCapacity = hugeCapacity(minCapacity);
            }

            buffer = Arrays.copyOf(buffer, newCapacity);
        }

        System.arraycopy(bytes, 0, buffer, count, bytes.length);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) { // overflow
            throw new OutOfMemoryError();
        }
        return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
    }
}
