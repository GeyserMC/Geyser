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

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPlaceBlockPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.Translators;
import org.geysermc.connector.utils.InventoryUtils;

import com.nukkitx.math.vector.Vector3f;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerInteractEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerUseItemPacket;
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket;

@Translator(packet = InventoryTransactionPacket.class)
public class BedrockInventoryTransactionTranslator extends PacketTranslator<InventoryTransactionPacket> {

    @Override
    public void translate(InventoryTransactionPacket packet, GeyserSession session) {
        switch (packet.getTransactionType()) {
            case NORMAL:
                Inventory inventory = session.getInventoryCache().getOpenInventory();
                if (inventory == null) inventory = session.getInventory();
                Translators.getInventoryTranslators().get(inventory.getWindowType()).translateActions(session, inventory, packet.getActions());
                break;
            case INVENTORY_MISMATCH:
                Inventory inv = session.getInventoryCache().getOpenInventory();
                if (inv == null) inv = session.getInventory();
                Translators.getInventoryTranslators().get(inv.getWindowType()).updateInventory(session, inv);
                InventoryUtils.updateCursor(session);
                break;
            case ITEM_USE:
                switch (packet.getActionType()) {
                    case 0:
                        ClientPlayerPlaceBlockPacket blockPacket = new ClientPlayerPlaceBlockPacket(
                                new Position(packet.getBlockPosition().getX(), packet.getBlockPosition().getY(), packet.getBlockPosition().getZ()),
                                BlockFace.values()[packet.getFace()],
                                Hand.MAIN_HAND,
                                packet.getClickPosition().getX(), packet.getClickPosition().getY(), packet.getClickPosition().getZ(),
                                false);
                        session.getDownstream().getSession().send(blockPacket);
                        break;
                    case 1:
                        ClientPlayerUseItemPacket useItemPacket = new ClientPlayerUseItemPacket(Hand.MAIN_HAND);
                        session.getDownstream().getSession().send(useItemPacket);
                        break;
                    case 2:
                        PlayerAction action = session.getGameMode() == GameMode.CREATIVE ? PlayerAction.START_DIGGING : PlayerAction.FINISH_DIGGING;
                        Position pos = new Position(packet.getBlockPosition().getX(), packet.getBlockPosition().getY(), packet.getBlockPosition().getZ());
                        ClientPlayerActionPacket breakPacket = new ClientPlayerActionPacket(action, pos, BlockFace.values()[packet.getFace()]);
                        session.getDownstream().getSession().send(breakPacket);
                        break;
                }
                break;
            case ITEM_RELEASE:
                if (packet.getActionType() == 0) {
                    // Followed to the Minecraft Protocol specification outlined at wiki.vg
                    ClientPlayerActionPacket releaseItemPacket = new ClientPlayerActionPacket(PlayerAction.RELEASE_USE_ITEM, new Position(0,0,0),
                            BlockFace.DOWN);
                    session.getDownstream().getSession().send(releaseItemPacket);
                }
                break;
            case ITEM_USE_ON_ENTITY:
                Entity entity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
                if (entity == null)
                    return;

                //https://wiki.vg/Protocol#Interact_Entity
                switch (packet.getActionType()) {
                    case 0: //Interact
                        Vector3f vector = packet.getClickPosition();
                        ClientPlayerInteractEntityPacket interactPacket = new ClientPlayerInteractEntityPacket((int) entity.getEntityId(),
                                InteractAction.INTERACT, Hand.MAIN_HAND);
                        ClientPlayerInteractEntityPacket interactAtPacket = new ClientPlayerInteractEntityPacket((int) entity.getEntityId(),
                                InteractAction.INTERACT_AT, vector.getX(), vector.getY(), vector.getZ(), Hand.MAIN_HAND);
                        session.getDownstream().getSession().send(interactPacket);
                        session.getDownstream().getSession().send(interactAtPacket);
                        break;
                    case 1: //Attack
                        ClientPlayerInteractEntityPacket attackPacket = new ClientPlayerInteractEntityPacket((int) entity.getEntityId(),
                                InteractAction.ATTACK);
                        session.getDownstream().getSession().send(attackPacket);
                        break;
                }
                break;
        }
    }
}
