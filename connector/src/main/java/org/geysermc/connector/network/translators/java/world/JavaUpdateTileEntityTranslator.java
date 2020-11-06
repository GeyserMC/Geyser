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

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.world.block.UpdatedTileType;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerUpdateTileEntityPacket;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.world.block.entity.BlockEntityTranslator;
import org.geysermc.connector.utils.BlockEntityUtils;
import org.geysermc.connector.utils.ChunkUtils;

@Translator(packet = ServerUpdateTileEntityPacket.class)
public class JavaUpdateTileEntityTranslator extends PacketTranslator<ServerUpdateTileEntityPacket> {
    private final boolean cacheChunks;

    public JavaUpdateTileEntityTranslator() {
        cacheChunks = GeyserConnector.getInstance().getConfig().isCacheChunks();
    }

    @Override
    public void translate(ServerUpdateTileEntityPacket packet, GeyserSession session) {
        String id = BlockEntityUtils.getBedrockBlockEntityId(packet.getType().name());
        if (packet.getNbt().isEmpty()) { // Fixes errors in CubeCraft sending empty NBT
            BlockEntityUtils.updateBlockEntity(session, null, packet.getPosition());
            return;
        }

        BlockEntityTranslator translator = BlockEntityUtils.getBlockEntityTranslator(id);
        // The Java block state is used in BlockEntityTranslator.translateTag() to make up for some inconsistencies
        // between Java block states and Bedrock block entity data
        int blockState = cacheChunks ?
                // Cache chunks is enabled; use chunk cache
                session.getConnector().getWorldManager().getBlockAt(session, packet.getPosition()) :
                // Cache chunks is not enabled; use block entity cache
                ChunkUtils.CACHED_BLOCK_ENTITIES.removeInt(packet.getPosition());
        BlockEntityUtils.updateBlockEntity(session, translator.getBlockEntityTag(id, packet.getNbt(), blockState), packet.getPosition());

        // If block entity is command block, OP permission level is appropriate, player is in creative mode and the NBT is not empty
        if (packet.getType() == UpdatedTileType.COMMAND_BLOCK && session.getOpPermissionLevel() >= 2 &&
                session.getGameMode() == GameMode.CREATIVE && packet.getNbt().size() > 5) {
            ContainerOpenPacket openPacket = new ContainerOpenPacket();
            openPacket.setBlockPosition(Vector3i.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
            openPacket.setId((byte) 1);
            openPacket.setType(ContainerType.COMMAND_BLOCK);
            openPacket.setUniqueEntityId(-1);
            session.sendUpstreamPacket(openPacket);
        }
    }
}
