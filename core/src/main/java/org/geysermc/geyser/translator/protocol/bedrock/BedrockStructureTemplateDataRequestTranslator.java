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

import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureSettings;
import org.cloudburstmc.protocol.bedrock.data.structure.StructureTemplateRequestOperation;
import org.cloudburstmc.protocol.bedrock.packet.StructureTemplateDataRequestPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.StructureBlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.UpdateStructureBlockAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.UpdateStructureBlockMode;


@Translator(packet = StructureTemplateDataRequestPacket.class)
public class BedrockStructureTemplateDataRequestTranslator extends PacketTranslator<StructureTemplateDataRequestPacket> {

    @Override
    public void translate(GeyserSession session, StructureTemplateDataRequestPacket packet) {
        
        if (packet.getOperation().equals(StructureTemplateRequestOperation.QUERY_SAVED_STRUCTURE)) {
            Vector3i size = packet.getSettings().getSize();
            StructureSettings settings = packet.getSettings();

            
            String currentStructureName = session.getStructureBlockCache().getCurrentStructureName();

            
            
            if (!packet.getSettings().getSize().equals(Vector3i.ZERO)) {
                if (currentStructureName == null) {
                    Vector3i offset = StructureBlockUtils.calculateOffset(settings.getRotation(), settings.getMirror(),
                            settings.getSize().getX(), settings.getSize().getZ());
                    session.getStructureBlockCache().setBedrockOffset(offset);
                    session.getStructureBlockCache().setCurrentStructureName(packet.getName());
                    StructureBlockUtils.sendStructureData(session, size, packet.getName());
                    return;
                } else if (packet.getName().equals(currentStructureName)) {
                    StructureBlockUtils.sendStructureData(session, size, packet.getName());
                    return;
                }
            }

            
            
            session.getStructureBlockCache().setCurrentStructureBlock(packet.getPosition());

            StructureBlockUtils.sendJavaStructurePacket(session,
                    packet.getPosition(),
                    Vector3i.ZERO, 
                    UpdateStructureBlockMode.LOAD,
                    UpdateStructureBlockAction.LOAD_STRUCTURE,
                    settings,
                    true,
                    packet.getName()
            );
        } else {
            StructureBlockUtils.sendEmptyStructureData(session);
        }
    }
}
