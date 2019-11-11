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
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityMetadataPacket;
import com.nukkitx.protocol.bedrock.data.EntityFlag;
import com.nukkitx.protocol.bedrock.packet.AddItemEntityPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.TranslatorsInit;

public class JavaEntityMetadataTranslator extends PacketTranslator<ServerEntityMetadataPacket> {

    @Override
    public void translate(ServerEntityMetadataPacket packet, GeyserSession session) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            entity = session.getPlayerEntity();
        }
        if (entity == null) return;

        if (entity.isValid()) {
            //temp sprint/sneak fix
            for (EntityMetadata metadata : packet.getMetadata()) {
                if (metadata.getId() == 0 && metadata.getType() == MetadataType.BYTE) {
                    byte xd = (byte)metadata.getValue();
		   entity.getMetadata().getFlags().setFlag(EntityFlag.SPRINTING, (xd & 0x08) == 0x08);
                   entity.getMetadata().getFlags().setFlag(EntityFlag.SNEAKING, (xd & 0x02) == 0x02);
                   entity.getMetadata().getFlags().setFlag(EntityFlag.SWIMMING, (xd & 0x10) == 0x10);
	           entity.getMetadata().getFlags().setFlag(EntityFlag.GLIDING, (xd & 0x80) == 0x80);
		   entity.getMetadata().getFlags().setFlag(EntityFlag.INVISIBLE, (xd & 0x20) == 0x20);
                } else if (entity.getEntityType() == EntityType.ITEM && metadata.getId() == 7) {
                    AddItemEntityPacket itemPacket = new AddItemEntityPacket();
                    itemPacket.setRuntimeEntityId(entity.getGeyserId());
                    itemPacket.setPosition(entity.getPosition());
                    itemPacket.setMotion(entity.getMotion());
                    itemPacket.setUniqueEntityId(entity.getGeyserId());
                    itemPacket.setFromFishing(false);
                    itemPacket.getMetadata().putAll(entity.getMetadata());
                    itemPacket.setItemInHand(TranslatorsInit.getItemTranslator().translateToBedrock((ItemStack) metadata.getValue()));
                    session.getUpstream().sendPacket(itemPacket);
                    return;
                }
            }

            // TODO: Make this actually useful lol
            SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
            entityDataPacket.setRuntimeEntityId(entity.getGeyserId());
            entityDataPacket.getMetadata().putAll(entity.getMetadata());

            session.getUpstream().sendPacket(entityDataPacket);
        } else {
            entity.spawnEntity(session);
        }
    }
}
