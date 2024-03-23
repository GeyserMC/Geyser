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
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureTemplateRequestOperation;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureTemplateResponseType;
import org.cloudburstmc.protocol.bedrock.packet.StructureTemplateDataRequestPacket;
import org.cloudburstmc.protocol.bedrock.packet.StructureTemplateDataResponsePacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

/**
 * Packet used in Bedrock to load structure size into the structure block GUI.
 * <p>
 * Java does not have this preview, instead, Java clients are forced out of the GUI to look at the area.
 */
@Translator(packet = StructureTemplateDataRequestPacket.class)
public class BedrockStructureTemplateDataRequestTranslator extends PacketTranslator<StructureTemplateDataRequestPacket> {

    private static final NbtMap EMPTY_STRUCTURE_DATA;

    static {
        NbtMapBuilder builder = NbtMap.builder();
        builder.putInt("format_version", 1);
        builder.putCompound("structure", NbtMap.builder()
                .putList("block_indices", NbtType.LIST, NbtList.EMPTY, NbtList.EMPTY)
                .putList("entities", NbtType.COMPOUND)
                .putCompound("palette", NbtMap.EMPTY)
                .build());
        builder.putList("structure_world_origin", NbtType.INT, 0, 0, 0);
        EMPTY_STRUCTURE_DATA = builder.build();
    }

    @Override
    public void translate(GeyserSession session, StructureTemplateDataRequestPacket packet) {
        GeyserImpl.getInstance().getLogger().error(packet.toString());
        if (packet.getOperation().equals(StructureTemplateRequestOperation.QUERY_SAVED_STRUCTURE)) {
            session.setCurrentStructureBlock(packet.getPosition());

            // Request a "load" from Java server so it sends us the structures size :p
            var settings = packet.getSettings();
            com.github.steveice10.mc.protocol.data.game.level.block.StructureRotation rotation = switch (settings.getRotation()) {
                case ROTATE_90 -> StructureRotation.CLOCKWISE_90;
                case ROTATE_180 -> StructureRotation.CLOCKWISE_180;
                case ROTATE_270 -> StructureRotation.COUNTERCLOCKWISE_90;
                default -> StructureRotation.NONE;
            };

            ServerboundSetStructureBlockPacket structureBlockPacket = new ServerboundSetStructureBlockPacket(
                    packet.getPosition(),
                    UpdateStructureBlockAction.LOAD_STRUCTURE,
                    UpdateStructureBlockMode.LOAD,
                    packet.getName(),
                    settings.getOffset(),
                    settings.getSize(),
                    StructureMirror.NONE,
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
            StructureTemplateDataResponsePacket responsePacket = new StructureTemplateDataResponsePacket();
            responsePacket.setName(packet.getName());
            responsePacket.setSave(true);
            responsePacket.setTag(EMPTY_STRUCTURE_DATA.toBuilder()
                    .putList("size", NbtType.INT, packet.getSettings().getSize().getX(),
                            packet.getSettings().getSize().getY(), packet.getSettings().getSize().getZ())
                    .build());
            responsePacket.setType(StructureTemplateResponseType.QUERY);
            session.sendUpstreamPacket(responsePacket);
        }
    }
}