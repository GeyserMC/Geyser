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

package org.geysermc.geyser.entity.type.player;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Pose;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.FloatEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.scoreboard.ScoreboardPosition;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamColor;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import com.nukkitx.protocol.bedrock.data.PlayerPermission;
import com.nukkitx.protocol.bedrock.data.command.CommandPermission;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.entity.EntityLinkData;
import com.nukkitx.protocol.bedrock.packet.*;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.entity.type.living.animal.tameable.ParrotEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.scoreboard.Objective;
import org.geysermc.geyser.scoreboard.Score;
import org.geysermc.geyser.scoreboard.Team;
import org.geysermc.geyser.scoreboard.UpdateType;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Getter @Setter
public class PlayerEntity extends LivingEntity {
    private GameProfile profile;
    private String username;
    private boolean playerList = true;  // Player is in the player list

    private Vector3i bedPosition;

    /**
     * Saves the parrot currently on the player's left shoulder; otherwise null
     */
    private ParrotEntity leftParrot;
    /**
     * Saves the parrot currently on the player's right shoulder; otherwise null
     */
    private ParrotEntity rightParrot;

    public PlayerEntity(GeyserSession session, long entityId, long geyserId, GameProfile gameProfile, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, gameProfile.getId(), EntityDefinitions.PLAYER, position, motion, yaw, pitch, headYaw);

        profile = gameProfile;
        username = gameProfile.getName();
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        // For the OptionalPack, set all bits as invisible by default as this matches Java Edition behavior
        dirtyMetadata.put(EntityData.MARK_VARIANT, 0xff);
    }

    @Override
    public void spawnEntity() {
        // Check to see if the player should have a belowname counterpart added
        Objective objective = session.getWorldCache().getScoreboard().getObjectiveSlots().get(ScoreboardPosition.BELOW_NAME);
        if (objective != null) {
            setBelowNameText(objective);
        }

        // The name can't be updated later (the entity metadata for it is ignored), so we need to check for this now
        updateDisplayName(null, false);

        AddPlayerPacket addPlayerPacket = new AddPlayerPacket();
        addPlayerPacket.setUuid(uuid);
        addPlayerPacket.setUsername(username);
        addPlayerPacket.setRuntimeEntityId(geyserId);
        addPlayerPacket.setUniqueEntityId(geyserId);
        addPlayerPacket.setPosition(position.sub(0, definition.offset(), 0));
        addPlayerPacket.setRotation(getBedrockRotation());
        addPlayerPacket.setMotion(motion);
        addPlayerPacket.setHand(hand);
        addPlayerPacket.getAdventureSettings().setCommandPermission(CommandPermission.NORMAL);
        addPlayerPacket.getAdventureSettings().setPlayerPermission(PlayerPermission.MEMBER);
        addPlayerPacket.setDeviceId("");
        addPlayerPacket.setPlatformChatId("");
        addPlayerPacket.getMetadata().putFlags(flags);
        dirtyMetadata.apply(addPlayerPacket.getMetadata());

        setFlagsDirty(false);

        long linkedEntityId = session.getEntityCache().getCachedPlayerEntityLink(entityId);
        if (linkedEntityId != -1) {
            Entity linkedEntity = session.getEntityCache().getEntityByJavaId(linkedEntityId);
            if (linkedEntity != null) {
                addPlayerPacket.getEntityLinks().add(new EntityLinkData(linkedEntity.getGeyserId(), geyserId, EntityLinkData.Type.RIDER, false, false));
            }
        }

        valid = true;
        session.sendUpstreamPacket(addPlayerPacket);
    }

    public void sendPlayer() {
        if (session.getEntityCache().getPlayerEntity(uuid) == null)
            return;

        if (session.getEntityCache().getEntityByGeyserId(geyserId) == null) {
            session.getEntityCache().spawnEntity(this);
        } else {
            spawnEntity();
        }
    }

    @Override
    public void moveAbsolute(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        setPosition(position);
        setYaw(yaw);
        setPitch(pitch);
        setHeadYaw(headYaw);

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
            leftParrot.moveAbsolute(position, yaw, pitch, headYaw, true, teleported);
        }
        if (rightParrot != null) {
            rightParrot.moveAbsolute(position, yaw, pitch, headYaw, true, teleported);
        }
    }

    @Override
    public void moveRelative(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        setYaw(yaw);
        setPitch(pitch);
        setHeadYaw(headYaw);
        this.position = Vector3f.from(position.getX() + relX, position.getY() + relY, position.getZ() + relZ);

        setOnGround(isOnGround);

        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(geyserId);
        movePlayerPacket.setPosition(position);
        movePlayerPacket.setRotation(getBedrockRotation());
        movePlayerPacket.setOnGround(isOnGround);
        movePlayerPacket.setMode(MovePlayerPacket.Mode.NORMAL);
        // If the player is moved while sleeping, we have to adjust their y, so it appears
        // correctly on Bedrock. This fixes GSit's lay.
        if (getFlag(EntityFlag.SLEEPING)) {
            if (bedPosition != null && (bedPosition.getY() == 0 || bedPosition.distanceSquared(position.toInt()) > 4)) {
                // Force the player movement by using a teleport
                movePlayerPacket.setPosition(Vector3f.from(position.getX(), position.getY() - definition.offset() + 0.2f, position.getZ()));
                movePlayerPacket.setMode(MovePlayerPacket.Mode.TELEPORT);
                movePlayerPacket.setTeleportationCause(MovePlayerPacket.TeleportationCause.UNKNOWN);
            }
        }
        session.sendUpstreamPacket(movePlayerPacket);
        if (leftParrot != null) {
            leftParrot.moveRelative(relX, relY, relZ, yaw, pitch, headYaw, true);
        }
        if (rightParrot != null) {
            rightParrot.moveRelative(relX, relY, relZ, yaw, pitch, headYaw, true);
        }
    }

    @Override
    public void updateHeadLookRotation(float headYaw) {
        moveRelative(0, 0, 0, yaw, pitch, headYaw, onGround);
        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(geyserId);
        movePlayerPacket.setPosition(position);
        movePlayerPacket.setRotation(getBedrockRotation());
        movePlayerPacket.setMode(MovePlayerPacket.Mode.HEAD_ROTATION);
        session.sendUpstreamPacket(movePlayerPacket);
    }

    @Override
    public void updatePositionAndRotation(double moveX, double moveY, double moveZ, float yaw, float pitch, boolean isOnGround) {
        moveRelative(moveX, moveY, moveZ, yaw, pitch, isOnGround);
        if (leftParrot != null) {
            leftParrot.moveRelative(moveX, moveY, moveZ, yaw, pitch, isOnGround);
        }
        if (rightParrot != null) {
            rightParrot.moveRelative(moveX, moveY, moveZ, yaw, pitch, isOnGround);
        }
    }

    @Override
    public void updateRotation(float yaw, float pitch, boolean isOnGround) {
        super.updateRotation(yaw, pitch, isOnGround);
        // Both packets need to be sent or else player head rotation isn't correctly updated
        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(geyserId);
        movePlayerPacket.setPosition(position);
        movePlayerPacket.setRotation(getBedrockRotation());
        movePlayerPacket.setOnGround(isOnGround);
        movePlayerPacket.setMode(MovePlayerPacket.Mode.HEAD_ROTATION);
        session.sendUpstreamPacket(movePlayerPacket);
        if (leftParrot != null) {
            leftParrot.updateRotation(yaw, pitch, isOnGround);
        }
        if (rightParrot != null) {
            rightParrot.updateRotation(yaw, pitch, isOnGround);
        }
    }

    @Override
    public void setPosition(Vector3f position) {
        super.setPosition(position.add(0, definition.offset(), 0));
    }

    @Override
    public Vector3i setBedPosition(EntityMetadata<Optional<Position>, ?> entityMetadata) {
        return bedPosition = super.setBedPosition(entityMetadata);
    }

    public void setAbsorptionHearts(FloatEntityMetadata entityMetadata) {
        // Extra hearts - is not metadata but an attribute on Bedrock
        UpdateAttributesPacket attributesPacket = new UpdateAttributesPacket();
        attributesPacket.setRuntimeEntityId(geyserId);
        // Setting to a higher maximum since plugins/datapacks can probably extend the Bedrock soft limit
        attributesPacket.setAttributes(Collections.singletonList(
                new AttributeData("minecraft:absorption", 0.0f, 1024f, entityMetadata.getPrimitiveValue(), 0.0f)));
        session.sendUpstreamPacket(attributesPacket);
    }

    public void setSkinVisibility(ByteEntityMetadata entityMetadata) {
        // OptionalPack usage for toggling skin bits
        // In Java Edition, a bit being set means that part should be enabled
        // However, to ensure that the pack still works on other servers, we invert the bit so all values by default
        // are true (0).
        dirtyMetadata.put(EntityData.MARK_VARIANT, ~entityMetadata.getPrimitiveValue() & 0xff);
    }

    public void setLeftParrot(EntityMetadata<CompoundTag, ?> entityMetadata) {
        setParrot(entityMetadata.getValue(), true);
    }

    public void setRightParrot(EntityMetadata<CompoundTag, ?> entityMetadata) {
        setParrot(entityMetadata.getValue(), false);
    }

    /**
     * Sets the parrot occupying the shoulder. Bedrock Edition requires a full entity whereas Java Edition just
     * spawns it from the NBT data provided
     */
    private void setParrot(CompoundTag tag, boolean isLeft) {
        if (tag != null && !tag.isEmpty()) {
            if ((isLeft && leftParrot != null) || (!isLeft && rightParrot != null)) {
                // No need to update a parrot's data when it already exists
                return;
            }
            // The parrot is a separate entity in Bedrock, but part of the player entity in Java //TODO is a UUID provided in NBT?
            ParrotEntity parrot = new ParrotEntity(session, 0, session.getEntityCache().getNextEntityId().incrementAndGet(),
                    null, EntityDefinitions.PARROT, position, motion, yaw, pitch, headYaw);
            parrot.spawnEntity();
            parrot.getDirtyMetadata().put(EntityData.VARIANT, tag.get("Variant").getValue());
            // Different position whether the parrot is left or right
            float offset = isLeft ? 0.4f : -0.4f;
            parrot.getDirtyMetadata().put(EntityData.RIDER_SEAT_POSITION, Vector3f.from(offset, -0.22, -0.1));
            parrot.getDirtyMetadata().put(EntityData.RIDER_ROTATION_LOCKED, 1);
            parrot.updateBedrockMetadata();
            SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
            EntityLinkData.Type type = isLeft ? EntityLinkData.Type.RIDER : EntityLinkData.Type.PASSENGER;
            linkPacket.setEntityLink(new EntityLinkData(geyserId, parrot.getGeyserId(), type, false, false));
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
    public void setDisplayName(EntityMetadata<Optional<Component>, ?> entityMetadata) {
        // Doesn't do anything for players
    }

    //todo this will become common entity logic once UUID support is implemented for them
    /**
     * @param useGivenTeam even if there is no team, update the username in the entity metadata anyway, and don't look for a team
     */
    public void updateDisplayName(@Nullable Team team, boolean useGivenTeam) {
        if (team == null && !useGivenTeam) {
            // Only search for the team if we are not supposed to use the given team
            // If the given team is null, this is intentional that we are being removed from the team
            team = session.getWorldCache().getScoreboard().getTeamFor(username);
        }

        boolean needsUpdate;
        String newDisplayName = this.username;
        if (team != null) {
            if (team.isVisibleFor(session.getPlayerEntity().getUsername())) {
                TeamColor color = team.getColor();
                String chatColor;
                if (color == TeamColor.NONE) {
                    chatColor = ChatColor.RESET;
                } else {
                    chatColor = MessageTranslator.toChatColor(color);
                }
                // We have to emulate what modern Java text already does for us and add the color to each section
                String prefix = team.getCurrentData().getPrefix();
                String suffix = team.getCurrentData().getSuffix();
                newDisplayName = chatColor + prefix + chatColor + this.username + chatColor + suffix;
            } else {
                // The name is not visible to the session player; clear name
                newDisplayName = "";
            }
            needsUpdate = useGivenTeam && !newDisplayName.equals(nametag);
            nametag = newDisplayName;
            dirtyMetadata.put(EntityData.NAMETAG, newDisplayName);
        } else if (useGivenTeam) {
            // The name has reset, if it was previously something else
            needsUpdate = !newDisplayName.equals(nametag);
            dirtyMetadata.put(EntityData.NAMETAG, this.username);
        } else {
            needsUpdate = false;
        }

        if (needsUpdate) {
            // Update the metadata as it won't be updated later
            SetEntityDataPacket packet = new SetEntityDataPacket();
            packet.getMetadata().put(EntityData.NAMETAG, newDisplayName);
            packet.setRuntimeEntityId(geyserId);
            session.sendUpstreamPacket(packet);
        }
    }

    @Override
    public void setDisplayNameVisible(BooleanEntityMetadata entityMetadata) {
        // Doesn't do anything for players
    }

    @Override
    protected void setDimensions(Pose pose) {
        float height;
        switch (pose) {
            case SNEAKING -> height = 1.5f;
            case FALL_FLYING, SPIN_ATTACK, SWIMMING -> height = 0.6f;
            default -> {
                super.setDimensions(pose);
                return;
            }
        }
        setBoundingBoxWidth(definition.width());
        setBoundingBoxHeight(height);
    }

    public void setBelowNameText(Objective objective) {
        if (objective != null && objective.getUpdateType() != UpdateType.REMOVE) {
            int amount;
            Score score = objective.getScores().get(username);
            if (score != null) {
                amount = score.getCurrentData().getScore();
            } else {
                amount = 0;
            }
            String displayString = amount + " " + objective.getDisplayName();

            if (valid) {
                // Already spawned - we still need to run the rest of this code because the spawn packet will be
                // providing the information
                SetEntityDataPacket packet = new SetEntityDataPacket();
                packet.setRuntimeEntityId(geyserId);
                packet.getMetadata().put(EntityData.SCORE_TAG, displayString);
                session.sendUpstreamPacket(packet);
            }
        } else if (valid) {
            SetEntityDataPacket packet = new SetEntityDataPacket();
            packet.setRuntimeEntityId(geyserId);
            packet.getMetadata().put(EntityData.SCORE_TAG, "");
            session.sendUpstreamPacket(packet);
        }
    }
}
