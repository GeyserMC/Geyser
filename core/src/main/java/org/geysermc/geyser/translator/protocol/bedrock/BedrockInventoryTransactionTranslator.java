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

package org.geysermc.geyser.translator.protocol.bedrock;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.InteractAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;
import com.nukkitx.math.vector.Vector3d;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.inventory.InventorySource;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.data.inventory.LegacySetItemSlotData;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import com.nukkitx.protocol.bedrock.packet.InventoryTransactionPacket;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import com.nukkitx.protocol.bedrock.packet.UpdateBlockPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.inventory.click.Click;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.geyser.util.CooldownUtils;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InventoryUtils;

import java.util.List;
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
    public void translate(GeyserSession session, InventoryTransactionPacket packet) {
        // Send book updates before opening inventories
        session.getBookEditCache().checkForSend();

        ItemMappings mappings = session.getItemMappings();

        switch (packet.getTransactionType()) {
            case NORMAL:
                if (packet.getActions().size() == 2) {
                    InventoryActionData worldAction = packet.getActions().get(0);
                    InventoryActionData containerAction = packet.getActions().get(1);
                    if (worldAction.getSource().getType() == InventorySource.Type.WORLD_INTERACTION
                            && worldAction.getSource().getFlag() == InventorySource.Flag.DROP_ITEM) {
                        boolean dropAll = worldAction.getToItem().getCount() > 1;

                        if (session.getPlayerInventory().getHeldItemSlot() != containerAction.getSlot()) {
                            // Dropping an item that you don't have selected isn't supported in Java, but we can workaround it with an inventory hack
                            PlayerInventory inventory = session.getPlayerInventory();
                            int hotbarSlot = inventory.getOffsetForHotbar(containerAction.getSlot());
                            Click clickType = dropAll ? Click.DROP_ALL : Click.DROP_ONE;
                            Int2ObjectMap<ItemStack> changedItem;
                            if (dropAll) {
                                inventory.setItem(hotbarSlot, GeyserItemStack.EMPTY, session);
                                changedItem = Int2ObjectMaps.singleton(hotbarSlot, null);
                            } else {
                                GeyserItemStack itemStack = inventory.getItem(hotbarSlot);
                                if (itemStack.isEmpty()) {
                                    return;
                                }
                                itemStack.sub(1);
                                changedItem = Int2ObjectMaps.singleton(hotbarSlot, itemStack.getItemStack());
                            }
                            ServerboundContainerClickPacket dropPacket = new ServerboundContainerClickPacket(
                                    inventory.getJavaId(), inventory.getStateId(), hotbarSlot, clickType.actionType, clickType.action,
                                    inventory.getCursor().getItemStack(), changedItem);
                            session.sendDownstreamPacket(dropPacket);
                            return;
                        }
                        if (session.getPlayerInventory().getItemInHand().isEmpty()) {
                            return;
                        }

                        ServerboundPlayerActionPacket dropPacket = new ServerboundPlayerActionPacket(
                                dropAll ? PlayerAction.DROP_ITEM_STACK : PlayerAction.DROP_ITEM,
                                Vector3i.ZERO,
                                Direction.DOWN,
                                0
                        );
                        session.sendDownstreamPacket(dropPacket);

                        if (dropAll) {
                            session.getPlayerInventory().setItemInHand(GeyserItemStack.EMPTY);
                        } else {
                            session.getPlayerInventory().getItemInHand().sub(1);
                        }
                    }
                }
                break;
            case INVENTORY_MISMATCH:
                break;
            case ITEM_USE:
                switch (packet.getActionType()) {
                    case 0 -> {
                        final Vector3i packetBlockPosition = packet.getBlockPosition();
                        Vector3i blockPos = BlockUtils.getBlockPosition(packetBlockPosition, packet.getBlockFace());

                        if (session.getGeyser().getConfig().isDisableBedrockScaffolding()) {
                            float yaw = session.getPlayerEntity().getYaw();
                            boolean isGodBridging = switch (packet.getBlockFace()) {
                                case 2 -> yaw <= -135f || yaw > 135f;
                                case 3 -> yaw <= 45f && yaw > -45f;
                                case 4 -> yaw > 45f && yaw <= 135f;
                                case 5 -> yaw <= -45f && yaw > -135f;
                                default -> false;
                            };
                            if (isGodBridging) {
                                restoreCorrectBlock(session, blockPos, packet);
                                return;
                            }
                        }

                        // Check to make sure the client isn't spamming interaction
                        // Based on Nukkit 1.0, with changes to ensure holding down still works
                        boolean hasAlreadyClicked = System.currentTimeMillis() - session.getLastInteractionTime() < 110.0 &&
                                packetBlockPosition.distanceSquared(session.getLastInteractionBlockPosition()) < 0.00001;
                        session.setLastInteractionBlockPosition(packetBlockPosition);
                        session.setLastInteractionPlayerPosition(session.getPlayerEntity().getPosition());
                        if (hasAlreadyClicked) {
                            break;
                        } else {
                            // Only update the interaction time if it's valid - that way holding down still works.
                            session.setLastInteractionTime(System.currentTimeMillis());
                        }

                        if (isIncorrectHeldItem(session, packet)) {
                            restoreCorrectBlock(session, blockPos, packet);
                            return;
                        }

                        // Bedrock sends block interact code for a Java entity so we send entity code back to Java
                        if (session.getBlockMappings().isItemFrame(packet.getBlockRuntimeId())) {
                            Entity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, packet.getBlockPosition());
                            if (itemFrameEntity != null) {
                                processEntityInteraction(session, packet, itemFrameEntity);
                                break;
                            }
                        }

                        /*
                        Checks to ensure that the range will be accepted by the server.
                        "Not in range" doesn't refer to how far a vanilla client goes (that's a whole other mess),
                        but how much a server will accept from the client maximum
                         */
                        // Blocks cannot be placed or destroyed outside of the world border
                        if (!session.getWorldBorder().isInsideBorderBoundaries()) {
                            restoreCorrectBlock(session, blockPos, packet);
                            return;
                        }

                        // CraftBukkit+ check - see https://github.com/PaperMC/Paper/blob/458db6206daae76327a64f4e2a17b67a7e38b426/Spigot-Server-Patches/0532-Move-range-check-for-block-placing-up.patch
                        Vector3f playerPosition = session.getPlayerEntity().getPosition();
                        playerPosition = playerPosition.down(EntityDefinitions.PLAYER.offset() - session.getEyeHeight());

                        boolean creative = session.getGameMode() == GameMode.CREATIVE;

                        float diffX = playerPosition.getX() - packetBlockPosition.getX();
                        float diffY = playerPosition.getY() - packetBlockPosition.getY();
                        float diffZ = playerPosition.getZ() - packetBlockPosition.getZ();
                        if (((diffX * diffX) + (diffY * diffY) + (diffZ * diffZ)) >
                                (creative ? CREATIVE_EYE_HEIGHT_PLACE_DISTANCE : SURVIVAL_EYE_HEIGHT_PLACE_DISTANCE)) {
                            restoreCorrectBlock(session, blockPos, packet);
                            return;
                        }

                        double clickPositionFullX = (double) packetBlockPosition.getX() + (double) packet.getClickPosition().getX();
                        double clickPositionFullY = (double) packetBlockPosition.getY() + (double) packet.getClickPosition().getY();
                        double clickPositionFullZ = (double) packetBlockPosition.getZ() + (double) packet.getClickPosition().getZ();

                        // More recent Paper check - https://github.com/PaperMC/Paper/blob/87e11bf7fdf48ecdf3e1cae383c368b9b61d7df9/patches/server/0470-Move-range-check-for-block-placing-up.patch
                        double clickDiffX = playerPosition.getX() - clickPositionFullX;
                        double clickDiffY = playerPosition.getY() - clickPositionFullY;
                        double clickDiffZ = playerPosition.getZ() - clickPositionFullZ;
                        if (((clickDiffX * clickDiffX) + (clickDiffY * clickDiffY) + (clickDiffZ * clickDiffZ)) >
                                (creative ? CREATIVE_EYE_HEIGHT_PLACE_DISTANCE : SURVIVAL_EYE_HEIGHT_PLACE_DISTANCE)) {
                            restoreCorrectBlock(session, blockPos, packet);
                            return;
                        }

                        Vector3f blockCenter = Vector3f.from(packetBlockPosition.getX() + 0.5f, packetBlockPosition.getY() + 0.5f, packetBlockPosition.getZ() + 0.5f);
                        // Vanilla check
                        if (!(session.getPlayerEntity().getPosition().sub(0, EntityDefinitions.PLAYER.offset(), 0)
                                .distanceSquared(blockCenter) < MAXIMUM_BLOCK_PLACING_DISTANCE)) {
                            // The client thinks that its blocks have been successfully placed. Restore the server's blocks instead.
                            restoreCorrectBlock(session, blockPos, packet);
                            return;
                        }

                        // More recent vanilla check (as of 1.18.2)
                        double clickDistanceX = clickPositionFullX - blockCenter.getX();
                        double clickDistanceY = clickPositionFullY - blockCenter.getY();
                        double clickDistanceZ = clickPositionFullZ - blockCenter.getZ();
                        if (!(Math.abs(clickDistanceX) < 1.0000001D && Math.abs(clickDistanceY) < 1.0000001D && Math.abs(clickDistanceZ) < 1.0000001D)) {
                            restoreCorrectBlock(session, blockPos, packet);
                            return;
                        }
                        /*
                        Block place checks end - client is good to go
                         */

                        if (packet.getItemInHand() != null && session.getItemMappings().getSpawnEggIds().contains(packet.getItemInHand().getId())) {
                            int blockState = session.getGeyser().getWorldManager().getBlockAt(session, packet.getBlockPosition());
                            if (blockState == BlockStateValues.JAVA_WATER_ID) {
                                // Otherwise causes multiple mobs to spawn - just send a use item packet
                                useItem(session, packet, blockState);
                                break;
                            }
                        }

                        ServerboundUseItemOnPacket blockPacket = new ServerboundUseItemOnPacket(
                                packet.getBlockPosition(),
                                Direction.VALUES[packet.getBlockFace()],
                                Hand.MAIN_HAND,
                                packet.getClickPosition().getX(), packet.getClickPosition().getY(), packet.getClickPosition().getZ(),
                                false,
                                session.getWorldCache().nextPredictionSequence());
                        session.sendDownstreamPacket(blockPacket);

                        if (packet.getItemInHand() != null) {
                            int itemId = packet.getItemInHand().getId();
                            int blockState = session.getGeyser().getWorldManager().getBlockAt(session, packet.getBlockPosition());
                            // Otherwise boats will not be able to be placed in survival and buckets, lily pads, frogspawn, and glass bottles won't work on mobile
                            if (session.getItemMappings().getBoatIds().contains(itemId) ||
                                    itemId == session.getItemMappings().getStoredItems().lilyPad() ||
                                    itemId == session.getItemMappings().getStoredItems().frogspawn()) {
                                useItem(session, packet, blockState);
                            } else if (itemId == session.getItemMappings().getStoredItems().glassBottle()) {
                                if (!session.isSneaking() && BlockStateValues.isCauldron(blockState) && !BlockStateValues.isNonWaterCauldron(blockState)) {
                                    // ServerboundUseItemPacket is not sent for water cauldrons and glass bottles
                                    return;
                                }
                                useItem(session, packet, blockState);
                            } else if (session.getItemMappings().getBucketIds().contains(itemId)) {
                                // Don't send ServerboundUseItemPacket for powder snow buckets
                                if (itemId != session.getItemMappings().getStoredItems().powderSnowBucket().getBedrockId()) {
                                    if (!session.isSneaking() && BlockStateValues.isCauldron(blockState)) {
                                        // ServerboundUseItemPacket is not sent for cauldrons and buckets
                                        return;
                                    }
                                    session.setPlacedBucket(useItem(session, packet, blockState));
                                } else {
                                    session.setPlacedBucket(true);
                                }
                            }
                        }

                        if (packet.getActions().isEmpty()) {
                            if (session.getOpPermissionLevel() >= 2 && session.getGameMode() == GameMode.CREATIVE) {
                                // Otherwise insufficient permissions
                                if (session.getBlockMappings().getJigsawStateIds().contains(packet.getBlockRuntimeId())) {
                                    ContainerOpenPacket openPacket = new ContainerOpenPacket();
                                    openPacket.setBlockPosition(packet.getBlockPosition());
                                    openPacket.setId((byte) 1);
                                    openPacket.setType(ContainerType.JIGSAW_EDITOR);
                                    openPacket.setUniqueEntityId(-1);
                                    session.sendUpstreamPacket(openPacket);
                                }
                            }
                        }
                        ItemMapping handItem = session.getPlayerInventory().getItemInHand().getMapping(session);
                        if (handItem.isBlock()) {
                            session.setLastBlockPlacePosition(blockPos);
                            session.setLastBlockPlacedId(handItem.getJavaIdentifier());
                        }
                        session.setInteracting(true);
                    }
                    case 1 -> {
                        if (isIncorrectHeldItem(session, packet)) {
                            InventoryTranslator.PLAYER_INVENTORY_TRANSLATOR.updateSlot(session, session.getPlayerInventory(), session.getPlayerInventory().getOffsetForHotbar(packet.getHotbarSlot()));
                            break;
                        }

                        // Handled when sneaking
                        if (session.getPlayerInventory().getItemInHand().getJavaId() == mappings.getStoredItems().shield().getJavaId()) {
                            break;
                        }

                        // Handled in ITEM_USE if the item is not milk
                        if (packet.getItemInHand() != null) {
                            if (session.getItemMappings().getBucketIds().contains(packet.getItemInHand().getId()) &&
                                    packet.getItemInHand().getId() != session.getItemMappings().getStoredItems().milkBucket().getBedrockId()) {
                                // Handled in case 0 if the item is not milk
                                break;
                            } else if (session.getItemMappings().getSpawnEggIds().contains(packet.getItemInHand().getId())) {
                                // Handled in case 0
                                break;
                            } else if (packet.getItemInHand().getId() == session.getItemMappings().getStoredItems().glassBottle()) {
                                // Handled in case 0
                                break;
                            }
                        }

                        ServerboundUseItemPacket useItemPacket = new ServerboundUseItemPacket(Hand.MAIN_HAND, session.getWorldCache().nextPredictionSequence());
                        session.sendDownstreamPacket(useItemPacket);

                        List<LegacySetItemSlotData> legacySlots = packet.getLegacySlots();
                        if (packet.getActions().size() == 1 && legacySlots.size() > 0) {
                            InventoryActionData actionData = packet.getActions().get(0);
                            LegacySetItemSlotData slotData = legacySlots.get(0);
                            if (slotData.getContainerId() == 6 && actionData.getToItem().getId() != 0) {
                                // The player is trying to swap out an armor piece that already has an item in it
                                if (session.getGeyser().getConfig().isAlwaysQuickChangeArmor()) {
                                    // Java doesn't know when a player is in its own inventory and not, so we
                                    // can abuse this feature to send a swap inventory packet
                                    int bedrockHotbarSlot = packet.getHotbarSlot();
                                    Click click = InventoryUtils.getClickForHotbarSwap(bedrockHotbarSlot);
                                    if (click != null && slotData.getSlots().length != 0) {
                                        Inventory playerInventory = session.getPlayerInventory();
                                        // Bedrock sends us the index of the slot in the armor container; armor in Java
                                        // Edition is offset by 5 in the player inventory
                                        int armorSlot = slotData.getSlots()[0] + 5;
                                        GeyserItemStack armorSlotItem = playerInventory.getItem(armorSlot);
                                        GeyserItemStack hotbarItem = playerInventory.getItem(playerInventory.getOffsetForHotbar(bedrockHotbarSlot));
                                        playerInventory.setItem(armorSlot, hotbarItem, session);
                                        playerInventory.setItem(bedrockHotbarSlot, armorSlotItem, session);

                                        Int2ObjectMap<ItemStack> changedSlots = new Int2ObjectOpenHashMap<>(2);
                                        changedSlots.put(armorSlot, hotbarItem.getItemStack());
                                        changedSlots.put(bedrockHotbarSlot, armorSlotItem.getItemStack());

                                        ServerboundContainerClickPacket clickPacket = new ServerboundContainerClickPacket(
                                                playerInventory.getJavaId(), playerInventory.getStateId(), armorSlot,
                                                click.actionType, click.action, null, changedSlots);
                                        session.sendDownstreamPacket(clickPacket);
                                    }
                                } else {
                                    // Disallowed; let's revert
                                    session.getInventoryTranslator().updateInventory(session, session.getPlayerInventory());
                                }
                            }
                        }
                    }
                    case 2 -> {
                        int blockState = session.getGameMode() == GameMode.CREATIVE ?
                                session.getGeyser().getWorldManager().getBlockAt(session, packet.getBlockPosition()) : session.getBreakingBlock();

                        session.setLastBlockPlacedId(null);
                        session.setLastBlockPlacePosition(null);

                        // Same deal with vanilla block placing as above.
                        if (!session.getWorldBorder().isInsideBorderBoundaries()) {
                            restoreCorrectBlock(session, packet.getBlockPosition(), packet);
                            return;
                        }

                        // This is working out the distance using 3d Pythagoras and the extra value added to the Y is the sneaking height of a java player.
                        Vector3f playerPosition = session.getPlayerEntity().getPosition();
                        Vector3f floatBlockPosition = packet.getBlockPosition().toFloat();
                        float diffX = playerPosition.getX() - (floatBlockPosition.getX() + 0.5f);
                        float diffY = (playerPosition.getY() - EntityDefinitions.PLAYER.offset()) - (floatBlockPosition.getY() + 0.5f) + 1.5f;
                        float diffZ = playerPosition.getZ() - (floatBlockPosition.getZ() + 0.5f);
                        float distanceSquared = diffX * diffX + diffY * diffY + diffZ * diffZ;
                        if (distanceSquared > MAXIMUM_BLOCK_DESTROYING_DISTANCE) {
                            restoreCorrectBlock(session, packet.getBlockPosition(), packet);
                            return;
                        }

                        int sequence = session.getWorldCache().nextPredictionSequence();
                        session.getWorldCache().markPositionInSequence(packet.getBlockPosition());
                        // -1 means we don't know what block they're breaking
                        if (blockState == -1) {
                            blockState = BlockStateValues.JAVA_AIR_ID;
                        }

                        LevelEventPacket blockBreakPacket = new LevelEventPacket();
                        blockBreakPacket.setType(LevelEventType.PARTICLE_DESTROY_BLOCK);
                        blockBreakPacket.setPosition(packet.getBlockPosition().toFloat());
                        blockBreakPacket.setData(session.getBlockMappings().getBedrockBlockId(blockState));
                        session.sendUpstreamPacket(blockBreakPacket);
                        session.setBreakingBlock(-1);

                        Entity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, packet.getBlockPosition());
                        if (itemFrameEntity != null) {
                            ServerboundInteractPacket attackPacket = new ServerboundInteractPacket(itemFrameEntity.getEntityId(),
                                    InteractAction.ATTACK, session.isSneaking());
                            session.sendDownstreamPacket(attackPacket);
                            break;
                        }

                        PlayerAction action = session.getGameMode() == GameMode.CREATIVE ? PlayerAction.START_DIGGING : PlayerAction.FINISH_DIGGING;
                        ServerboundPlayerActionPacket breakPacket = new ServerboundPlayerActionPacket(action, packet.getBlockPosition(), Direction.VALUES[packet.getBlockFace()], sequence);
                        session.sendDownstreamPacket(breakPacket);
                    }
                }
                break;
            case ITEM_RELEASE:
                if (packet.getActionType() == 0) {
                    // Followed to the Minecraft Protocol specification outlined at wiki.vg
                    ServerboundPlayerActionPacket releaseItemPacket = new ServerboundPlayerActionPacket(PlayerAction.RELEASE_USE_ITEM, Vector3i.ZERO,
                            Direction.DOWN, 0);
                    session.sendDownstreamPacket(releaseItemPacket);
                }
                break;
            case ITEM_USE_ON_ENTITY:
                Entity entity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
                if (entity == null)
                    return;

                //https://wiki.vg/Protocol#Interact_Entity
                switch (packet.getActionType()) {
                    case 0 -> processEntityInteraction(session, packet, entity); // Interact
                    case 1 -> { // Attack
                        int entityId;
                        if (entity.getDefinition() == EntityDefinitions.ENDER_DRAGON) {
                            // Redirects the attack to its body entity, this only happens when
                            // attacking the underbelly of the ender dragon
                            entityId = entity.getEntityId() + 3;
                        } else {
                            entityId = entity.getEntityId();
                        }
                        ServerboundInteractPacket attackPacket = new ServerboundInteractPacket(entityId,
                                InteractAction.ATTACK, session.isSneaking());
                        session.sendDownstreamPacket(attackPacket);

                        // Since 1.19.10, LevelSoundEventPackets are no longer sent by the client when attacking entities
                        CooldownUtils.sendCooldown(session);
                    }
                }
                break;
        }
    }

    private void processEntityInteraction(GeyserSession session, InventoryTransactionPacket packet, Entity entity) {
        Vector3f entityPosition = entity.getPosition();
        if (!session.getWorldBorder().isInsideBorderBoundaries(entityPosition)) {
            // No transaction is able to go through (as of Java Edition 1.18.1)
            return;
        }

        Vector3f clickPosition = packet.getClickPosition().sub(entityPosition);
        boolean isSpectator = session.getGameMode() == GameMode.SPECTATOR;
        for (Hand hand : EntityUtils.HANDS) {
            session.sendDownstreamPacket(new ServerboundInteractPacket(entity.getEntityId(),
                    InteractAction.INTERACT_AT, clickPosition.getX(), clickPosition.getY(), clickPosition.getZ(),
                    hand, session.isSneaking()));

            InteractionResult result;
            if (isSpectator) {
                result = InteractionResult.PASS;
            } else {
                result = entity.interactAt(hand);
            }

            if (!result.consumesAction()) {
                session.sendDownstreamPacket(new ServerboundInteractPacket(entity.getEntityId(),
                        InteractAction.INTERACT, hand, session.isSneaking()));
                if (!isSpectator) {
                    result = entity.interact(hand);
                }
            }

            if (result.consumesAction()) {
                if (result.shouldSwing() && hand == Hand.OFF_HAND) {
                    // Currently, Bedrock will send us the arm swing packet in most cases. But it won't for offhand.
                    session.sendDownstreamPacket(new ServerboundSwingPacket(hand));
                    // Note here to look into sending the animation packet back to Bedrock
                }
                return;
            }
        }
    }

    /**
     * Restore the correct block state from the server without updating the chunk cache.
     *
     * @param session the session of the Bedrock client
     * @param blockPos the block position to restore
     */
    private void restoreCorrectBlock(GeyserSession session, Vector3i blockPos, InventoryTransactionPacket packet) {
        int javaBlockState = session.getGeyser().getWorldManager().getBlockAt(session, blockPos);
        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(blockPos);
        updateBlockPacket.setRuntimeId(session.getBlockMappings().getBedrockBlockId(javaBlockState));
        updateBlockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(updateBlockPacket);

        UpdateBlockPacket updateWaterPacket = new UpdateBlockPacket();
        updateWaterPacket.setDataLayer(1);
        updateWaterPacket.setBlockPosition(blockPos);
        updateWaterPacket.setRuntimeId(BlockRegistries.WATERLOGGED.get().contains(javaBlockState) ? session.getBlockMappings().getBedrockWaterId() : session.getBlockMappings().getBedrockAirId());
        updateWaterPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(updateWaterPacket);

        // Reset the item in hand to prevent "missing" blocks
        InventoryTranslator.PLAYER_INVENTORY_TRANSLATOR.updateSlot(session, session.getPlayerInventory(), session.getPlayerInventory().getOffsetForHotbar(packet.getHotbarSlot()));
    }

    private boolean isIncorrectHeldItem(GeyserSession session, InventoryTransactionPacket packet) {
        int javaSlot = session.getPlayerInventory().getOffsetForHotbar(packet.getHotbarSlot());
        int expectedItemId = ItemTranslator.getBedrockItemId(session, session.getPlayerInventory().getItem(javaSlot));
        int heldItemId = packet.getItemInHand() == null ? ItemData.AIR.getId() : packet.getItemInHand().getId();

        if (expectedItemId != heldItemId) {
            session.getGeyser().getLogger().debug(session.bedrockUsername() + "'s held item has desynced! Expected: " + expectedItemId + " Received: " + heldItemId);
            session.getGeyser().getLogger().debug("Packet: " + packet);
            return true;
        }
        return false;
    }

    private boolean useItem(GeyserSession session, InventoryTransactionPacket packet, int blockState) {
        // Update the player's inventory to remove any items added by the client itself
        Inventory playerInventory = session.getPlayerInventory();
        int heldItemSlot = playerInventory.getOffsetForHotbar(packet.getHotbarSlot());
        InventoryTranslator.PLAYER_INVENTORY_TRANSLATOR.updateSlot(session, playerInventory, heldItemSlot);
        if (playerInventory.getItem(heldItemSlot).getAmount() > 1) {
            if (packet.getItemInHand().getId() == session.getItemMappings().getStoredItems().bucket() ||
                packet.getItemInHand().getId() == session.getItemMappings().getStoredItems().glassBottle()) {
                // Using a stack of buckets or glass bottles will result in an item being added to the first empty slot.
                // We need to revert the item in case the interaction fails. The order goes from left to right in the
                // hotbar. Then left to right and top to bottom in the inventory.
                for (int i = 0; i < 36; i++) {
                    int slot = i;
                    if (i < 9) {
                        slot = playerInventory.getOffsetForHotbar(slot);
                    }
                    if (playerInventory.getItem(slot).isEmpty()) {
                        InventoryTranslator.PLAYER_INVENTORY_TRANSLATOR.updateSlot(session, playerInventory, slot);
                        break;
                    }
                }
            }
        }
        // Check if the player is interacting with a block
        if (!session.isSneaking()) {
            if (BlockRegistries.INTERACTIVE.get().contains(blockState)) {
                return false;
            }

            boolean mayBuild = session.getGameMode() == GameMode.SURVIVAL || session.getGameMode() == GameMode.CREATIVE;
            if (mayBuild && BlockRegistries.INTERACTIVE_MAY_BUILD.get().contains(blockState)) {
                return false;
            }
        }

        Vector3f target = packet.getBlockPosition().toFloat().add(packet.getClickPosition());
        lookAt(session, target);

        ServerboundUseItemPacket itemPacket = new ServerboundUseItemPacket(Hand.MAIN_HAND, session.getWorldCache().nextPredictionSequence());
        session.sendDownstreamPacket(itemPacket);
        return true;
    }

    /**
     * Determine the rotation necessary to activate this transaction.
     *
     * The position between the intended click position and the player can be determined with two triangles.
     * First, we compute the difference of the X and Z coordinates:
     *
     * Player position (0, 0)
     * |
     * |
     * |
     * |_____________ Intended target (-3, 2)
     *
     * We then use the Pythagorean Theorem to find the direct line (hypotenuse) on the XZ plane. Finding the angle of the
     * triangle from there, closest to the player, gives us our yaw rotation value
     * Then doing the same using the new XZ distance and Y difference, we can find the direct line of sight from the
     * player to the intended target, and the pitch rotation value. We can then send the necessary packets to update
     * the player's rotation.
     *
     * @param session the Geyser Session
     * @param target the position to look at
     */
    private void lookAt(GeyserSession session, Vector3f target) {
        // Use the bounding box's position since we need the player's position seen by the Java server
        Vector3d playerPosition = session.getCollisionManager().getPlayerBoundingBox().getBottomCenter();
        float xDiff = (float) (target.getX() - playerPosition.getX());
        float yDiff = (float) (target.getY() - (playerPosition.getY() + session.getEyeHeight()));
        float zDiff = (float) (target.getZ() - playerPosition.getZ());

        // First triangle on the XZ plane
        float yaw = (float) -Math.toDegrees(Math.atan2(xDiff, zDiff));
        // Second triangle on the Y axis using the hypotenuse of the first triangle as a side
        double xzHypot = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
        float pitch = (float) -Math.toDegrees(Math.atan2(yDiff, xzHypot));

        SessionPlayerEntity entity = session.getPlayerEntity();
        ServerboundMovePlayerPosRotPacket returnPacket = new ServerboundMovePlayerPosRotPacket(entity.isOnGround(), playerPosition.getX(), playerPosition.getY(), playerPosition.getZ(), entity.getYaw(), entity.getPitch());
        // This matches Java edition behavior
        ServerboundMovePlayerPosRotPacket movementPacket = new ServerboundMovePlayerPosRotPacket(entity.isOnGround(), playerPosition.getX(), playerPosition.getY(), playerPosition.getZ(), yaw, pitch);
        session.sendDownstreamPacket(movementPacket);

        if (session.getLookBackScheduledFuture() != null) {
            session.getLookBackScheduledFuture().cancel(false);
        }
        if (Math.abs(entity.getYaw() - yaw) > 1f || Math.abs(entity.getPitch() - pitch) > 1f) {
            session.setLookBackScheduledFuture(session.scheduleInEventLoop(() -> {
                Vector3d newPlayerPosition = session.getCollisionManager().getPlayerBoundingBox().getBottomCenter();
                if (!newPlayerPosition.equals(playerPosition) || entity.getYaw() != returnPacket.getYaw() || entity.getPitch() != returnPacket.getPitch()) {
                    // The player moved/rotated so there is no need to change their rotation back
                    return;
                }
                session.sendDownstreamPacket(returnPacket);
            }, 150, TimeUnit.MILLISECONDS));
        }
    }
}
