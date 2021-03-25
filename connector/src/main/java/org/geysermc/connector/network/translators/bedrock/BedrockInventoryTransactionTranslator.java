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

package org.geysermc.connector.network.translators.bedrock;

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
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlags;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.inventory.InventorySource;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.connector.entity.CommandBlockMinecartEntity;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.ItemFrameEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.inventory.GeyserItemStack;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.sound.EntitySoundInteractionHandler;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.utils.BlockUtils;

import java.util.concurrent.TimeUnit;

/**
 * BedrockInventoryTransactionTranslator handles most interactions between the client and the world,
 * or the client and their inventory.
 */
@Translator(packet = InventoryTransactionPacket.class)
public class BedrockInventoryTransactionTranslator extends PacketTranslator<InventoryTransactionPacket> {

    private static final float MAXIMUM_BLOCK_PLACING_DISTANCE = 64f;
    private static final int CREATIVE_EYE_HEIGHT_PLACE_DISTANCE = 49;
    private static final int SURVIVAL_EYE_HEIGHT_PLACE_DISTANCE = 36;
    private static final float MAXIMUM_BLOCK_DESTROYING_DISTANCE = 36f;

    @Override
    public void translate(InventoryTransactionPacket packet, GeyserSession session) {
        // Send book updates before opening inventories
        session.getBookEditCache().checkForSend();

        switch (packet.getTransactionType()) {
            case NORMAL:
                if (packet.getActions().size() == 2) {
                    InventoryActionData worldAction = packet.getActions().get(0);
                    InventoryActionData containerAction = packet.getActions().get(1);
                    if (worldAction.getSource().getType() == InventorySource.Type.WORLD_INTERACTION
                            && worldAction.getSource().getFlag() == InventorySource.Flag.DROP_ITEM) {
                        session.addInventoryTask(() -> {
                            if (session.getPlayerInventory().getHeldItemSlot() != containerAction.getSlot() ||
                                    session.getPlayerInventory().getItemInHand().isEmpty()) {
                                return;
                            }

                            boolean dropAll = worldAction.getToItem().getCount() > 1;
                            ClientPlayerActionPacket dropAllPacket = new ClientPlayerActionPacket(
                                    dropAll ? PlayerAction.DROP_ITEM_STACK : PlayerAction.DROP_ITEM,
                                    BlockUtils.POSITION_ZERO,
                                    BlockFace.DOWN
                            );
                            session.sendDownstreamPacket(dropAllPacket);

                            if (dropAll) {
                                session.getPlayerInventory().setItemInHand(GeyserItemStack.EMPTY);
                            } else {
                                session.getPlayerInventory().getItemInHand().sub(1);
                            }
                        });
                    }
                }
                break;
            case INVENTORY_MISMATCH:
                break;
            case ITEM_USE:
                switch (packet.getActionType()) {
                    case 0:
                        // Check to make sure the client isn't spamming interaction
                        // Based on Nukkit 1.0, with changes to ensure holding down still works
                        boolean hasAlreadyClicked = System.currentTimeMillis() - session.getLastInteractionTime() < 110.0 &&
                                packet.getBlockPosition().distanceSquared(session.getLastInteractionBlockPosition()) < 0.00001;
                        session.setLastInteractionBlockPosition(packet.getBlockPosition());
                        session.setLastInteractionPlayerPosition(session.getPlayerEntity().getPosition());
                        if (hasAlreadyClicked) {
                            break;
                        } else {
                            // Only update the interaction time if it's valid - that way holding down still works.
                            session.setLastInteractionTime(System.currentTimeMillis());
                        }

                        // Bedrock sends block interact code for a Java entity so we send entity code back to Java
                        if (session.getBlockTranslator().isItemFrame(packet.getBlockRuntimeId()) &&
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

                        Vector3i blockPos = BlockUtils.getBlockPosition(packet.getBlockPosition(), packet.getBlockFace());
                        /*
                        Checks to ensure that the range will be accepted by the server.
                        "Not in range" doesn't refer to how far a vanilla client goes (that's a whole other mess),
                        but how much a server will accept from the client maximum
                         */
                        // CraftBukkit+ check - see https://github.com/PaperMC/Paper/blob/458db6206daae76327a64f4e2a17b67a7e38b426/Spigot-Server-Patches/0532-Move-range-check-for-block-placing-up.patch
                        Vector3f playerPosition = session.getPlayerEntity().getPosition();
                        EntityFlags flags = session.getPlayerEntity().getMetadata().getFlags();

                        // Adjust position for current eye height
                        if (flags.getFlag(EntityFlag.SNEAKING)) {
                            playerPosition = playerPosition.sub(0, (EntityType.PLAYER.getOffset() - 1.27f), 0);
                        } else if (flags.getFlag(EntityFlag.SWIMMING) || flags.getFlag(EntityFlag.GLIDING) || flags.getFlag(EntityFlag.DAMAGE_NEARBY_MOBS)) {
                            // Swimming, gliding, or using the trident spin attack
                            playerPosition = playerPosition.sub(0, (EntityType.PLAYER.getOffset() - 0.4f), 0);
                        } else if (flags.getFlag(EntityFlag.SLEEPING)) {
                            playerPosition = playerPosition.sub(0, (EntityType.PLAYER.getOffset() - 0.2f), 0);
                        } // else, we don't have to modify the position

                        float diffX = playerPosition.getX() - packet.getBlockPosition().getX();
                        float diffY = playerPosition.getY() - packet.getBlockPosition().getY();
                        float diffZ = playerPosition.getZ() - packet.getBlockPosition().getZ();
                        if (((diffX * diffX) + (diffY * diffY) + (diffZ * diffZ)) >
                                (session.getGameMode().equals(GameMode.CREATIVE) ? CREATIVE_EYE_HEIGHT_PLACE_DISTANCE : SURVIVAL_EYE_HEIGHT_PLACE_DISTANCE)) {
                            restoreCorrectBlock(session, blockPos, packet);
                            return;
                        }

                        // Vanilla check
                        if (!(session.getPlayerEntity().getPosition().sub(0, EntityType.PLAYER.getOffset(), 0)
                                .distanceSquared(packet.getBlockPosition().toFloat().add(0.5f, 0.5f, 0.5f)) < MAXIMUM_BLOCK_PLACING_DISTANCE)) {
                            // The client thinks that its blocks have been successfully placed. Restore the server's blocks instead.
                            restoreCorrectBlock(session, blockPos, packet);
                            return;
                        }
                        /*
                        Block place checks end - client is good to go
                         */

                        ClientPlayerPlaceBlockPacket blockPacket = new ClientPlayerPlaceBlockPacket(
                                new Position(packet.getBlockPosition().getX(), packet.getBlockPosition().getY(), packet.getBlockPosition().getZ()),
                                BlockFace.values()[packet.getBlockFace()],
                                Hand.MAIN_HAND,
                                packet.getClickPosition().getX(), packet.getClickPosition().getY(), packet.getClickPosition().getZ(),
                                false);
                        session.sendDownstreamPacket(blockPacket);

                        // Otherwise boats will not be able to be placed in survival and buckets won't work on mobile
                        if (packet.getItemInHand() != null && ItemRegistry.BOATS.contains(packet.getItemInHand().getId())) {
                            ClientPlayerUseItemPacket itemPacket = new ClientPlayerUseItemPacket(Hand.MAIN_HAND);
                            session.sendDownstreamPacket(itemPacket);
                        }
                        // Check actions, otherwise buckets may be activated when block inventories are accessed
                        else if (packet.getItemInHand() != null && ItemRegistry.BUCKETS.contains(packet.getItemInHand().getId())) {
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
                                int blockState = session.getBlockTranslator().getJavaBlockState(packet.getBlockRuntimeId());
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

                        ItemEntry handItem = ItemRegistry.getItem(packet.getItemInHand());
                        if (handItem.isBlock()) {
                            session.setLastBlockPlacePosition(blockPos);
                            session.setLastBlockPlacedId(handItem.getJavaIdentifier());
                        }
                        session.setInteracting(true);
                        break;
                    case 1:
                        // Handled in Entity.java
                        if (session.getPlayerInventory().getItemInHand().getJavaId() == ItemRegistry.SHIELD.getJavaId()) {
                            break;
                        }

                        // Handled in ITEM_USE if the item is not milk
                        if (packet.getItemInHand() != null && ItemRegistry.BUCKETS.contains(packet.getItemInHand().getId()) &&
                                packet.getItemInHand().getId() != ItemRegistry.MILK_BUCKET.getBedrockId()) {
                            break;
                        }

                        ClientPlayerUseItemPacket useItemPacket = new ClientPlayerUseItemPacket(Hand.MAIN_HAND);
                        session.sendDownstreamPacket(useItemPacket);
                        break;
                    case 2:
                        int blockState = session.getGameMode() == GameMode.CREATIVE ?
                                session.getConnector().getWorldManager().getBlockAt(session, packet.getBlockPosition()) : session.getBreakingBlock();

                        session.setLastBlockPlacedId(null);
                        session.setLastBlockPlacePosition(null);

                        // Same deal with vanilla block placing as above.
                        // This is working out the distance using 3d Pythagoras and the extra value added to the Y is the sneaking height of a java player.
                        playerPosition = session.getPlayerEntity().getPosition();
                        Vector3f floatBlockPosition = packet.getBlockPosition().toFloat();
                        diffX = playerPosition.getX() - (floatBlockPosition.getX() + 0.5f);
                        diffY = (playerPosition.getY() - EntityType.PLAYER.getOffset()) - (floatBlockPosition.getY() + 0.5f) + 1.5f;
                        diffZ = playerPosition.getZ() - (floatBlockPosition.getZ() + 0.5f);
                        float distanceSquared = diffX * diffX + diffY * diffY + diffZ * diffZ;
                        if (distanceSquared > MAXIMUM_BLOCK_DESTROYING_DISTANCE) {
                            restoreCorrectBlock(session, packet.getBlockPosition(), packet);
                            return;
                        }

                        LevelEventPacket blockBreakPacket = new LevelEventPacket();
                        blockBreakPacket.setType(LevelEventType.PARTICLE_DESTROY_BLOCK);
                        blockBreakPacket.setPosition(packet.getBlockPosition().toFloat());
                        blockBreakPacket.setData(session.getBlockTranslator().getBedrockBlockId(blockState));
                        session.sendUpstreamPacket(blockBreakPacket);
                        session.setBreakingBlock(BlockTranslator.JAVA_AIR_ID);

                        long frameEntityId = ItemFrameEntity.getItemFrameEntityId(session, packet.getBlockPosition());
                        if (frameEntityId != -1 && session.getEntityCache().getEntityByJavaId(frameEntityId) != null) {
                            ClientPlayerInteractEntityPacket attackPacket = new ClientPlayerInteractEntityPacket((int) frameEntityId, InteractAction.ATTACK, session.isSneaking());
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
                    ClientPlayerActionPacket releaseItemPacket = new ClientPlayerActionPacket(PlayerAction.RELEASE_USE_ITEM, BlockUtils.POSITION_ZERO,
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

    /**
     * Restore the correct block state from the server without updating the chunk cache.
     *
     * @param session the session of the Bedrock client
     * @param blockPos the block position to restore
     */
    private void restoreCorrectBlock(GeyserSession session, Vector3i blockPos, InventoryTransactionPacket packet) {
        int javaBlockState = session.getConnector().getWorldManager().getBlockAt(session, blockPos);
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(blockPos);
        updateBlockPacket.setRuntimeId(session.getBlockTranslator().getBedrockBlockId(javaBlockState));
        updateBlockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(updateBlockPacket);

        UpdateBlockPacket updateWaterPacket = new UpdateBlockPacket();
        updateWaterPacket.setDataLayer(1);
        updateWaterPacket.setBlockPosition(blockPos);
        updateWaterPacket.setRuntimeId(BlockTranslator.isWaterlogged(javaBlockState) ? session.getBlockTranslator().getBedrockWaterId() : session.getBlockTranslator().getBedrockAirId());
        updateWaterPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(updateWaterPacket);

        // Reset the item in hand to prevent "missing" blocks
        InventorySlotPacket slotPacket = new InventorySlotPacket();
        slotPacket.setContainerId(ContainerId.INVENTORY);
        slotPacket.setSlot(packet.getHotbarSlot());
        slotPacket.setItem(packet.getItemInHand());
        session.sendUpstreamPacket(slotPacket);
    }
}
