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

package org.geysermc.geyser.translator.protocol.java.level;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEventPacket;
import org.geysermc.common.PlatformType;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.sound.BlockSoundInteractionTranslator;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.util.ChunkUtils;

@Translator(packet = ClientboundBlockUpdatePacket.class)
public class JavaBlockUpdateTranslator extends PacketTranslator<ClientboundBlockUpdatePacket> {

    @Override
    public void translate(GeyserSession session, ClientboundBlockUpdatePacket packet) {
        Position pos = packet.getEntry().getPosition();
        boolean updatePlacement = session.getGeyser().getPlatformType() != PlatformType.SPIGOT && // Spigot simply listens for the block place event
                session.getGeyser().getWorldManager().getBlockAt(session, pos) != packet.getEntry().getBlock();
        ChunkUtils.updateBlock(session, packet.getEntry().getBlock(), pos);
        if (updatePlacement) {
            this.checkPlace(session, packet);
        }
        this.checkInteract(session, packet);
    }

    private boolean checkPlace(GeyserSession session, ClientboundBlockUpdatePacket packet) {
        Vector3i lastPlacePos = session.getLastBlockPlacePosition();
        if (lastPlacePos == null) {
            return false;
        }
        if ((lastPlacePos.getX() != packet.getEntry().getPosition().getX()
                || lastPlacePos.getY() != packet.getEntry().getPosition().getY()
                || lastPlacePos.getZ() != packet.getEntry().getPosition().getZ())) {
            return false;
        }

        // We need to check if the identifier is the same, else a packet with the sound of what the
        // player has in their hand is played, despite if the block is being placed or not
        boolean contains = false;
        String identifier = BlockRegistries.JAVA_BLOCKS.get(packet.getEntry().getBlock()).getItemIdentifier();
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
        placeBlockSoundPacket.setExtraData(session.getBlockMappings().getBedrockBlockId(packet.getEntry().getBlock()));
        placeBlockSoundPacket.setIdentifier(":");
        session.sendUpstreamPacket(placeBlockSoundPacket);
        session.setLastBlockPlacePosition(null);
        session.setLastBlockPlacedId(null);
        return true;
    }

    private void checkInteract(GeyserSession session, ClientboundBlockUpdatePacket packet) {
        Vector3i lastInteractPos = session.getLastInteractionBlockPosition();
        if (lastInteractPos == null || !session.isInteracting()) {
            return;
        }
        if ((lastInteractPos.getX() != packet.getEntry().getPosition().getX()
                || lastInteractPos.getY() != packet.getEntry().getPosition().getY()
                || lastInteractPos.getZ() != packet.getEntry().getPosition().getZ())) {
            return;
        }
        String identifier = BlockRegistries.JAVA_IDENTIFIERS.get().get(packet.getEntry().getBlock());
        session.setInteracting(false);
        BlockSoundInteractionTranslator.handleBlockInteraction(session, lastInteractPos.toFloat(), identifier);
    }
}
