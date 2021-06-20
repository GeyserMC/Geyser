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

package org.geysermc.platform.spigot.world;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEventPacket;
import lombok.AllArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.platform.spigot.world.manager.GeyserSpigotWorldManager;

@AllArgsConstructor
public class GeyserSpigotBlockPlaceListener implements Listener {

    private final GeyserConnector connector;
    private final GeyserSpigotWorldManager worldManager;

    @EventHandler
    public void place(final BlockPlaceEvent event) {
        for (GeyserSession session : connector.getPlayers()) {
            if (event.getPlayer() == Bukkit.getPlayer(session.getPlayerEntity().getUsername())) {
                LevelSoundEventPacket placeBlockSoundPacket = new LevelSoundEventPacket();
                placeBlockSoundPacket.setSound(SoundEvent.PLACE);
                placeBlockSoundPacket.setPosition(Vector3f.from(event.getBlockPlaced().getX(), event.getBlockPlaced().getY(), event.getBlockPlaced().getZ()));
                placeBlockSoundPacket.setBabySound(false);
                if (worldManager.isLegacy()) {
                    placeBlockSoundPacket.setExtraData(session.getBlockTranslator().getBedrockBlockId(worldManager.getBlockAt(session,
                            event.getBlockPlaced().getX(), event.getBlockPlaced().getY(), event.getBlockPlaced().getZ())));
                } else {
                    String javaBlockId = event.getBlockPlaced().getBlockData().getAsString();
                    placeBlockSoundPacket.setExtraData(session.getBlockTranslator().getBedrockBlockId(BlockTranslator.getJavaIdBlockMap().getOrDefault(javaBlockId, BlockTranslator.JAVA_AIR_ID)));
                }
                placeBlockSoundPacket.setIdentifier(":");
                session.sendUpstreamPacket(placeBlockSoundPacket);
                session.setLastBlockPlacePosition(null);
                session.setLastBlockPlacedId(null);
                break;
            }
        }
    }

}
