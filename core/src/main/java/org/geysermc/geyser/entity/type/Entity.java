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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Pose;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlags;
import com.nukkitx.protocol.bedrock.packet.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.GeyserDirtyMetadata;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.EntityUtils;
import org.geysermc.geyser.util.InteractionResult;
import org.geysermc.geyser.util.InteractiveTag;
import org.geysermc.geyser.util.MathUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
public class Entity {
    protected final GeyserSession session;

    protected int entityId;
    protected final long geyserId;
    protected UUID uuid;

    protected Vector3f position;
    protected Vector3f motion;

    /**
     * x = Yaw, y = Pitch, z = HeadYaw
     */
    protected float yaw;
    protected float pitch;
    protected float headYaw;

    /**
     * Saves if the entity should be on the ground. Otherwise entities like parrots are flapping when rotating
     */
    protected boolean onGround;

    protected EntityDefinition<?> definition;

    /**
     * Indicates if the entity has been initialized and spawned
     */
    protected boolean valid;

    /* Metadata about this specific entity */
    @Setter(AccessLevel.NONE)
    private float boundingBoxHeight;
    @Setter(AccessLevel.NONE)
    private float boundingBoxWidth;
    @Setter(AccessLevel.NONE)
    protected String nametag = "";
    @Setter(AccessLevel.NONE)
    protected boolean silent = false;
    /* Metadata end */

    protected List<Entity> passengers = Collections.emptyList();
    protected Entity vehicle;
    /**
     * A container to store temporary metadata before it's sent to Bedrock.
     */
    protected final GeyserDirtyMetadata dirtyMetadata = new GeyserDirtyMetadata();
    /**
     * The entity flags for the Bedrock entity.
     * These must always be saved - if flags are updated and the other values aren't present, the Bedrock client will
     * think they are set to false.
     */
    @Getter(AccessLevel.NONE)
    protected final EntityFlags flags = new EntityFlags();
    /**
     * Indicates if flags have been updated and need to be sent to the client.
     */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.PROTECTED) // For players
    private boolean flagsDirty = false;

    public Entity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        this.session = session;

        this.entityId = entityId;
        this.geyserId = geyserId;
        this.uuid = uuid;
        this.definition = definition;
        this.motion = motion;
        this.yaw = yaw;
        this.pitch = pitch;
        this.headYaw = headYaw;

        this.valid = false;

        setPosition(position);
        setAirSupply(getMaxAir());

        initializeMetadata();
    }

    /**
     * Called on entity spawn. Used to populate the entity metadata and flags with default values.
     */
    protected void initializeMetadata() {
        dirtyMetadata.put(EntityData.SCALE, 1f);
        dirtyMetadata.put(EntityData.COLOR, (byte) 0);
        dirtyMetadata.put(EntityData.MAX_AIR_SUPPLY, getMaxAir());
        setDimensions(Pose.STANDING);
        setFlag(EntityFlag.HAS_GRAVITY, true);
        setFlag(EntityFlag.HAS_COLLISION, true);
        setFlag(EntityFlag.CAN_SHOW_NAME, true);
        setFlag(EntityFlag.CAN_CLIMB, true);
        // Let the Java server (or us) supply all sounds for an entity
        setClientSideSilent();
    }

    protected void setClientSideSilent() {
        setFlag(EntityFlag.SILENT, true);
    }

    public void spawnEntity() {
        AddEntityPacket addEntityPacket = new AddEntityPacket();
        addEntityPacket.setIdentifier(definition.identifier());
        addEntityPacket.setRuntimeEntityId(geyserId);
        addEntityPacket.setUniqueEntityId(geyserId);
        addEntityPacket.setPosition(position);
        addEntityPacket.setMotion(motion);
        addEntityPacket.setRotation(getBedrockRotation());
        addEntityPacket.getMetadata().putFlags(flags);
        dirtyMetadata.apply(addEntityPacket.getMetadata());
        addAdditionalSpawnData(addEntityPacket);

        valid = true;
        session.sendUpstreamPacket(addEntityPacket);

        flagsDirty = false;

        if (session.getGeyser().getConfig().isDebugMode()) {
            EntityType type = definition.entityType();
            String name = type != null ? type.name() : getClass().getSimpleName();
            session.getGeyser().getLogger().debug("Spawned entity " + name + " at location " + position + " with id " + geyserId + " (java id " + entityId + ")");
        }
    }

    /**
     * To be overridden in other entity classes, if additional things need to be done to the spawn entity packet.
     */
    public void addAdditionalSpawnData(AddEntityPacket addEntityPacket) {
    }

    /**
     * Despawns the entity
     *
     * @return can be deleted
     */
    public boolean despawnEntity() {
        if (!valid) return true;

        for (Entity passenger : passengers) { // Make sure all passengers on the despawned entity are updated
            if (passenger == null) continue;
            passenger.setVehicle(null);
            passenger.setFlag(EntityFlag.RIDING, false);
            passenger.updateBedrockMetadata();
        }

        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.setUniqueEntityId(geyserId);
        session.sendUpstreamPacket(removeEntityPacket);

        valid = false;
        return true;
    }

    public void moveRelative(double relX, double relY, double relZ, float yaw, float pitch, boolean isOnGround) {
        moveRelative(relX, relY, relZ, yaw, pitch, getHeadYaw(), isOnGround);
    }

    public void moveRelative(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        position = Vector3f.from(position.getX() + relX, position.getY() + relY, position.getZ() + relZ);

        MoveEntityDeltaPacket moveEntityPacket = new MoveEntityDeltaPacket();
        moveEntityPacket.setRuntimeEntityId(geyserId);
        if (relX != 0.0) {
            moveEntityPacket.setX(position.getX());
            moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_X);
        }
        if (relY != 0.0) {
            moveEntityPacket.setY(position.getY());
            moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Y);
        }
        if (relZ != 0.0) {
            moveEntityPacket.setZ(position.getZ());
            moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Z);
        }
        if (pitch != this.pitch) {
            this.pitch = pitch;
            moveEntityPacket.setPitch(pitch);
            moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_PITCH);
        }
        if (yaw != this.yaw) {
            this.yaw = yaw;
            moveEntityPacket.setYaw(yaw);
            moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_YAW);
        }
        if (headYaw != this.headYaw) {
            this.headYaw = headYaw;
            moveEntityPacket.setHeadYaw(headYaw);
            moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_HEAD_YAW);
        }
        setOnGround(isOnGround);
        if (isOnGround) {
            moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.ON_GROUND);
        }
        session.sendUpstreamPacket(moveEntityPacket);
    }

    public void moveAbsolute(Vector3f position, float yaw, float pitch, boolean isOnGround, boolean teleported) {
        moveAbsolute(position, yaw, pitch, getHeadYaw(), isOnGround, teleported);
    }

    public void moveAbsolute(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        setPosition(position);
        // Setters are intentional so it can be overridden in places like AbstractArrowEntity
        setYaw(yaw);
        setPitch(pitch);
        setHeadYaw(headYaw);
        setOnGround(isOnGround);

        MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
        moveEntityPacket.setRuntimeEntityId(geyserId);
        moveEntityPacket.setPosition(position);
        moveEntityPacket.setRotation(getBedrockRotation());
        moveEntityPacket.setOnGround(isOnGround);
        moveEntityPacket.setTeleported(teleported);

        session.sendUpstreamPacket(moveEntityPacket);
    }

    /**
     * Teleports an entity to a new location. Used in JavaTeleportEntityTranslator.
     * @param position The new position of the entity.
     * @param yaw The new yaw of the entity.
     * @param pitch The new pitch of the entity.
     * @param isOnGround Whether the entity is currently on the ground.
     */
    public void teleport(Vector3f position, float yaw, float pitch, boolean isOnGround) {
        // teleport will always set the headYaw to yaw
        moveAbsolute(position, yaw, pitch, yaw, isOnGround, false);
    }

    /**
     * Updates an entity's head position. Used in JavaRotateHeadTranslator.
     * @param headYaw The new head rotation of the entity.
     */
    public void updateHeadLookRotation(float headYaw) {
        moveRelative(0, 0, 0, getYaw(), getPitch(), headYaw, isOnGround());
    }

    /**
     * Updates an entity's position and rotation. Used in JavaMoveEntityPosRotTranslator.
     * @param moveX The new X offset of the current position.
     * @param moveY The new Y offset of the current position.
     * @param moveZ The new Z offset of the current position.
     * @param yaw The new yaw of the entity.
     * @param pitch The new pitch of the entity.
     * @param isOnGround Whether the entity is currently on the ground.
     */
    public void updatePositionAndRotation(double moveX, double moveY, double moveZ, float yaw, float pitch, boolean isOnGround) {
        moveRelative(moveX, moveY, moveZ, yaw, pitch, getHeadYaw(), isOnGround);
    }

    /**
     * Updates an entity's rotation. Used in JavaMoveEntityRotTranslator.
     * @param yaw The new yaw of the entity.
     * @param pitch The new pitch of the entity.
     * @param isOnGround Whether the entity is currently on the ground.
     */
    public void updateRotation(float yaw, float pitch, boolean isOnGround) {
        updatePositionAndRotation(0, 0, 0, yaw, pitch, isOnGround);
    }

    public final boolean getFlag(EntityFlag flag) {
        return flags.getFlag(flag);
    }

    /**
     * Updates a flag value and determines if the flags would need synced with the Bedrock client.
     */
    public final void setFlag(EntityFlag flag, boolean value) {
        flagsDirty |= flags.setFlag(flag, value);
    }

    /**
     * Sends the Bedrock metadata to the client
     */
    public void updateBedrockMetadata() {
        if (!valid) {
            return;
        }

        if (dirtyMetadata.hasEntries() || flagsDirty) {
            SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
            entityDataPacket.setRuntimeEntityId(geyserId);
            if (flagsDirty) {
                entityDataPacket.getMetadata().putFlags(flags);
                flagsDirty = false;
            }
            dirtyMetadata.apply(entityDataPacket.getMetadata());
            session.sendUpstreamPacket(entityDataPacket);
        }
    }

    public void setFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.ON_FIRE, ((xd & 0x01) == 0x01) && !getFlag(EntityFlag.FIRE_IMMUNE)); // Otherwise immune entities sometimes flicker onfire
        setFlag(EntityFlag.SNEAKING, (xd & 0x02) == 0x02);
        setFlag(EntityFlag.SPRINTING, (xd & 0x08) == 0x08);

        // Swimming is ignored here and instead we rely on the pose
        setFlag(EntityFlag.GLIDING, (xd & 0x80) == 0x80);

        setInvisible((xd & 0x20) == 0x20);
    }

    /**
     * Set a boolean - whether the entity is invisible or visible
     *
     * @param value true if the entity is invisible
     */
    protected void setInvisible(boolean value) {
        setFlag(EntityFlag.INVISIBLE, value);
    }

    /**
     * Set an int from 0 - this entity's maximum air - (air / maxAir) represents the percentage of bubbles left
     */
    public final void setAir(IntEntityMetadata entityMetadata) {
        setAirSupply(entityMetadata.getPrimitiveValue());
    }

    protected void setAirSupply(int amount) {
        dirtyMetadata.put(EntityData.AIR_SUPPLY, (short) MathUtils.constrain(amount, 0, getMaxAir()));
    }

    protected short getMaxAir() {
        return 300;
    }

    public void setDisplayName(EntityMetadata<Optional<Component>, ?> entityMetadata) {
        Optional<Component> name = entityMetadata.getValue();
        if (name.isPresent()) {
            nametag = MessageTranslator.convertMessage(name.get(), session.locale());
            dirtyMetadata.put(EntityData.NAMETAG, nametag);
        } else if (!nametag.isEmpty()) {
            // Clear nametag
            dirtyMetadata.put(EntityData.NAMETAG, "");
        }
    }

    public void setDisplayNameVisible(BooleanEntityMetadata entityMetadata) {
        dirtyMetadata.put(EntityData.NAMETAG_ALWAYS_SHOW, (byte) (entityMetadata.getPrimitiveValue() ? 1 : 0));
    }

    public final void setSilent(BooleanEntityMetadata entityMetadata) {
        silent = entityMetadata.getPrimitiveValue();
    }

    public void setGravity(BooleanEntityMetadata entityMetadata) {
        setFlag(EntityFlag.HAS_GRAVITY, !entityMetadata.getPrimitiveValue());
    }

    /**
     * Usually used for bounding box and not animation.
     */
    public void setPose(Pose pose) {
        setFlag(EntityFlag.SLEEPING, pose.equals(Pose.SLEEPING));
        // Triggered when crawling
        setFlag(EntityFlag.SWIMMING, pose.equals(Pose.SWIMMING));
        setDimensions(pose);
    }

    /**
     * Set the height and width of the entity's bounding box
     */
    protected void setDimensions(Pose pose) {
        // No flexibility options for basic entities
        setBoundingBoxHeight(definition.height());
        setBoundingBoxWidth(definition.width());
    }

    public boolean setBoundingBoxHeight(float height) {
        if (height != boundingBoxHeight) {
            boundingBoxHeight = height;
            dirtyMetadata.put(EntityData.BOUNDING_BOX_HEIGHT, boundingBoxHeight);

            updatePassengerOffsets();
            return true;
        }
        return false;
    }

    public void setBoundingBoxWidth(float width) {
        if (width != boundingBoxWidth) {
            boundingBoxWidth = width;
            dirtyMetadata.put(EntityData.BOUNDING_BOX_WIDTH, boundingBoxWidth);
        }
    }

    /**
     * Set a float from 0-1 - how strong the "frozen" overlay should be on screen.
     */
    public float setFreezing(IntEntityMetadata entityMetadata) {
        // The value that Java edition gives us is in ticks, but Bedrock uses a float percentage of the strength 0.0 -> 1.0
        // The Java client caps its freezing tick percentage at 140
        int freezingTicks = Math.min(entityMetadata.getPrimitiveValue(), 140);
        float freezingPercentage = freezingTicks / 140f;
        dirtyMetadata.put(EntityData.FREEZING_EFFECT_STRENGTH, freezingPercentage);
        return freezingPercentage;
    }

    public void setRiderSeatPosition(Vector3f position) {
        dirtyMetadata.put(EntityData.RIDER_SEAT_POSITION, position);
    }

    /**
     * If true, the entity should be shaking on the client's end.
     *
     * @return whether {@link EntityFlag#SHAKING} should be set to true.
     */
    protected boolean isShaking() {
        return false;
    }

    /**
     * x = Pitch, y = Yaw, z = HeadYaw
     *
     * @return the bedrock rotation
     */
    public Vector3f getBedrockRotation() {
        return Vector3f.from(getPitch(), getYaw(), getHeadYaw());
    }

    /**
     * Update the mount offsets of each passenger on this vehicle
     */
    protected void updatePassengerOffsets() {
        for (Entity passenger : passengers) {
            if (passenger != null) {
                boolean rider = passengers.get(0) == this;
                EntityUtils.updateMountOffset(passenger, this, rider, true, passengers.size() > 1);
                passenger.updateBedrockMetadata();
            }
        }
    }

    /**
     * Update this entity's mount offset
     */
    protected void updateMountOffset() {
        if (vehicle != null) {
            boolean rider = vehicle.getPassengers().get(0) == this;
            EntityUtils.updateMountOffset(this, vehicle, rider, true, vehicle.getPassengers().size() > 1);
            updateBedrockMetadata();
        }
    }

    public boolean isAlive() {
        return this.valid;
    }

    /**
     * Update the suggestion that the client currently has on their screen for this entity (for example, "Feed" or "Ride")
     */
    public final void updateInteractiveTag() {
        InteractiveTag tag = InteractiveTag.NONE;
        for (Hand hand: EntityUtils.HANDS) {
            tag = testInteraction(hand);
            if (tag != InteractiveTag.NONE) {
                break;
            }
        }
        session.getPlayerEntity().getDirtyMetadata().put(EntityData.INTERACTIVE_TAG, tag.getValue());
        session.getPlayerEntity().updateBedrockMetadata();
    }

    /**
     * Test interacting with the given hand to see if we should send a tag to the Bedrock client.
     * Should usually mirror {@link #interact(Hand)} without any side effects.
     */
    protected InteractiveTag testInteraction(Hand hand) {
        return InteractiveTag.NONE;
    }

    /**
     * Simulates interacting with an entity. The code here should mirror Java Edition code to the best of its ability,
     * to ensure packet parity as well as functionality parity (such as sound effect responses).
     */
    public InteractionResult interact(Hand hand) {
        return InteractionResult.PASS;
    }

    /**
     * Simulates interacting with this entity at a specific click point. As of Java Edition 1.18.1, this is only used for armor stands.
     */
    public InteractionResult interactAt(Hand hand) {
        return InteractionResult.PASS;
    }

    /**
     * Send an entity event of the specified type to the Bedrock player from this entity.
     */
    public final void playEntityEvent(EntityEventType type) {
        playEntityEvent(type, 0);
    }

    /**
     * Send an entity event of the specified type with the specified data to the Bedrock player from this entity.
     */
    public final void playEntityEvent(EntityEventType type, int data) {
        EntityEventPacket packet = new EntityEventPacket();
        packet.setRuntimeEntityId(geyserId);
        packet.setType(type);
        packet.setData(data);
        session.sendUpstreamPacket(packet);
    }

    @SuppressWarnings("unchecked")
    public <I extends Entity> I as(Class<I> entityClass) {
        return entityClass.isInstance(this) ? (I) this : null;
    }
}
