/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.entity;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.CommandPermission;
import com.nukkitx.protocol.bedrock.data.PlayerPermission;
import com.nukkitx.protocol.bedrock.packet.AddPlayerPacket;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;

import lombok.Getter;
import lombok.Setter;

import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.session.cache.EntityEffectCache;
import org.geysermc.connector.utils.SkinUtils;

import java.util.UUID;

@Getter @Setter
public class PlayerEntity extends LivingEntity {
    private GameProfile profile;
    private UUID uuid;
    private String username;
    private long lastSkinUpdate = -1;
    private boolean playerList = true;
    private final EntityEffectCache effectCache;

    public PlayerEntity(GameProfile gameProfile, long entityId, long geyserId, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, EntityType.PLAYER, position, motion, rotation);

        profile = gameProfile;
        uuid = gameProfile.getId();
        username = gameProfile.getName();
        effectCache = new EntityEffectCache();
        if (geyserId == 1) valid = true;
    }

    @Override
    public boolean despawnEntity(GeyserSession session) {
        super.despawnEntity(session);
        return !playerList; // don't remove from cache when still on playerlist
    }

    @Override
    public void spawnEntity(GeyserSession session) {
        if (geyserId == 1) return;

        AddPlayerPacket addPlayerPacket = new AddPlayerPacket();
        addPlayerPacket.setUuid(uuid);
        addPlayerPacket.setUsername(username);
        addPlayerPacket.setRuntimeEntityId(geyserId);
        addPlayerPacket.setUniqueEntityId(geyserId);
        addPlayerPacket.setPosition(position);
        addPlayerPacket.setRotation(getBedrockRotation());
        addPlayerPacket.setMotion(motion);
        addPlayerPacket.setHand(hand);
        addPlayerPacket.getAdventureSettings().setCommandPermission(CommandPermission.NORMAL);
        addPlayerPacket.getAdventureSettings().setPlayerPermission(PlayerPermission.VISITOR);
        addPlayerPacket.setDeviceId("");
        addPlayerPacket.setPlatformChatId("");
        addPlayerPacket.getMetadata().putAll(metadata);

        valid = true;
        session.getUpstream().sendPacket(addPlayerPacket);

        updateEquipment(session);
        updateBedrockAttributes(session);
    }

    public void sendPlayer(GeyserSession session) {
        if (getLastSkinUpdate() == -1) {
            if (playerList) {
                PlayerListPacket playerList = new PlayerListPacket();
                playerList.setAction(PlayerListPacket.Action.ADD);
                playerList.getEntries().add(SkinUtils.buildDefaultEntry(profile, geyserId));
                session.getUpstream().sendPacket(playerList);
            }
        }

        if (session.getUpstream().isInitialized() && session.getEntityCache().getEntityByGeyserId(geyserId) == null) {
            session.getEntityCache().spawnEntity(this);
        } else {
            spawnEntity(session);
        }

        if (!playerList) {
            // remove from playerlist if player isn't on playerlist
            GeyserConnector.getInstance().getGeneralThreadPool().execute(() -> {
                PlayerListPacket playerList = new PlayerListPacket();
                playerList.setAction(PlayerListPacket.Action.REMOVE);
                playerList.getEntries().add(new PlayerListPacket.Entry(uuid));
                session.getUpstream().sendPacket(playerList);
            });
        }
    }

    @Override
    public void moveAbsolute(GeyserSession session, Vector3f position, Vector3f rotation, boolean isOnGround) {
        setPosition(position);
        setRotation(rotation);

        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(geyserId);
        movePlayerPacket.setPosition(this.position);
        movePlayerPacket.setRotation(getBedrockRotation());
        movePlayerPacket.setOnGround(isOnGround);
        movePlayerPacket.setMode(MovePlayerPacket.Mode.NORMAL);

        session.getUpstream().sendPacket(movePlayerPacket);
    }

    @Override
    public void moveRelative(GeyserSession session, double relX, double relY, double relZ, Vector3f rotation, boolean isOnGround) {
        setRotation(rotation);
        this.position = Vector3f.from(position.getX() + relX, position.getY() + relY, position.getZ() + relZ);

        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(geyserId);
        movePlayerPacket.setPosition(position);
        movePlayerPacket.setRotation(getBedrockRotation());
        movePlayerPacket.setOnGround(isOnGround);
        movePlayerPacket.setMode(MovePlayerPacket.Mode.NORMAL);
        session.getUpstream().sendPacket(movePlayerPacket);
    }

    @Override
    public void setPosition(Vector3f position) {
        this.position = position.add(0, entityType.getOffset(), 0);
    }
}
