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

package org.geysermc.geyser.platform.spigot.world;

import com.github.steveice10.mc.protocol.data.game.level.block.value.PistonValueType;
import com.nukkitx.math.vector.Vector3i;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.PistonCache;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.translator.level.block.entity.PistonBlockEntity;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.platform.spigot.world.manager.GeyserSpigotWorldManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GeyserPistonListener implements Listener {
    private final GeyserImpl geyser;
    private final GeyserSpigotWorldManager worldManager;

    public GeyserPistonListener(GeyserImpl geyser, GeyserSpigotWorldManager worldManager) {
        this.geyser = geyser;
        this.worldManager = worldManager;
    }

    // The handlers' parent class cannot be registered
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        onPistonAction(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        onPistonAction(event);
    }

    private void onPistonAction(BlockPistonEvent event) {
        if (event.isCancelled()) {
            return;
        }

        World world = event.getBlock().getWorld();
        boolean isExtend = event instanceof BlockPistonExtendEvent;

        Location location = event.getBlock().getLocation();
        Vector3i position = getVector(location);
        PistonValueType type = isExtend ? PistonValueType.PUSHING : PistonValueType.PULLING;
        boolean sticky = event.isSticky();

        Object2IntMap<Vector3i> attachedBlocks = new Object2IntOpenHashMap<>();
        boolean blocksFilled = false;

        for (Map.Entry<UUID, GeyserSession> entry : geyser.getSessionManager().getSessions().entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.getWorld().equals(world)) {
                continue;
            }
            GeyserSession session = entry.getValue();

            int dX = Math.abs(location.getBlockX() - player.getLocation().getBlockX()) >> 4;
            int dZ = Math.abs(location.getBlockZ() - player.getLocation().getBlockZ()) >> 4;
            if ((dX * dX + dZ * dZ) > session.getRenderDistance() * session.getRenderDistance()) {
                // Ignore pistons outside the player's render distance
                continue;
            }

            // Trying to grab the blocks from the world like other platforms would result in the moving piston block
            // being returned instead.
            if (!blocksFilled) {
                // Blocks currently require a player for 1.12, so let's just leech off one player to get all blocks
                // and call it a day for the rest of the sessions (mostly to save on execution time)
                List<Block> blocks = isExtend ? ((BlockPistonExtendEvent) event).getBlocks() : ((BlockPistonRetractEvent) event).getBlocks();
                for (Block block : blocks) {
                    Location attachedLocation = block.getLocation();
                    int blockId = worldManager.getBlockNetworkId(player, block,
                            attachedLocation.getBlockX(), attachedLocation.getBlockY(), attachedLocation.getBlockZ());
                    // Ignore blocks that will be destroyed
                    if (BlockStateValues.canPistonMoveBlock(blockId, isExtend)) {
                        attachedBlocks.put(getVector(attachedLocation), blockId);
                    }
                }
                blocksFilled = true;
            }

            int pistonBlockId = worldManager.getBlockNetworkId(player, event.getBlock(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
            // event.getDirection() is unreliable
            Direction orientation = BlockStateValues.getPistonOrientation(pistonBlockId);

            session.executeInEventLoop(() -> {
                PistonCache pistonCache = session.getPistonCache();
                PistonBlockEntity blockEntity = pistonCache.getPistons().computeIfAbsent(position, pos ->
                        new PistonBlockEntity(session, position, orientation, sticky, !isExtend));
                blockEntity.setAction(type, attachedBlocks);
            });
        }
    }

    private Vector3i getVector(Location location) {
        return Vector3i.from(location.getX(), location.getY(), location.getZ());
    }
}
