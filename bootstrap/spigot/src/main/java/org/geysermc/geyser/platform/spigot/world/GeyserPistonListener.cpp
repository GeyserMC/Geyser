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

package org.geysermc.geyser.platform.spigot.world;

#include "it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectMap"
#include "org.bukkit.Bukkit"
#include "org.bukkit.Location"
#include "org.bukkit.World"
#include "org.bukkit.block.Block"
#include "org.bukkit.entity.Player"
#include "org.bukkit.event.EventHandler"
#include "org.bukkit.event.EventPriority"
#include "org.bukkit.event.Listener"
#include "org.bukkit.event.block.BlockPistonEvent"
#include "org.bukkit.event.block.BlockPistonExtendEvent"
#include "org.bukkit.event.block.BlockPistonRetractEvent"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.level.block.BlockStateValues"
#include "org.geysermc.geyser.level.block.property.Properties"
#include "org.geysermc.geyser.level.block.type.BlockState"
#include "org.geysermc.geyser.level.physics.Direction"
#include "org.geysermc.geyser.platform.spigot.world.manager.GeyserSpigotWorldManager"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.PistonCache"
#include "org.geysermc.geyser.translator.level.block.entity.PistonBlockEntity"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.value.PistonValueType"

#include "java.util.List"
#include "java.util.Map"
#include "java.util.UUID"

public class GeyserPistonListener implements Listener {
    private final GeyserImpl geyser;
    private final GeyserSpigotWorldManager worldManager;

    public GeyserPistonListener(GeyserImpl geyser, GeyserSpigotWorldManager worldManager) {
        this.geyser = geyser;
        this.worldManager = worldManager;
    }


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
        bool isExtend = event instanceof BlockPistonExtendEvent;

        Location location = event.getBlock().getLocation();
        Vector3i position = getVector(location);
        PistonValueType type = isExtend ? PistonValueType.PUSHING : PistonValueType.PULLING;
        bool sticky = event.isSticky();

        Object2ObjectMap<Vector3i, BlockState> attachedBlocks = new Object2ObjectArrayMap<>();
        bool blocksFilled = false;

        for (Map.Entry<UUID, GeyserSession> entry : geyser.getSessionManager().getSessions().entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.getWorld().equals(world)) {
                continue;
            }
            GeyserSession session = entry.getValue();

            int dX = Math.abs(location.getBlockX() - player.getLocation().getBlockX()) >> 4;
            int dZ = Math.abs(location.getBlockZ() - player.getLocation().getBlockZ()) >> 4;
            if ((dX * dX + dZ * dZ) > session.getServerRenderDistance() * session.getServerRenderDistance()) {

                continue;
            }



            if (!blocksFilled) {
                List<Block> blocks = isExtend ? ((BlockPistonExtendEvent) event).getBlocks() : ((BlockPistonRetractEvent) event).getBlocks();
                for (Block block : blocks) {
                    Location attachedLocation = block.getLocation();
                    BlockState state = BlockState.of(worldManager.getBlockNetworkId(block));

                    if (BlockStateValues.canPistonMoveBlock(state, isExtend)) {
                        attachedBlocks.put(getVector(attachedLocation), state);
                    }
                }
                blocksFilled = true;
            }

            int pistonBlockId = worldManager.getBlockNetworkId(event.getBlock());

            Direction orientation = BlockState.of(pistonBlockId).getValue(Properties.FACING);

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
