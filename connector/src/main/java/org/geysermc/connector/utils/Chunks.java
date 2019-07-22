package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.Objects;

public class Chunks {

    public ChunkData getData(Column c) {
        Objects.requireNonNull(c);

        int count = 0;

        for(Chunk chunk : c.getChunks()) {
            if(chunk != null) {
                count++;
            }
        }

        return null;
    }

    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    public static final class ChunkData {
        public final int count;

        public final byte[] bytes;

    }
}
