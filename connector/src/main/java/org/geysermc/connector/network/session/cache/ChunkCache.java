/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Setter;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.network.translators.world.chunk.GeyserColumn;
import org.geysermc.connector.utils.MathUtils;

public class ChunkCache {
    private final boolean cache;
    private final Long2ObjectMap<GeyserColumn> chunks;

    @Setter
    private int minY;
    @Setter
    private int heightY;

    public ChunkCache(GeyserSession session) {
        this.cache = !session.getConnector().getWorldManager().hasOwnChunkCache(); // To prevent Spigot from initializing
        chunks = cache ? new Long2ObjectOpenHashMap<>() : null;
    }

    public void addToCache(Column chunk) {
        if (!cache) {
            return;
        }

        long chunkPosition = MathUtils.chunkPositionToLong(chunk.getX(), chunk.getZ());
        GeyserColumn geyserColumn = GeyserColumn.from(this, chunk);
        chunks.put(chunkPosition, geyserColumn);
    }

    public GeyserColumn getChunk(int chunkX, int chunkZ)  {
        long chunkPosition = MathUtils.chunkPositionToLong(chunkX, chunkZ);
        return chunks.getOrDefault(chunkPosition, null);
    }

    public void updateBlock(int x, int y, int z, int block) {
        if (!cache) {
            return;
        }

        GeyserColumn column = this.getChunk(x >> 4, z >> 4);
        if (column == null) {
            return;
        }

        if (y < minY || (y >> 4) > column.getChunks().length - 1) {
            // Y likely goes above or below the height limit of this world
            return;
        }

        Chunk chunk = column.getChunks()[(y >> 4) - getChunkMinY()];
        if (chunk == null) {
            if (block != BlockTranslator.JAVA_AIR_ID) {
                // A previously empty chunk, which is no longer empty as a block has been added to it
                chunk = new Chunk();
                // Fixes the chunk assuming that all blocks is the `block` variable we are updating. /shrug
                chunk.getPalette().stateToId(BlockTranslator.JAVA_AIR_ID);
                column.getChunks()[(y >> 4) - getChunkMinY()] = chunk;
            } else {
                // Nothing to update
                return;
            }
        }

        chunk.set(x & 0xF, y & 0xF, z & 0xF, block);
    }

    public int getBlockAt(int x, int y, int z) {
        if (!cache) {
            return BlockTranslator.JAVA_AIR_ID;
        }

        GeyserColumn column = this.getChunk(x >> 4, z >> 4);
        if (column == null) {
            return BlockTranslator.JAVA_AIR_ID;
        }

        if (y < minY || (y >> 4) > column.getChunks().length - 1) {
            // Y likely goes above or below the height limit of this world
            return BlockTranslator.JAVA_AIR_ID;
        }

        Chunk chunk = column.getChunks()[(y >> 4) - getChunkMinY()];
        if (chunk != null) {
            return chunk.get(x & 0xF, y & 0xF, z & 0xF);
        }

        return BlockTranslator.JAVA_AIR_ID;
    }

    public void removeChunk(int chunkX, int chunkZ) {
        if (!cache) {
            return;
        }

        long chunkPosition = MathUtils.chunkPositionToLong(chunkX, chunkZ);
        chunks.remove(chunkPosition);
    }

    public int getChunkMinY() {
        return minY >> 4;
    }

    public int getChunkHeightY() {
        return heightY >> 4;
    }
}
