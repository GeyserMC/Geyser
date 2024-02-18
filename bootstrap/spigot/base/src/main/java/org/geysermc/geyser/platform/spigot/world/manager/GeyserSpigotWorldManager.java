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

package org.geysermc.geyser.platform.spigot.world.manager;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityInfo;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.erosion.bukkit.BukkitLecterns;
import org.geysermc.erosion.bukkit.BukkitUtils;
import org.geysermc.erosion.bukkit.PickBlockUtils;
import org.geysermc.erosion.bukkit.SchedulerUtils;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.level.GameRule;
import org.geysermc.geyser.level.WorldManager;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.BlockEntityUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * The base world manager to use when there is no supported NMS revision
 */
public class GeyserSpigotWorldManager extends WorldManager {
    private final Plugin plugin;
    private final BukkitLecterns lecterns;

    public GeyserSpigotWorldManager(Plugin plugin) {
        this.plugin = plugin;
        this.lecterns = new BukkitLecterns(plugin);
    }

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        Player bukkitPlayer;
        if ((bukkitPlayer = Bukkit.getPlayer(session.getPlayerEntity().getUsername())) == null) {
            return BlockStateValues.JAVA_AIR_ID;
        }
        World world = bukkitPlayer.getWorld();
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            // If the chunk isn't loaded, how could we even be here?
            return BlockStateValues.JAVA_AIR_ID;
        }

        return getBlockNetworkId(world.getBlockAt(x, y, z));
    }

    public int getBlockNetworkId(Block block) {
        if (SchedulerUtils.FOLIA && !Bukkit.isOwnedByCurrentRegion(block)) {
            // Terrible behavior, but this is basically what's always been happening behind the scenes anyway.
            CompletableFuture<String> blockData = new CompletableFuture<>();
            Bukkit.getRegionScheduler().execute(this.plugin, block.getLocation(), () -> blockData.complete(block.getBlockData().getAsString()));
            return BlockRegistries.JAVA_IDENTIFIER_TO_ID.getOrDefault(blockData.join(), BlockStateValues.JAVA_AIR_ID);
        }
        return BlockRegistries.JAVA_IDENTIFIER_TO_ID.getOrDefault(block.getBlockData().getAsString(), BlockStateValues.JAVA_AIR_ID);
    }

    @Override
    public boolean hasOwnChunkCache() {
        return true;
    }

    @Override
    public void sendLecternData(GeyserSession session, int x, int y, int z) {
        Player bukkitPlayer;
        if ((bukkitPlayer = Bukkit.getPlayer(session.getPlayerEntity().getUsername())) == null) {
            return;
        }

        Block block = bukkitPlayer.getWorld().getBlockAt(x, y, z);
        // Run as a task to prevent async issues
        SchedulerUtils.runTask(this.plugin, () -> sendLecternData(session, block, false), block);
    }

    public void sendLecternData(GeyserSession session, int x, int z, List<BlockEntityInfo> blockEntityInfos) {
        Player bukkitPlayer;
        if ((bukkitPlayer = Bukkit.getPlayer(session.getPlayerEntity().getUsername())) == null) {
            return;
        }
        if (SchedulerUtils.FOLIA) {
            Chunk chunk = getChunk(bukkitPlayer.getWorld(), x, z);
            if (chunk == null) {
                return;
            }
            Bukkit.getRegionScheduler().execute(this.plugin, bukkitPlayer.getWorld(), x, z, () ->
                sendLecternData(session, chunk, blockEntityInfos));
        } else {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                Chunk chunk = getChunk(bukkitPlayer.getWorld(), x, z);
                if (chunk == null) {
                    return;
                }
                sendLecternData(session, chunk, blockEntityInfos);
            });
        }
    }

    private @Nullable Chunk getChunk(World world, int x, int z) {
        if (!world.isChunkLoaded(x, z)) {
            return null;
        }
        return world.getChunkAt(x, z);
    }

    private void sendLecternData(GeyserSession session, Chunk chunk, List<BlockEntityInfo> blockEntityInfos) {
        //noinspection ForLoopReplaceableByForEach - avoid constructing Iterator
        for (int i = 0; i < blockEntityInfos.size(); i++) {
            BlockEntityInfo info = blockEntityInfos.get(i);
            Block block = chunk.getBlock(info.getX(), info.getY(), info.getZ());
            sendLecternData(session, block, true);
        }
    }

    private void sendLecternData(GeyserSession session, Block block, boolean isChunkLoad) {
        NbtMap blockEntityTag = this.lecterns.getLecternData(block, isChunkLoad);
        if (blockEntityTag != null) {
            BlockEntityUtils.updateBlockEntity(session, blockEntityTag, BukkitUtils.getVector(block.getLocation()));
        }
    }

    @Override
    public boolean shouldExpectLecternHandled(GeyserSession session) {
        return true;
    }

    public boolean getGameRuleBool(GeyserSession session, GameRule gameRule) {
        org.bukkit.GameRule<?> bukkitGameRule = org.bukkit.GameRule.getByName(gameRule.getJavaID());
        if (bukkitGameRule == null) {
            GeyserImpl.getInstance().getLogger().debug("Unknown game rule " + gameRule.getJavaID());
            return gameRule.getDefaultBooleanValue();
        }

        Player bukkitPlayer = Objects.requireNonNull(Bukkit.getPlayer(session.getPlayerEntity().getUuid()));
        Object value = bukkitPlayer.getWorld().getGameRuleValue(bukkitGameRule);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        GeyserImpl.getInstance().getLogger().debug("Expected a bool for " + gameRule + " but got " + value);
        return gameRule.getDefaultBooleanValue();
    }

    @Override
    public int getGameRuleInt(GeyserSession session, GameRule gameRule) {
        org.bukkit.GameRule<?> bukkitGameRule = org.bukkit.GameRule.getByName(gameRule.getJavaID());
        if (bukkitGameRule == null) {
            GeyserImpl.getInstance().getLogger().debug("Unknown game rule " + gameRule.getJavaID());
            return gameRule.getDefaultIntValue();
        }
        Player bukkitPlayer = Objects.requireNonNull(Bukkit.getPlayer(session.getPlayerEntity().getUuid()));
        Object value = bukkitPlayer.getWorld().getGameRuleValue(bukkitGameRule);
        if (value instanceof Integer intValue) {
            return intValue;
        }
        GeyserImpl.getInstance().getLogger().debug("Expected an int for " + gameRule + " but got " + value);
        return gameRule.getDefaultIntValue();
    }

    @Override
    public GameMode getDefaultGameMode(GeyserSession session) {
        return GameMode.byId(Bukkit.getDefaultGameMode().ordinal());
    }

    @Override
    public boolean hasPermission(GeyserSession session, String permission) {
        Player player = Bukkit.getPlayer(session.javaUuid());
        if (player != null) {
            return player.hasPermission(permission);
        }
        return false;
    }

    @Override
    public @NonNull CompletableFuture<@Nullable CompoundTag> getPickItemNbt(GeyserSession session, int x, int y, int z, boolean addNbtData) {
        CompletableFuture<@Nullable CompoundTag> future = new CompletableFuture<>();
        Player bukkitPlayer;
        if ((bukkitPlayer = Bukkit.getPlayer(session.getPlayerEntity().getUuid())) == null) {
            future.complete(null);
            return future;
        }
        Block block = bukkitPlayer.getWorld().getBlockAt(x, y, z);
        // Paper 1.19.3 complains about async access otherwise.
        // java.lang.IllegalStateException: Tile is null, asynchronous access?
        SchedulerUtils.runTask(this.plugin, () -> future.complete(PickBlockUtils.pickBlock(block)), block);
        return future;
    }

    /**
     * This should be set to true if we are post-1.13 but before the latest version, and we should convert the old block state id
     * to the current one.
     *
     * @return whether there is a difference between client block state and server block state that requires extra processing
     */
    public boolean isLegacy() {
        return false;
    }
}
