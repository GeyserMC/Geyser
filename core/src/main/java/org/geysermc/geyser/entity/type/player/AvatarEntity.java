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
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.skin.SkinData;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkinManager;
import org.geysermc.geyser.skin.SkinProvider;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.ResolvableProfile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class AvatarEntity extends LivingEntity {
    public static final float SNEAKING_POSE_HEIGHT = 1.5f;
    protected static final List<AbilityLayer> BASE_ABILITY_LAYER;

    @Getter
    protected String username;

    @Getter
    @Setter
    @Nullable
    Map<GameProfile.TextureType, GameProfile.Texture> textures;

    private String cachedScore = "";
    private boolean scoreVisible = true;

    @Getter
    @Nullable
    protected Vector3i bedPosition;

    static {
        AbilityLayer abilityLayer = new AbilityLayer();
        abilityLayer.setLayerType(AbilityLayer.Type.BASE);
        Ability[] abilities = Ability.values();
        Collections.addAll(abilityLayer.getAbilitiesSet(), abilities); 
        Collections.addAll(abilityLayer.getAbilityValues(), abilities); 
        BASE_ABILITY_LAYER = Collections.singletonList(abilityLayer);
    }

    public AvatarEntity(EntitySpawnContext context, String username) {
        super(context);
        this.username = username;
        this.nametag = username;
    }

    @Override
    protected void initializeMetadata() {
        super.initializeMetadata();
        
        dirtyMetadata.put(EntityDataTypes.MARK_VARIANT, 0xff);
    }

    @Override
    public void spawnEntity() {
        AddPlayerPacket addPlayerPacket = new AddPlayerPacket();
        addPlayerPacket.setUuid(uuid);
        addPlayerPacket.setUsername(username);
        addPlayerPacket.setRuntimeEntityId(geyserId);
        addPlayerPacket.setUniqueEntityId(geyserId);
        addPlayerPacket.setPosition(position()); 
        addPlayerPacket.setRotation(bedrockRotation());
        addPlayerPacket.setMotion(motion);
        addPlayerPacket.setHand(ItemTranslator.translateToBedrock(session, getMainHandItem()));
        addPlayerPacket.getAdventureSettings().setCommandPermission(CommandPermission.ANY);
        addPlayerPacket.getAdventureSettings().setPlayerPermission(PlayerPermission.MEMBER);
        addPlayerPacket.setDeviceId("");
        addPlayerPacket.setPlatformChatId("");
        addPlayerPacket.setGameType(GameType.SURVIVAL); 
        addPlayerPacket.setAbilityLayers(BASE_ABILITY_LAYER); 
        addPlayerPacket.getMetadata().putFlags(flags);
        dirtyMetadata.apply(addPlayerPacket.getMetadata());

        setFlagsDirty(false);

        valid = true;
        session.sendUpstreamPacket(addPlayerPacket);
    }

    
    @Override
    public boolean shouldLerp() {
        return false;
    }

    @Override
    public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        setPosition(position);
        setYaw(yaw);
        setPitch(pitch);
        setHeadYaw(headYaw);

        setOnGround(isOnGround);

        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(geyserId);
        movePlayerPacket.setPosition(bedrockPosition());
        movePlayerPacket.setRotation(bedrockRotation());
        movePlayerPacket.setOnGround(isOnGround);
        movePlayerPacket.setMode(this instanceof SessionPlayerEntity || teleported ? MovePlayerPacket.Mode.TELEPORT : MovePlayerPacket.Mode.NORMAL);
        if (movePlayerPacket.getMode() == MovePlayerPacket.Mode.TELEPORT) {
            movePlayerPacket.setTeleportationCause(MovePlayerPacket.TeleportationCause.BEHAVIOR);
        }

        session.sendUpstreamPacket(movePlayerPacket);

        if (teleported && !(this instanceof SessionPlayerEntity)) {
            
            updateHeadLookRotation(headYaw);
        }
    }

    @Override
    public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        setYaw(yaw);
        setPitch(pitch);
        setHeadYaw(headYaw);
        this.position = this.position.add(relX, relY, relZ);

        setOnGround(isOnGround);

        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(geyserId);
        movePlayerPacket.setPosition(bedrockPosition());
        movePlayerPacket.setRotation(bedrockRotation());
        movePlayerPacket.setOnGround(isOnGround);
        movePlayerPacket.setMode(this instanceof SessionPlayerEntity ? MovePlayerPacket.Mode.TELEPORT : MovePlayerPacket.Mode.NORMAL);

        if (movePlayerPacket.getMode() == MovePlayerPacket.Mode.TELEPORT) {
            movePlayerPacket.setTeleportationCause(MovePlayerPacket.TeleportationCause.BEHAVIOR);
        }

        session.sendUpstreamPacket(movePlayerPacket);
    }

    @Override
    public @Nullable Vector3i setBedPosition(EntityMetadata<Optional<Vector3i>, ?> entityMetadata) {
        bedPosition = super.setBedPosition(entityMetadata);
        if (bedPosition != null) {
            
            
            
            dirtyMetadata.put(EntityDataTypes.PLAYER_FLAGS, (byte) 2);
        } else {
            
            dirtyMetadata.put(EntityDataTypes.PLAYER_FLAGS, (byte) 0);
            return null;
        }
        return bedPosition;
    }

    public void setSkin(ResolvableProfile profile) {
        SkinManager.resolveProfile(profile).thenAccept(resolved -> setSkin(resolved, null));
    }

    public void setSkin(GameProfile profile, @Nullable Runnable after) {
        Map<GameProfile.TextureType, GameProfile.Texture> textures;
        try {
            textures = profile.getTextures(false);
        } catch (IllegalStateException e) {
            GeyserImpl.getInstance().getLogger().debug("Error loading textures for profile (%s)! Got: %s", profile, e);
            textures = null;
        }
        setSkin(textures, after);
    }

    public void setSkin(@Nullable Map<GameProfile.TextureType, GameProfile.Texture> textures, @Nullable Runnable after) {
        if (Objects.equals(textures, this.textures)) {
            return;
        }

        this.textures = textures;
        SkinManager.requestAndHandleSkinAndCape(this, session, after == null ? null : skin -> after.run());
    }

    public void setSkinVisibility(ByteEntityMetadata entityMetadata) {
        
        
        
        
        dirtyMetadata.put(EntityDataTypes.MARK_VARIANT, ~entityMetadata.getPrimitiveValue() & 0xff);
    }

    @Override
    public String getDisplayName(boolean includeStandardName) {
        if (this instanceof PlayerEntity) {
            return username;
        }
        return super.getDisplayName(includeStandardName);
    }

    @Override
    public void setCustomName(EntityMetadata<Optional<Component>, ?> entityMetadata) {
        
        if (!(this instanceof PlayerEntity)) {
            super.setCustomName(entityMetadata);
        }
    }

    @Override
    public void setCustomNameVisible(BooleanEntityMetadata entityMetadata) {
        
        if (!(this instanceof PlayerEntity)) {
            super.setCustomNameVisible(entityMetadata);
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

    
    public abstract boolean isListed();

    @Override
    protected void scoreVisibility(boolean show) {
        boolean visibilityChanged = scoreVisible != show;
        scoreVisible = show;
        if (!visibilityChanged) {
            return;
        }
        
        
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
            
            if (session.getGeyser().getWorldManager().blockAt(session, this.position.toInt()).is(Blocks.WATER)) {
                setFlag(EntityFlag.SWIMMING, true);
            } else {
                setFlag(EntityFlag.CRAWLING, true);

                
                
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

    @Override
    public Vector3f bedrockPosition() {
        
        if (bedPosition != null && getFlag(EntityFlag.SLEEPING)) {
            return position.up(0.2f);
        }
        return super.bedrockPosition();
    }

    public @Nullable String getSkinId() {
        if (textures != null) {
            GameProfile.Texture texture = textures.get(GameProfile.TextureType.SKIN);
            if (texture != null) {
                return texture.getHash();
            }
        }

        SkinData fallback = SkinProvider.determineFallbackSkinData(this.uuid);
        return fallback.skin.textureUrl;
    }
}
