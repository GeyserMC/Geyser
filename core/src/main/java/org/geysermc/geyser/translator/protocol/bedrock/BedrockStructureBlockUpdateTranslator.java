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
import org.cloudburstmc.protocol.bedrock.data.structure.StructureBlockType;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureEditorData;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureSettings;
import org.cloudburstmc.protocol.bedrock.packet.StructureBlockUpdatePacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

import java.util.Objects;

@Translator(packet = StructureBlockUpdatePacket.class)
public class BedrockStructureBlockUpdateTranslator extends PacketTranslator<StructureBlockUpdatePacket> {

    @Override
    public void translate(GeyserSession session, StructureBlockUpdatePacket packet) {
        StructureEditorData data = packet.getEditorData();
        StructureSettings settings = data.getSettings();

        UpdateStructureBlockAction action = UpdateStructureBlockAction.UPDATE_DATA;
        if (packet.isPowered()) {
            if (data.getType() == StructureBlockType.LOAD) {
                action = UpdateStructureBlockAction.LOAD_STRUCTURE;
            } else if (data.getType() == StructureBlockType.SAVE) {
                action = UpdateStructureBlockAction.SAVE_STRUCTURE;
            }
        }

        UpdateStructureBlockMode mode = switch (data.getType()) {
            case CORNER -> UpdateStructureBlockMode.CORNER;
            case DATA -> UpdateStructureBlockMode.DATA;
            case LOAD -> UpdateStructureBlockMode.LOAD;
            default -> UpdateStructureBlockMode.SAVE;
        };

        StructureMirror mirror = switch (data.getSettings().getMirror()) {
            case X -> StructureMirror.FRONT_BACK;
            case Z -> StructureMirror.LEFT_RIGHT;
            default -> StructureMirror.NONE;
        };

        com.github.steveice10.mc.protocol.data.game.level.block.StructureRotation rotation = switch (settings.getRotation()) {
            case ROTATE_90 -> StructureRotation.CLOCKWISE_90;
            case ROTATE_180 -> StructureRotation.CLOCKWISE_180;
            case ROTATE_270 -> StructureRotation.COUNTERCLOCKWISE_90;
            default -> StructureRotation.NONE;
        };

        Vector3i offset = settings.getOffset().sub(Objects.requireNonNull(session.getStructureBlockCache().getBedrockOffset()));

        ServerboundSetStructureBlockPacket structureBlockPacket = new ServerboundSetStructureBlockPacket(
                packet.getBlockPosition(),
                action,
                mode,
                data.getName(),
                offset,
                settings.getSize(),
                mirror,
                rotation,
                "",
                settings.getIntegrityValue(),
                settings.getIntegritySeed(),
                settings.isIgnoringEntities(),
                false,
                data.isBoundingBoxVisible()
        );

        session.getStructureBlockCache().clear();
        session.sendDownstreamPacket(structureBlockPacket);
    }
}
