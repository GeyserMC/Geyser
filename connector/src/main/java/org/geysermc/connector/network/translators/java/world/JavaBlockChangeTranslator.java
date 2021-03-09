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

package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEventPacket;
import org.geysermc.common.PlatformType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.sound.BlockSoundInteractionHandler;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.utils.ChunkUtils;

@Translator(packet = ServerBlockChangePacket.class)
public class JavaBlockChangeTranslator extends PacketTranslator<ServerBlockChangePacket> {

    @Override
    public void translate(ServerBlockChangePacket packet, GeyserSession session) {
        Position pos = packet.getRecord().getPosition();
        boolean updatePlacement = session.getConnector().getPlatformType() != PlatformType.SPIGOT && // Spigot simply listens for the block place event
                !(session.getConnector().getConfig().isCacheChunks() &&
                session.getConnector().getWorldManager().getBlockAt(session, pos) == packet.getRecord().getBlock());
        ChunkUtils.updateBlock(session, packet.getRecord().getBlock(), pos);
        if (updatePlacement) {
            this.checkPlace(session, packet);
        }
        this.checkInteract(session, packet);
    }

    private boolean checkPlace(GeyserSession session, ServerBlockChangePacket packet) {
        Vector3i lastPlacePos = session.getLastBlockPlacePosition();
        if (lastPlacePos == null) {
            return false;
        }
        if ((lastPlacePos.getX() != packet.getRecord().getPosition().getX()
                || lastPlacePos.getY() != packet.getRecord().getPosition().getY()
                || lastPlacePos.getZ() != packet.getRecord().getPosition().getZ())) {
            return false;
        }

        // We need to check if the identifier is the same, else a packet with the sound of what the
        // player has in their hand is played, despite if the block is being placed or not
        boolean contains = false;
        String identifier = BlockTranslator.getJavaIdBlockMap().inverse().get(packet.getRecord().getBlock()).split("\\[")[0];
        if (identifier.equals(session.getLastBlockPlacedId())) {
            contains = true;
        }

        if (!contains) {
            session.setLastBlockPlacePosition(null);
            session.setLastBlockPlacedId(null);
            return false;
        }

        // This is not sent from the server, so we need to send it this way
        LevelSoundEventPacket placeBlockSoundPacket = new LevelSoundEventPacket();
        placeBlockSoundPacket.setSound(SoundEvent.PLACE);
        placeBlockSoundPacket.setPosition(lastPlacePos.toFloat());
        placeBlockSoundPacket.setBabySound(false);
        placeBlockSoundPacket.setExtraData(session.getBlockTranslator().getBedrockBlockId(packet.getRecord().getBlock()));
        placeBlockSoundPacket.setIdentifier(":");
        session.sendUpstreamPacket(placeBlockSoundPacket);
        session.setLastBlockPlacePosition(null);
        session.setLastBlockPlacedId(null);
        return true;
    }

    private void checkInteract(GeyserSession session, ServerBlockChangePacket packet) {
        Vector3i lastInteractPos = session.getLastInteractionBlockPosition();
        if (lastInteractPos == null || !session.isInteracting()) {
            return;
        }
        if ((lastInteractPos.getX() != packet.getRecord().getPosition().getX()
                || lastInteractPos.getY() != packet.getRecord().getPosition().getY()
                || lastInteractPos.getZ() != packet.getRecord().getPosition().getZ())) {
            return;
        }
        String identifier = BlockTranslator.getJavaIdBlockMap().inverse().get(packet.getRecord().getBlock());
        session.setInteracting(false);
        BlockSoundInteractionHandler.handleBlockInteraction(session, lastInteractPos.toFloat(), identifier);
    }
}
