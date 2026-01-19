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

package org.geysermc.geyser.entity.type.player;

import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.packet.PlayerListPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.geyser.api.entity.type.player.GeyserPlayerEntity;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.living.animal.tameable.ParrotEntity;
import org.geysermc.geyser.util.PlayerListUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata;

import java.util.Collections;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter @Setter
public class PlayerEntity extends AvatarEntity implements GeyserPlayerEntity {

    /**
     * Saves the parrot currently on the player's left shoulder; otherwise null
     */
    private @Nullable ParrotEntity leftParrot;
    /**
     * Saves the parrot currently on the player's right shoulder; otherwise null
     */
    private @Nullable ParrotEntity rightParrot;

    /**
     * Whether this player is currently listed.
     */
    private boolean listed = false;

    public PlayerEntity(EntitySpawnContext context, String username, @Nullable String texturesProperty) {
        super(context, username);
        this.texturesProperty = texturesProperty;
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();

        // Since 1.20.60, the nametag does not show properly if this is not set :/
        // The nametag does disappear properly when the player is invisible though.
        dirtyMetadata.put(EntityDataTypes.NAMETAG_ALWAYS_SHOW, (byte) 1);
    }

    @Override
    public void despawnEntity() {
        super.despawnEntity();

        // Remove the entries if limited entries are desired
        // While we do not explicitly list players, the skin loading code would
        // send the player list entry after fetching the skin sent by the Java server.
        if (PlayerListUtils.shouldLimitPlayerListEntries(session)) {
            PlayerListPacket packet = new PlayerListPacket();
            packet.getEntries().add(new PlayerListPacket.Entry(getTabListUuid()));
            packet.setAction(PlayerListPacket.Action.REMOVE);
            session.sendUpstreamPacket(packet);

            // To ensure waypoints still remain, if any were added while the
            // player had a valid player list entry
            session.getWaypointCache().unlistPlayer(this);
        }

        // Since we re-use player entities: Clear flags, held item, etc
        this.resetMetadata();
        this.nametag = username;

        this.equipment.clear();
    }

    public void resetMetadata() {
        // Reset all metadata to their default values
        // This is used when a player respawns
        this.flags.clear();
        this.initializeMetadata();

        // Explicitly reset all metadata not handled by initializeMetadata
        setParrot(OptionalInt.empty(), true);
        setParrot(OptionalInt.empty(), false);
    }

    public void sendPlayer() {
        if (session.getEntityCache().getPlayerEntity(uuid) == null)
            return;

        session.getEntityCache().spawnEntity(this);
    }

    @Override
    public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        super.moveAbsoluteRaw(position, yaw, pitch, headYaw, isOnGround, teleported);
        if (leftParrot != null) {
            leftParrot.moveAbsoluteRaw(position, yaw, pitch, headYaw, true, teleported);
        }
        if (rightParrot != null) {
            rightParrot.moveAbsoluteRaw(position, yaw, pitch, headYaw, true, teleported);
        }
    }

    @Override
    public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        super.moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
        if (leftParrot != null) {
            leftParrot.moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, true);
        }
        if (rightParrot != null) {
            rightParrot.moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, true);
        }
    }

    public void setAbsorptionHearts(FloatEntityMetadata entityMetadata) {
        // Extra hearts - is not metadata but an attribute on Bedrock
        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
        attributesPacket.setRuntimeEntityId(geyserId);
        // Setting to a higher maximum since plugins/datapacks can probably extend the Bedrock soft limit
        attributesPacket.setAttributes(Collections.singletonList(
                GeyserAttributeType.ABSORPTION.getAttribute(entityMetadata.getPrimitiveValue())));
        session.sendUpstreamPacket(attributesPacket);
    }

    public void setLeftParrot(EntityMetadata<OptionalInt, ?> entityMetadata) {
        setParrot(entityMetadata.getValue(), true);
    }

    public void setRightParrot(EntityMetadata<OptionalInt, ?> entityMetadata) {
        setParrot(entityMetadata.getValue(), false);
    }

    /**
     * Sets the parrot occupying the shoulder. Bedrock Edition requires a full entity whereas Java Edition just
     * spawns it from the NBT data provided
     */
    protected void setParrot(OptionalInt variant, boolean isLeft) {
        if (variant.isPresent()) {
            if ((isLeft && leftParrot != null) || (!isLeft && rightParrot != null)) {
                // No need to update a parrot's data when it already exists
                return;
            }
            // The parrot is a separate entity in Bedrock, but part of the player entity in Java
            EntitySpawnContext context = EntitySpawnContext.inherited(session, EntityDefinitions.PARROT, this, position);
            ParrotEntity parrot = new ParrotEntity(context);
            parrot.spawnEntity();
            parrot.getDirtyMetadata().put(EntityDataTypes.VARIANT, variant.getAsInt());
            // Different position whether the parrot is left or right
            float offset = isLeft ? 0.4f : -0.4f;
            parrot.getDirtyMetadata().put(EntityDataTypes.SEAT_OFFSET, Vector3f.from(offset, -0.22, -0.1));
            parrot.getDirtyMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION, true);
            parrot.updateBedrockMetadata();
            SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
            EntityLinkData.Type type = isLeft ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER;
            linkPacket.setEntityLink(new EntityLinkData(geyserId, parrot.geyserId(), type, false, false, 0f));
            // Delay, or else spawned-in players won't get the link
            // TODO: Find a better solution.
            session.scheduleInEventLoop(() -> session.sendUpstreamPacket(linkPacket), 500, TimeUnit.MILLISECONDS);
            if (isLeft) {
                leftParrot = parrot;
            } else {
                rightParrot = parrot;
            }
        } else {
            Entity parrot = isLeft ? leftParrot : rightParrot;
            if (parrot != null) {
                parrot.despawnEntity();
                if (isLeft) {
                    leftParrot = null;
                } else {
                    rightParrot = null;
                }
            }
        }
    }

    @Override
    public String teamIdentifier() {
        return username;
    }

    // TODO test mannequins
    @Override
    protected void setNametag(@Nullable String nametag, boolean fromDisplayName) {
        // when fromDisplayName, LivingEntity will call scoreboard code. After that
        // setNametag is called again with fromDisplayName on false
        if (nametag == null && !fromDisplayName) {
            // nametag = null means reset, so reset it back to username
            nametag = username;
        }
        super.setNametag(nametag, fromDisplayName);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the UUID that should be used when dealing with Bedrock's tab list.
     */
    public UUID getTabListUuid() {
        return uuid();
    }

    @Override
    public Vector3f position() {
        return this.position.down(definition.offset());
    }

    // From 1.21.8 code, should be correct since some pose should be prioritized.
    public Pose getDesiredPose() {
        if (this.getBedPosition() != null) {
            return Pose.SLEEPING;
        } else if (this.getFlag(EntityFlag.SWIMMING) || this.getFlag(EntityFlag.CRAWLING)) {
            return Pose.SWIMMING;
        } else if (this.getFlag(EntityFlag.GLIDING)) {
            return Pose.FALL_FLYING;
        } else if (this.getFlag(EntityFlag.DAMAGE_NEARBY_MOBS)) {
            return Pose.SPIN_ATTACK;
        } else {
            return this.getFlag(EntityFlag.SNEAKING) && !session.isFlying() ? Pose.SNEAKING : Pose.STANDING;
        }
    }
}
