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

package org.geysermc.geyser.entity.type;

import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityDeltaPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.MinecartStep;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveMinecartPacket;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class MinecartEntity extends Entity implements Tickable {
    private static final int POS_ROT_LERP_TICKS = 3;

    private final List<MinecartStep> lerpSteps = new LinkedList<>();
    private final List<MinecartStep> currentLerpSteps = new LinkedList<>();

    private MinecartStep lastCompletedStep = new MinecartStep(Vector3d.ZERO, Vector3d. ZERO, 0.0F, 0.0F, 0.0F);
    private float currentStepsTotalWeight = 0.0F;
    private int lerpDelay = 0;

    private PartialStep cachedPartialStep;
    private int cachedStepDelay;
    private float cachedDelta;

    public MinecartEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position.add(0d, definition.offset(), 0d), motion, yaw, pitch, headYaw);
    }

    public void setCustomBlock(IntEntityMetadata entityMetadata) {
        // Optional block state -> "0" is air, aka none
        // Sets whether the custom block should be enabled
        dirtyMetadata.put(EntityDataTypes.CUSTOM_DISPLAY, (byte) (entityMetadata.getPrimitiveValue() != 0 ? 1 : 0));
        dirtyMetadata.put(EntityDataTypes.DISPLAY_BLOCK_STATE, session.getBlockMappings().getBedrockBlock(entityMetadata.getPrimitiveValue()));
    }

    public void setCustomBlockOffset(IntEntityMetadata entityMetadata) {
        dirtyMetadata.put(EntityDataTypes.DISPLAY_OFFSET, entityMetadata.getPrimitiveValue());
    }

    @Override
    public void tick() {
        if (!session.isUsingExperimentalMinecartLogic()) {
            return;
        }

        // All minecart lerp code here and in the methods below has been based off of the code in the Java NewMinecartBehavior class
        lerpDelay--;
        if (lerpDelay <= 0) {
            updateCompletedStep();
            currentLerpSteps.clear();
            if (!lerpSteps.isEmpty()) {
                currentLerpSteps.addAll(lerpSteps);
                lerpSteps.clear();
                currentStepsTotalWeight = 0.0F;

                for (MinecartStep step : currentLerpSteps) {
                    currentStepsTotalWeight += step.weight();
                }

                lerpDelay = currentStepsTotalWeight == 0.0F ? 0 : POS_ROT_LERP_TICKS;
            }
        }

        if (isLerping()) {
            float delta = 1.0F; // This is always 1, maybe it should be removed

            Vector3f position = getCurrentLerpPosition(delta).toFloat();
            Vector3f movement = getCurrentLerpMovement(delta).toFloat();
            setPosition(position);
            setMotion(movement);

            setYaw(180.0F - getCurrentLerpYaw(delta));
            setPitch(getCurrentLerpPitch(delta));

            MoveEntityDeltaPacket moveEntityPacket = new MoveEntityDeltaPacket();
            moveEntityPacket.setRuntimeEntityId(geyserId);

            moveEntityPacket.setX(position.getX());
            moveEntityPacket.setY(position.getY() + definition.offset());
            moveEntityPacket.setZ(position.getZ());
            moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_X);
            moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Y);
            moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Z);

            moveEntityPacket.setYaw(getYaw());
            moveEntityPacket.setPitch(getPitch());
            moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_YAW);
            moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_PITCH);

            SetEntityMotionPacket entityMotionPacket = new SetEntityMotionPacket();
            entityMotionPacket.setRuntimeEntityId(geyserId);
            entityMotionPacket.setMotion(movement);

            session.sendUpstreamPacket(moveEntityPacket);
            session.sendUpstreamPacket(entityMotionPacket);
        }
    }

    public void handleMinecartMovePacket(ClientboundMoveMinecartPacket packet) {
        lerpSteps.addAll(packet.getLerpSteps());
    }

    private boolean isLerping() {
        return !currentLerpSteps.isEmpty();
    }

    private float getCurrentLerpPitch(float delta) {
        PartialStep partialStep = getCurrentLerpStep(delta);
        return lerpRotation(partialStep.delta, partialStep.previousStep.xRot(), partialStep.currentStep.xRot());
    }

    private float getCurrentLerpYaw(float delta) {
        PartialStep partialStep = getCurrentLerpStep(delta);
        return lerpRotation(partialStep.delta, partialStep.previousStep.yRot(), partialStep.currentStep.yRot());
    }

    private Vector3d getCurrentLerpPosition(float delta) {
        PartialStep partialStep = getCurrentLerpStep(delta);
        return lerp(partialStep.delta, partialStep.previousStep.position(), partialStep.currentStep.position());
    }

    private Vector3d getCurrentLerpMovement(float delta) {
        PartialStep partialStep = getCurrentLerpStep(delta);
        return lerp(partialStep.delta, partialStep.previousStep.movement(), partialStep.currentStep.movement());
    }

    private PartialStep getCurrentLerpStep(float delta) {
        if (cachedDelta != delta || lerpDelay != cachedStepDelay || cachedPartialStep == null) {
            float g = ((POS_ROT_LERP_TICKS - lerpDelay) + delta) / POS_ROT_LERP_TICKS;
            float totalWeight = 0.0F;
            float stepDelta = 1.0F;
            boolean foundStep = false;

            int step;
            for (step = 0; step < currentLerpSteps.size(); step++) {
                float currentWeight = currentLerpSteps.get(step).weight();
                if (!(currentWeight <= 0.0F)) {
                    totalWeight += currentWeight;
                    if ((double) totalWeight >= currentStepsTotalWeight * (double) g) {
                        float h = totalWeight - currentWeight;
                        stepDelta = (g * currentStepsTotalWeight - h) / currentWeight;
                        foundStep = true;
                        break;
                    }
                }
            }

            if (!foundStep) {
                step = currentLerpSteps.size() - 1;
            }

            MinecartStep currentStep = currentLerpSteps.get(step);
            MinecartStep previousStep = step > 0 ? currentLerpSteps.get(step - 1) : lastCompletedStep;
            cachedPartialStep = new PartialStep(stepDelta, currentStep, previousStep);
            cachedStepDelay = lerpDelay;
            cachedDelta = delta;
        }
        return cachedPartialStep;
    }

    private void updateCompletedStep() {
        lastCompletedStep = new MinecartStep(position.toDouble(), motion.toDouble(), yaw, pitch, 0.0F);
    }

    @Override
    public void moveAbsolute(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        super.moveAbsolute(position.add(0d, this.definition.offset(), 0d), yaw, pitch, headYaw, isOnGround, teleported);
    }

    @Override
    public Vector3f getBedrockRotation() {
        // Note: minecart rotation on rails does not care about the actual rotation value
        return Vector3f.from(0, getYaw(), 0);
    }

    @Override
    public boolean doesJumpDismount() {
        // This is a little bit misleading because jumping is literally the only way to dismount for Touch users.
        // Therefore, do this so we won't lock jumping to let Touch user able to dismount.
        return false;
    }

    @Override
    protected InteractiveTag testInteraction(Hand hand) {
        if (definition == EntityDefinitions.CHEST_MINECART || definition == EntityDefinitions.HOPPER_MINECART) {
            return InteractiveTag.OPEN_CONTAINER;
        } else {
            if (session.isSneaking() || definition == EntityDefinitions.TNT_MINECART) {
                return InteractiveTag.NONE;
            } else if (!passengers.isEmpty()) {
                // Can't enter if someone is inside
                return InteractiveTag.NONE;
            } else {
                // Attempt to enter
                return InteractiveTag.RIDE_MINECART;
            }
        }
    }

    @Override
    public InteractionResult interact(Hand hand) {
        if (definition == EntityDefinitions.CHEST_MINECART || definition == EntityDefinitions.HOPPER_MINECART) {
            // Opening the UI of this minecart
            return InteractionResult.SUCCESS;
        } else {
            if (session.isSneaking()) {
                return InteractionResult.PASS;
            } else if (!passengers.isEmpty()) {
                // Can't enter if someone is inside
                return InteractionResult.PASS;
            } else {
                // Attempt to enter
                return InteractionResult.SUCCESS;
            }
        }
    }

    private static Vector3d lerp(double delta, Vector3d start, Vector3d end) {
        return Vector3d.from(lerp(delta, start.getX(), end.getX()), lerp(delta, start.getY(), end.getY()), lerp(delta, start.getZ(), end.getZ()));
    }

    public static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    private static float lerpRotation(float delta, float start, float end) {
        return start + delta * MathUtils.wrapDegrees(end - start);
    }

    private record PartialStep(float delta, MinecartStep currentStep, MinecartStep previousStep) {
    }
}
