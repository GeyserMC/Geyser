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
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.entity.type.player.GeyserPlayerEntity;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.living.animal.tameable.ParrotEntity;
import org.geysermc.geyser.util.PlayerListUtils;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata;

import java.util.Collections;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter @Setter
public class PlayerEntity extends AvatarEntity implements GeyserPlayerEntity {

    
    private @Nullable ParrotEntity leftParrot;
    
    private @Nullable ParrotEntity rightParrot;

    
    private boolean listed = false;

    public PlayerEntity(EntitySpawnContext context, GameProfile profile) {
        super(context, profile.getName());
        this.customNameVisible = true;
        try {
            this.textures = profile.getTextures(true);
        } catch (Exception e) {
            GeyserImpl.getInstance().getLogger().debug("Error loading textures for player!" + profile, e);
            this.textures = null;
        }
    }

    public PlayerEntity(EntitySpawnContext context, String username, @Nullable Map<GameProfile.TextureType, GameProfile.Texture> textureMap) {
        super(context, username);
        this.customNameVisible = true;
        this.textures = textureMap;
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();

        
        
        dirtyMetadata.put(EntityDataTypes.NAMETAG_ALWAYS_SHOW, (byte) 1);
    }

    @Override
    public void despawnEntity() {
        super.despawnEntity();

        
        
        
        if (PlayerListUtils.shouldLimitPlayerListEntries(session)) {
            PlayerListPacket packet = new PlayerListPacket();
            packet.getEntries().add(new PlayerListPacket.Entry(getTabListUuid()));
            packet.setAction(PlayerListPacket.Action.REMOVE);
            session.sendUpstreamPacket(packet);

            
            
            session.getWaypointCache().unlistPlayer(this);
        }

        
        this.resetMetadata();
        this.nametag = username;

        this.equipment.clear();
    }

    public void resetMetadata() {
        
        
        this.flags.clear();
        this.initializeMetadata();

        
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
        
        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
        attributesPacket.setRuntimeEntityId(geyserId);
        
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

    
    protected void setParrot(OptionalInt variant, boolean isLeft) {
        if (variant.isPresent()) {
            if ((isLeft && leftParrot != null) || (!isLeft && rightParrot != null)) {
                
                return;
            }
            
            EntitySpawnContext context = EntitySpawnContext.inherited(session, EntityDefinitions.PARROT, this, position);
            ParrotEntity parrot = new ParrotEntity(context);
            parrot.spawnEntity();
            parrot.getDirtyMetadata().put(EntityDataTypes.VARIANT, variant.getAsInt());
            
            float offset = isLeft ? 0.4f : -0.4f;
            parrot.getDirtyMetadata().put(EntityDataTypes.SEAT_OFFSET, Vector3f.from(offset, -0.22, -0.1));
            parrot.getDirtyMetadata().put(EntityDataTypes.SEAT_LOCK_RIDER_ROTATION, true);
            parrot.updateBedrockMetadata();
            SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
            EntityLinkData.Type type = isLeft ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER;
            linkPacket.setEntityLink(new EntityLinkData(geyserId, parrot.geyserId(), type, false, false, 0f));
            
            
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

    @Override
    protected void setNametag(@Nullable String nametag, boolean applyTeamStyling) {
        
        
        if (nametag == null && !applyTeamStyling) {
            
            nametag = username;
        }
        super.setNametag(nametag, applyTeamStyling);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    
    public UUID getTabListUuid() {
        return uuid();
    }

    
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
