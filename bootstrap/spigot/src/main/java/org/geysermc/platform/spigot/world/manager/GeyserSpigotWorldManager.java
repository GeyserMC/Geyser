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

import com.github.steveice10.mc.protocol.MinecraftConstants;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.translators.LecternInventoryTranslator;
import org.geysermc.connector.network.translators.world.GeyserWorldManager;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.utils.BlockEntityUtils;
import org.geysermc.connector.utils.GameRule;

import java.util.ArrayList;
import java.util.List;

/**
 * The base world manager to use when there is no supported NMS revision
 */
public class GeyserSpigotWorldManager extends GeyserWorldManager {
    /**
     * The current client protocol version for ViaVersion usage.
     */
    protected static final int CLIENT_PROTOCOL_VERSION = MinecraftConstants.PROTOCOL_VERSION;

    private final Plugin plugin;

    public GeyserSpigotWorldManager(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public int getBlockAt(GeyserSession session, int x, int y, int z) {
        Player bukkitPlayer;
        if ((bukkitPlayer = Bukkit.getPlayer(session.getPlayerEntity().getUsername())) == null) {
            return BlockTranslator.JAVA_AIR_ID;
        }
        World world = bukkitPlayer.getWorld();
        return BlockTranslator.getJavaIdBlockMap().getOrDefault(world.getBlockAt(x, y, z).getBlockData().getAsString(), BlockTranslator.JAVA_AIR_ID);
    }

    @Override
    public boolean hasOwnChunkCache() {
        return true;
    }

    @Override
    public NbtMap getLecternDataAt(GeyserSession session, int x, int y, int z, boolean isChunkLoad) {
        // Run as a task to prevent async issues
        Runnable lecternInfoGet = () -> {
            Player bukkitPlayer;
            if ((bukkitPlayer = Bukkit.getPlayer(session.getPlayerEntity().getUsername())) == null) {
                return;
            }

            Block block = bukkitPlayer.getWorld().getBlockAt(x, y, z);
            if (!(block.getState() instanceof Lectern)) {
                session.getConnector().getLogger().error("Lectern expected at: " + Vector3i.from(x, y, z).toString() + " but was not! " + block.toString());
                return;
            }

            Lectern lectern = (Lectern) block.getState();
            ItemStack itemStack = lectern.getInventory().getItem(0);
            if (itemStack == null || !(itemStack.getItemMeta() instanceof BookMeta)) {
                if (!isChunkLoad) {
                    // We need to update the lectern since it's not going to be updated otherwise
                    BlockEntityUtils.updateBlockEntity(session, LecternInventoryTranslator.getBaseLecternTag(x, y, z, 0).build(), Vector3i.from(x, y, z));
                }
                // We don't care; return
                return;
            }

            BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();
            // On the count: allow the book to show/open even there are no pages. We know there is a book here, after all, and this matches Java behavior
            boolean hasBookPages = bookMeta.getPageCount() > 0;
            NbtMapBuilder lecternTag = LecternInventoryTranslator.getBaseLecternTag(x, y, z, hasBookPages ? bookMeta.getPageCount() : 1);
            lecternTag.putInt("page", lectern.getPage() / 2);
            NbtMapBuilder bookTag = NbtMap.builder()
                    .putByte("Count", (byte) itemStack.getAmount())
                    .putShort("Damage", (short) 0)
                    .putString("Name", "minecraft:writable_book");
            List<NbtMap> pages = new ArrayList<>(bookMeta.getPageCount());
            if (hasBookPages) {
                for (String page : bookMeta.getPages()) {
                    NbtMapBuilder pageBuilder = NbtMap.builder()
                            .putString("photoname", "")
                            .putString("text", page);
                    pages.add(pageBuilder.build());
                }
            } else {
                // Empty page
                NbtMapBuilder pageBuilder = NbtMap.builder()
                        .putString("photoname", "")
                        .putString("text", "");
                pages.add(pageBuilder.build());
            }
            
            bookTag.putCompound("tag", NbtMap.builder().putList("pages", NbtType.COMPOUND, pages).build());
            lecternTag.putCompound("book", bookTag.build());
            NbtMap blockEntityTag = lecternTag.build();
            BlockEntityUtils.updateBlockEntity(session, blockEntityTag, Vector3i.from(x, y, z));
        };

        if (isChunkLoad) {
            // Delay to ensure the chunk is sent first, and then the lectern data
            Bukkit.getScheduler().runTaskLater(this.plugin, lecternInfoGet, 5);
        } else {
            Bukkit.getScheduler().runTask(this.plugin, lecternInfoGet);
        }
        return LecternInventoryTranslator.getBaseLecternTag(x, y, z, 0).build(); // Will be updated later
    }

    @Override
    public boolean shouldExpectLecternHandled() {
        return true;
    }

    public Boolean getGameRuleBool(GeyserSession session, GameRule gameRule) {
        return Boolean.parseBoolean(Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getGameRuleValue(gameRule.getJavaID()));
    }

    @Override
    public int getGameRuleInt(GeyserSession session, GameRule gameRule) {
        return Integer.parseInt(Bukkit.getPlayer(session.getPlayerEntity().getUsername()).getWorld().getGameRuleValue(gameRule.getJavaID()));
    }

    @Override
    public boolean hasPermission(GeyserSession session, String permission) {
        return Bukkit.getPlayer(session.getPlayerEntity().getUsername()).hasPermission(permission);
    }

    /**
     * This must be set to true if we are pre-1.13, and {@link BlockData#getAsString() does not exist}.
     *
     * This should be set to true if we are post-1.13 but before the latest version, and we should convert the old block state id
     * to the current one.
     *
     * @return whether there is a difference between client block state and server block state that requires extra processing
     */
    public boolean isLegacy() {
        return false;
    }
}
