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
 *
 */

package org.geysermc.platform.bukkit.world;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.WorldManager;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;

public class GeyserBukkitWorldManager extends WorldManager {

    @Override
    public BlockState getBlockAt(GeyserSession session, int x, int y, int z) {
        if (session.getPlayerEntity() == null) {
            return BlockTranslator.AIR;
        }
        return BlockTranslator.getJavaIdBlockMap().get(Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getBlockAt(x, y, z).getBlockData().getAsString());
    }

    @Override
    public void setGameRule(GeyserSession session, String name, Object value) {
        World world = Bukkit.getWorld("world");
        world.setGameRuleValue(name, (String) value);
    }

    @Override
    public void setPlayerGameMode(GeyserSession session, GameMode gameMode) {
        Bukkit.getPlayer(session.getPlayerEntity().getUsername()).setGameMode(org.bukkit.GameMode.valueOf(gameMode.name()));
    }
}
