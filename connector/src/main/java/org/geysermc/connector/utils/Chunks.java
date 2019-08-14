package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import gnu.trove.list.TByteList;
import gnu.trove.list.array.TByteArrayList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.geysermc.connector.network.translators.TranslatorsInit;

import java.util.Objects;

public class Chunks {

    public static ChunkData getData(Column c) {
        Objects.requireNonNull(c);

        int count = 0;

        for(Chunk chunk : c.getChunks()) {
            if(chunk != null) {
                count++;
            }
        }

        int block = 0;

        TByteList list = new TByteArrayList(4096 * 4);

        for(int i = 0; i < 256; i++) {
            list.add((byte) 0);
        }

        for(Chunk chunk : c.getChunks()) {
            if (chunk != null) {
                list.add((byte) 0);
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; x < 16; x++) {
                        for (int z = 0; x < 16; x++) {
                            try {
                                list.add((byte) TranslatorsInit.getItemTranslator().getBedrockBlock(chunk.getBlocks().get(x, y, z)).getId());
                            } catch (NullPointerException e) {
                                list.add((byte) 0);
                            }

                            block++;
                        }
                    }
                }

                for (int x = 0; x < 16; x++) {
                    for (int y = 0; x < 16; x++) {
                        for (int z = 0; x < 16; x++) {
                            try {
                                list.add((byte) TranslatorsInit.getItemTranslator().getBedrockBlock(chunk.getBlocks().get(x, y, z)).getData());
                            } catch (NullPointerException e) {
                                list.add((byte) 0);
                            }

                            block++;
                        }
                    }
                }
            }
        }

        list.add((byte) 0);
        list.add((byte) 0);

        return new ChunkData(count, list.toArray());
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public static final class ChunkData {
        public final int count;

        public final byte[] bytes;

    }
}
