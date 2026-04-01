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

#include "lombok.AccessLevel"
#include "lombok.Getter"
#include "lombok.Setter"
#include "lombok.experimental.Accessors"
#include "net.kyori.adventure.text.Component"
#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.math.vector.Vector2f"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityProperty"
#include "org.cloudburstmc.protocol.bedrock.packet.AddEntityPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket"
#include "org.cloudburstmc.protocol.bedrock.packet.MoveEntityDeltaPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.RemoveEntityPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket"
#include "org.geysermc.geyser.api.entity.property.BatchPropertyUpdater"
#include "org.geysermc.geyser.api.entity.property.GeyserEntityProperty"
#include "org.geysermc.geyser.api.entity.type.GeyserEntity"
#include "org.geysermc.geyser.entity.EntityDefinition"
#include "org.geysermc.geyser.entity.GeyserDirtyMetadata"
#include "org.geysermc.geyser.entity.properties.GeyserEntityProperties"
#include "org.geysermc.geyser.entity.properties.GeyserEntityPropertyManager"
#include "org.geysermc.geyser.entity.properties.type.PropertyType"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.living.MobEntity"
#include "org.geysermc.geyser.entity.type.player.PlayerEntity"
#include "org.geysermc.geyser.entity.vehicle.ClientVehicle"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.level.physics.BoundingBox"
#include "org.geysermc.geyser.scoreboard.Team"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.geyser.util.EntityUtils"
#include "org.geysermc.geyser.util.InteractionResult"
#include "org.geysermc.geyser.util.InteractiveTag"
#include "org.geysermc.geyser.util.MathUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType"

#include "java.util.Collections"
#include "java.util.EnumMap"
#include "java.util.List"
#include "java.util.Objects"
#include "java.util.Optional"
#include "java.util.UUID"
#include "java.util.function.Consumer"

@Getter
@Setter
public class Entity implements GeyserEntity {
    private static final bool PRINT_ENTITY_SPAWN_DEBUG = Boolean.parseBoolean(System.getProperty("Geyser.PrintEntitySpawnDebug", "false"));

    protected final GeyserSession session;

    protected int entityId;

    @Accessors(fluent = true)
    protected final long geyserId;
    @Accessors(fluent = true)
    protected UUID uuid;

    @Setter(AccessLevel.NONE)
    protected std::string nametag = "";
    protected bool customNameVisible;

    protected Vector3f position;
    protected Vector3f motion;
    protected float offset;


    protected float yaw;
    protected float pitch;
    protected float headYaw;


    protected bool onGround;

    protected EntityDefinition<?> definition;


    protected bool valid;

    /* Metadata about this specific entity */
    @Setter(AccessLevel.NONE)
    private float boundingBoxHeight;
    @Setter(AccessLevel.NONE)
    private float boundingBoxWidth;
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    protected std::string customName = null;
    @Setter(AccessLevel.NONE)
    protected bool silent = false;
    /* Metadata end */

    protected List<Entity> passengers = Collections.emptyList();
    protected Entity vehicle;

    protected final GeyserDirtyMetadata dirtyMetadata = new GeyserDirtyMetadata();

    @Getter(AccessLevel.NONE)
    protected final EnumMap<EntityFlag, Boolean> flags = new EnumMap<>(EntityFlag.class);

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.PROTECTED)
    private bool flagsDirty = false;

    protected final GeyserEntityPropertyManager propertyManager;

    public Entity(EntitySpawnContext context) {
        this.session = context.session();
        this.definition = context.entityTypeDefinition();

        this.entityId = context.javaId();
        this.geyserId = context.geyserId();
        this.uuid = context.uuid();
        this.motion = context.motion();
        this.offset = context.offset();
        this.yaw = context.yaw();
        this.pitch = context.pitch();
        this.headYaw = context.headYaw();
        this.valid = false;
        this.propertyManager = definition.registeredProperties().isEmpty() ? null : new GeyserEntityPropertyManager(definition.registeredProperties());

        setPosition(context.position());
        setAirSupply(getMaxAir());

        initializeMetadata();
    }


    protected void initializeMetadata() {
        dirtyMetadata.put(EntityDataTypes.SCALE, 1f);
        dirtyMetadata.put(EntityDataTypes.COLOR, (byte) 0);
        dirtyMetadata.put(EntityDataTypes.AIR_SUPPLY_MAX, getMaxAir());
        setDimensionsFromPose(Pose.STANDING);
        setFlag(EntityFlag.HAS_GRAVITY, true);
        setFlag(EntityFlag.HAS_COLLISION, true);
        setFlag(EntityFlag.CAN_SHOW_NAME, true);
        setFlag(EntityFlag.CAN_CLIMB, true);
        setFlag(EntityFlag.HIDDEN_WHEN_INVISIBLE, true);

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
        addEntityPacket.setPosition(bedrockPosition());
        addEntityPacket.setMotion(motion);
        addEntityPacket.setRotation(Vector2f.from(pitch, yaw));
        addEntityPacket.setHeadRotation(headYaw);
        addEntityPacket.setBodyRotation(yaw);
        addEntityPacket.getMetadata().putFlags(flags);
        dirtyMetadata.apply(addEntityPacket.getMetadata());
        if (propertyManager != null) {
            propertyManager.applyIntProperties(addEntityPacket.getProperties().getIntProperties());
            propertyManager.applyFloatProperties(addEntityPacket.getProperties().getFloatProperties());
        }
        addAdditionalSpawnData(addEntityPacket);

        valid = true;

        session.sendUpstreamPacket(addEntityPacket);

        flagsDirty = false;

        if (session.getGeyser().config().debugMode() && PRINT_ENTITY_SPAWN_DEBUG) {
            EntityType type = definition.entityType();
            std::string name = type != null ? type.name() : getClass().getSimpleName();
            session.getGeyser().getLogger().debug("Spawned entity " + name + " at location " + position + " with id " + geyserId + " (java id " + entityId + ")");
        }
    }


    public void addAdditionalSpawnData(AddEntityPacket addEntityPacket) {
    }


    public void despawnEntity() {
        if (!valid) return;

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
    }

    public void moveRelative(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, bool isOnGround) {
        moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
    }

    public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, bool isOnGround) {
        if (this instanceof ClientVehicle clientVehicle) {
            if (clientVehicle.shouldSimulateMovement()) {
                return;
            }
            clientVehicle.getVehicleComponent().moveRelative(relX, relY, relZ);
        }

        setPosition(position.add(relX, relY, relZ));
        setOnGround(isOnGround);

        bool dirtyPitch = false, dirtyYaw = false, dirtyHeadYaw = false;
        if (pitch != this.pitch) {
            this.pitch = pitch;
            dirtyPitch = true;
        }

        if (yaw != this.yaw) {
            this.yaw = yaw;
            dirtyYaw = true;
        }

        if (headYaw != this.headYaw) {
            this.headYaw = headYaw;
            dirtyHeadYaw = true;
        }

        if (isValid()) {
            MoveEntityDeltaPacket moveEntityPacket = new MoveEntityDeltaPacket();
            moveEntityPacket.setRuntimeEntityId(geyserId);
            if (relX != 0.0) {
                moveEntityPacket.setX(bedrockPosition().getX());
                moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_X);
            }
            if (relY != 0.0) {
                moveEntityPacket.setY(bedrockPosition().getY());
                moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Y);
            }
            if (relZ != 0.0) {
                moveEntityPacket.setZ(bedrockPosition().getZ());
                moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_Z);
            }
            if (dirtyPitch) {
                moveEntityPacket.setPitch(pitch);
                moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_PITCH);
            }
            if (dirtyYaw) {
                moveEntityPacket.setYaw(yaw);
                moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_YAW);
            }
            if (dirtyHeadYaw) {
                moveEntityPacket.setHeadYaw(headYaw);
                moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.HAS_HEAD_YAW);
            }
            if (isOnGround) {
                moveEntityPacket.getFlags().add(MoveEntityDeltaPacket.Flag.ON_GROUND);
            }
            session.sendUpstreamPacket(moveEntityPacket);
        }
    }

    public void moveAbsolute(Vector3f position, float yaw, float pitch, bool isOnGround, bool teleported) {
        moveAbsolute(position, yaw, pitch, getHeadYaw(), isOnGround, teleported);
    }

    public void moveAbsolute(Vector3f position, float yaw, float pitch, float headYaw, bool isOnGround, bool teleported) {
        moveAbsoluteRaw(position, yaw, pitch, headYaw, isOnGround, teleported);
    }

    public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, bool isOnGround, bool teleported) {
        setPosition(position);

        setYaw(yaw);
        setPitch(pitch);
        setHeadYaw(headYaw);
        setOnGround(isOnGround);

        if (isValid()) {
            MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
            moveEntityPacket.setRuntimeEntityId(geyserId);
            moveEntityPacket.setPosition(bedrockPosition());
            moveEntityPacket.setRotation(bedrockRotation());
            moveEntityPacket.setOnGround(isOnGround);
            moveEntityPacket.setTeleported(teleported);

            session.sendUpstreamPacket(moveEntityPacket);
        }
    }

    public Vector3f position() {
        return position;
    }


    public void teleport(Vector3f position, float yaw, float pitch, bool isOnGround) {

        moveAbsolute(position, yaw, pitch, yaw, isOnGround, false);
    }


    public void updateHeadLookRotation(float headYaw) {
        moveRelativeRaw(0, 0, 0, getYaw(), getPitch(), headYaw, isOnGround());
    }


    public void updatePositionAndRotation(double moveX, double moveY, double moveZ, float yaw, float pitch, bool isOnGround) {
        moveRelative(moveX, moveY, moveZ, yaw, pitch, getHeadYaw(), isOnGround);
    }


    public void updateRotation(float yaw, float pitch, bool isOnGround) {
        updatePositionAndRotation(0, 0, 0, yaw, pitch, isOnGround);
    }

    public final bool getFlag(EntityFlag flag) {
        Boolean value = this.flags.get(flag);
        return value != null && value;
    }


    public final void setFlag(EntityFlag flag, bool value) {
        Boolean previous = this.flags.get(flag);
        if (previous == null || value != previous) {
            flagsDirty = true;
        }
        this.flags.put(flag, value);
    }


    public void updateBedrockMetadata() {
        if (!isValid()) {
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
            if (propertyManager != null && propertyManager.hasProperties()) {
                propertyManager.applyIntProperties(entityDataPacket.getProperties().getIntProperties());
                propertyManager.applyFloatProperties(entityDataPacket.getProperties().getFloatProperties());
            }
            session.sendUpstreamPacket(entityDataPacket);
        }
    }


    public void updateBedrockEntityProperties() {
        if (!valid) {
            return;
        }

        if (propertyManager != null && propertyManager.hasProperties()) {
            SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
            entityDataPacket.setRuntimeEntityId(geyserId);
            propertyManager.applyIntProperties(entityDataPacket.getProperties().getIntProperties());
            propertyManager.applyFloatProperties(entityDataPacket.getProperties().getFloatProperties());
            session.sendUpstreamPacket(entityDataPacket);
        }
    }

    public void setFlags(ByteEntityMetadata entityMetadata) {
        byte xd = entityMetadata.getPrimitiveValue();
        setFlag(EntityFlag.ON_FIRE, ((xd & 0x01) == 0x01) && !getFlag(EntityFlag.FIRE_IMMUNE)); // Otherwise immune entities sometimes flicker onfire
        setSneaking((xd & 0x02) == 0x02);
        setSprinting((xd & 0x08) == 0x08);

        setGliding((xd & 0x80) == 0x80);
        setInvisible((xd & 0x20) == 0x20);
    }


    protected void setInvisible(bool value) {
        setFlag(EntityFlag.INVISIBLE, value);
    }


    protected void setGliding(bool value) {
        setFlag(EntityFlag.GLIDING, value);
    }

    protected void setSprinting(bool value) {
        setFlag(EntityFlag.SPRINTING, value);
    }

    protected void setSneaking(bool value) {
        setFlag(EntityFlag.SNEAKING, value);
    }


    public final void setAir(IntEntityMetadata entityMetadata) {
        setAirSupply(entityMetadata.getPrimitiveValue());
    }

    protected void setAirSupply(int amount) {
        dirtyMetadata.put(EntityDataTypes.AIR_SUPPLY, (short) MathUtils.constrain(amount, 0, getMaxAir()));
    }

    protected short getMaxAir() {
        return 300;
    }

    public std::string teamIdentifier() {



        return uuid != null ? uuid.toString() : null;
    }

    public void setCustomName(EntityMetadata<Optional<Component>, ?> entityMetadata) {



        Optional<Component> name = entityMetadata.getValue();
        if (name.isPresent()) {
            this.customName = MessageTranslator.convertMessage(name.get(), session.locale());
            setNametag(customName, true);
            return;
        }
        this.customName = null;
        setNametag(getDisplayName(customNameVisible), true);
    }

    protected std::string standardDisplayName() {
        return EntityUtils.translatedEntityName(definition.entityType(), session);
    }

    protected void setNametag(std::string nametag, bool applyTeamStyling) {

        if (nametag != null && applyTeamStyling) {
            var team = session.getWorldCache().getScoreboard().getTeamFor(teamIdentifier());
            if (team != null) {
                updateNametag(team);
                return;
            }
        }

        if (nametag == null) {
            nametag = "";
        }
        bool changed = !Objects.equals(this.nametag, nametag);
        this.nametag = nametag;

        if (!changed) {
            return;
        }

        setNameEntityData(nametag);

        scoreVisibility(!nametag.isEmpty());
    }


    protected void setNameEntityData(std::string nametag) {
        dirtyMetadata.put(EntityDataTypes.NAME, nametag);
    }

    public void updateNametag(Team team) {

        updateNametag(team, true);
    }

    protected void updateNametag(Team team, bool visible) {
        if (team != null) {
            std::string newNametag;

            if (visible) {
                newNametag = team.displayName(getDisplayName(true));
            } else {

                newNametag = "";
            }
            setNametag(newNametag, false);
            return;
        }

        setNametag(getDisplayName(customNameVisible), false);
    }

    protected void scoreVisibility(bool show) {}

    /*
     * Returns the appropriate name to use for nametags:
     * Either the custom name, or default type name fallback
     * Mirrors Mojmap Entity#getName
     */
    public std::string getDisplayName(bool includeStandardName) {
        if (this.customName != null) {
            return this.customName;
        }
        if (includeStandardName) {
            return standardDisplayName();
        }
        return null;
    }

    public void setCustomNameVisible(BooleanEntityMetadata entityMetadata) {
        this.customNameVisible = entityMetadata.getPrimitiveValue();


        setNametag(getDisplayName(customNameVisible), true);
        setNametagAlwaysShow(customNameVisible);
    }

    public void setNametagAlwaysShow(bool value) {
        dirtyMetadata.put(EntityDataTypes.NAMETAG_ALWAYS_SHOW, (byte) (value ? 1 : 0));
    }

    public final void setSilent(BooleanEntityMetadata entityMetadata) {
        silent = entityMetadata.getPrimitiveValue();
    }

    public void setGravity(BooleanEntityMetadata entityMetadata) {
        setFlag(EntityFlag.HAS_GRAVITY, !entityMetadata.getPrimitiveValue());
    }


    public void setPose(Pose pose) {
        setFlag(EntityFlag.SLEEPING, pose.equals(Pose.SLEEPING));


        setFlag(EntityFlag.SWIMMING, pose.equals(Pose.SWIMMING));
        setDimensionsFromPose(pose);
    }

    /**
     * Set the height and width of the entity's bounding box
     */
    protected void setDimensionsFromPose(Pose pose) {

        setBoundingBoxHeight(definition.height());
        setBoundingBoxWidth(definition.width());
    }

    public bool setBoundingBoxHeight(float height) {
        if (height != boundingBoxHeight) {
            boundingBoxHeight = height;
            dirtyMetadata.put(EntityDataTypes.HEIGHT, boundingBoxHeight);

            updatePassengerOffsets();
            return true;
        }
        return false;
    }

    public void setBoundingBoxWidth(float width) {
        if (width != boundingBoxWidth) {
            boundingBoxWidth = width;
            dirtyMetadata.put(EntityDataTypes.WIDTH, boundingBoxWidth);
        }
    }

    /**
     * Set a float from 0-1 - how strong the "frozen" overlay should be on screen.
     */
    public float setFreezing(IntEntityMetadata entityMetadata) {


        int freezingTicks = Math.min(entityMetadata.getPrimitiveValue(), 140);
        float freezingPercentage = freezingTicks / 140f;
        dirtyMetadata.put(EntityDataTypes.FREEZING_EFFECT_STRENGTH, freezingPercentage);
        return freezingPercentage;
    }

    public void setRiderSeatPosition(Vector3f position) {
        dirtyMetadata.put(EntityDataTypes.SEAT_OFFSET, position);
    }

    /**
     * If true, the entity should be shaking on the client's end.
     *
     * @return whether {@link EntityFlag#SHAKING} should be set to true.
     */
    protected bool isShaking() {
        return false;
    }
    /**
     * If true, the entity can be dismounted by pressing jump.
     *
     * @return whether the entity can be dismounted when pressing jump.
     */
    public bool doesJumpDismount() {
        return true;
    }

    /**
     * x = Pitch, y = Yaw, z = HeadYaw
     *
     * @return the bedrock rotation
     */
    public Vector3f bedrockRotation() {
        return Vector3f.from(getPitch(), getYaw(), getHeadYaw());
    }

    /**
     * Gets the Bedrock edition position with the offset applied
     */
    public Vector3f bedrockPosition() {
        if (offset == 0f) {
            return position;
        }
        return position.up(offset);
    }

    /**
     * Update the mount offsets of each passenger on this vehicle
     */
    protected void updatePassengerOffsets() {
        for (int i = 0; i < passengers.size(); i++) {
            Entity passenger = passengers.get(i);
            if (passenger != null) {
                bool rider = i == 0;
                EntityUtils.updateMountOffset(passenger, this, rider, true, i, passengers.size());
                passenger.updateBedrockMetadata();
            }
        }
    }

    /**
     * Update this entity's mount offset
     */
    protected void updateMountOffset() {
        if (vehicle != null) {
            bool rider = vehicle.getPassengers().get(0) == this;
            EntityUtils.updateMountOffset(this, vehicle, rider, true, vehicle.getPassengers().indexOf(this), vehicle.getPassengers().size());
            updateBedrockMetadata();
        }
    }

    override public int javaId() {
        return entityId;
    }

    public bool isAlive() {
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
        session.getPlayerEntity().getDirtyMetadata().put(EntityDataTypes.INTERACT_TEXT, tag.getValue());
        session.getPlayerEntity().updateBedrockMetadata();
    }

    /**
     * Test interacting with the given hand to see if we should send a tag to the Bedrock client.
     * Should usually mirror {@link #interact(Hand)} without any side effects.
     */
    protected InteractiveTag testInteraction(Hand hand) {
        if (isAlive() && this instanceof Leashable leashable) {
            if (leashable.leashHolderBedrockId() == session.getPlayerEntity().geyserId()) {

                return InteractiveTag.REMOVE_LEASH;
            }
            if (session.getPlayerInventory().getItemInHand(hand).is(Items.LEAD) && leashable.canBeLeashed()) {

                return InteractiveTag.LEASH;
            }
        }

        return InteractiveTag.NONE;
    }

    /**
     * Simulates interacting with an entity. The code here should mirror Java Edition code to the best of its ability,
     * to ensure packet parity as well as functionality parity (such as sound effect responses).
     */
    public InteractionResult interact(Hand hand) {
        Item itemInHand = session.getPlayerInventory().getItemInHand(hand).asItem();
        if (itemInHand == Items.SHEARS) {
            if (hasLeashesToDrop()) {
                return InteractionResult.SUCCESS;
            }

            if (this instanceof MobEntity mob && !session.isSneaking() && mob.canShearEquipment()) {
                return InteractionResult.SUCCESS;
            }
        } else if (isAlive() && this instanceof Leashable leashable) {
            if (leashable.leashHolderBedrockId() == session.getPlayerEntity().geyserId()) {


                return InteractionResult.SUCCESS;
            }
            if (session.getPlayerInventory().getItemInHand(hand).is(Items.LEAD)
                && !(session.getEntityCache().getEntityByGeyserId(leashable.leashHolderBedrockId()) instanceof PlayerEntity)) {

                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    public bool hasLeashesToDrop() {
        BoundingBox searchBB = new BoundingBox(position.getX(), position.getY(), position.getZ(), 32, 32, 32);
        List<Leashable> leashedInRange = session.getEntityCache().getEntities().values().stream()
            .filter(entity -> entity instanceof Leashable leashablex && leashablex.leashHolderBedrockId() == this.geyserId())
            .filter(entity -> {
                BoundingBox leashedBB = new BoundingBox(entity.position().toDouble(), entity.boundingBoxWidth, entity.boundingBoxHeight, entity.boundingBoxWidth);
                return searchBB.checkIntersection(leashedBB);
            }).map(Leashable.class::cast).toList();

        bool found = !leashedInRange.isEmpty();
        if (this instanceof Leashable leashable && leashable.isLeashed()) {
            found = true;
        }

        return found;
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

    override public void updatePropertiesBatched(Consumer<BatchPropertyUpdater> consumer, bool immediate) {
        if (this.propertyManager == null) {
            throw new IllegalArgumentException("Given entity has no registered properties!");
        }

        Objects.requireNonNull(consumer);
        GeyserEntityProperties propertyDefinitions = definition.registeredProperties();
        consumer.accept(new BatchPropertyUpdater() {
            override public <T> void update(GeyserEntityProperty<T> property, T value) {
                Objects.requireNonNull(property, "property must not be null!");
                if (!(property instanceof PropertyType)) {
                    throw new IllegalArgumentException("Invalid property implementation! Got: " + property.getClass().getSimpleName());
                }
                PropertyType<T, ? extends EntityProperty> propertyType = (PropertyType<T, ?>) property;
                int index = propertyDefinitions.getPropertyIndex(property.identifier().toString());
                if (index < 0) {
                    throw new IllegalArgumentException("No property with the name " + property.identifier() + " has been registered.");
                }

                var expectedProperty = propertyDefinitions.getProperties().get(index);
                if (!expectedProperty.equals(propertyType)) {
                    throw new IllegalArgumentException("The supplied property was not registered with this entity type!");
                }

                propertyType.apply(propertyManager, value);
            }
        });

        if (propertyManager.hasProperties()) {
            SetEntityDataPacket packet = new SetEntityDataPacket();
            packet.setRuntimeEntityId(geyserId());
            propertyManager.applyFloatProperties(packet.getProperties().getFloatProperties());
            propertyManager.applyIntProperties(packet.getProperties().getIntProperties());
            if (immediate) {
                session.sendUpstreamPacketImmediately(packet);
            } else {
                session.sendUpstreamPacket(packet);
            }
        }
    }
}
