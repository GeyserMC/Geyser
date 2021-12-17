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

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEventPacket;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.platform.spigot.world.manager.GeyserSpigotWorldManager;

@AllArgsConstructor
public class GeyserSpigotBlockPlaceListener implements Listener {
    private final GeyserImpl geyser;
    private final GeyserSpigotWorldManager worldManager;

    @EventHandler
    public void place(final BlockPlaceEvent event) {
        GeyserSession session = geyser.connectionByUuid(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }

        LevelSoundEventPacket placeBlockSoundPacket = new LevelSoundEventPacket();
        placeBlockSoundPacket.setSound(SoundEvent.PLACE);
        placeBlockSoundPacket.setPosition(Vector3f.from(event.getBlockPlaced().getX(), event.getBlockPlaced().getY(), event.getBlockPlaced().getZ()));
        placeBlockSoundPacket.setBabySound(false);
        if (worldManager.isLegacy()) {
            placeBlockSoundPacket.setExtraData(session.getBlockMappings().getBedrockBlockId(worldManager.getBlockAt(session,
                    event.getBlockPlaced().getX(), event.getBlockPlaced().getY(), event.getBlockPlaced().getZ())));
        } else {
            String javaBlockId = event.getBlockPlaced().getBlockData().getAsString();
            placeBlockSoundPacket.setExtraData(session.getBlockMappings().getBedrockBlockId(BlockRegistries.JAVA_IDENTIFIERS.get().getOrDefault(javaBlockId, BlockStateValues.JAVA_AIR_ID)));
        }
        placeBlockSoundPacket.setIdentifier(":");
        session.sendUpstreamPacket(placeBlockSoundPacket);
        session.setLastBlockPlacePosition(null);
        session.setLastBlockPlacedId(null);
    }
}
