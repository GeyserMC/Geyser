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

import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.GeyserWorldManager;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;

import java.util.List;
import java.util.function.Consumer;

public class GeyserModWorldManager extends GeyserWorldManager {

    private static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();
    private final MinecraftServer server;

    public GeyserModWorldManager(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        // If the protocol version of Geyser and the server are not the
        // same, fallback to the chunk cache. May be able to update this
        // in the future to use ViaVersion however, like Spigot does.
        if (SharedConstants.getCurrentVersion().getProtocolVersion() != GameProtocol.getJavaProtocolVersion()) {
            return super.getBlockAt(session, x, y, z);
        }

        ServerPlayer player = this.getPlayer(session);
        if (player == null) {
            return 0;
        }

        Level level = player.level();
        if (y < level.getMinY()) {
            return 0;
        }

        // Only loads active chunks, and doesn't delegate to main thread
        ChunkAccess chunk = ((ServerChunkCache) level.getChunkSource()).chunkMap.getChunkToSend(ChunkPos.asLong(x >> 4, z >> 4));
        if (chunk == null) {
            return 0;
        }

        int worldOffset = level.getMinY() >> 4;
        int chunkOffset = (y >> 4) - worldOffset;
        if (chunkOffset < chunk.getSections().length) {
            LevelChunkSection section = chunk.getSections()[chunkOffset];
            if (section != null && !section.hasOnlyAir()) {
                return Block.getId(section.getBlockState(x & 15, y & 15, z & 15));
            }
        }

        return 0;
    }

    @Override
    public boolean hasOwnChunkCache() {
        return SharedConstants.getCurrentVersion().getProtocolVersion() == GameProtocol.getJavaProtocolVersion();
    }

    @Override
    public GameMode getDefaultGameMode(GeyserSession session) {
        return GameMode.byId(server.getDefaultGameType().getId());
    }

    @Override
    public void getDecoratedPotData(GeyserSession session, Vector3i pos, Consumer<List<String>> apply) {
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
    }

    private ServerPlayer getPlayer(GeyserSession session) {
        return server.getPlayerList().getPlayer(session.getPlayerEntity().getUuid());
    }
}
