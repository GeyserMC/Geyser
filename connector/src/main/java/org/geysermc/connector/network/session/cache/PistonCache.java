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

package org.geysermc.connector.network.session.cache;

import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerPositionPacket;
import com.nukkitx.math.vector.Vector3d;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityMotionPacket;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.entity.player.SessionPlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.collision.CollisionManager;
import org.geysermc.connector.network.translators.world.block.entity.PistonBlockEntity;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PistonCache {
    private final GeyserSession session;

    private final Map<Vector3i, PistonBlockEntity> pistons = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());

    @Getter @Setter
    private Vector3d playerDisplacement = Vector3d.ZERO;
    @Getter @Setter
    private Vector3f playerMotion = Vector3f.ZERO;

    private ScheduledFuture<?> updater;

    public PistonCache(GeyserSession session) {
        this.session = session;
    }

    public void update() {
        playerDisplacement = Vector3d.ZERO;
        playerMotion = Vector3f.ZERO;

        if (session.isClosed() || pistons.isEmpty()) {
            updater.cancel(false);
            return;
        }

        pistons.values().forEach(PistonBlockEntity::update);
        pistons.entrySet().removeIf((entry) -> entry.getValue().isDone());

        SessionPlayerEntity playerEntity = session.getPlayerEntity();
        if (!playerDisplacement.equals(Vector3d.ZERO) && playerMotion.getY() == 0) {
            CollisionManager collisionManager = session.getCollisionManager();
            if (collisionManager.correctPlayerPosition()) {
                Vector3d position = Vector3d.from(collisionManager.getPlayerBoundingBox().getMiddleX(), collisionManager.getPlayerBoundingBox().getMiddleY() - (collisionManager.getPlayerBoundingBox().getSizeY() / 2), collisionManager.getPlayerBoundingBox().getMiddleZ());
                playerEntity.setPosition(position.toFloat(), true);
                // Using MoveEntityAbsolutePacket for teleporting seems to be smoother than MovePlayerPacket
                // It also keeps motion from slime blocks
                MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
                moveEntityPacket.setRuntimeEntityId(playerEntity.getGeyserId());
                moveEntityPacket.setPosition(playerEntity.getPosition());
                moveEntityPacket.setRotation(playerEntity.getBedrockRotation());
                moveEntityPacket.setOnGround(playerEntity.isOnGround());
                moveEntityPacket.setTeleported(true);
                session.sendUpstreamPacket(moveEntityPacket);

                ClientPlayerPositionPacket playerPositionPacket = new ClientPlayerPositionPacket(true, position.getX(), position.getY(), position.getZ());
                session.sendDownstreamPacket(playerPositionPacket);
            }
        }
        if (!playerMotion.equals(Vector3f.ZERO)) {
            playerEntity.setMotion(playerMotion);
            System.out.println(playerMotion);
            SetEntityMotionPacket setEntityMotionPacket = new SetEntityMotionPacket();
            setEntityMotionPacket.setRuntimeEntityId(playerEntity.getGeyserId());
            setEntityMotionPacket.setMotion(playerMotion);
            session.sendUpstreamPacket(setEntityMotionPacket);
        }
    }

    public PistonBlockEntity getPistonAt(Vector3i position) {
        return pistons.get(position);
    }

    public void putPiston(PistonBlockEntity pistonBlockEntity) {
        pistons.put(pistonBlockEntity.getPosition(), pistonBlockEntity);

        if (updater == null || updater.isDone()) {
            updater = session.getConnector().getGeneralThreadPool().scheduleAtFixedRate(this::update, 0, 50, TimeUnit.MILLISECONDS);
        }
    }

    public void clear() {
        pistons.clear();
    }
}
