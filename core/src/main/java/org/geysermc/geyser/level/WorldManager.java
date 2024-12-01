/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.level;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.erosion.util.BlockPositionIterator;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemCodecHelper;
import org.geysermc.mcprotocollib.protocol.data.game.setting.Difficulty;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Class that manages or retrieves various information
 * from the world. Everything in this class should be
 * safe to return null or an empty value in the event
 * that chunk caching or anything of the sort is disabled
 * on the standalone version of Geyser.
 */
public abstract class WorldManager {

    @NonNull
    public final BlockState blockAt(GeyserSession session, Vector3i vector) {
        return this.blockAt(session, vector.getX(), vector.getY(), vector.getZ());
    }

    @NonNull
    public BlockState blockAt(GeyserSession session, int x, int y, int z) {
        return BlockState.of(this.getBlockAt(session, x, y, z));
    }

    /**
     * Gets the Java block state at the specified location
     *
     * @param session the session
     * @param vector the position
     * @return the block state at the specified location
     */
    public final int getBlockAt(GeyserSession session, Vector3i vector) {
        return this.getBlockAt(session, vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * Gets the Java block state at the specified location
     *
     * @param session the session
     * @param x the x coordinate to get the block at
     * @param y the y coordinate to get the block at
     * @param z the z coordinate to get the block at
     * @return the block state at the specified location
     */
    public abstract int getBlockAt(GeyserSession session, int x, int y, int z);

    public final CompletableFuture<Integer> getBlockAtAsync(GeyserSession session, Vector3i vector) {
        return this.getBlockAtAsync(session, vector.getX(), vector.getY(), vector.getZ());
    }

    public CompletableFuture<Integer> getBlockAtAsync(GeyserSession session, int x, int y, int z) {
        return CompletableFuture.completedFuture(this.getBlockAt(session, x, y, z));
    }

    public int[] getBlocksAt(GeyserSession session, BlockPositionIterator iter) {
        int[] blocks = new int[iter.getMaxIterations()];
        for (; iter.hasNext(); iter.next()) {
            int networkId = this.getBlockAt(session, iter.getX(), iter.getY(), iter.getZ());
            blocks[iter.getIteration()] = networkId;
        }
        return blocks;
    }

    /**
     * Checks whether or not this world manager requires a separate chunk cache/has access to more block data than the chunk cache.
     * <p>
     * Some world managers (e.g. Spigot) can provide access to block data outside of the chunk cache, and even with chunk caching disabled. This
     * method provides a means to check if this manager has this capability.
     *
     * @return whether or not this world manager has access to more block data than the chunk cache
     */
    public abstract boolean hasOwnChunkCache();

    /**
     * Updates a gamerule value on the Java server
     *
     * @param session The session of the user that requested the change
     * @param name The gamerule to change
     * @param value The new value for the gamerule
     */
    public void setGameRule(GeyserSession session, String name, Object value) {
        session.sendCommand("gamerule " + name + " " + value);
    }

    /**
     * Gets a gamerule value as a boolean
     *
     * @param session The session of the user that requested the value
     * @param gameRule The gamerule to fetch the value of
     * @return The boolean representation of the value
     */
    public abstract boolean getGameRuleBool(GeyserSession session, GameRule gameRule);

    /**
     * Get a gamerule value as an integer
     *
     * @param session The session of the user that requested the value
     * @param gameRule The gamerule to fetch the value of
     * @return The integer representation of the value
     */
    public abstract int getGameRuleInt(GeyserSession session, GameRule gameRule);

    /**
     * Change the game mode of the given session
     *
     * @param session The session of the player to change the game mode of
     * @param gameMode The game mode to change the player to
     */
    public void setPlayerGameMode(GeyserSession session, GameMode gameMode) {
        session.sendCommand("gamemode " + gameMode.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Get the default game mode of the server
     *
     * @param session the player requesting the default game mode
     * @return the default game mode of the server, or Survival if unknown.
     */
    public abstract GameMode getDefaultGameMode(GeyserSession session);

    /**
     * Change the default game mode of the session's server
     *
     * @param session the player making the change
     * @param gameMode the new default game mode
     */
    public void setDefaultGameMode(GeyserSession session, GameMode gameMode) {
        session.sendCommand("defaultgamemode " + gameMode.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Change the difficulty of the Java server
     *
     * @param session The session of the user that requested the change
     * @param difficulty The difficulty to change to
     */
    public void setDifficulty(GeyserSession session, Difficulty difficulty) {
        session.sendCommand("difficulty " + difficulty.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Returns a list of biome identifiers available on the server.
     */
    public String @Nullable [] getBiomeIdentifiers(boolean withTags) {
        return null;
    }

    /**
     * Retrieves decorated pot sherds from the server. Used to ensure the data is not erased on animation sent
     * through the BlockEntityDataPacket.
     */
    public void getDecoratedPotData(GeyserSession session, Vector3i pos, Consumer<List<String>> apply) {
    }

    protected static final Function<Int2ObjectMap<byte[]>, DataComponents> RAW_TRANSFORMER = map -> {
        try {
            Map<DataComponentType<?>, DataComponent<?, ?>> components = new HashMap<>();
            Int2ObjectMaps.fastForEach(map, entry -> {
                DataComponentType type = DataComponentType.from(entry.getIntKey());
                ByteBuf buf = Unpooled.wrappedBuffer(entry.getValue());
                DataComponent value = type.readDataComponent(ItemCodecHelper.INSTANCE, buf);
                components.put(type, value);
            });
            return new DataComponents(components);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    };
}
