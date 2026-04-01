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

import lombok.Getter;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.vehicle.BoatVehicleComponent;
import org.geysermc.geyser.entity.vehicle.ClientVehicle;
import org.geysermc.geyser.entity.vehicle.VehicleComponent;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPaddleBoatPacket;

public class BoatEntity extends Entity implements Tickable, Leashable, ClientVehicle {

    
    private static final String BUOYANCY_DATA = "{\"apply_gravity\":true,\"base_buoyancy\":1.0,\"big_wave_probability\":0.02999999932944775," +
            "\"big_wave_speed\":10.0,\"drag_down_on_buoyancy_removed\":0.0,\"liquid_blocks\":[\"minecraft:water\"," +
            "\"minecraft:flowing_water\"],\"simulate_waves\":false}";

    private final BoatVehicleComponent vehicleComponent = new BoatVehicleComponent(this, 0);

    private boolean isPaddlingLeft;
    private float paddleTimeLeft;
    private boolean isPaddlingRight;
    private float paddleTimeRight;
    private boolean doTick;

    
    @Getter
    protected final BoatVariant variant;

    private long leashHolderBedrockId = -1;

    
    private final float ROWING_SPEED = 0.04f;

    public BoatEntity(EntitySpawnContext context, BoatVariant variant) {
        super(context);
        
        setYaw(yaw + 90);
        setPitch(0);
        setHeadYaw(headYaw + 90);
        this.variant = variant;

        dirtyMetadata.put(EntityDataTypes.VARIANT, variant.ordinal());

        
        dirtyMetadata.put(EntityDataTypes.IS_BUOYANT, true);
        dirtyMetadata.put(EntityDataTypes.BUOYANCY_DATA, BUOYANCY_DATA);;
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        
        setFlag(EntityFlag.COLLIDABLE, true);
    }

    @Override
    public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        
        setPosition(position);
        setYaw(yaw + 90);
        setHeadYaw(yaw + 90);
        setOnGround(isOnGround);

        MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
        moveEntityPacket.setRuntimeEntityId(geyserId);
        moveEntityPacket.setPosition(bedrockPosition());
        moveEntityPacket.setRotation(bedrockRotation());
        moveEntityPacket.setOnGround(isOnGround);
        moveEntityPacket.setTeleported(teleported);

        session.sendUpstreamPacket(moveEntityPacket);
    }

    @Override
    public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        super.moveRelativeRaw(relX, relY, relZ, yaw, 0, yaw, isOnGround);
    }

    @Override
    public void updatePositionAndRotation(double moveX, double moveY, double moveZ, float yaw, float pitch, boolean isOnGround) {
        moveRelative(moveX, moveY, moveZ, yaw + 90, 0, 0, isOnGround);
    }

    @Override
    public void updateRotation(float yaw, float pitch, boolean isOnGround) {
        moveRelativeRaw(0, 0, 0, yaw + 90, 0, 0, isOnGround);
    }

    public void setPaddlingLeft(BooleanEntityMetadata entityMetadata) {
        isPaddlingLeft = entityMetadata.getPrimitiveValue();
        if (!isPaddlingLeft) {
            paddleTimeLeft = 0.0f;
            dirtyMetadata.put(EntityDataTypes.ROW_TIME_LEFT, 0.0f);
        }
    }

    public void setPaddlingRight(BooleanEntityMetadata entityMetadata) {
        isPaddlingRight = entityMetadata.getPrimitiveValue();
        if (!isPaddlingRight) {
            paddleTimeRight = 0.0f;
            dirtyMetadata.put(EntityDataTypes.ROW_TIME_RIGHT, 0.0f);
        }
    }

    @Override
    public void setLeashHolderBedrockId(long bedrockId) {
        this.leashHolderBedrockId = bedrockId;
        dirtyMetadata.put(EntityDataTypes.LEASH_HOLDER, bedrockId);
    }

    @Override
    protected InteractiveTag testInteraction(Hand hand) {
        InteractiveTag tag = super.testInteraction(hand);
        if (tag != InteractiveTag.NONE) {
            return tag;
        }
        if (session.isSneaking()) {
            return InteractiveTag.NONE;
        } else if (passengers.size() < 2) {
            return InteractiveTag.BOARD_BOAT;
        } else {
            return InteractiveTag.NONE;
        }
    }

    @Override
    public InteractionResult interact(Hand hand) {
        InteractionResult result = super.interact(hand);
        if (result != InteractionResult.PASS) {
            return result;
        }
        if (session.isSneaking()) {
            return InteractionResult.PASS;
        } else {
            
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public void tick() {
        
        if (session.getPlayerEntity().getVehicle() == this) {
            
            ServerboundPaddleBoatPacket steerPacket = new ServerboundPaddleBoatPacket(session.isSteeringLeft(), session.isSteeringRight());
            session.sendDownstreamGamePacket(steerPacket);

            // If the vehicle is not controlled by the client, then we will have to send the client the rowing time else the animation won't play!
            if (session.isInClientPredictedVehicle()) {
                return;
            }
        }

        doTick = !doTick; // Run every other tick
        if (!doTick || passengers.isEmpty()) {
            return;
        }

        Entity rower = passengers.get(0);
        if (rower == null) {
            return;
        }

        if (isPaddlingLeft) {
            paddleTimeLeft += ROWING_SPEED;
            dirtyMetadata.put(EntityDataTypes.ROW_TIME_LEFT, paddleTimeLeft);
        }
        if (isPaddlingRight) {
            paddleTimeRight += ROWING_SPEED;
            dirtyMetadata.put(EntityDataTypes.ROW_TIME_RIGHT, paddleTimeRight);
        }

        if (isPaddlingLeft || isPaddlingRight) {
            updateBedrockMetadata();
        }
    }

    @Override
    public long leashHolderBedrockId() {
        return this.leashHolderBedrockId;
    }

    @Override
    public VehicleComponent<?> getVehicleComponent() {
        return this.vehicleComponent;
    }

    @Override
    public Vector3f getRiddenInput(Vector2f input) {
        return Vector3f.ZERO;
    }

    @Override
    public float getVehicleSpeed() {
        return 0;
    }

    @Override
    public boolean shouldSimulateMovement() {
        return !session.isInClientPredictedVehicle() && !passengers.isEmpty() && this.session.getPlayerEntity() == passengers.get(0);
    }

    
    public enum BoatVariant {
        OAK,
        SPRUCE,
        BIRCH,
        JUNGLE,
        ACACIA,
        DARK_OAK,
        MANGROVE,
        BAMBOO,
        CHERRY,
        PALE_OAK;

        BoatVariant() {}
    }
}
