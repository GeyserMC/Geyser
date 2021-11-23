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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Equipment;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundSetEquipmentPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.FakeHeadProvider;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = ClientboundSetEquipmentPacket.class)
public class JavaSetEquipmentTranslator extends PacketTranslator<ClientboundSetEquipmentPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundSetEquipmentPacket packet) {
        Entity entity;
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            entity = session.getPlayerEntity();
        } else {
            entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        }

        if (entity == null)
            return;

        if (!(entity instanceof LivingEntity livingEntity)) {
            session.getGeyser().getLogger().debug("Attempted to add armor to a non-living entity type (" +
                    entity.getDefinition().entityType().name() + ").");
            return;
        }

        boolean armorUpdated = false;
        boolean mainHandUpdated = false;
        boolean offHandUpdated = false;
        for (Equipment equipment : packet.getEquipment()) {
            ItemData item = ItemTranslator.translateToBedrock(session, equipment.getItem());
            switch (equipment.getSlot()) {
                case HELMET -> {
                    ItemStack javaItem = equipment.getItem();
                    if (livingEntity instanceof PlayerEntity
                            && javaItem != null
                            && javaItem.getId() == session.getItemMappings().getStoredItems().playerHead().getJavaId()
                            && javaItem.getNbt() != null
                            && javaItem.getNbt().get("SkullOwner") instanceof CompoundTag profile) {
                        FakeHeadProvider.setHead(session, (PlayerEntity) livingEntity, profile);
                    } else {
                        FakeHeadProvider.restoreOriginalSkin(session, livingEntity);
                    }

                    livingEntity.setHelmet(item);
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
