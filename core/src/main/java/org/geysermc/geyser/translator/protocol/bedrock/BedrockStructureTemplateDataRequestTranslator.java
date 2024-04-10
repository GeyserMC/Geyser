/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.bedrock;

import com.github.steveice10.mc.protocol.data.game.inventory.UpdateStructureBlockAction;
import com.github.steveice10.mc.protocol.data.game.inventory.UpdateStructureBlockMode;
import com.github.steveice10.mc.protocol.data.game.level.block.StructureMirror;
import com.github.steveice10.mc.protocol.data.game.level.block.StructureRotation;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetStructureBlockPacket;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureSettings;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureTemplateRequestOperation;
import org.cloudburstmc.protocol.bedrock.packet.StructureTemplateDataRequestPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.StructureBlockUtils;

/**
 * Packet used in Bedrock to load structure size into the structure block GUI. It is sent every time the GUI is opened.
 * Or, if the player updates the structure name. Which we can use to request the structure size from the Java server!
 * <p>
 * Java does not have this preview, instead, Java clients are forced out of the GUI to look at the area.
 */
@Translator(packet = StructureTemplateDataRequestPacket.class)
public class BedrockStructureTemplateDataRequestTranslator extends PacketTranslator<StructureTemplateDataRequestPacket> {

    @Override
    public void translate(GeyserSession session, StructureTemplateDataRequestPacket packet) {
        if (packet.getOperation().equals(StructureTemplateRequestOperation.QUERY_SAVED_STRUCTURE)) {
            // If we send a load packet to the Java server when the structure size is known, it would place the structure.
            if (!packet.getSettings().getSize().equals(Vector3i.ZERO)) {
                if (session.getStructureSettings() == null) {
                    GeyserImpl.getInstance().getLogger().info("saving old settings!" + packet.getSettings());
                    session.setStructureSettings(packet.getSettings());
                }
                // Otherwise, the Bedrock client can't load the structure in
                StructureBlockUtils.sendEmptyStructureData(session, packet);
                return;
            }
            session.setCurrentStructureBlock(packet.getPosition());

            // Request a "load" from Java server, so it sends us the structure's size :p
            StructureSettings settings = packet.getSettings();
            com.github.steveice10.mc.protocol.data.game.level.block.StructureRotation rotation = switch (settings.getRotation()) {
                case ROTATE_90 -> StructureRotation.CLOCKWISE_90;
                case ROTATE_180 -> StructureRotation.CLOCKWISE_180;
                case ROTATE_270 -> StructureRotation.COUNTERCLOCKWISE_90;
                default -> StructureRotation.NONE;
            };

            StructureMirror mirror = switch (settings.getMirror()) {
                case X -> StructureMirror.FRONT_BACK;
                case Z -> StructureMirror.LEFT_RIGHT;
                default -> StructureMirror.NONE;
            };

            ServerboundSetStructureBlockPacket structureBlockPacket = new ServerboundSetStructureBlockPacket(
                    packet.getPosition(),
                    UpdateStructureBlockAction.LOAD_STRUCTURE,
                    UpdateStructureBlockMode.LOAD,
                    packet.getName(),
                    settings.getOffset(),
                    settings.getSize(),
                    mirror,
                    rotation,
                    "",
                    settings.getIntegrityValue(),
                    settings.getIntegritySeed(),
                    settings.isIgnoringEntities(),
                    false,
                    true
            );
            session.sendDownstreamPacket(structureBlockPacket);
        } else {
            StructureBlockUtils.sendEmptyStructureData(session, packet);
        }
    }
}