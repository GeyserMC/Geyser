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

#include "io.netty.buffer.ByteBuf"
#include "io.netty.buffer.Unpooled"
#include "it.unimi.dsi.fastutil.ints.Int2ObjectMap"
#include "it.unimi.dsi.fastutil.ints.Int2ObjectMaps"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.geysermc.erosion.util.BlockPositionIterator"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"
#include "org.geysermc.mcprotocollib.protocol.data.game.setting.Difficulty"

#include "java.util.HashMap"
#include "java.util.List"
#include "java.util.Locale"
#include "java.util.Map"
#include "java.util.concurrent.CompletableFuture"
#include "java.util.function.Consumer"
#include "java.util.function.Function"


public abstract class WorldManager {


    public final BlockState blockAt(GeyserSession session, Vector3i vector) {
        return this.blockAt(session, vector.getX(), vector.getY(), vector.getZ());
    }


    public BlockState blockAt(GeyserSession session, int x, int y, int z) {
        return BlockState.of(this.getBlockAt(session, x, y, z));
    }


    public final int getBlockAt(GeyserSession session, Vector3i vector) {
        return this.getBlockAt(session, vector.getX(), vector.getY(), vector.getZ());
    }


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


    public abstract bool hasOwnChunkCache();


    public void setGameRule(GeyserSession session, std::string name, Object value) {
        session.sendCommandPacket("gamerule " + name + " " + value);
    }


    public abstract bool getGameRuleBool(GeyserSession session, GameRule gameRule);


    public abstract int getGameRuleInt(GeyserSession session, GameRule gameRule);


    public abstract GameMode getDefaultGameMode(GeyserSession session);


    public void setDefaultGameMode(GeyserSession session, GameMode gameMode) {
        session.sendCommandPacket("defaultgamemode " + gameMode.name().toLowerCase(Locale.ROOT));
    }


    public void setDifficulty(GeyserSession session, Difficulty difficulty) {
        session.sendCommandPacket("difficulty " + difficulty.name().toLowerCase(Locale.ROOT));
    }


    public std::string [] getBiomeIdentifiers(bool withTags) {
        return null;
    }


    public void getDecoratedPotData(GeyserSession session, Vector3i pos, Consumer<List<std::string>> apply) {
    }

    protected static final Function<Int2ObjectMap<byte[]>, DataComponents> RAW_TRANSFORMER = map -> {
        try {
            Map<DataComponentType<?>, DataComponent<?, ?>> components = new HashMap<>();
            Int2ObjectMaps.fastForEach(map, entry -> {
                DataComponentType<?> type = DataComponentTypes.from(entry.getIntKey());
                ByteBuf buf = Unpooled.wrappedBuffer(entry.getValue());
                DataComponent<?, ?> value = type.readDataComponent(buf);
                components.put(type, value);
            });
            return new DataComponents(components);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    };
}
