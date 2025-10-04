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

package org.geysermc.geyser.platform.mod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.adapters.WorldAdapter;
import org.geysermc.geyser.level.GeyserWorldManager;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.setting.Difficulty;

import java.util.List;
import java.util.function.Consumer;

public class GeyserNativeModWorldManager extends GeyserWorldManager {

    private final WorldAdapter<ServerLevel> adapter;
    private final MinecraftServer server;

    public GeyserNativeModWorldManager(MinecraftServer server) {
        this.server = server;
        this.adapter = (WorldAdapter<ServerLevel>) PlatformAdapters.getWorldAdapter();
    }

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        ServerPlayer player = getPlayer(session);
        if (player == null) {
            return Block.JAVA_AIR_ID;
        }

        return adapter.getBlockAt(player.level(), x, y, z);
    }

    @Override
    public void setDifficulty(GeyserSession session, Difficulty difficulty) {
        net.minecraft.world.Difficulty minecraftDifficulty = net.minecraft.world.Difficulty.byName(difficulty.name().toLowerCase());

        if (minecraftDifficulty != null) {
            server.setDifficulty(minecraftDifficulty, false);
        }
    }

    @Override
    public void setDefaultGameMode(GeyserSession session, GameMode gameMode) {
        server.setDefaultGameType(GameType.byId(gameMode.ordinal()));
    }

    @Override
    public GameMode getDefaultGameMode(GeyserSession session) {
        return GameMode.byId(server.getDefaultGameType().getId());
    }

    @Override
    public String @Nullable [] getBiomeIdentifiers(boolean withTags) {
        return super.getBiomeIdentifiers(withTags);
    }

    // TODO World Adapter this, probably doesn't work in older versions (Hence the try catch)
    @Override
    public void getDecoratedPotData(GeyserSession session, Vector3i pos, Consumer<List<String>> apply) {
        try {
            server.execute(() -> {
                ServerPlayer player = getPlayer(session);
                if (player == null) {
                    return;
                }

                BlockPos blockPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
                // Don't create a new block entity if invalid
                //noinspection resource - level() is just a getter
                BlockEntity blockEntity = player.level().getChunkAt(blockPos).getBlockEntity(blockPos);
                if (blockEntity instanceof DecoratedPotBlockEntity pot) {
                    List<String> sherds = pot.getDecorations().ordered()
                            .stream().map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                            .toList();
                    apply.accept(sherds);
                }
            });
        } catch (Exception e) {
            super.getDecoratedPotData(session, pos, apply);
        }
    }

    private ServerPlayer getPlayer(GeyserSession session) {
        return server.getPlayerList().getPlayer(session.getPlayerEntity().getUuid());
    }
}
