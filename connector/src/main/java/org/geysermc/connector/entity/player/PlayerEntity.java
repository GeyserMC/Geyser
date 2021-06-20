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

package org.geysermc.connector.entity.player;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Pose;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.PlayerPermission;
import com.nukkitx.protocol.bedrock.data.command.CommandPermission;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.entity.EntityLinkData;
import com.nukkitx.protocol.bedrock.packet.AddPlayerPacket;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityLinkPacket;
import com.nukkitx.protocol.bedrock.packet.UpdateAttributesPacket;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.LivingEntity;
import org.geysermc.connector.entity.attribute.Attribute;
import org.geysermc.connector.entity.attribute.AttributeType;
import org.geysermc.connector.entity.living.animal.tameable.ParrotEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.scoreboard.Team;
import org.geysermc.connector.utils.AttributeUtils;
import org.geysermc.connector.network.translators.chat.MessageTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter @Setter
public class PlayerEntity extends LivingEntity {
    private GameProfile profile;
    private UUID uuid;
    private String username;
    private boolean playerList = true;  // Player is in the player list

    /**
     * Saves the parrot currently on the player's left shoulder; otherwise null
     */
    private ParrotEntity leftParrot;
    /**
     * Saves the parrot currently on the player's right shoulder; otherwise null
     */
    private ParrotEntity rightParrot;

    public PlayerEntity(GameProfile gameProfile, long entityId, long geyserId, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, EntityType.PLAYER, position, motion, rotation);

        profile = gameProfile;
        uuid = gameProfile.getId();
        username = gameProfile.getName();

        // For the OptionalPack, set all bits as invisible by default as this matches Java Edition behavior
        metadata.put(EntityData.MARK_VARIANT, 0xff);
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        AddPlayerPacket addPlayerPacket = new AddPlayerPacket();
        addPlayerPacket.setUuid(uuid);
        addPlayerPacket.setUsername(username);
        addPlayerPacket.setRuntimeEntityId(geyserId);
        addPlayerPacket.setUniqueEntityId(geyserId);
        addPlayerPacket.setPosition(position.clone().sub(0, EntityType.PLAYER.getOffset(), 0));
        addPlayerPacket.setRotation(getBedrockRotation());
        addPlayerPacket.setMotion(motion);
        addPlayerPacket.setHand(hand);
        addPlayerPacket.getAdventureSettings().setCommandPermission(CommandPermission.NORMAL);
        addPlayerPacket.getAdventureSettings().setPlayerPermission(PlayerPermission.MEMBER);
        addPlayerPacket.setDeviceId("");
        addPlayerPacket.setPlatformChatId("");
        addPlayerPacket.getMetadata().putAll(metadata);

        long linkedEntityId = session.getEntityCache().getCachedPlayerEntityLink(entityId);
        if (linkedEntityId != -1) {
            Entity linkedEntity = session.getEntityCache().getEntityByJavaId(linkedEntityId);
            if (linkedEntity != null) {
                addPlayerPacket.getEntityLinks().add(new EntityLinkData(linkedEntity.getGeyserId(), geyserId, EntityLinkData.Type.RIDER, false, false));
            }
        }

        valid = true;
        session.sendUpstreamPacket(addPlayerPacket);

        updateAllEquipment(session);
        updateBedrockAttributes(session);
    }

    public void sendPlayer(GeyserSession session) {
        if (session.getEntityCache().getPlayerEntity(uuid) == null)
            return;

        if (session.getUpstream().isInitialized() && session.getEntityCache().getEntityByGeyserId(geyserId) == null) {
            session.getEntityCache().spawnEntity(this);
        } else {
            spawnEntity(session);
        }
    }

    @Override
    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround, boolean teleported) {
        setPosition(position);
        setRotation(rotation);

        setOnGround(isOnGround);

        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(geyserId);
        movePlayerPacket.setPosition(this.position);
        movePlayerPacket.setRotation(getBedrockRotation());
        movePlayerPacket.setOnGround(isOnGround);
        movePlayerPacket.setMode(teleported ? MovePlayerPacket.Mode.TELEPORT : MovePlayerPacket.Mode.NORMAL);

        if (teleported) {
            movePlayerPacket.setTeleportationCause(MovePlayerPacket.TeleportationCause.UNKNOWN);
        }

        session.sendUpstreamPacket(movePlayerPacket);
        if (leftParrot != null) {
            leftParrot.moveAbsolute(session, position, rotation, true, teleported);
        }
        if (rightParrot != null) {
            rightParrot.moveAbsolute(session, position, rotation, true, teleported);
        }
    }

    @Override
    public void moveRelative(GeyserSession session, double relX, double relY, double relZ, Vector3f rotation, boolean isOnGround) {
        setRotation(rotation);
        this.position = Vector3f.from(position.getX() + relX, position.getY() + relY, position.getZ() + relZ);

        // If this is the player logged in through this Geyser session
        if (geyserId == 1) {
            session.getCollisionManager().updatePlayerBoundingBox(position);
        }
        setOnGround(isOnGround);

        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(geyserId);
        movePlayerPacket.setPosition(position);
        movePlayerPacket.setRotation(getBedrockRotation());
        movePlayerPacket.setOnGround(isOnGround);
        movePlayerPacket.setMode(MovePlayerPacket.Mode.NORMAL);
        // If the player is moved while sleeping, we have to adjust their y, so it appears
        // correctly on Bedrock. This fixes GSit's lay.
        if (metadata.getFlags().getFlag(EntityFlag.SLEEPING)) {
            Vector3i bedPosition = metadata.getPos(EntityData.BED_POSITION);
            if (bedPosition != null && (bedPosition.getY() == 0 || bedPosition.distanceSquared(position.toInt()) > 4)) {
                // Force the player movement by using a teleport
                movePlayerPacket.setPosition(Vector3f.from(position.getX(), position.getY() - entityType.getOffset() + 0.2f, position.getZ()));
                movePlayerPacket.setMode(MovePlayerPacket.Mode.TELEPORT);
                movePlayerPacket.setTeleportationCause(MovePlayerPacket.TeleportationCause.UNKNOWN);
            }
        }
        session.sendUpstreamPacket(movePlayerPacket);
        if (leftParrot != null) {
            leftParrot.moveRelative(session, relX, relY, relZ, rotation, true);
        }
        if (rightParrot != null) {
            rightParrot.moveRelative(session, relX, relY, relZ, rotation, true);
        }
    }

    @Override
    public void updateHeadLookRotation(GeyserSession session, float headYaw) {
        moveRelative(session, 0, 0, 0, Vector3f.from(rotation.getX(), rotation.getY(), headYaw), onGround);
        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(geyserId);
        movePlayerPacket.setPosition(position);
        movePlayerPacket.setRotation(getBedrockRotation());
        movePlayerPacket.setMode(MovePlayerPacket.Mode.HEAD_ROTATION);
        session.sendUpstreamPacket(movePlayerPacket);
    }

    @Override
    public void updatePositionAndRotation(GeyserSession session, double moveX, double moveY, double moveZ, float yaw, float pitch, boolean isOnGround) {
        moveRelative(session, moveX, moveY, moveZ, yaw, pitch, isOnGround);
        if (leftParrot != null) {
            leftParrot.moveRelative(session, moveX, moveY, moveZ, yaw, pitch, isOnGround);
        }
        if (rightParrot != null) {
            rightParrot.moveRelative(session, moveX, moveY, moveZ, yaw, pitch, isOnGround);
        }
    }

    @Override
    public void updateRotation(GeyserSession session, float yaw, float pitch, boolean isOnGround) {
        super.updateRotation(session, yaw, pitch, isOnGround);
        // Both packets need to be sent or else player head rotation isn't correctly updated
        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(geyserId);
        movePlayerPacket.setPosition(position);
        movePlayerPacket.setRotation(getBedrockRotation());
        movePlayerPacket.setOnGround(isOnGround);
        movePlayerPacket.setMode(MovePlayerPacket.Mode.HEAD_ROTATION);
        session.sendUpstreamPacket(movePlayerPacket);
        if (leftParrot != null) {
            leftParrot.updateRotation(session, yaw, pitch, isOnGround);
        }
        if (rightParrot != null) {
            rightParrot.updateRotation(session, yaw, pitch, isOnGround);
        }
    }

    @Override
    public void setPosition(Vector3f position) {
        setPosition(position, true);
    }

    /**
     * Set the player position and specify if the entity type's offset should be added. Set to false when the player
     * sends us a move packet where the offset is already added
     *
     * @param position the new position of the Bedrock player
     * @param includeOffset whether to include the offset
     */
    public void setPosition(Vector3f position, boolean includeOffset) {
        this.position = includeOffset ? position.add(0, entityType.getOffset(), 0) : position;
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        super.updateBedrockMetadata(entityMetadata, session);

        if (entityMetadata.getId() == 2) {
            String username = this.username;
            Component name = (Component) entityMetadata.getValue();
            if (name != null) {
                username = MessageTranslator.convertMessage(name);
            }
            Team team = session.getWorldCache().getScoreboard().getTeamFor(username);
            if (team != null) {
                String displayName = "";
                if (team.isVisibleFor(session.getPlayerEntity().getUsername())) {
                    displayName = MessageTranslator.toChatColor(team.getColor()) + username;
                    displayName = team.getCurrentData().getDisplayName(displayName);
                }
                metadata.put(EntityData.NAMETAG, displayName);
            }
        }

        // Extra hearts - is not metadata but an attribute on Bedrock
        if (entityMetadata.getId() == 15) {
            UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
            attributesPacket.setRuntimeEntityId(geyserId);
            List<AttributeData> attributes = new ArrayList<>();
            // Setting to a higher maximum since plugins/datapacks can probably extend the Bedrock soft limit
            attributes.add(new AttributeData("minecraft:absorption", 0.0f, 1024f, (float) entityMetadata.getValue(), 0.0f));
            attributesPacket.setAttributes(attributes);
            session.sendUpstreamPacket(attributesPacket);
        }

        if (entityMetadata.getId() == 17) {
            // OptionalPack usage for toggling skin bits
            // In Java Edition, a bit being set means that part should be enabled
            // However, to ensure that the pack still works on other servers, we invert the bit so all values by default
            // are true (0).
            metadata.put(EntityData.MARK_VARIANT, ~((byte) entityMetadata.getValue()) & 0xff);
        }

        // Parrot occupying shoulder
        if (entityMetadata.getId() == 19 || entityMetadata.getId() == 20) {
            CompoundTag tag = (CompoundTag) entityMetadata.getValue();
            if (tag != null && !tag.isEmpty()) {
                if ((entityMetadata.getId() == 19 && leftParrot != null) || (entityMetadata.getId() == 20 && rightParrot != null)) {
                    // No need to update a parrot's data when it already exists
                    return;
                }
                // The parrot is a separate entity in Bedrock, but part of the player entity in Java
                ParrotEntity parrot = new ParrotEntity(0, session.getEntityCache().getNextEntityId().incrementAndGet(),
                        EntityType.PARROT, position, motion, rotation);
                parrot.spawnEntity(session);
                parrot.getMetadata().put(EntityData.VARIANT, tag.get("Variant").getValue());
                // Different position whether the parrot is left or right
                float offset = (entityMetadata.getId() == 18) ? 0.4f : -0.4f;
                parrot.getMetadata().put(EntityData.RIDER_SEAT_POSITION, Vector3f.from(offset, -0.22, -0.1));
                parrot.getMetadata().put(EntityData.RIDER_ROTATION_LOCKED, 1);
                parrot.updateBedrockMetadata(session);
                SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
                EntityLinkData.Type type = (entityMetadata.getId() == 18) ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER;
                linkPacket.setEntityLink(new EntityLinkData(geyserId, parrot.getGeyserId(), type, false));
                // Delay, or else spawned-in players won't get the link
                // TODO: Find a better solution. This problem also exists with item frames
                session.getConnector().getGeneralThreadPool().schedule(() -> session.sendUpstreamPacket(linkPacket), 500, TimeUnit.MILLISECONDS);
                if (entityMetadata.getId() == 18) {
                    leftParrot = parrot;
                } else {
                    rightParrot = parrot;
                }
            } else {
                Entity parrot = (entityMetadata.getId() == 19 ? leftParrot : rightParrot);
                if (parrot != null) {
                    parrot.despawnEntity(session);
                    if (entityMetadata.getId() == 19) {
                        leftParrot = null;
                    } else {
                        rightParrot = null;
                    }
                }
            }
        }
    }

    @Override
    protected void setDimensions(Pose pose) {
        float height;
        switch (pose) {
            case SNEAKING:
                height = 1.5f;
                break;
            case FALL_FLYING:
            case SPIN_ATTACK:
            case SWIMMING:
                height = 0.6f;
                break;
            default:
                super.setDimensions(pose);
                return;
        }
        metadata.put(EntityData.BOUNDING_BOX_WIDTH, entityType.getWidth());
        metadata.put(EntityData.BOUNDING_BOX_HEIGHT, height);
    }

    @Override
    public void updateBedrockAttributes(GeyserSession session) { // TODO: Don't use duplicated code
        if (!valid) return;

        List<AttributeData> attributes = new ArrayList<>();
        for (Map.Entry<AttributeType, Attribute> entry : this.attributes.entrySet()) {
            if (!entry.getValue().getType().isBedrockAttribute())
                continue;

            attributes.add(AttributeUtils.getBedrockAttribute(entry.getValue()));
        }

        UpdateAttributesPacket updateAttributesPacket = new UpdateAttributesPacket();
        updateAttributesPacket.setRuntimeEntityId(geyserId);
        updateAttributesPacket.setAttributes(attributes);
        session.sendUpstreamPacket(updateAttributesPacket);
    }
}
