/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.platform.spigot.world.manager;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.storage.BlockStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.geyser.adapters.spigot.SpigotAdapters;
import org.geysermc.geyser.adapters.spigot.SpigotWorldAdapter;

/**
 * Used with ViaVersion and pre-1.13.
 */
public class GeyserSpigot1_12NativeWorldManager extends GeyserSpigot1_12WorldManager {
    private final SpigotWorldAdapter adapter;

    public GeyserSpigot1_12NativeWorldManager(Plugin plugin) {
        super(plugin);
        this.adapter = SpigotAdapters.getWorldAdapter();
        // Unlike post-1.13, we can't build up a cache of block states, because block entities need some special conversion
    }

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        Player player = Bukkit.getPlayer(session.getPlayerEntity().getUsername());
        if (player == null) {
            return BlockTranslator.JAVA_AIR_ID;
        }
        // Get block entity storage
        BlockStorage storage = Via.getManager().getConnectionManager().getConnectedClient(player.getUniqueId()).get(BlockStorage.class);
        int blockId = adapter.getBlockAt(player.getWorld(), x, y, z);
        return getLegacyBlock(storage, blockId, x, y, z);
    }
}
