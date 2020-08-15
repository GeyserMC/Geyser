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
 */

package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTileEntityPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.world.block.entity.BlockEntityTranslator;
import org.geysermc.connector.network.translators.world.block.entity.SkullBlockEntityTranslator;
import org.geysermc.connector.utils.BlockEntityUtils;
import org.geysermc.connector.utils.ChunkUtils;

@Translator(packet = ServerUpdateTileEntityPacket.class)
public class JavaUpdateTileEntityTranslator extends PacketTranslator<ServerUpdateTileEntityPacket> {

    @Override
    public void translate(ServerUpdateTileEntityPacket packet, GeyserSession session) {
        String id = BlockEntityUtils.getBedrockBlockEntityId(packet.getType().name());
        if (packet.getNbt().isEmpty()) { // Fixes errors in CubeCraft sending empty NBT
            BlockEntityUtils.updateBlockEntity(session, null, packet.getPosition());
            return;
        }
        BlockEntityTranslator translator = BlockEntityUtils.getBlockEntityTranslator(id);
        // If not null then the BlockState is used in BlockEntityTranslator.translateTag()
        if (ChunkUtils.CACHED_BLOCK_ENTITIES.get(packet.getPosition()) != null) {
            int blockState = ChunkUtils.CACHED_BLOCK_ENTITIES.getOrDefault(packet.getPosition(), 0);
            // Check for custom skulls.
            if (packet.getNbt().contains("SkullOwner") && SkullBlockEntityTranslator.ALLOW_CUSTOM_SKULLS) {
                CompoundTag owner = packet.getNbt().get("SkullOwner");
                if (owner.contains("Properties")) {
                    SkullBlockEntityTranslator.spawnPlayer(session, packet.getNbt(), blockState);
                }
            }
            BlockEntityUtils.updateBlockEntity(session, translator.getBlockEntityTag(id, packet.getNbt(),
                    blockState), packet.getPosition());
            ChunkUtils.CACHED_BLOCK_ENTITIES.remove(packet.getPosition(), blockState);
        } else {
            BlockEntityUtils.updateBlockEntity(session, translator.getBlockEntityTag(id, packet.getNbt(), 0), packet.getPosition());
        }
    }
}
