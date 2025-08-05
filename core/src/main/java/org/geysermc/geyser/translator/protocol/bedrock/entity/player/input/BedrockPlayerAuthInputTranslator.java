/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.bedrock.entity.player.input;

import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.InputMode;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.AbstractHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.LlamaEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.protocol.bedrock.BedrockInventoryTransactionTranslator;
import org.geysermc.geyser.util.CooldownUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;

import java.util.HashSet;
import java.util.Set;

@Translator(packet = PlayerAuthInputPacket.class)
public final class BedrockPlayerAuthInputTranslator extends PacketTranslator<PlayerAuthInputPacket> {

    @Override
    public void translate(GeyserSession session, PlayerAuthInputPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();

        session.setClientTicks(packet.getTick());

        boolean wasJumping = session.getInputCache().wasJumping();
        session.getInputCache().processInputs(entity, packet);

        ServerboundPlayerCommandPacket sprintPacket = null;

        Set<PlayerAuthInputData> inputData = packet.getInputData();
        // These inputs are sent in order, so if e.g. START_GLIDING and STOP_GLIDING are both present,
        // it's important to make sure we send the last known status instead of both to the Java server.
        Set<PlayerAuthInputData> leftOverInputData = new HashSet<>(packet.getInputData());
        for (PlayerAuthInputData input : inputData) {
            leftOverInputData.remove(input);
            switch (input) {
                case PERFORM_ITEM_INTERACTION -> processItemUseTransaction(session, packet.getItemUseTransaction());
                case PERFORM_BLOCK_ACTIONS -> BedrockBlockActions.translate(session, packet.getPlayerActions());
                case START_SWIMMING -> session.setSwimming(true);
                case STOP_SWIMMING -> session.setSwimming(false);
                case START_CRAWLING -> session.setCrawling(true);
                case STOP_CRAWLING -> session.setCrawling(false);
                case START_SPRINTING -> {
                    if (!leftOverInputData.contains(PlayerAuthInputData.STOP_SPRINTING)) {
                        // Check if the player is standing on but not surrounded by water; don't allow sprinting in that case
                        // resolves <https://github.com/GeyserMC/Geyser/issues/1705>
                        if (!GameProtocol.is1_21_80orHigher(session) && session.getCollisionManager().isPlayerTouchingWater() && !session.getCollisionManager().isPlayerInWater()) {
                            // Update movement speed attribute to prevent sprinting on water. This is fixed in 1.21.80+ natively.
                            UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
                            attributesPacket.setRuntimeEntityId(entity.getGeyserId());
                            attributesPacket.getAttributes().addAll(entity.getAttributes().values());
                            session.sendUpstreamPacket(attributesPacket);
                        } else {
                            sprintPacket = new ServerboundPlayerCommandPacket(entity.javaId(), PlayerState.START_SPRINTING);
                            session.setSprinting(true);
                        }
                    }
                }
                case STOP_SPRINTING -> {
                    // Don't send sprinting update when we weren't sprinting
                    if (!leftOverInputData.contains(PlayerAuthInputData.START_SPRINTING) && session.isSprinting()) {
                        sprintPacket = new ServerboundPlayerCommandPacket(entity.javaId(), PlayerState.STOP_SPRINTING);
                        session.setSprinting(false);
                    }
                }
                case START_FLYING -> { // Since 1.20.30
                    if (session.isCanFly()) {
                        if (session.getGameMode() == GameMode.SPECTATOR) {
                            // should already be flying
                            session.sendAdventureSettings();
                            break;
                        }

                        if (session.getPlayerEntity().getFlag(EntityFlag.SWIMMING) && session.getCollisionManager().isPlayerInWater()) {
                            // As of 1.18.1, Java Edition cannot fly while in water, but it can fly while crawling
                            // If this isn't present, swimming on a 1.13.2 server and then attempting to fly will put you into a flying/swimming state that is invalid on JE
                            session.sendAdventureSettings();
                            break;
                        }

                        session.setFlying(true);
                        session.sendDownstreamGamePacket(new ServerboundPlayerAbilitiesPacket(true));
                    } else {
                        // Stop flying & remind the client about not trying to fly :)
                        session.setFlying(false);
                        session.sendAdventureSettings();
                    }
                }
                case STOP_FLYING -> {
                    session.setFlying(false);
                    session.sendDownstreamGamePacket(new ServerboundPlayerAbilitiesPacket(false));
                }
                case START_GLIDING -> {
                    // Bedrock can send both start_glide and stop_glide in the same packet.
                    // We only want to start gliding if the client has not stopped gliding in the same tick.
                    // last replicated on 1.21.70 by "walking" and jumping while in water
                    if (!leftOverInputData.contains(PlayerAuthInputData.STOP_GLIDING)) {
                        if (entity.canStartGliding()) {
                            // On Java you can't start gliding while flying
                            if (session.isFlying()) {
                                session.setFlying(false);
                                session.sendDownstreamGamePacket(new ServerboundPlayerAbilitiesPacket(false));
                            }
                            session.setGliding(true);
                            session.sendDownstreamGamePacket(new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_ELYTRA_FLYING));
                        } else {
                            entity.forceFlagUpdate();
                            session.setGliding(false);
                            // return to flying if we can't start gliding
                            if (session.isFlying()) {
                                session.sendAdventureSettings();
                            }
                        }
                    }
                }
                case START_SPIN_ATTACK -> session.setSpinAttack(true);
                case STOP_SPIN_ATTACK -> session.setSpinAttack(false);
                case STOP_GLIDING -> {
                    // Java doesn't allow elytra gliding to stop mid-air.
                    boolean shouldBeGliding = entity.isGliding() && entity.canStartGliding();
                    // Always update; Bedrock can get real weird if the gliding state is mismatching
                    entity.forceFlagUpdate();
                    session.setGliding(shouldBeGliding);
                }
                case MISSED_SWING -> {
                    session.setLastAirHitTick(session.getTicks());

                    if (session.getArmAnimationTicks() != 0 && session.getArmAnimationTicks() != 1) {
                        session.sendDownstreamGamePacket(new ServerboundSwingPacket(Hand.MAIN_HAND));
                        session.activateArmAnimationTicking();
                    }

                    // Touch devices expect an animation packet sent back to them
                    if (packet.getInputMode().equals(InputMode.TOUCH)) {
                        AnimatePacket animatePacket = new AnimatePacket();
                        animatePacket.setAction(AnimatePacket.Action.SWING_ARM);
                        animatePacket.setRuntimeEntityId(session.getPlayerEntity().getGeyserId());
                        session.sendUpstreamPacket(animatePacket);
                    }

                    // Java edition sends a cooldown when hitting air.
                    CooldownUtils.sendCooldown(session);
                }
            }
        }

        // Vehicle input is send before player movement
        processVehicleInput(session, packet, wasJumping);

        // Java edition sends sprinting after vehicle input, but before player movement
        if (sprintPacket != null) {
            session.sendDownstreamGamePacket(sprintPacket);
        }

        BedrockMovePlayer.translate(session, packet);

        // Only set steering values when the vehicle is a boat and when the client is actually in it
        if (entity.getVehicle() instanceof BoatEntity && inputData.contains(PlayerAuthInputData.IN_CLIENT_PREDICTED_IN_VEHICLE)) {
            boolean up = inputData.contains(PlayerAuthInputData.UP);
            // Yes. These are flipped. Welcome to Bedrock edition.
            // Hi random stranger. I am six days into updating for 1.21.3. How's it going?
            session.setSteeringLeft(up || inputData.contains(PlayerAuthInputData.PADDLE_RIGHT));
            session.setSteeringRight(up || inputData.contains(PlayerAuthInputData.PADDLE_LEFT));
        }
    }

    private static void processItemUseTransaction(GeyserSession session, ItemUseTransaction transaction) {
        if (transaction.getActionType() == 2) {
            int blockState = session.getGameMode() == GameMode.CREATIVE ?
                session.getGeyser().getWorldManager().getBlockAt(session, transaction.getBlockPosition()) : session.getBreakingBlock();

            session.setLastBlockPlaced(null);
            session.setLastBlockPlacePosition(null);

            // Same deal with vanilla block placing as above.
            if (!session.getWorldBorder().isInsideBorderBoundaries()) {
                BedrockInventoryTransactionTranslator.restoreCorrectBlock(session, transaction.getBlockPosition());
                return;
            }

            Vector3f playerPosition = session.getPlayerEntity().getPosition();
            playerPosition = playerPosition.down(EntityDefinitions.PLAYER.offset() - session.getEyeHeight());

            if (!BedrockInventoryTransactionTranslator.canInteractWithBlock(session, playerPosition, transaction.getBlockPosition())) {
                BedrockInventoryTransactionTranslator.restoreCorrectBlock(session, transaction.getBlockPosition());
                return;
            }

            int sequence = session.getWorldCache().nextPredictionSequence();
            session.getWorldCache().markPositionInSequence(transaction.getBlockPosition());
            // -1 means we don't know what block they're breaking
            if (blockState == -1) {
                blockState = Block.JAVA_AIR_ID;
            }

            LevelEventPacket blockBreakPacket = new LevelEventPacket();
            blockBreakPacket.setType(LevelEvent.PARTICLE_DESTROY_BLOCK);
            blockBreakPacket.setPosition(transaction.getBlockPosition().toFloat());
            blockBreakPacket.setData(session.getBlockMappings().getBedrockBlockId(blockState));
            session.sendUpstreamPacket(blockBreakPacket);
            session.setBreakingBlock(-1);

            Entity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, transaction.getBlockPosition());
            if (itemFrameEntity != null) {
                ServerboundInteractPacket attackPacket = new ServerboundInteractPacket(itemFrameEntity.getEntityId(),
                    InteractAction.ATTACK, session.isSneaking());
                session.sendDownstreamGamePacket(attackPacket);
                return;
            }

            PlayerAction action = session.getGameMode() == GameMode.CREATIVE ? PlayerAction.START_DIGGING : PlayerAction.FINISH_DIGGING;
            ServerboundPlayerActionPacket breakPacket = new ServerboundPlayerActionPacket(action, transaction.getBlockPosition(), Direction.VALUES[transaction.getBlockFace()], sequence);
            session.sendDownstreamGamePacket(breakPacket);
        } else {
            session.getGeyser().getLogger().error("Unhandled item use transaction type!");
            if (session.getGeyser().getLogger().isDebug()) {
                session.getGeyser().getLogger().debug(transaction);
            }
        }
    }

    private static void processVehicleInput(GeyserSession session, PlayerAuthInputPacket packet, boolean wasJumping) {
        Entity vehicle = session.getPlayerEntity().getVehicle();
        if (vehicle == null) {
            return;
        }
        if (vehicle instanceof ClientVehicle) {
            session.getPlayerEntity().setVehicleInput(packet.getMotion());
        }

        boolean sendMovement = false;
        if (vehicle instanceof AbstractHorseEntity && !(vehicle instanceof LlamaEntity)) {
            sendMovement = !(vehicle instanceof ClientVehicle);
        } else if (vehicle instanceof BoatEntity) {
            // The player is either the only or the front rider.
            sendMovement = vehicle.getPassengers().size() == 1 || session.getPlayerEntity().isRidingInFront();
        }

        if (vehicle instanceof AbstractHorseEntity && !vehicle.getFlag(EntityFlag.HAS_DASH_COOLDOWN)) {
            // Behavior verified as of Java Edition 1.21.3
            int currentJumpingTicks = session.getInputCache().getJumpingTicks();
            if (currentJumpingTicks < 0) {
                session.getInputCache().setJumpingTicks(++currentJumpingTicks);
                if (currentJumpingTicks == 0) {
                    session.getInputCache().setJumpScale(0);
                }
            }

            boolean holdingJump = packet.getInputData().contains(PlayerAuthInputData.JUMPING);
            if (wasJumping && !holdingJump) {
                // Jump released
                // Yes, I'm fairly certain that entity ID is correct.
                int finalVehicleJumpStrength = GenericMath.floor(session.getInputCache().getJumpScale() * 100f);
                session.sendDownstreamGamePacket(new ServerboundPlayerCommandPacket(session.getPlayerEntity().getEntityId(),
                    PlayerState.START_HORSE_JUMP, finalVehicleJumpStrength));
                session.getInputCache().setJumpingTicks(-10);
                session.getPlayerEntity().setVehicleJumpStrength(finalVehicleJumpStrength);
            } else if (!wasJumping && holdingJump) {
                session.getInputCache().setJumpingTicks(0);
                session.getInputCache().setJumpScale(0);
            } else if (holdingJump) {
                session.getInputCache().setJumpingTicks(++currentJumpingTicks);
                if (currentJumpingTicks < 10) {
                    session.getInputCache().setJumpScale(session.getInputCache().getJumpingTicks() * 0.1F);
                } else {
                    session.getInputCache().setJumpScale(0.8f + 2.0f / (currentJumpingTicks - 9) * 0.1f);
                }
            }
        } else {
            session.getInputCache().setJumpScale(0);
        }

        if (sendMovement) {
            vehicle.setOnGround(packet.getInputData().contains(PlayerAuthInputData.VERTICAL_COLLISION) && session.getPlayerEntity().getLastTickEndVelocity().getY() < 0);
            Vector3f vehiclePosition = packet.getPosition();
            Vector2f vehicleRotation = packet.getVehicleRotation();
            if (vehicleRotation == null) {
                return; // If the client just got in or out of a vehicle for example.
            }

            if (session.getWorldBorder().isPassingIntoBorderBoundaries(vehiclePosition, false)) {
                Vector3f position = vehicle.getPosition();
                if (vehicle instanceof BoatEntity boat) {
                    // Undo the changes usually applied to the boat
                    boat.moveAbsoluteWithoutAdjustments(position, vehicle.getYaw(), vehicle.isOnGround(), true);
                } else {
                    // This doesn't work if teleported is false
                    vehicle.moveAbsolute(position,
                        vehicle.getYaw(), vehicle.getPitch(), vehicle.getHeadYaw(),
                        vehicle.isOnGround(), true);
                }
                return;
            }

            if (vehicle instanceof BoatEntity) {
                // Remove some Y position to prevents boats flying up
                vehiclePosition = vehiclePosition.down(vehicle.getDefinition().offset());
            }

            vehicle.setPosition(vehiclePosition);
            ServerboundMoveVehiclePacket moveVehiclePacket = new ServerboundMoveVehiclePacket(
                vehiclePosition.toDouble(),
                vehicleRotation.getY() - 90, vehiclePosition.getX(), // TODO I wonder if this is related to the horse spinning bugs...
                vehicle.isOnGround()
            );
            session.sendDownstreamGamePacket(moveVehiclePacket);
        }
    }
}
