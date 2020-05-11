/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.network.session.cache;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;
import lombok.Getter;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.network.translators.world.chunk.ChunkPosition;

import java.util.HashMap;
import java.util.Map;

public class ChunkCache {

    private boolean cache;
    private final GeyserSession session;

    @Getter
    private Map<ChunkPosition, Column> chunks = new HashMap<>();

    public ChunkCache(GeyserSession session) {
        this.session = session;
        this.cache = session.getConnector().getConfig().isCacheChunks();
    }

    public void addToCache(Column chunk) {
        if (!cache) {
            return;
        }
        ChunkPosition position = new ChunkPosition(chunk.getX(), chunk.getZ());
        chunks.put(position, chunk);
    }

    public void updateBlock(Position position, BlockState block) {
        if (!cache) {
            return;
        }
        ChunkPosition chunkPosition = new ChunkPosition(position.getX() >> 4, position.getZ() >> 4);
        if (!chunks.containsKey(chunkPosition))
            return;

        Column column = chunks.get(chunkPosition);
        Chunk chunk = column.getChunks()[position.getY() >> 4];
        Position blockPosition = chunkPosition.getChunkBlock(position.getX(), position.getY(), position.getZ());
        if (chunk != null) {
            chunk.set(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(), block);
        }
    }

    public BlockState getBlockAt(Position position) {
        if (!cache) {
            return BlockTranslator.AIR;
        }
        ChunkPosition chunkPosition = new ChunkPosition(position.getX() >> 4, position.getZ() >> 4);
        if (!chunks.containsKey(chunkPosition))
            return BlockTranslator.AIR;

        Column column = chunks.get(chunkPosition);
        Chunk chunk = column.getChunks()[position.getY() >> 4];
        Position blockPosition = chunkPosition.getChunkBlock(position.getX(), position.getY(), position.getZ());
        if (chunk != null) {
            return chunk.get(blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
        }

        return BlockTranslator.AIR;
    }

    public void removeChunk(ChunkPosition position) {
        if (!cache) {
            return;
        }
        chunks.remove(position);
    }
}
