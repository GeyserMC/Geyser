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

package org.geysermc.geyser.entity.type;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Pose;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.IntEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlags;
import com.nukkitx.protocol.bedrock.packet.AddEntityPacket;
import com.nukkitx.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import com.nukkitx.protocol.bedrock.packet.RemoveEntityPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.GeyserDirtyMetadata;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.MathUtils;

import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
public class Entity {
    protected final GeyserSession session;

    protected long entityId;
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

    protected boolean valid;

    /* Metadata about this specific entity */
    @Setter(AccessLevel.NONE)
    protected float boundingBoxHeight;
    @Setter(AccessLevel.NONE)
    protected float boundingBoxWidth;
    @Setter(AccessLevel.NONE)
    protected String nametag = "";
    /* Metadata end */

    protected LongOpenHashSet passengers = new LongOpenHashSet();
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

    public Entity(GeyserSession session, long entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
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
        dirtyMetadata.put(EntityData.COLOR, 0);
        dirtyMetadata.put(EntityData.MAX_AIR_SUPPLY, getMaxAir());
        setDimensions(Pose.STANDING);
        setFlag(EntityFlag.HAS_GRAVITY, true);
        setFlag(EntityFlag.HAS_COLLISION, true);
        setFlag(EntityFlag.CAN_SHOW_NAME, true);
        setFlag(EntityFlag.CAN_CLIMB, true);
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

        for (long passenger : passengers) { // Make sure all passengers on the despawned entity are updated
            Entity entity = session.getEntityCache().getEntityByJavaId(passenger);
            if (entity == null) continue;
            entity.setFlag(EntityFlag.RIDING, false);
            entity.updateBedrockMetadata();
        }

        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.setUniqueEntityId(geyserId);
        session.sendUpstreamPacket(removeEntityPacket);

        valid = false;
        return true;
    }

    public void moveRelative(double relX, double relY, double relZ, float yaw, float pitch, boolean isOnGround) {
        moveRelative(relX, relY, relZ, yaw, pitch, this.headYaw, isOnGround);
    }

    public void moveRelative(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        setYaw(yaw);
        setPitch(pitch);
        setHeadYaw(headYaw);
        setOnGround(isOnGround);
        this.position = Vector3f.from(position.getX() + relX, position.getY() + relY, position.getZ() + relZ);

        MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
        moveEntityPacket.setRuntimeEntityId(geyserId);
        moveEntityPacket.setPosition(position);
        moveEntityPacket.setRotation(getBedrockRotation());
        moveEntityPacket.setOnGround(isOnGround);
        moveEntityPacket.setTeleported(false);

        session.sendUpstreamPacket(moveEntityPacket);
    }

    public void moveAbsolute(Vector3f position, float yaw, float pitch, boolean isOnGround, boolean teleported) {
        moveAbsolute(position, yaw, pitch, this.headYaw, isOnGround, teleported);
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
        moveAbsolute(position, yaw, pitch, isOnGround, false);
    }

    /**
     * Updates an entity's head position. Used in JavaRotateHeadTranslator.
     * @param headYaw The new head rotation of the entity.
     */
    public void updateHeadLookRotation(float headYaw) {
        moveRelative(0, 0, 0, headYaw, pitch, this.headYaw, onGround);
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
        moveRelative(moveX, moveY, moveZ, this.yaw, pitch, yaw, isOnGround);
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

    protected int getMaxAir() {
        return 300;
    }

    public void setDisplayName(EntityMetadata<Optional<Component>, ?> entityMetadata) {
        Optional<Component> name = entityMetadata.getValue();
        if (name.isPresent()) {
            nametag = MessageTranslator.convertMessage(name.get(), session.getLocale());
            dirtyMetadata.put(EntityData.NAMETAG, nametag);
        } else if (!nametag.isEmpty()) {
            // Clear nametag
            dirtyMetadata.put(EntityData.NAMETAG, "");
        }
    }

    public void setDisplayNameVisible(BooleanEntityMetadata entityMetadata) {
        dirtyMetadata.put(EntityData.NAMETAG_ALWAYS_SHOW, (byte) (entityMetadata.getPrimitiveValue() ? 1 : 0));
    }

    public void setGravity(BooleanEntityMetadata entityMetadata) {
        setFlag(EntityFlag.HAS_GRAVITY, !entityMetadata.getPrimitiveValue());
    }

    /**
     * Usually used for bounding box and not animation.
     */
    public void setPose(EntityMetadata<Pose, ?> entityMetadata) {
        Pose pose = entityMetadata.getValue();

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

    public void setBoundingBoxHeight(float height) {
        if (height != boundingBoxHeight) {
            boundingBoxHeight = height;
            dirtyMetadata.put(EntityData.BOUNDING_BOX_HEIGHT, boundingBoxHeight);
        }
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
     * x = Pitch, y = HeadYaw, z = Yaw
     *
     * @return the bedrock rotation
     */
    public Vector3f getBedrockRotation() {
        return Vector3f.from(pitch, headYaw, yaw);
    }

    @SuppressWarnings("unchecked")
    public <I extends Entity> I as(Class<I> entityClass) {
        return entityClass.isInstance(this) ? (I) this : null;
    }

    public <I extends Entity> boolean is(Class<I> entityClass) {
        return entityClass.isInstance(this);
    }
}
