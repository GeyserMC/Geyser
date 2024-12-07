/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.bedrock.entity.player;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.packet.InteractPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket;
import org.geysermc.geyser.entity.type.ChestBoatEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.living.animal.horse.AbstractHorseEntity;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;

import java.util.concurrent.TimeUnit;

@Translator(packet = InteractPacket.class)
public class BedrockInteractTranslator extends PacketTranslator<InteractPacket> {

    @Override
    public void translate(GeyserSession session, InteractPacket packet) {
        Entity entity;
        if (packet.getRuntimeEntityId() == session.getPlayerEntity().getGeyserId()) {
            //Player is not in entity cache
            entity = session.getPlayerEntity();
        } else {
            entity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
        }
        if (entity == null)
            return;

        switch (packet.getAction()) {
            case INTERACT:
                if (session.getPlayerInventory().getItemInHand().asItem() == Items.SHIELD) {
                    break;
                }
                ServerboundInteractPacket interactPacket = new ServerboundInteractPacket(entity.getEntityId(),
                        InteractAction.INTERACT, Hand.MAIN_HAND, session.isSneaking());
                session.sendDownstreamGamePacket(interactPacket);
                break;
            case DAMAGE:
                ServerboundInteractPacket attackPacket = new ServerboundInteractPacket(entity.getEntityId(),
                        InteractAction.ATTACK, Hand.MAIN_HAND, session.isSneaking());
                session.sendDownstreamGamePacket(attackPacket);
                break;
            case LEAVE_VEHICLE:
                ServerboundPlayerCommandPacket sneakPacket = new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_SNEAKING);
                session.sendDownstreamGamePacket(sneakPacket);

                Entity currentVehicle = session.getPlayerEntity().getVehicle();
                if (currentVehicle != null) {
                    session.setMountVehicleScheduledFuture(session.scheduleInEventLoop(() -> {
                        if (session.getPlayerEntity().getVehicle() == null) {
                            return;
                        }

                        long vehicleBedrockId = currentVehicle.getGeyserId();
                        if (session.getPlayerEntity().getVehicle().getGeyserId() == vehicleBedrockId) {
                            // The Bedrock client, as of 1.19.51, dismounts on its end. The server may not agree with this.
                            // If the server doesn't agree with our dismount (sends a packet saying we dismounted),
                            // then remount the player.
                            SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
                            linkPacket.setEntityLink(new EntityLinkData(vehicleBedrockId, session.getPlayerEntity().getGeyserId(), EntityLinkData.Type.PASSENGER, true, false));
                            session.sendUpstreamPacket(linkPacket);
                        }
                    }, 1, TimeUnit.SECONDS));
                }
                break;
            case MOUSEOVER:
                // Handle the buttons for mobile - "Mount", etc; and the suggestions for console - "ZL: Mount", etc
                if (packet.getRuntimeEntityId() != 0) {
                    Entity interactEntity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
                    session.setMouseoverEntity(interactEntity);
                    if (interactEntity == null) {
                        return;
                    }

                    interactEntity.updateInteractiveTag();
                } else {
                    if (session.getMouseoverEntity() != null) {
                        // No interactive tag should be sent
                        session.setMouseoverEntity(null);
                        session.getPlayerEntity().getDirtyMetadata().put(EntityDataTypes.INTERACT_TEXT, "");
                        session.getPlayerEntity().updateBedrockMetadata();
                    }
                }
                break;
            case OPEN_INVENTORY:
                if (session.getOpenInventory() == null) {
                    Entity ridingEntity = session.getPlayerEntity().getVehicle();
                    if (ridingEntity instanceof AbstractHorseEntity || ridingEntity instanceof ChestBoatEntity) {
                        // This mob has an inventory of its own that we should open instead.
                        ServerboundPlayerCommandPacket openVehicleWindowPacket = new ServerboundPlayerCommandPacket(session.getPlayerEntity().getEntityId(), PlayerState.OPEN_VEHICLE_INVENTORY);
                        session.sendDownstreamGamePacket(openVehicleWindowPacket);
                    } else {
                        InventoryUtils.openInventory(session, session.getPlayerInventory());
                    }
                }
        }
    }
}
