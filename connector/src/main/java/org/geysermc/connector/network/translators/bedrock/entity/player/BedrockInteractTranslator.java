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

package org.geysermc.connector.network.translators.bedrock.entity.player;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerInteractEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerStatePacket;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import com.nukkitx.protocol.bedrock.packet.InteractPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.living.animal.horse.AbstractHorseEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.utils.InteractiveTagManager;

@Translator(packet = InteractPacket.class)
public class BedrockInteractTranslator extends PacketTranslator<InteractPacket> {

    @Override
    public void translate(InteractPacket packet, GeyserSession session) {
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
                if (session.getPlayerInventory().getItemInHand().getJavaId() == ItemRegistry.SHIELD.getJavaId()) {
                    break;
                }
                ClientPlayerInteractEntityPacket interactPacket = new ClientPlayerInteractEntityPacket((int) entity.getEntityId(),
                        InteractAction.INTERACT, Hand.MAIN_HAND, session.isSneaking());
                session.sendDownstreamPacket(interactPacket);
                break;
            case DAMAGE:
                ClientPlayerInteractEntityPacket attackPacket = new ClientPlayerInteractEntityPacket((int) entity.getEntityId(),
                        InteractAction.ATTACK, Hand.MAIN_HAND, session.isSneaking());
                session.sendDownstreamPacket(attackPacket);
                break;
            case LEAVE_VEHICLE:
                ClientPlayerStatePacket sneakPacket = new ClientPlayerStatePacket((int) entity.getEntityId(), PlayerState.START_SNEAKING);
                session.sendDownstreamPacket(sneakPacket);
                session.setRidingVehicleEntity(null);
                break;
            case MOUSEOVER:
                // Handle the buttons for mobile - "Mount", etc; and the suggestions for console - "ZL: Mount", etc
                if (packet.getRuntimeEntityId() != 0) {
                    Entity interactEntity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
                    session.setMouseoverEntity(interactEntity);
                    if (interactEntity == null) {
                        return;
                    }

                    InteractiveTagManager.updateTag(session, interactEntity);
                } else {
                    if (session.getMouseoverEntity() != null) {
                        // No interactive tag should be sent
                        session.setMouseoverEntity(null);
                        session.getPlayerEntity().getMetadata().put(EntityData.INTERACTIVE_TAG, "");
                        session.getPlayerEntity().updateBedrockMetadata(session);
                    }
                }
                break;
            case OPEN_INVENTORY:
                if (session.getOpenInventory() == null) {
                    Entity ridingEntity = session.getRidingVehicleEntity();
                    if (ridingEntity instanceof AbstractHorseEntity) {
                        if (ridingEntity.getMetadata().getFlags().getFlag(EntityFlag.TAMED)) {
                            // We should request to open the horse inventory instead
                            ClientPlayerStatePacket openHorseWindowPacket = new ClientPlayerStatePacket((int) session.getPlayerEntity().getEntityId(), PlayerState.OPEN_HORSE_INVENTORY);
                            session.sendDownstreamPacket(openHorseWindowPacket);
                        }
                    } else {
                        session.setOpenInventory(session.getPlayerInventory());

                        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
                        containerOpenPacket.setId((byte) 0);
                        containerOpenPacket.setType(ContainerType.INVENTORY);
                        containerOpenPacket.setUniqueEntityId(-1);
                        containerOpenPacket.setBlockPosition(entity.getPosition().toInt());
                        session.sendUpstreamPacket(containerOpenPacket);
                    }
                }
                break;
        }
    }
}
