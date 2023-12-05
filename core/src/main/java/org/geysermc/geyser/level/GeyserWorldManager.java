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

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockEntityInfo;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.erosion.packet.backendbound.*;
import org.geysermc.erosion.util.BlockPositionIterator;
import org.geysermc.erosion.util.LecternUtils;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.BlockEntityUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GeyserWorldManager extends WorldManager {
    private final Object2ObjectMap<String, String> gameruleCache = new Object2ObjectOpenHashMap<>();

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        var erosionHandler = session.getErosionHandler().getAsActive();
        if (erosionHandler == null) {
            return session.getChunkCache().getBlockAt(x, y, z);
        }
        CompletableFuture<Integer> future = new CompletableFuture<>(); // Boxes
        erosionHandler.setPendingLookup(future);
        erosionHandler.sendPacket(new BackendboundBlockRequestPacket(0, Vector3i.from(x, y, z)));
        return future.join();
    }

    @Override
    public CompletableFuture<Integer> getBlockAtAsync(GeyserSession session, int x, int y, int z) {
        var erosionHandler = session.getErosionHandler().getAsActive();
        if (erosionHandler == null) {
            return super.getBlockAtAsync(session, x, y, z);
        }
        CompletableFuture<Integer> future = new CompletableFuture<>(); // Boxes
        int transactionId = erosionHandler.getNextTransactionId();
        erosionHandler.getAsyncPendingLookups().put(transactionId, future);
        erosionHandler.sendPacket(new BackendboundBlockRequestPacket(transactionId, Vector3i.from(x, y, z)));
        return future;
    }

    @Override
    public int[] getBlocksAt(GeyserSession session, BlockPositionIterator iter) {
        var erosionHandler = session.getErosionHandler().getAsActive();
        if (erosionHandler == null) {
            return super.getBlocksAt(session, iter);
        }
        CompletableFuture<int[]> future = new CompletableFuture<>();
        erosionHandler.setPendingBatchLookup(future);
        erosionHandler.sendPacket(new BackendboundBatchBlockRequestPacket(iter));
        return future.join();
    }

    @Override
    public boolean hasOwnChunkCache() {
        // This implementation can only fetch data from the session chunk cache
        return false;
    }

    @Override
    public void sendLecternData(GeyserSession session, int x, int z, List<BlockEntityInfo> blockEntityInfos) {
        var erosionHandler = session.getErosionHandler().getAsActive();
        if (erosionHandler == null) {
            // No-op - don't send any additional information other than what the chunk has already sent
            return;
        }
        List<Vector3i> vectors = new ObjectArrayList<>(blockEntityInfos.size());
        //noinspection ForLoopReplaceableByForEach - avoid constructing iterator
        for (int i = 0; i < blockEntityInfos.size(); i++) {
            BlockEntityInfo info = blockEntityInfos.get(i);
            vectors.add(Vector3i.from(info.getX(), info.getY(), info.getZ()));
        }
        erosionHandler.sendPacket(new BackendboundBatchBlockEntityPacket(x, z, vectors));
    }

    @Override
    public void sendLecternData(GeyserSession session, int x, int y, int z) {
        var erosionHandler = session.getErosionHandler().getAsActive();
        if (erosionHandler != null) {
            erosionHandler.sendPacket(new BackendboundBlockEntityPacket(Vector3i.from(x, y, z)));
            return;
        }

        // Without direct server access, we can't get lectern information on-the-fly.
        // I should have set this up so it's only called when there is a book in the block state. - Camotoy
        NbtMapBuilder lecternTag = LecternUtils.getBaseLecternTag(x, y, z, 1);
        lecternTag.putCompound("book", NbtMap.builder()
                .putByte("Count", (byte) 1)
                .putShort("Damage", (short) 0)
                .putString("Name", "minecraft:written_book")
                .putCompound("tag", NbtMap.builder()
                        .putString("photoname", "")
                        .putString("text", "")
                        .build())
                .build());
        lecternTag.putInt("page", -1); // I'm surprisingly glad this exists - it forces Bedrock to stop reading immediately. Usually.
        BlockEntityUtils.updateBlockEntity(session, lecternTag.build(), Vector3i.from(x, y, z));
    }

    @Override
    public boolean shouldExpectLecternHandled(GeyserSession session) {
        return session.getErosionHandler().isActive();
    }

    @Override
    public void setGameRule(GeyserSession session, String name, Object value) {
        super.setGameRule(session, name, value);
        gameruleCache.put(name, String.valueOf(value));
    }

    @Override
    public boolean getGameRuleBool(GeyserSession session, GameRule gameRule) {
        String value = gameruleCache.get(gameRule.getJavaID());
        if (value != null) {
            return Boolean.parseBoolean(value);
        }

        return gameRule.getDefaultBooleanValue();
    }

    @Override
    public int getGameRuleInt(GeyserSession session, GameRule gameRule) {
        String value = gameruleCache.get(gameRule.getJavaID());
        if (value != null) {
            return Integer.parseInt(value);
        }

        return gameRule.getDefaultIntValue();
    }

    @Override
    public GameMode getDefaultGameMode(GeyserSession session) {
        return GameMode.SURVIVAL;
    }

    @Override
    public boolean hasPermission(GeyserSession session, String permission) {
        return false;
    }

    @NonNull
    @Override
    public CompletableFuture<@Nullable CompoundTag> getPickItemNbt(GeyserSession session, int x, int y, int z, boolean addNbtData) {
        var erosionHandler = session.getErosionHandler().getAsActive();
        if (erosionHandler == null) {
            return super.getPickItemNbt(session, x, y, z, addNbtData);
        }
        CompletableFuture<CompoundTag> future = new CompletableFuture<>();
        erosionHandler.setPickBlockLookup(future);
        erosionHandler.sendPacket(new BackendboundPickBlockPacket(Vector3i.from(x, y, z)));
        return future;
    }
}
