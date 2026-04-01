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
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.entity.vehicle.HorseVehicleComponent;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.network.GameProtocol;
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
        session.setInClientPredictedVehicle(packet.getInputData().contains(PlayerAuthInputData.IN_CLIENT_PREDICTED_IN_VEHICLE) && entity.getVehicle() != null && GameProtocol.is1_26_10orHigher(session.protocolVersion()));

        boolean wasJumping = session.getInputCache().wasJumping();
        session.getInputCache().processInputs(entity, packet);
        session.getBlockBreakHandler().handlePlayerAuthInputPacket(packet);

        ServerboundPlayerCommandPacket sprintPacket = null;

        Set<PlayerAuthInputData> inputData = packet.getInputData();
        
        
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
                    
                    if (!leftOverInputData.contains(PlayerAuthInputData.START_SPRINTING) && session.isSprinting()) {
                        sprintPacket = new ServerboundPlayerCommandPacket(entity.javaId(), PlayerState.STOP_SPRINTING);
                        session.setSprinting(false);
                    }
                }
                case START_FLYING -> { 
                    if (session.isCanFly()) {
                        if (session.getGameMode() == GameMode.SPECTATOR) {
                            
                            session.sendAdventureSettings();
                            break;
                        }

                        if (session.getPlayerEntity().getFlag(EntityFlag.SWIMMING) && session.getCollisionManager().isPlayerInWater()) {
                            
                            
                            session.sendAdventureSettings();
                            break;
                        }

                        session.setFlying(true);
                        session.sendDownstreamGamePacket(new ServerboundPlayerAbilitiesPacket(true));
                    } else {
                        
                        session.setFlying(false);
                        session.sendAdventureSettings();
                    }
                }
                case STOP_FLYING -> {
                    session.setFlying(false);
                    session.sendDownstreamGamePacket(new ServerboundPlayerAbilitiesPacket(false));
                }
                case START_GLIDING -> {
                    
                    
                    
                    if (!leftOverInputData.contains(PlayerAuthInputData.STOP_GLIDING)) {
                        if (entity.canStartGliding()) {
                            
                            if (session.isFlying()) {
                                session.setFlying(false);
                                session.sendDownstreamGamePacket(new ServerboundPlayerAbilitiesPacket(false));
                            }
                            entity.setFlag(EntityFlag.GLIDING, true);
                            session.sendDownstreamGamePacket(new ServerboundPlayerCommandPacket(entity.getEntityId(), PlayerState.START_ELYTRA_FLYING));
                        } else {
                            entity.forceFlagUpdate();
                            entity.setFlag(EntityFlag.GLIDING, false);
                            
                            if (session.isFlying()) {
                                session.sendAdventureSettings();
                            }
                        }
                    }
                }
                case START_SPIN_ATTACK -> entity.setFlag(EntityFlag.DAMAGE_NEARBY_MOBS, true);
                case STOP_SPIN_ATTACK -> entity.setFlag(EntityFlag.DAMAGE_NEARBY_MOBS, false);
                case STOP_GLIDING -> {
                    
                    boolean shouldBeGliding = entity.isGliding() && entity.canStartGliding();
                    
                    entity.forceFlagUpdate();
                    entity.setFlag(EntityFlag.GLIDING, shouldBeGliding);
                }
                case MISSED_SWING -> {
                    session.setLastAirHitTick(session.getTicks());

                    if (session.getArmAnimationTicks() != 0 && session.getArmAnimationTicks() != 1) {
                        session.sendDownstreamGamePacket(new ServerboundSwingPacket(Hand.MAIN_HAND));
                        session.activateArmAnimationTicking();
                    }

                    
                    if (packet.getInputMode().equals(InputMode.TOUCH)) {
                        AnimatePacket animatePacket = new AnimatePacket();
                        animatePacket.setAction(AnimatePacket.Action.SWING_ARM);
                        animatePacket.setRuntimeEntityId(session.getPlayerEntity().geyserId());
                        session.sendUpstreamPacket(animatePacket);
                    }

                    
                    CooldownUtils.setCooldownHitTime(session);
                }
            }
        }

        
        
        
        
        final Pose pose = entity.getDesiredPose();
        if (pose != session.getPose()) {
            session.setPose(pose);
            entity.setDimensionsFromPose(session.getPose());
            entity.updateBedrockMetadata();
        }

        
        processVehicleInput(session, packet, wasJumping);

        
        if (sprintPacket != null) {
            session.sendDownstreamGamePacket(sprintPacket);
        }

        BedrockMovePlayer.translate(session, packet);

        
        if (session.isSpawned()) {
            session.sendDownstreamGamePacket(ServerboundClientTickEndPacket.INSTANCE);
        }

        
        if (entity.getVehicle() instanceof BoatEntity && session.isInClientPredictedVehicle()) {
            boolean up = inputData.contains(PlayerAuthInputData.UP);
            
            
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

        boolean inClientPredictedVehicle = session.isInClientPredictedVehicle();
        if (vehicle instanceof ClientVehicle) {
            
            boolean isMobileAndClassicMovement = packet.getInputMode() == InputMode.TOUCH && packet.getInputInteractionModel() == InputInteractionModel.CLASSIC;
            if (isMobileAndClassicMovement && vehicle instanceof BoatEntity) {
                
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
            
            sendMovement = inClientPredictedVehicle && (vehicle.getPassengers().size() == 1 || session.getPlayerEntity().isRidingInFront());
        }

        if ((vehicle instanceof AbstractHorseEntity || vehicle instanceof AbstractNautilusEntity) && !vehicle.getFlag(EntityFlag.HAS_DASH_COOLDOWN)) {
            
            int currentJumpingTicks = session.getInputCache().getJumpingTicks();
            if (currentJumpingTicks < 0) {
                session.getInputCache().setJumpingTicks(++currentJumpingTicks);
                if (currentJumpingTicks == 0) {
                    session.getInputCache().setJumpScale(0);
                }
            }

            boolean holdingJump = packet.getInputData().contains(PlayerAuthInputData.JUMPING);
            if (wasJumping && !holdingJump) {
                
                
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
            
            
            Vector3f position = vehicle.position();
            final BoundingBox box = new BoundingBox(
                position.up(vehicle.getBoundingBoxHeight() / 2f).toDouble(),
                vehicle.getBoundingBoxWidth(), vehicle.getBoundingBoxHeight(), vehicle.getBoundingBoxWidth()
            );

            
            Vector3d movement = session.getPlayerEntity().getLastTickEndVelocity().toDouble();
            Vector3d correctedMovement = session.getCollisionManager().correctMovementForCollisions(movement, box, true, false);
            vehicle.setOnGround(correctedMovement.getY() != movement.getY() && session.getPlayerEntity().getLastTickEndVelocity().getY() < 0);

            Vector3f vehiclePosition = packet.getPosition().down(vehicle.getOffset());
            Vector2f vehicleRotation = packet.getVehicleRotation();
            if (vehicleRotation == null) {
                return; 
            }

            if (session.getWorldBorder().isPassingIntoBorderBoundaries(vehiclePosition, false)) {
                
                vehicle.moveAbsoluteRaw(position, vehicle.getYaw(), vehicle.getPitch(), vehicle.getHeadYaw(),
                    vehicle.isOnGround(), true);
                return;
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
