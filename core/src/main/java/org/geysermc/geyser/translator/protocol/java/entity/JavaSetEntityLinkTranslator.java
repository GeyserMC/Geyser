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

package org.geysermc.geyser.translator.protocol.java.entity;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityLinkPacket;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.living.MobEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

/**
 * Called when a leash is attached, removed or updated from an entity
 */
@Translator(packet = ClientboundSetEntityLinkPacket.class)
public class JavaSetEntityLinkTranslator extends PacketTranslator<ClientboundSetEntityLinkPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundSetEntityLinkPacket packet) {
        Entity holderId;
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            holderId = session.getPlayerEntity();
        } else {
            holderId = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        }
        if (!(holderId instanceof MobEntity mobEntity)) {
            return;
        }

        Entity attachedToId;
        if (packet.getAttachedToId() == session.getPlayerEntity().getEntityId()) {
            attachedToId = session.getPlayerEntity();
        } else {
            attachedToId = session.getEntityCache().getEntityByJavaId(packet.getAttachedToId());
            if ((attachedToId == null || packet.getAttachedToId() == 0)) {
                // Is not being leashed
                mobEntity.setFlag(EntityFlag.LEASHED, false);
                mobEntity.setLeashHolderBedrockId(-1L);
                mobEntity.updateBedrockMetadata();
                EntityEventPacket eventPacket = new EntityEventPacket();
                eventPacket.setRuntimeEntityId(holderId.getGeyserId());
                eventPacket.setType(EntityEventType.REMOVE_LEASH);
                eventPacket.setData(0);
                session.sendUpstreamPacket(eventPacket);
                return;
            }
        }

        mobEntity.setFlag(EntityFlag.LEASHED, true);
        mobEntity.setLeashHolderBedrockId(attachedToId.getGeyserId());
        holderId.updateBedrockMetadata();
    }
}
