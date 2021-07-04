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

package org.geysermc.connector.network.translators.world.chunk;

import com.github.steveice10.mc.protocol.data.game.chunk.Chunk;
import com.github.steveice10.mc.protocol.data.game.chunk.Column;
import lombok.Getter;
import org.geysermc.connector.network.session.cache.ChunkCache;

/**
 * Acts as a lightweight version of {@link Column} that doesn't store
 * biomes or heightmaps.
 */
public class GeyserColumn {
    @Getter
    private final Chunk[] chunks;

    private GeyserColumn(Chunk[] chunks) {
        this.chunks = chunks;
    }

    public static GeyserColumn from(ChunkCache chunkCache, Column column) {
        int chunkHeightY = chunkCache.getChunkHeightY();
        Chunk[] chunks;
        if (chunkHeightY < column.getChunks().length) {
            chunks = new Chunk[chunkHeightY];
            // TODO addresses https://github.com/Steveice10/MCProtocolLib/pull/598#issuecomment-862782392
            System.arraycopy(column.getChunks(), 0, chunks, 0, chunks.length);
        } else {
            chunks = column.getChunks();
        }
        return new GeyserColumn(chunks);
    }
}
