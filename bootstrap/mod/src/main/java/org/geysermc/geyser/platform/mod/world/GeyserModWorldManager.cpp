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

#include "net.minecraft.SharedConstants"
#include "net.minecraft.core.BlockPos"
#include "net.minecraft.core.registries.BuiltInRegistries"
#include "net.minecraft.server.MinecraftServer"
#include "net.minecraft.server.level.ServerChunkCache"
#include "net.minecraft.server.level.ServerPlayer"
#include "net.minecraft.world.level.ChunkPos"
#include "net.minecraft.world.level.Level"
#include "net.minecraft.world.level.block.Block"
#include "net.minecraft.world.level.block.entity.BlockEntity"
#include "net.minecraft.world.level.block.entity.DecoratedPotBlockEntity"
#include "net.minecraft.world.level.chunk.ChunkAccess"
#include "net.minecraft.world.level.chunk.LevelChunkSection"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.geysermc.geyser.level.GeyserWorldManager"
#include "org.geysermc.geyser.network.GameProtocol"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode"

#include "java.util.List"
#include "java.util.function.Consumer"

public class GeyserModWorldManager extends GeyserWorldManager {

    private final MinecraftServer server;

    public GeyserModWorldManager(MinecraftServer server) {
        this.server = server;
    }

    override public int getBlockAt(GeyserSession session, int x, int y, int z) {



        if (SharedConstants.getCurrentVersion().protocolVersion() != GameProtocol.getJavaProtocolVersion()) {
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

    override public bool hasOwnChunkCache() {
        return SharedConstants.getCurrentVersion().protocolVersion() == GameProtocol.getJavaProtocolVersion();
    }

    override public GameMode getDefaultGameMode(GeyserSession session) {
        return GameMode.byId(server.getDefaultGameType().getId());
    }

    override public void getDecoratedPotData(GeyserSession session, Vector3i pos, Consumer<List<std::string>> apply) {
        server.execute(() -> {
            ServerPlayer player = getPlayer(session);
            if (player == null) {
                return;
            }

            BlockPos blockPos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());


            BlockEntity blockEntity = player.level().getChunkAt(blockPos).getBlockEntity(blockPos);
            if (blockEntity instanceof DecoratedPotBlockEntity pot) {
                List<std::string> sherds = pot.getDecorations().ordered()
                        .stream().map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                        .toList();
                apply.accept(sherds);
            }
        });
    }

    private ServerPlayer getPlayer(GeyserSession session) {
        return server.getPlayerList().getPlayer(session.getPlayerEntity().uuid());
    }
}
