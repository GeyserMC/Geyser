/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.Ability;
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.geysermc.geyser.api.skin.SkinData;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkinManager;
import org.geysermc.geyser.skin.SkullSkinManager;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.util.ChunkUtils;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.ResolvableProfile;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public abstract class AvatarEntity extends LivingEntity {
    public static final float SNEAKING_POSE_HEIGHT = 1.5f;
    protected static final List<AbilityLayer> BASE_ABILITY_LAYER;

    @Getter
    protected String username;

    /**
     * The textures property from the GameProfile.
     */
    @Getter
    @Setter
    @Nullable
    protected String texturesProperty; // TODO no direct setter, rather one that updates the skin

    private String cachedScore = "";
    private boolean scoreVisible = true;

    @Getter
    @Nullable
    private Vector3i bedPosition;

    static {
        AbilityLayer abilityLayer = new AbilityLayer();
        abilityLayer.setLayerType(AbilityLayer.Type.BASE);
        Ability[] abilities = Ability.values();
        Collections.addAll(abilityLayer.getAbilitiesSet(), abilities); // Apparently all the abilities you're working with
        Collections.addAll(abilityLayer.getAbilityValues(), abilities); // Apparently all the abilities the player can work with
        BASE_ABILITY_LAYER = Collections.singletonList(abilityLayer);
    }

    public AvatarEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition,
                        Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw, String username) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
        this.username = username;
        this.nametag = username;
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        // For the OptionalPack, set all bits as invisible by default as this matches Java Edition behavior
        dirtyMetadata.put(EntityDataTypes.MARK_VARIANT, 0xff);
    }

    @Override
    public void spawnEntity() {
        AddPlayerPacket addPlayerPacket = new AddPlayerPacket();
        addPlayerPacket.setUuid(uuid);
        addPlayerPacket.setUsername(username);
        addPlayerPacket.setRuntimeEntityId(geyserId);
        addPlayerPacket.setUniqueEntityId(geyserId);
        addPlayerPacket.setPosition(position.sub(0, definition.offset(), 0));
        addPlayerPacket.setRotation(getBedrockRotation());
        addPlayerPacket.setMotion(motion);
        addPlayerPacket.setHand(ItemTranslator.translateToBedrock(session, getMainHandItem()));
        addPlayerPacket.getAdventureSettings().setCommandPermission(CommandPermission.ANY);
        addPlayerPacket.getAdventureSettings().setPlayerPermission(PlayerPermission.MEMBER);
        addPlayerPacket.setDeviceId("");
        addPlayerPacket.setPlatformChatId("");
        addPlayerPacket.setGameType(GameType.SURVIVAL); //TODO
        addPlayerPacket.setAbilityLayers(BASE_ABILITY_LAYER); // Recommended to be added since 1.19.10, but only needed here for permissions viewing
        addPlayerPacket.getMetadata().putFlags(flags);
        dirtyMetadata.apply(addPlayerPacket.getMetadata());

        setFlagsDirty(false);

        valid = true;
        session.sendUpstreamPacket(addPlayerPacket);
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
        movePlayerPacket.setMode(this instanceof SessionPlayerEntity || teleported ? MovePlayerPacket.Mode.TELEPORT : MovePlayerPacket.Mode.NORMAL);
        if (movePlayerPacket.getMode() == MovePlayerPacket.Mode.TELEPORT) {
            movePlayerPacket.setTeleportationCause(MovePlayerPacket.TeleportationCause.BEHAVIOR);
        }

        session.sendUpstreamPacket(movePlayerPacket);

        if (teleported && !(this instanceof SessionPlayerEntity)) {
            // As of 1.19.0, head yaw seems to be ignored during teleports, also don't do this for session player.
            updateHeadLookRotation(headYaw);
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
        movePlayerPacket.setMode(this instanceof SessionPlayerEntity ? MovePlayerPacket.Mode.TELEPORT : MovePlayerPacket.Mode.NORMAL);
        // If the player is moved while sleeping, we have to adjust their y, so it appears
        // correctly on Bedrock. This fixes GSit's lay.
        if (getFlag(EntityFlag.SLEEPING)) {
            if (bedPosition != null && (bedPosition.getY() == 0 || bedPosition.distanceSquared(position.toInt()) > 4)) {
                // Force the player movement by using a teleport
                movePlayerPacket.setPosition(Vector3f.from(position.getX(), position.getY() - definition.offset() + 0.2f, position.getZ()));
                movePlayerPacket.setMode(MovePlayerPacket.Mode.TELEPORT);
            }
        }

        if (movePlayerPacket.getMode() == MovePlayerPacket.Mode.TELEPORT) {
            movePlayerPacket.setTeleportationCause(MovePlayerPacket.TeleportationCause.BEHAVIOR);
        }

        session.sendUpstreamPacket(movePlayerPacket);
    }

    @Override
    public void setPosition(Vector3f position) {
        if (this.bedPosition != null) {
            // As of Bedrock 1.21.22 and Fabric 1.21.1
            // Messes with Bedrock if we send this to the client itself, though.
            super.setPosition(position.up(0.2f));
        } else {
            super.setPosition(position.add(0, definition.offset(), 0));
        }
    }

    @Override
    public @Nullable Vector3i setBedPosition(EntityMetadata<Optional<Vector3i>, ?> entityMetadata) {
        bedPosition = super.setBedPosition(entityMetadata);
        if (bedPosition != null) {
            // Required to sync position of entity to bed
            // Fixes https://github.com/GeyserMC/Geyser/issues/3595 on vanilla 1.19.3 servers - did not happen on Paper
            this.setPosition(bedPosition.toFloat());

            // TODO evaluate if needed
            int bed = session.getGeyser().getWorldManager().getBlockAt(session, bedPosition);
            // Bed has to be updated, or else player is floating in the air
            ChunkUtils.updateBlock(session, bed, bedPosition);

            // Indicate that the player should enter the sleep cycle
            // Has to be a byte or it does not work
            // (Bed position is what actually triggers sleep - "pose" is only optional)
            dirtyMetadata.put(EntityDataTypes.PLAYER_FLAGS, (byte) 2);
        } else {
            // Player is no longer sleeping
            dirtyMetadata.put(EntityDataTypes.PLAYER_FLAGS, (byte) 0);
            return null;
        }
        return bedPosition;
    }

    public void setSkin(ResolvableProfile profile, boolean cape) {
        SkinManager.resolveProfile(profile).thenAccept(resolved -> setSkin(resolved, cape, null));
    }

    public void setSkin(GameProfile profile, boolean cape, @Nullable Runnable after) {
        GameProfile.Property textures = profile.getProperty("textures");
        if (textures != null) {
            setSkin(textures.getValue(), cape, after);
        } else {
            setSkin((String) null, cape, after);
        }
    }

    public void setSkin(String texturesProperty, boolean cape, @Nullable Runnable after) {
        if (Objects.equals(texturesProperty, this.texturesProperty)) {
            return;
        }

        this.texturesProperty = texturesProperty;
        if (cape) {
            SkinManager.requestAndHandleSkinAndCape(this, session, after == null ? null : skin -> after.run());
        } else {
            SkullSkinManager.requestAndHandleSkin(this, session, after == null ? null :skin -> after.run());
        }
    }

    public void setSkinVisibility(ByteEntityMetadata entityMetadata) {
        // OptionalPack usage for toggling skin bits
        // In Java Edition, a bit being set means that part should be enabled
        // However, to ensure that the pack still works on other servers, we invert the bit so all values by default
        // are true (0).
        dirtyMetadata.put(EntityDataTypes.MARK_VARIANT, ~entityMetadata.getPrimitiveValue() & 0xff);
    }

    @Override
    public String getDisplayName() {
        return username;
    }

    @Override
    public void setDisplayName(EntityMetadata<Optional<Component>, ?> entityMetadata) {
        // Doesn't do anything for players
        if (!(this instanceof PlayerEntity)) {
            super.setDisplayName(entityMetadata);
        }
    }

    @Override
    public void setDisplayNameVisible(BooleanEntityMetadata entityMetadata) {
        // Doesn't do anything for players
        if (!(this instanceof PlayerEntity)) {
            super.setDisplayNameVisible(entityMetadata);
        }
    }

    public void setBelowNameText(String text) {
        if (text == null) {
            text = "";
        }

        boolean changed = !Objects.equals(cachedScore, text);
        cachedScore = text;
        if (scoreVisible && changed) {
            dirtyMetadata.put(EntityDataTypes.SCORE, text);
        }
    }

    /**
     * Whether this entity is listed on the player list.
     * Since player entities are used for e.g. custom skulls too, we need to hack around
     * limitations introduced in 1.21.130 to ensure skins are correctly applied. 
     * @see SkinManager#sendSkinPacket(GeyserSession, AvatarEntity, SkinData)
     * @return whether this player entity is listed
     */
    public abstract boolean isListed();

    @Override
    protected void scoreVisibility(boolean show) {
        boolean visibilityChanged = scoreVisible != show;
        scoreVisible = show;
        if (!visibilityChanged) {
            return;
        }
        // if the player has no cachedScore, we never have to change the score.
        // hide = set to "" (does nothing), show = change from "" (does nothing)
        if (cachedScore.isEmpty()) {
            return;
        }
        dirtyMetadata.put(EntityDataTypes.SCORE, show ? cachedScore : "");
    }

    @Override
    public void setPose(Pose pose) {
        super.setPose(pose);
        setFlag(EntityFlag.SWIMMING, false);
        setFlag(EntityFlag.CRAWLING, false);

        if (pose == Pose.SWIMMING) {
            // This is just for, so we know if player is swimming or crawling.
            if (session.getGeyser().getWorldManager().blockAt(session, position.down(EntityDefinitions.PLAYER.offset()).toInt()).is(Blocks.WATER)) {
                setFlag(EntityFlag.SWIMMING, true);
            } else {
                setFlag(EntityFlag.CRAWLING, true);

                // Look at https://github.com/GeyserMC/Geyser/issues/5316, we're fixing this by spoofing player pitch to 0.
                // Don't do this for session player however, as that teleport them back and messed up their rotation.
                if (!(this instanceof SessionPlayerEntity)) {
                    updateRotation(this.yaw, 0, this.onGround);
                }
            }
        }
    }

    @Override
    public void setPitch(float pitch) {
        super.setPitch(getFlag(EntityFlag.CRAWLING) ? 0 : pitch);
    }

    @Override
    public void setDimensionsFromPose(Pose pose) {
        float height;
        float width;
        switch (pose) {
            case SNEAKING -> {
                height = SNEAKING_POSE_HEIGHT;
                width = definition.width();
            }
            case FALL_FLYING, SPIN_ATTACK, SWIMMING -> {
                height = 0.6f;
                width = definition.width();
            }
            case DYING -> {
                height = 0.2f;
                width = 0.2f;
            }
            default -> {
                super.setDimensionsFromPose(pose);
                return;
            }
        }
        setBoundingBoxWidth(width);
        setBoundingBoxHeight(height);
    }
}
