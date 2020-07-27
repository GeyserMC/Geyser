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
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import org.geysermc.connector.network.session.GeyserSession;

public class GeyserWorldManager extends WorldManager {

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        return session.getChunkCache().getBlockAt(new Position(x, y, z));
    }

    @Override
    public void setGameRule(GeyserSession session, String name, Object value) {
        session.getDownstream().getSession().send(new ClientChatPacket("/gamerule " + name + " " + value));
    }

    @Override
    public void setPlayerGameMode(GeyserSession session, GameMode gameMode) {
        session.getDownstream().getSession().send(new ClientChatPacket("/gamemode " + gameMode.name().toLowerCase()));
    }

    @Override
    public GameMode getDefaultGameMode(GeyserSession session) {
        return GameMode.SURVIVAL;
    }

    @Override
    public void setDefaultGameMode(GeyserSession session, GameMode gameMode) {
        session.getDownstream().getSession().send(new ClientChatPacket("/defaultgamemode " + gameMode.name().toLowerCase()));
    }

    @Override
    public void setDifficulty(GeyserSession session, Difficulty difficulty) {
        session.getDownstream().getSession().send(new ClientChatPacket("/difficulty " + difficulty.name().toLowerCase()));
    }
}
