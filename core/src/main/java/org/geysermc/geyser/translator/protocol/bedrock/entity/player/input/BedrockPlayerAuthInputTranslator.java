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
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.InputInteractionModel;
import org.cloudburstmc.protocol.bedrock.data.InputMode;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.packet.AnimatePacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.living.animal.horse.AbstractHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.LlamaEntity;
import org.geysermc.geyser.entity.type.living.animal.nautilus.AbstractNautilusEntity;
import org.geysermc.geyser.entity.type.living.animal.nautilus.NautilusEntity;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.entity.vehicle.HorseVehicleComponent;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.CooldownUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientTickEndPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundMoveVehiclePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerAbilitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSwingPacket;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Translator(packet = PlayerAuthInputPacket.class)
public final class BedrockPlayerAuthInputTranslator extends PacketTranslator<PlayerAuthInputPacket> {

    @Override
    public void translate(GeyserSession session, PlayerAuthInputPacket packet) {
        SessionPlayerEntity entity = session.getPlayerEntity();

        session.setClientTicks(packet.getTick());
        session.setInClientPredictedVehicle(packet.getInputData().contains(PlayerAuthInputData.IN_CLIENT_PREDICTED_IN_VEHICLE) && entity.getVehicle() != null);

        boolean wasJumping = session.getInputCache().wasJumping();
        session.getInputCache().processInputs(entity, packet);
        session.getBlockBreakHandler().handlePlayerAuthInputPacket(packet);

        ServerboundPlayerCommandPacket sprintPacket = null;

        Set<PlayerAuthInputData> inputData = packet.getInputData();
        // These inputs are sent in order, so if e.g. START_GLIDING and STOP_GLIDING are both present,
        // it's important to make sure we send the last known status instead of both to the Java server.
        Set<PlayerAuthInputData> leftOverInputData = new HashSet<>(packet.getInputData());
        for (PlayerAuthInputData input : inputData) {
            leftOverInputData.remove(input);
            switch (input) {
                case PERFORM_ITEM_INTERACTION -> processItemUseTransaction(session, packet.getItemUseTransaction());
                case PERFORM_ITEM_STACK_REQUEST -> session.getPlayerInventoryHolder().translateRequests(List.of(packet.getItemStackRequest()));
                case START_SWIMMING -> entity.setFlag(EntityFlag.SWIMMING, true);
                case STOP_SWIMMING -> entity.setFlag(EntityFlag.SWIMMING, false);
                case START_CRAWLING -> entity.setFlag(EntityFlag.CRAWLING, true);
                case STOP_CRAWLING -> entity.setFlag(EntityFlag.CRAWLING, false);
                case START_SPRINTING -> {
                    if (!leftOverInputData.contains(PlayerAuthInputData.STOP_SPRINTING)) {
                        if (!session.isSprinting()) {
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
                            entity.setFlag(EntityFlag.GLIDING, true);
                            session.sendDownstreamGamePacket(new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_ELYTRA_FLYING));
                        } else {
                            entity.forceFlagUpdate();
                            entity.setFlag(EntityFlag.GLIDING, false);
                            // return to flying if we can't start gliding
                            if (session.isFlying()) {
                                session.sendAdventureSettings();
                            }
                        }
                    }
                }
                case START_SPIN_ATTACK -> entity.setFlag(EntityFlag.DAMAGE_NEARBY_MOBS, true);
                case STOP_SPIN_ATTACK -> entity.setFlag(EntityFlag.DAMAGE_NEARBY_MOBS, false);
                case STOP_GLIDING -> {
                    // Java doesn't allow elytra gliding to stop mid-air.
                    boolean shouldBeGliding = entity.isGliding() && entity.canStartGliding();
                    // Always update; Bedrock can get real weird if the gliding state is mismatching
                    entity.forceFlagUpdate();
                    entity.setFlag(EntityFlag.GLIDING, shouldBeGliding);
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

        // The player will calculate the "desired" pose at the end of every tick, if this pose still invalid then
        // it will consider the smaller pose, but we don't need to calculate that, we can go off what the client sent us.
        // Also set the session pose directly and set the metadata directly since we don't want setPose method inside entity to change
        // the current entity flag again.
        final Pose pose = entity.getDesiredPose();
        if (pose != session.getPose()) {
            session.setPose(pose);
            entity.setDimensionsFromPose(session.getPose());
            entity.updateBedrockMetadata();
        }

        // Vehicle input is send before player movement
        processVehicleInput(session, packet, wasJumping);

        // Java edition sends sprinting after vehicle input, but before player movement
        if (sprintPacket != null) {
            session.sendDownstreamGamePacket(sprintPacket);
        }

        BedrockMovePlayer.translate(session, packet);

        // This is the best way send this since most modern anticheat will expect this to be in sync with the player movement packet.
        if (session.isSpawned()) {
            session.sendDownstreamGamePacket(ServerboundClientTickEndPacket.INSTANCE);
        }

        // Only set steering values when the vehicle is a boat and when the client is actually in it
        if (entity.getVehicle() instanceof BoatEntity && session.isInClientPredictedVehicle()) {
            boolean up = inputData.contains(PlayerAuthInputData.UP);
            // Yes. These are flipped. Welcome to Bedrock edition.
            // Hi random stranger. I am six days into updating for 1.21.3. How's it going?
            session.setSteeringLeft(up || inputData.contains(PlayerAuthInputData.PADDLE_RIGHT));
            session.setSteeringRight(up || inputData.contains(PlayerAuthInputData.PADDLE_LEFT));
        }
    }

    private static void processItemUseTransaction(GeyserSession session, ItemUseTransaction transaction) {
        if (transaction.getActionType() == 2) {
            session.setLastBlockPlaced(null);
            session.setLastBlockPlacePosition(null);
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

        // TODO: Should we also check for protocol version here? If yes then this should be test on multiple platform first.
        boolean inClientPredictedVehicle = packet.getInputData().contains(PlayerAuthInputData.IN_CLIENT_PREDICTED_IN_VEHICLE);
        if (vehicle instanceof ClientVehicle) {
            // Classic input mode for boat vehicle send PADDLE_LEFT/RIGHT instead of motion values.
            boolean isMobileAndClassicMovement = packet.getInputMode() == InputMode.TOUCH && packet.getInputInteractionModel() == InputInteractionModel.CLASSIC;
            if (isMobileAndClassicMovement && vehicle instanceof BoatEntity) {
                // Press both left and right to move forward and press 1 to turn the boat.
                boolean left = packet.getInputData().contains(PlayerAuthInputData.PADDLE_LEFT), right = packet.getInputData().contains(PlayerAuthInputData.PADDLE_RIGHT);
                if (left && right) {
                    session.getPlayerEntity().setVehicleInput(Vector2f.UNIT_Y);
                } else {
                    session.getPlayerEntity().setVehicleInput(Vector2f.UNIT_X.mul(left ? -1 : right ? 1 : 0));
                }
            } else {
                session.getPlayerEntity().setVehicleInput(packet.getMotion());
            }
        }

        boolean sendMovement = false;
        if (vehicle instanceof AbstractHorseEntity && !(vehicle instanceof LlamaEntity)) {
            sendMovement = inClientPredictedVehicle;
        } else if (vehicle instanceof BoatEntity) {
            // The player is either the only or the front rider.
            sendMovement = inClientPredictedVehicle && (vehicle.getPassengers().size() == 1 || session.getPlayerEntity().isRidingInFront());
        }

        if ((vehicle instanceof AbstractHorseEntity || vehicle instanceof AbstractNautilusEntity) && !vehicle.getFlag(EntityFlag.HAS_DASH_COOLDOWN)) {
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

                if (vehicle instanceof AbstractHorseEntity horse && horse.getVehicleComponent() instanceof HorseVehicleComponent horseVehicleComponent) {
                    horseVehicleComponent.setAllowStandSliding(true);
                }
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
            // We only need to determine onGround status this way for client predicted vehicles.
            // For other vehicle, Geyser already handle it in VehicleComponent or the Java server handle it.
            Vector3f position = vehicle.getPosition();
            final BoundingBox box = new BoundingBox(
                position.down(vehicle instanceof BoatEntity ? vehicle.getDefinition().offset() : 0).up(vehicle.getBoundingBoxHeight() / 2f).toDouble(),
                vehicle.getBoundingBoxWidth(), vehicle.getBoundingBoxHeight(), vehicle.getBoundingBoxWidth()
            );

            // Manually calculate the vertical collision ourselves, the VERTICAL_COLLISION input data is inaccurate inside a vehicle!
            Vector3d movement = session.getPlayerEntity().getLastTickEndVelocity().toDouble();
            Vector3d correctedMovement = session.getCollisionManager().correctMovementForCollisions(movement, box, true, false);
            vehicle.setOnGround(correctedMovement.getY() != movement.getY() && session.getPlayerEntity().getLastTickEndVelocity().getY() < 0);

            Vector3f vehiclePosition = packet.getPosition();
            Vector2f vehicleRotation = packet.getVehicleRotation();
            if (vehicleRotation == null) {
                return; // If the client just got in or out of a vehicle for example.
            }

            if (session.getWorldBorder().isPassingIntoBorderBoundaries(vehiclePosition, false)) {
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
                vehicle instanceof BoatEntity ? vehicleRotation.getY() - 90 : vehicleRotation.getY(), vehiclePosition.getX(),
                vehicle.isOnGround()
            );
            session.sendDownstreamGamePacket(moveVehiclePacket);
        }
    }
}
