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

package org.geysermc.connector.network.translators.bedrock;

import com.nukkitx.protocol.bedrock.data.EntityData;
import com.nukkitx.protocol.bedrock.data.EntityDataMap;
import com.nukkitx.protocol.bedrock.data.EntityFlag;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerState;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerInteractEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerStatePacket;
import com.nukkitx.protocol.bedrock.packet.InteractPacket;
import org.geysermc.connector.network.translators.item.ItemRegistry;

@Translator(packet = InteractPacket.class)
public class BedrockInteractTranslator extends PacketTranslator<InteractPacket> {

    @Override
    public void translate(InteractPacket packet, GeyserSession session) {
        Entity entity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
        if (entity == null)
            return;

        switch (packet.getAction()) {
            case INTERACT:
                if (session.getInventory().getItem(session.getInventory().getHeldItemSlot() + 36).getId() == ItemRegistry.SHIELD) {
                    break;
                }
                ClientPlayerInteractEntityPacket interactPacket = new ClientPlayerInteractEntityPacket((int) entity.getEntityId(),
                        InteractAction.INTERACT, Hand.MAIN_HAND);
                session.sendDownstreamPacket(interactPacket);
                break;
            case DAMAGE:
                ClientPlayerInteractEntityPacket attackPacket = new ClientPlayerInteractEntityPacket((int) entity.getEntityId(),
                        InteractAction.ATTACK, Hand.MAIN_HAND);
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
                    if (interactEntity == null)
                        return;
                    EntityDataMap entityMetadata = interactEntity.getMetadata();

                    String interactiveTag;
                    switch (interactEntity.getEntityType()) {
                        case BOAT:
                            interactiveTag = "action.interact.ride.boat";
                            break;
                        case DONKEY:
                        case HORSE:
                        case LLAMA:
                        case MULE:
                        case SKELETON_HORSE:
                        case TRADER_LLAMA:
                        case ZOMBIE_HORSE:
                            if (entityMetadata.getFlags().getFlag(EntityFlag.TAMED)) {
                                interactiveTag = "action.interact.ride.horse";
                            } else {
                                interactiveTag = "action.interact.mount";
                            }
                            break;
                        case MINECART:
                            interactiveTag = "action.interact.ride.minecart";
                            break;
                        case PIG:
                            if (entityMetadata.getFlags().getFlag(EntityFlag.SADDLED)) {
                                interactiveTag = "action.interact.mount";
                            } else interactiveTag = "";
                            break;
                        case VILLAGER:
                            if (entityMetadata.getInt(EntityData.VARIANT) != 14 && entityMetadata.getInt(EntityData.VARIANT) != 0
                            && entityMetadata.getFloat(EntityData.SCALE) >= 0.75f) { // Not a nitwit, has a profession and is not a baby
                                interactiveTag = "action.interact.trade";
                            } else interactiveTag = "";
                            break;
                        case WANDERING_TRADER:
                            interactiveTag = "action.interact.trade"; // Since you can always trade with a wandering villager, presumably.
                            break;
                        default:
                            return; // No need to process any further since there is no interactive tag
                    }
                    session.getPlayerEntity().getMetadata().put(EntityData.INTERACTIVE_TAG, interactiveTag);
                    session.getPlayerEntity().updateBedrockMetadata(session);
                } else {
                    if (!(session.getPlayerEntity().getMetadata().get(EntityData.INTERACTIVE_TAG) == null) ||
                    !(session.getPlayerEntity().getMetadata().get(EntityData.INTERACTIVE_TAG) == "")) {
                        // No interactive tag should be sent
                        session.getPlayerEntity().getMetadata().remove(EntityData.INTERACTIVE_TAG);
                        session.getPlayerEntity().updateBedrockMetadata(session);
                    }
                }
                break;
        }
    }
}
