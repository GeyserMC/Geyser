/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.LivingEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEquipmentPacket;
import com.nukkitx.protocol.bedrock.data.ItemData;
import org.geysermc.connector.network.translators.item.ItemTranslator;

@Translator(packet = ServerEntityEquipmentPacket.class)
public class JavaEntityEquipmentTranslator extends PacketTranslator<ServerEntityEquipmentPacket> {

    @Override
    public void translate(ServerEntityEquipmentPacket packet, GeyserSession session) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            entity = session.getPlayerEntity();
        }

        if (entity == null)
            return;

        if (!(entity instanceof LivingEntity)) {
            session.getConnector().getLogger().debug("Attempted to add armor to a non-living entity type (" +
                    entity.getEntityType().name() + ").");
            return;
        }

        LivingEntity livingEntity = (LivingEntity) entity;
        ItemData item = ItemTranslator.translateToBedrock(session, packet.getItem());
        switch (packet.getSlot()) {
            case HELMET:
                livingEntity.setHelmet(item);
                break;
            case CHESTPLATE:
                livingEntity.setChestplate(item);
                break;
            case LEGGINGS:
                livingEntity.setLeggings(item);
                break;
            case BOOTS:
                livingEntity.setBoots(item);
                break;
            case MAIN_HAND:
                livingEntity.setHand(item);
                break;
            case OFF_HAND:
                livingEntity.setOffHand(item);
                break;
        }

        livingEntity.updateEquipment(session);
    }
}
