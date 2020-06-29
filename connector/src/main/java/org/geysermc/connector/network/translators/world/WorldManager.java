/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.world;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.nukkitx.math.vector.Vector3i;
import org.geysermc.connector.network.session.GeyserSession;

/**
 * Class that manages or retrieves various information
 * from the world. Everything in this class should be
 * safe to return null or an empty value in the event
 * that chunk caching or anything of the sort is disabled
 * on the standalone version of Geyser.
 */
public abstract class WorldManager {

    /**
     * Gets the block state at the specified location
     *
     * @param session the session
     * @param position the position
     * @return the block state at the specified location
     */
    public int getBlockAt(GeyserSession session, Position position) {
        return this.getBlockAt(session, position.getX(), position.getY(), position.getZ());
    }

    /**
     * Gets the block state at the specified location
     *
     * @param session the session
     * @param vector the position
     * @return the block state at the specified location
     */
    public int getBlockAt(GeyserSession session, Vector3i vector) {
        return this.getBlockAt(session, vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * Gets the block state at the specified location
     *
     * @param session the session
     * @param x the x coordinate to get the block at
     * @param y the y coordinate to get the block at
     * @param z the z coordinate to get the block at
     * @return the block state at the specified location
     */
    public abstract int getBlockAt(GeyserSession session, int x, int y, int z);
}
