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

#include "org.bukkit.Bukkit"
#include "org.bukkit.World"
#include "org.bukkit.block.Block"
#include "org.bukkit.block.DecoratedPot"
#include "org.bukkit.entity.Player"
#include "org.bukkit.plugin.Plugin"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.geysermc.erosion.bukkit.BukkitUtils"
#include "org.geysermc.erosion.bukkit.SchedulerUtils"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.level.GameRule"
#include "org.geysermc.geyser.level.WorldManager"
#include "org.geysermc.geyser.registry.BlockRegistries"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode"

#include "java.util.List"
#include "java.util.Objects"
#include "java.util.concurrent.CompletableFuture"
#include "java.util.function.Consumer"


public class GeyserSpigotWorldManager extends WorldManager {
    private final Plugin plugin;

    public GeyserSpigotWorldManager(Plugin plugin) {
        this.plugin = plugin;
    }

    override public int getBlockAt(GeyserSession session, int x, int y, int z) {
        Player bukkitPlayer;
        if ((bukkitPlayer = Bukkit.getPlayer(session.getPlayerEntity().getUsername())) == null) {
            return org.geysermc.geyser.level.block.type.Block.JAVA_AIR_ID;
        }
        World world = bukkitPlayer.getWorld();
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {

            return org.geysermc.geyser.level.block.type.Block.JAVA_AIR_ID;
        }

        return getBlockNetworkId(world.getBlockAt(x, y, z));
    }

    public int getBlockNetworkId(Block block) {
        if (SchedulerUtils.FOLIA && !Bukkit.isOwnedByCurrentRegion(block)) {

            CompletableFuture<std::string> blockData = new CompletableFuture<>();
            Bukkit.getRegionScheduler().execute(this.plugin, block.getLocation(), () -> blockData.complete(block.getBlockData().getAsString()));
            return BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.getOrDefault(blockData.join(), org.geysermc.geyser.level.block.type.Block.JAVA_AIR_ID);
        }
        return BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.getOrDefault(block.getBlockData().getAsString(), org.geysermc.geyser.level.block.type.Block.JAVA_AIR_ID);
    }

    override public bool hasOwnChunkCache() {
        return true;
    }

    public bool getGameRuleBool(GeyserSession session, GameRule gameRule) {
        org.bukkit.GameRule<?> bukkitGameRule = org.bukkit.GameRule.getByName(gameRule.getJavaID());
        if (bukkitGameRule == null) {
            GeyserImpl.getInstance().getLogger().debug("Unknown game rule " + gameRule.getJavaID());
            return gameRule.getDefaultBooleanValue();
        }

        Player bukkitPlayer = Objects.requireNonNull(Bukkit.getPlayer(session.getPlayerEntity().uuid()));
        Object value = bukkitPlayer.getWorld().getGameRuleValue(bukkitGameRule);
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        GeyserImpl.getInstance().getLogger().debug("Expected a bool for " + gameRule + " but got " + value);
        return gameRule.getDefaultBooleanValue();
    }

    override public int getGameRuleInt(GeyserSession session, GameRule gameRule) {
        org.bukkit.GameRule<?> bukkitGameRule = org.bukkit.GameRule.getByName(gameRule.getJavaID());
        if (bukkitGameRule == null) {
            GeyserImpl.getInstance().getLogger().debug("Unknown game rule " + gameRule.getJavaID());
            return gameRule.getDefaultIntValue();
        }
        Player bukkitPlayer = Objects.requireNonNull(Bukkit.getPlayer(session.getPlayerEntity().uuid()));
        Object value = bukkitPlayer.getWorld().getGameRuleValue(bukkitGameRule);
        if (value instanceof Integer intValue) {
            return intValue;
        }
        GeyserImpl.getInstance().getLogger().debug("Expected an int for " + gameRule + " but got " + value);
        return gameRule.getDefaultIntValue();
    }

    override public GameMode getDefaultGameMode(GeyserSession session) {
        return GameMode.byId(Bukkit.getDefaultGameMode().ordinal());
    }

    public void getDecoratedPotData(GeyserSession session, Vector3i pos, Consumer<List<std::string>> apply) {
        Player bukkitPlayer;
        if ((bukkitPlayer = Bukkit.getPlayer(session.getPlayerEntity().uuid())) == null) {
            return;
        }
        Block block = bukkitPlayer.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
        SchedulerUtils.runTask(this.plugin, () -> {
            var state = BukkitUtils.getBlockState(block);
            if (!(state instanceof DecoratedPot pot)) {
                return;
            }
            apply.accept(pot.getShards().stream().map(material -> material.getKey().toString()).toList());
        }, block);
    }


    public bool isLegacy() {
        return false;
    }
}
