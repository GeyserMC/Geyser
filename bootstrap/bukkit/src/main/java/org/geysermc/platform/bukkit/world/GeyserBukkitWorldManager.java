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

import com.github.steveice10.mc.protocol.data.game.world.block.BlockState;

import org.bukkit.Bukkit;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.WorldManager;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.platform.bukkit.GeyserBukkitLogger;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.BlockIdData;

public class GeyserBukkitWorldManager extends WorldManager {

    private final GeyserBukkitLogger logger;

    public GeyserBukkitWorldManager(GeyserBukkitLogger logger) {
        this.logger = logger;
    }

    @Override
    public BlockState getBlockAt(GeyserSession session, int x, int y, int z) {
        if (session.getPlayerEntity() == null) {
            return BlockTranslator.AIR;
        }
        try {
            return BlockTranslator.getJavaIdBlockMap().get(Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getBlockAt(x, y, z).getBlockData().getAsString());
        } catch (NoSuchMethodError ex) {
            if (BlockIdData.blockIdMapping != null) {
                String name = BlockIdData.numberIdToString.get(Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getBlockAt(x, y, z).getType().getId());
                if (BlockIdData.blockIdMapping.containsKey(name)) {
                    String[] blockStates = BlockIdData.blockIdMapping.get(name);
                    if (blockStates.length == 1) {
                        name = blockStates[0];
                    } else {
                        for (int i = 0; i < blockStates.length; i++) {
                            System.out.println("Data mapping: " + blockStates[i]);
                        }
                        int data = Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getBlockAt(x, y, z).getData();
                        if (data <= blockStates.length) {
                            name = blockStates[data];
                        }
                    }
                }
                BlockState blockState = BlockTranslator.getJavaIdBlockMap().get("minecraft:" + name);
                if (blockState == null) {
                    logger.debug("Block State was null while trying to get block " + name + " for player " + session.getPlayerEntity().getUsername());
                    return BlockTranslator.AIR;
                }
                return blockState;
            }
            return BlockTranslator.AIR;
        }
    }
}
