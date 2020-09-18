/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.world;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.chunk.ChunkPosition;
import org.geysermc.connector.utils.GameRule;

public class GeyserWorldManager extends WorldManager {

    private static final Object2ObjectMap<String, String> gameruleCache = new Object2ObjectOpenHashMap<>();

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        return session.getChunkCache().getBlockAt(new Position(x, y, z));
    }

    @Override
    public int[] getBiomeDataAt(GeyserSession session, int x, int z) {
        if (!session.getConnector().getConfig().isCacheChunks())
            return new int[1024];
        return session.getChunkCache().getChunks().get(new ChunkPosition(x, z)).getBiomeData();
    }

    @Override
    public void setGameRule(GeyserSession session, String name, Object value) {
        session.sendDownstreamPacket(new ClientChatPacket("/gamerule " + name + " " + value));
        gameruleCache.put(name, String.valueOf(value));
    }

    @Override
    public Boolean getGameRuleBool(GeyserSession session, GameRule gameRule) {
        String value = gameruleCache.get(gameRule.getJavaID());
        if (value != null) {
            return Boolean.parseBoolean(value);
        }

        return gameRule.getDefaultValue() != null ? (Boolean) gameRule.getDefaultValue() : false;
    }

    @Override
    public int getGameRuleInt(GeyserSession session, GameRule gameRule) {
        String value = gameruleCache.get(gameRule.getJavaID());
        if (value != null) {
            return Integer.parseInt(value);
        }

        return gameRule.getDefaultValue() != null ? (int) gameRule.getDefaultValue() : 0;
    }

    @Override
    public void setPlayerGameMode(GeyserSession session, GameMode gameMode) {
        session.sendDownstreamPacket(new ClientChatPacket("/gamemode " + gameMode.name().toLowerCase()));
    }

    @Override
    public void setDifficulty(GeyserSession session, Difficulty difficulty) {
        session.sendDownstreamPacket(new ClientChatPacket("/difficulty " + difficulty.name().toLowerCase()));
    }

    @Override
    public boolean hasPermission(GeyserSession session, String permission) {
        return false;
    }
}
