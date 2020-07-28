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

package org.geysermc.platform.spigot.world;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.setting.Difficulty;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.GeyserWorldManager;
import org.geysermc.connector.network.translators.world.WorldManager;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.utils.Gamerule;
import us.myles.ViaVersion.protocols.protocol1_13_1to1_13.Protocol1_13_1To1_13;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.data.MappingData;

@AllArgsConstructor
public class GeyserSpigotWorldManager extends GeyserWorldManager {

    private final boolean isLegacy;
    // You need ViaVersion to connect to an older server with Geyser.
    // However, we still check for ViaVersion in case there's some other way that gets Geyser on a pre-1.13 Bukkit server
    private final boolean isViaVersion;

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        if (session.getPlayerEntity() == null) {
            return BlockTranslator.AIR;
        }
        if (isLegacy) {
            return getLegacyBlock(session, x, y, z, isViaVersion);
        }
        return BlockTranslator.getJavaIdBlockMap().get(Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getBlockAt(x, y, z).getBlockData().getAsString());
    }

    @SuppressWarnings("deprecation")
    public static int getLegacyBlock(GeyserSession session, int x, int y, int z, boolean isViaVersion) {
        if (isViaVersion) {
            Block block = Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getBlockAt(x, y, z);
            // Black magic that gets the old block state ID
            int oldBlockId = (block.getType().getId() << 4) | (block.getData() & 0xF);
            // Convert block state from old version -> 1.13 -> 1.13.1 -> 1.14 -> 1.15 -> 1.16
            int thirteenBlockId = us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.MappingData.blockMappings.getNewId(oldBlockId);
            int thirteenPointOneBlockId = Protocol1_13_1To1_13.getNewBlockStateId(thirteenBlockId);
            int fourteenBlockId = us.myles.ViaVersion.protocols.protocol1_14to1_13_2.data.MappingData.blockStateMappings.getNewId(thirteenPointOneBlockId);
            int fifteenBlockId = us.myles.ViaVersion.protocols.protocol1_15to1_14_4.data.MappingData.blockStateMappings.getNewId(fourteenBlockId);
            return MappingData.blockStateMappings.getNewId(fifteenBlockId);
        } else {
            return BlockTranslator.AIR;
        }
    }

    @Override
    public GameMode getDefaultGameMode(GeyserSession session) {
        return GameMode.valueOf(Bukkit.getDefaultGameMode().name());
    }

    @Override
    public Boolean getGameRuleBool(GeyserSession session, Gamerule gamerule) {
        return Boolean.parseBoolean(Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getGameRuleValue(gamerule.getJavaID()));
    }

    @Override
    public int getGameRuleInt(GeyserSession session, Gamerule gamerule) {
        return Integer.parseInt(Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getGameRuleValue(gamerule.getJavaID()));
    }

    @Override
    public boolean hasPermission(GeyserSession session, String permission) {
        return Bukkit.getPlayer(session.getPlayerEntity().getUsername()).hasPermission(permission);
    }
}
