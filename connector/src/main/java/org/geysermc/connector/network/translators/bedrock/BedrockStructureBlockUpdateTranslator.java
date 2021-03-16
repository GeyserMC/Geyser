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

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.window.UpdateStructureBlockAction;
import com.github.steveice10.mc.protocol.data.game.window.UpdateStructureBlockMode;
import com.github.steveice10.mc.protocol.data.game.world.block.StructureMirror;
import com.github.steveice10.mc.protocol.data.game.world.block.StructureRotation;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientUpdateStructureBlockPacket;
import com.nukkitx.protocol.bedrock.data.structure.StructureBlockType;
import com.nukkitx.protocol.bedrock.data.structure.StructureEditorData;
import com.nukkitx.protocol.bedrock.data.structure.StructureSettings;
import com.nukkitx.protocol.bedrock.packet.StructureBlockUpdatePacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

@Translator(packet = StructureBlockUpdatePacket.class)
public class BedrockStructureBlockUpdateTranslator extends PacketTranslator<StructureBlockUpdatePacket> {

    @Override
    public void translate(StructureBlockUpdatePacket packet, GeyserSession session) {
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

        UpdateStructureBlockMode mode;
        switch (data.getType()) {
            case CORNER:
                mode = UpdateStructureBlockMode.CORNER;
                break;
            case DATA:
                mode = UpdateStructureBlockMode.DATA;
                break;
            case LOAD:
                mode = UpdateStructureBlockMode.LOAD;
                break;
            case SAVE:
            default:
                mode = UpdateStructureBlockMode.SAVE;
                break;
        }

        // Ignore mirror - Java appears to mirror on an axis, while Bedrock mirrors in place
        StructureMirror mirror = StructureMirror.NONE;

        StructureRotation rotation;
        switch (settings.getRotation()) {
            case ROTATE_90:
                rotation = StructureRotation.CLOCKWISE_90;
                break;
            case ROTATE_180:
                rotation = StructureRotation.CLOCKWISE_180;
                break;
            case ROTATE_270:
                rotation = StructureRotation.COUNTERCLOCKWISE_90;
                break;
            default:
                rotation = StructureRotation.NONE;
                break;
        }

        ClientUpdateStructureBlockPacket updatePacket = new ClientUpdateStructureBlockPacket(
                new Position(packet.getBlockPosition().getX(), packet.getBlockPosition().getY(), packet.getBlockPosition().getZ()),
                action, mode, data.getName(),
                new Position(settings.getOffset().getX(), settings.getOffset().getY(), settings.getOffset().getZ()),
                new Position(settings.getSize().getX(), settings.getSize().getY(), settings.getSize().getZ()),
                mirror, rotation, "",
                (settings.getIntegrityValue() / 100f), settings.getIntegritySeed(), settings.isIgnoringEntities(),
                false,
                data.isBoundingBoxVisible()
        );
        session.sendDownstreamPacket(updatePacket);
    }
}
