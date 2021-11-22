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

package org.geysermc.geyser.session.cache;

import com.github.steveice10.mc.protocol.data.game.chunk.DataPalette;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.chunk.GeyserChunk;
import org.geysermc.geyser.util.MathUtils;

public class ChunkCache {
    private final boolean cache;
    private final Long2ObjectMap<GeyserChunk> chunks;

    @Setter
    private int minY;
    @Setter
    private int heightY;

    /**
     * Whether the Bedrock client believes they are in a world with a minimum of -64 and maximum of 320
     */
    @Getter
    @Setter
    private boolean isExtendedHeight = false;

    public ChunkCache(GeyserSession session) {
        this.cache = !session.getGeyser().getWorldManager().hasOwnChunkCache(); // To prevent Spigot from initializing
        chunks = cache ? new Long2ObjectOpenHashMap<>() : null;
    }

    public void addToCache(int x, int z, DataPalette[] chunks) {
        if (!cache) {
            return;
        }

        long chunkPosition = MathUtils.chunkPositionToLong(x, z);
        GeyserChunk geyserChunk = GeyserChunk.from(chunks);
        this.chunks.put(chunkPosition, geyserChunk);
    }

    /**
     * Doesn't check for cache enabled, so don't use this without checking that first!
     */
    private GeyserChunk getChunk(int chunkX, int chunkZ) {
        long chunkPosition = MathUtils.chunkPositionToLong(chunkX, chunkZ);
        return chunks.getOrDefault(chunkPosition, null);
    }

    public void updateBlock(int x, int y, int z, int block) {
        if (!cache) {
            return;
        }

        GeyserChunk chunk = this.getChunk(x >> 4, z >> 4);
        if (chunk == null) {
            return;
        }

        if (y < minY || ((y - minY) >> 4) > chunk.sections().length - 1) {
            // Y likely goes above or below the height limit of this world
            return;
        }

        DataPalette palette = chunk.sections()[(y - minY) >> 4];
        if (palette == null) {
            if (block != BlockStateValues.JAVA_AIR_ID) {
                // A previously empty chunk, which is no longer empty as a block has been added to it
                palette = DataPalette.createForChunk();
                // Fixes the chunk assuming that all blocks is the `block` variable we are updating. /shrug
                palette.getPalette().stateToId(BlockStateValues.JAVA_AIR_ID);
                chunk.sections()[(y - minY) >> 4] = palette;
            } else {
                // Nothing to update
                return;
            }
        }

        palette.set(x & 0xF, y & 0xF, z & 0xF, block);
    }

    public int getBlockAt(int x, int y, int z) {
        if (!cache) {
            return BlockStateValues.JAVA_AIR_ID;
        }

        GeyserChunk column = this.getChunk(x >> 4, z >> 4);
        if (column == null) {
            return BlockStateValues.JAVA_AIR_ID;
        }

        if (y < minY || ((y - minY) >> 4) > column.sections().length - 1) {
            // Y likely goes above or below the height limit of this world
            return BlockStateValues.JAVA_AIR_ID;
        }

        DataPalette chunk = column.sections()[(y - minY) >> 4];
        if (chunk != null) {
            return chunk.get(x & 0xF, y & 0xF, z & 0xF);
        }

        return BlockStateValues.JAVA_AIR_ID;
    }

    public void removeChunk(int chunkX, int chunkZ) {
        if (!cache) {
            return;
        }

        long chunkPosition = MathUtils.chunkPositionToLong(chunkX, chunkZ);
        chunks.remove(chunkPosition);
    }

    /**
     * Manually clears all entries in the chunk cache.
     * The server is responsible for clearing chunk entries if out of render distance (for example) or switching dimensions,
     * but it is the client that must clear sections in the event of proxy switches.
     */
    public void clear() {
        if (!cache) {
            return;
        }

        chunks.clear();
    }

    public int getChunkMinY() {
        return minY >> 4;
    }

    public int getChunkHeightY() {
        return heightY >> 4;
    }
}
