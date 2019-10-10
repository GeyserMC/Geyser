/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.java.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket;
import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.data.EntityFlag;
import com.nukkitx.protocol.bedrock.data.EntityFlags;
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.utils.MessageUtils;

public class JavaEntityMetadataTranslator extends PacketTranslator<ServerEntityMetadataPacket> {

    @Override
    public void translate(ServerEntityMetadataPacket packet, GeyserSession session) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        EntityFlags entityflags = new EntityFlags();
        //EntityType entitytype = entity.getEntityType();
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            entity = session.getPlayerEntity();
        }
        if (entity == null)
            return;

        if (entity.isValid()) {
            SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
            entityDataPacket.setRuntimeEntityId(entity.getGeyserId());
            entityDataPacket.getMetadata().putAll(entity.getMetadata());

            for(EntityMetadata meta : packet.getMetadata()) {
                switch(meta.getId()) {
                    case 0: //Entity Flags
                        entityflags.setFlag(EntityFlag.ON_FIRE, ((byte) meta.getValue() & 0x01) > 0);
                        entityflags.setFlag(EntityFlag.SNEAKING, ((byte) meta.getValue() & 0x02) > 0);
                        entityflags.setFlag(EntityFlag.SPRINTING, ((byte) meta.getValue() & 0x08) > 0);
                        entityflags.setFlag(EntityFlag.SWIMMING, ((byte) meta.getValue() & 0x10) > 0);
                        entityflags.setFlag(EntityFlag.INVISIBLE, ((byte) meta.getValue() & 0x20) > 0);
                        //entityflags.setFlag(EntityFlag.GLOWING, ((byte) meta.getValue() & 0x40) > 0); <- Future proof for when bedrock adds glowing effect
                        entityflags.setFlag(EntityFlag.GLIDING, ((byte) meta.getValue() & 0x80) > 0);
                        break;

                    case 1: //Air
                        entityDataPacket.getMetadata().put(EntityData.AIR, meta.getValue());
                        break;

                    case 2: //Custom Name
                        if(meta.getValue() != null) {
                            entityDataPacket.getMetadata().put(EntityData.NAMETAG, MessageUtils.getBedrockMessage((Message) meta.getValue()));
                        }
                        break;

                    case 3: //Custom Name Visible
                        if((boolean) meta.getValue() == true) {
                            entityDataPacket.getMetadata().put(EntityData.ALWAYS_SHOW_NAMETAG, (byte) 1);
                        }
                        else
                            entityDataPacket.getMetadata().put(EntityData.ALWAYS_SHOW_NAMETAG, (byte) 0);
                        break;

                    case 4: //Silent
                        entityflags.setFlag(EntityFlag.SILENT, (boolean) meta.getValue());
                        break;

                    case 5: //No gravity
                        entityflags.setFlag(EntityFlag.HAS_GRAVITY, !(boolean) meta.getValue());
                        break;
                }
            }

            entityDataPacket.getMetadata().putFlags(entityflags);
            session.getUpstream().sendPacket(entityDataPacket);
        } else {
            entity.spawnEntity(session);
        }
    }
}
