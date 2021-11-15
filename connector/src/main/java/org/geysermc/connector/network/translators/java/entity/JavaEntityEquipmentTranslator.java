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

package org.geysermc.connector.network.translators.java.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Equipment;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityEquipmentPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.LivingEntity;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.skin.FakeHeadProvider;

@Translator(packet = ServerEntityEquipmentPacket.class)
public class JavaEntityEquipmentTranslator extends PacketTranslator<ServerEntityEquipmentPacket> {

    @Override
    public void translate(GeyserSession session, ServerEntityEquipmentPacket packet) {
        Entity entity;
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            entity = session.getPlayerEntity();
        } else {
            entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        }

        if (entity == null)
            return;

        if (!(entity instanceof LivingEntity livingEntity)) {
            session.getConnector().getLogger().debug("Attempted to add armor to a non-living entity type (" +
                    entity.getEntityType().name() + ").");
            return;
        }

        boolean armorUpdated = false;
        boolean mainHandUpdated = false;
        boolean offHandUpdated = false;
        for (Equipment equipment : packet.getEquipment()) {
            ItemData item = ItemTranslator.translateToBedrock(session, equipment.getItem());
            switch (equipment.getSlot()) {
                case HELMET -> {
                    CompoundTag profile = null;

                    if (livingEntity instanceof PlayerEntity
                            && session.getItemMappings().getMapping(item).getJavaIdentifier().equals("minecraft:player_head")
                            && equipment.getItem().getNbt() != null
                            && equipment.getItem().getNbt().contains("SkullOwner")
                            && equipment.getItem().getNbt().get("SkullOwner") instanceof CompoundTag) {
                        profile = equipment.getItem().getNbt().get("SkullOwner");
                    }

                    if (profile != null) {
                        FakeHeadProvider.setHead(session, (PlayerEntity) livingEntity, profile);
                        livingEntity.setHelmet(ItemData.AIR);
                    } else {
                        FakeHeadProvider.restoreOriginalSkin(session, livingEntity);
                        livingEntity.setHelmet(item);
                    }

                    armorUpdated = true;
                }
                case CHESTPLATE -> {
                    livingEntity.setChestplate(item);
                    armorUpdated = true;
                }
                case LEGGINGS -> {
                    livingEntity.setLeggings(item);
                    armorUpdated = true;
                }
                case BOOTS -> {
                    livingEntity.setBoots(item);
                    armorUpdated = true;
                }
                case MAIN_HAND -> {
                    livingEntity.setHand(item);
                    mainHandUpdated = true;
                }
                case OFF_HAND -> {
                    livingEntity.setOffHand(item);
                    offHandUpdated = true;
                }
            }
        }

        if (armorUpdated) {
            livingEntity.updateArmor(session);
        }
        if (mainHandUpdated) {
            livingEntity.updateMainHand(session);
        }
        if (offHandUpdated) {
            livingEntity.updateOffHand(session);
        }
    }
}
