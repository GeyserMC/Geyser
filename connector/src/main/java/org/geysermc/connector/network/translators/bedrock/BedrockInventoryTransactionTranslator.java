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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerInteractEntityPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPlaceBlockPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerUseItemPacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import com.nukkitx.protocol.bedrock.packet.InventorySlotPacket;
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.connector.entity.CommandBlockMinecartEntity;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.ItemFrameEntity;
import org.geysermc.connector.entity.living.merchant.AbstractMerchantEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.sound.EntitySoundInteractionHandler;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.utils.BlockUtils;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.concurrent.TimeUnit;

@Translator(packet = InventoryTransactionPacket.class)
public class BedrockInventoryTransactionTranslator extends PacketTranslator<InventoryTransactionPacket> {

    @Override
    public void translate(InventoryTransactionPacket packet, GeyserSession session) {
        switch (packet.getTransactionType()) {
            case NORMAL:
                Inventory inventory = session.getInventoryCache().getOpenInventory();
                if (inventory == null) inventory = session.getInventory();
                InventoryTranslator.INVENTORY_TRANSLATORS.get(inventory.getWindowType()).translateActions(session, inventory, packet.getActions());
                break;
            case INVENTORY_MISMATCH:
                Inventory inv = session.getInventoryCache().getOpenInventory();
                if (inv == null) inv = session.getInventory();
                InventoryTranslator.INVENTORY_TRANSLATORS.get(inv.getWindowType()).updateInventory(session, inv);
                InventoryUtils.updateCursor(session);
                break;
            case ITEM_USE:
                switch (packet.getActionType()) {
                    case 0:
                        // Check to make sure the client isn't spamming interaction
                        // Based on Nukkit 1.0, with changes to ensure holding down still works
                        boolean hasAlreadyClicked = System.currentTimeMillis() - session.getLastInteractionTime() < 110.0 &&
                                packet.getBlockPosition().distanceSquared(session.getLastInteractionPosition()) < 0.00001;
                        session.setLastInteractionPosition(packet.getBlockPosition());
                        if (hasAlreadyClicked) {
                            break;
                        } else {
                            // Only update the interaction time if it's valid - that way holding down still works.
                            session.setLastInteractionTime(System.currentTimeMillis());
                        }

                        // Bedrock sends block interact code for a Java entity so we send entity code back to Java
                        if (BlockTranslator.isItemFrame(packet.getBlockRuntimeId()) &&
                                session.getEntityCache().getEntityByJavaId(ItemFrameEntity.getItemFrameEntityId(session, packet.getBlockPosition())) != null) {
                            Vector3f vector = packet.getClickPosition();
                            ClientPlayerInteractEntityPacket interactPacket = new ClientPlayerInteractEntityPacket((int) ItemFrameEntity.getItemFrameEntityId(session, packet.getBlockPosition()),
                                    InteractAction.INTERACT, Hand.MAIN_HAND, session.isSneaking());
                            ClientPlayerInteractEntityPacket interactAtPacket = new ClientPlayerInteractEntityPacket((int) ItemFrameEntity.getItemFrameEntityId(session, packet.getBlockPosition()),
                                    InteractAction.INTERACT_AT, vector.getX(), vector.getY(), vector.getZ(), Hand.MAIN_HAND, session.isSneaking());
                            session.sendDownstreamPacket(interactPacket);
                            session.sendDownstreamPacket(interactAtPacket);
                            break;
                        }

                        ClientPlayerPlaceBlockPacket blockPacket = new ClientPlayerPlaceBlockPacket(
                                new Position(packet.getBlockPosition().getX(), packet.getBlockPosition().getY(), packet.getBlockPosition().getZ()),
                                BlockFace.values()[packet.getBlockFace()],
                                Hand.MAIN_HAND,
                                packet.getClickPosition().getX(), packet.getClickPosition().getY(), packet.getClickPosition().getZ(),
                                false);
                        session.sendDownstreamPacket(blockPacket);

                        // Otherwise boats will not be able to be placed in survival and buckets wont work on mobile
                        if (packet.getItemInHand() != null && packet.getItemInHand().getId() == ItemRegistry.BOAT.getBedrockId()) {
                            ClientPlayerUseItemPacket itemPacket = new ClientPlayerUseItemPacket(Hand.MAIN_HAND);
                            session.sendDownstreamPacket(itemPacket);
                        }
                        // Check actions, otherwise buckets may be activated when block inventories are accessed
                        else if (packet.getItemInHand() != null && packet.getItemInHand().getId() == ItemRegistry.BUCKET.getBedrockId()) {
                            // Let the server decide if the bucket item should change, not the client, and revert the changes the client made
                            InventorySlotPacket slotPacket = new InventorySlotPacket();
                            slotPacket.setContainerId(ContainerId.INVENTORY);
                            slotPacket.setSlot(packet.getHotbarSlot());
                            slotPacket.setItem(packet.getItemInHand());
                            session.sendUpstreamPacket(slotPacket);
                            // Delay the interaction in case the client doesn't intend to actually use the bucket
                            // See BedrockActionTranslator.java
                            session.setBucketScheduledFuture(session.getConnector().getGeneralThreadPool().schedule(() -> {
                                ClientPlayerUseItemPacket itemPacket = new ClientPlayerUseItemPacket(Hand.MAIN_HAND);
                                session.sendDownstreamPacket(itemPacket);
                            }, 5, TimeUnit.MILLISECONDS));
                        }

                        if (packet.getActions().isEmpty()) {
                            if (session.getOpPermissionLevel() >= 2 && session.getGameMode() == GameMode.CREATIVE) {
                                // Otherwise insufficient permissions
                                int blockState = BlockTranslator.getJavaBlockState(packet.getBlockRuntimeId());
                                String blockName = BlockTranslator.getJavaIdBlockMap().inverse().getOrDefault(blockState, "");
                                // In the future this can be used for structure blocks too, however not all elements
                                // are available in each GUI
                                if (blockName.contains("jigsaw")) {
                                    ContainerOpenPacket openPacket = new ContainerOpenPacket();
                                    openPacket.setBlockPosition(packet.getBlockPosition());
                                    openPacket.setId((byte) 1);
                                    openPacket.setType(ContainerType.JIGSAW_EDITOR);
                                    openPacket.setUniqueEntityId(-1);
                                    session.sendUpstreamPacket(openPacket);
                                }
                            }
                        }

                        Vector3i blockPos = BlockUtils.getBlockPosition(packet.getBlockPosition(), packet.getBlockFace());
                        ItemEntry handItem = ItemRegistry.getItem(packet.getItemInHand());
                        if (handItem.isBlock()) {
                            session.setLastBlockPlacePosition(blockPos);
                            session.setLastBlockPlacedId(handItem.getJavaIdentifier());
                        }
                        session.setInteracting(true);
                        break;
                    case 1:
                        ItemStack shieldSlot = session.getInventory().getItem(session.getInventory().getHeldItemSlot() + 36);
                        // Handled in Entity.java
                        if (shieldSlot != null && shieldSlot.getId() == ItemRegistry.SHIELD.getJavaId()) {
                            break;
                        }

                        // Handled in ITEM_USE if the item is not milk
                        if (packet.getItemInHand() != null && packet.getItemInHand().getId() == ItemRegistry.BUCKET.getBedrockId() &&
                                packet.getItemInHand().getDamage() != 1) {
                            break;
                        }

                        ClientPlayerUseItemPacket useItemPacket = new ClientPlayerUseItemPacket(Hand.MAIN_HAND);
                        session.sendDownstreamPacket(useItemPacket);
                        break;
                    case 2:
                        int blockState = session.getConnector().getWorldManager().getBlockAt(session, packet.getBlockPosition().getX(), packet.getBlockPosition().getY(), packet.getBlockPosition().getZ());
                        double blockHardness = BlockTranslator.JAVA_RUNTIME_ID_TO_HARDNESS.get(blockState);
                        if (session.getGameMode() == GameMode.CREATIVE || (session.getConnector().getConfig().isCacheChunks() && blockHardness == 0)) {
                            session.setLastBlockPlacedId(null);
                            session.setLastBlockPlacePosition(null);

                            LevelEventPacket blockBreakPacket = new LevelEventPacket();
                            blockBreakPacket.setType(LevelEventType.PARTICLE_DESTROY_BLOCK);
                            blockBreakPacket.setPosition(packet.getBlockPosition().toFloat());
                            blockBreakPacket.setData(BlockTranslator.getBedrockBlockId(blockState));
                            session.sendUpstreamPacket(blockBreakPacket);
                        }

                        if (ItemFrameEntity.positionContainsItemFrame(session, packet.getBlockPosition()) &&
                                session.getEntityCache().getEntityByJavaId(ItemFrameEntity.getItemFrameEntityId(session, packet.getBlockPosition())) != null) {
                            ClientPlayerInteractEntityPacket attackPacket = new ClientPlayerInteractEntityPacket((int) ItemFrameEntity.getItemFrameEntityId(session, packet.getBlockPosition()),
                                    InteractAction.ATTACK, session.isSneaking());
                            session.sendDownstreamPacket(attackPacket);
                            break;
                        }

                        PlayerAction action = session.getGameMode() == GameMode.CREATIVE ? PlayerAction.START_DIGGING : PlayerAction.FINISH_DIGGING;
                        Position pos = new Position(packet.getBlockPosition().getX(), packet.getBlockPosition().getY(), packet.getBlockPosition().getZ());
                        ClientPlayerActionPacket breakPacket = new ClientPlayerActionPacket(action, pos, BlockFace.values()[packet.getBlockFace()]);
                        session.sendDownstreamPacket(breakPacket);
                        break;
                }
                break;
            case ITEM_RELEASE:
                if (packet.getActionType() == 0) {
                    // Followed to the Minecraft Protocol specification outlined at wiki.vg
                    ClientPlayerActionPacket releaseItemPacket = new ClientPlayerActionPacket(PlayerAction.RELEASE_USE_ITEM, new Position(0,0,0),
                            BlockFace.DOWN);
                    session.sendDownstreamPacket(releaseItemPacket);
                }
                break;
            case ITEM_USE_ON_ENTITY:
                Entity entity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
                if (entity == null)
                    return;

                //https://wiki.vg/Protocol#Interact_Entity
                switch (packet.getActionType()) {
                    case 0: //Interact
                        if (entity instanceof CommandBlockMinecartEntity) {
                            // The UI is handled client-side on Java Edition
                            // Ensure OP permission level and gamemode is appropriate
                            if (session.getOpPermissionLevel() < 2 || session.getGameMode() != GameMode.CREATIVE) return;
                            ContainerOpenPacket openPacket = new ContainerOpenPacket();
                            openPacket.setBlockPosition(Vector3i.ZERO);
                            openPacket.setId((byte) 1);
                            openPacket.setType(ContainerType.COMMAND_BLOCK);
                            openPacket.setUniqueEntityId(entity.getGeyserId());
                            session.sendUpstreamPacket(openPacket);
                            break;
                        }
                        Vector3f vector = packet.getClickPosition();
                        ClientPlayerInteractEntityPacket interactPacket = new ClientPlayerInteractEntityPacket((int) entity.getEntityId(),
                                InteractAction.INTERACT, Hand.MAIN_HAND, session.isSneaking());
                        ClientPlayerInteractEntityPacket interactAtPacket = new ClientPlayerInteractEntityPacket((int) entity.getEntityId(),
                                InteractAction.INTERACT_AT, vector.getX(), vector.getY(), vector.getZ(), Hand.MAIN_HAND, session.isSneaking());
                        session.sendDownstreamPacket(interactPacket);
                        session.sendDownstreamPacket(interactAtPacket);

                        EntitySoundInteractionHandler.handleEntityInteraction(session, vector, entity);

                        if (entity instanceof AbstractMerchantEntity) {
                            session.setLastInteractedVillagerEid(packet.getRuntimeEntityId());
                        }
                        break;
                    case 1: //Attack
                        if (entity.getEntityType() == EntityType.ENDER_DRAGON) {
                            // Redirects the attack to its body entity, this only happens when
                            // attacking the underbelly of the ender dragon
                            ClientPlayerInteractEntityPacket attackPacket = new ClientPlayerInteractEntityPacket((int) entity.getEntityId() + 3,
                                    InteractAction.ATTACK, session.isSneaking());
                            session.sendDownstreamPacket(attackPacket);
                        } else {
                            ClientPlayerInteractEntityPacket attackPacket = new ClientPlayerInteractEntityPacket((int) entity.getEntityId(),
                                    InteractAction.ATTACK, session.isSneaking());
                            session.sendDownstreamPacket(attackPacket);
                        }
                        break;
                }
                break;
        }
    }
}
