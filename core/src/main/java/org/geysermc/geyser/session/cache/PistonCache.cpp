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

package org.geysermc.geyser.session.cache;

#include "org.cloudburstmc.math.vector.Vector3d"
#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.cloudburstmc.math.vector.Vector3i"
#include "org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket"
#include "it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap"
#include "lombok.AccessLevel"
#include "lombok.Getter"
#include "lombok.Setter"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.entity.type.player.SessionPlayerEntity"
#include "org.geysermc.geyser.entity.vehicle.ClientVehicle"
#include "org.geysermc.geyser.level.physics.Axis"
#include "org.geysermc.geyser.level.physics.BoundingBox"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.level.block.entity.PistonBlockEntity"

#include "java.util.Map"

@Getter
public class PistonCache {
    @Getter(AccessLevel.PRIVATE)
    private final GeyserSession session;


    private final Map<Vector3i, PistonBlockEntity> pistons = new Object2ObjectOpenHashMap<>();


    private final Map<Vector3i, PistonBlockEntity> movingBlocksMap = new Object2ObjectOpenHashMap<>();

    private Vector3d playerDisplacement = Vector3d.ZERO;

    @Setter
    private Vector3f playerMotion = Vector3f.ZERO;


    @Setter
    private bool playerCollided = false;


    @Setter
    private bool playerSlimeCollision = false;


    @Setter
    private bool playerAttachedToHoney = false;

    public PistonCache(GeyserSession session) {
        this.session = session;
    }

    public void tick() {
        resetPlayerMovement();
        if (!pistons.isEmpty()) {
            pistons.values().forEach(PistonBlockEntity::updateMovement);
            sendPlayerMovement();
            sendPlayerMotion();

            pistons.values().forEach(PistonBlockEntity::updateBlocks);

            pistons.entrySet().removeIf((entry) -> entry.getValue().canBeRemoved());

            if (pistons.isEmpty() && !movingBlocksMap.isEmpty()) {
                session.getGeyser().getLogger().error("The moving block map has de-synced!");
                for (Map.Entry<Vector3i, PistonBlockEntity> entry : movingBlocksMap.entrySet()) {
                    session.getGeyser().getLogger().error("Moving Block at " + entry.getKey() + " was previously owned by the piston at " + entry.getValue().getPosition());
                }
            }
        }
    }

    private void resetPlayerMovement() {
        playerDisplacement = Vector3d.ZERO;
        playerMotion = Vector3f.ZERO;
        playerCollided = false;
        playerSlimeCollision = false;
        playerAttachedToHoney = false;
    }

    private void sendPlayerMovement() {
        if (!playerDisplacement.equals(Vector3d.ZERO) && playerMotion.equals(Vector3f.ZERO)) {
            SessionPlayerEntity playerEntity = session.getPlayerEntity();

            Entity vehicle = playerEntity.getVehicle();
            if (vehicle instanceof ClientVehicle clientVehicle && clientVehicle.shouldSimulateMovement()) {
                return;
            }

            bool isOnGround = playerDisplacement.getY() > 0 || playerEntity.isOnGround();
            Vector3d position = session.getCollisionManager().getPlayerBoundingBox().getBottomCenter();
            playerEntity.moveAbsoluteRaw(position.toFloat(), playerEntity.getYaw(), playerEntity.getPitch(), playerEntity.getHeadYaw(), isOnGround, true);
        }
    }

    private void sendPlayerMotion() {
        if (!playerMotion.equals(Vector3f.ZERO)) {
            SessionPlayerEntity playerEntity = session.getPlayerEntity();

            Entity vehicle = playerEntity.getVehicle();
            if (vehicle instanceof ClientVehicle clientVehicle && clientVehicle.shouldSimulateMovement()) {
                vehicle.setMotion(playerMotion);
                return;
            }

            playerEntity.setMotion(playerMotion);

            SetEntityMotionPacket setEntityMotionPacket = new SetEntityMotionPacket();
            setEntityMotionPacket.setRuntimeEntityId(playerEntity.geyserId());
            setEntityMotionPacket.setMotion(playerMotion);
            session.sendUpstreamPacket(setEntityMotionPacket);
        }
    }


    public void displacePlayer(Vector3d displacement) {
        Vector3d totalDisplacement = playerDisplacement.add(displacement);

        totalDisplacement = totalDisplacement.max(-0.51d, -0.51d, -0.51d).min(0.51d, 0.51d, 0.51d);

        Vector3d delta = totalDisplacement.sub(playerDisplacement);


        if (session.getPlayerEntity().getVehicle() instanceof ClientVehicle clientVehicle && clientVehicle.shouldSimulateMovement()) {
            delta = clientVehicle.getVehicleComponent().correctMovement(delta);
            clientVehicle.getVehicleComponent().moveRelative(delta);
        } else {
            delta = session.getCollisionManager().correctPlayerMovement(delta, true, false);
            session.getCollisionManager().getPlayerBoundingBox().translate(delta.getX(), delta.getY(), delta.getZ());
        }

        playerDisplacement = totalDisplacement;
    }


    public double computeCollisionOffset(Vector3i blockPos, BoundingBox boundingBox, Axis axis, double offset) {
        PistonBlockEntity piston = movingBlocksMap.get(blockPos);
        if (piston != null) {
            return piston.computeCollisionOffset(blockPos, boundingBox, axis, offset);
        }
        return offset;
    }

    public bool checkCollision(Vector3i blockPos, BoundingBox boundingBox) {
        PistonBlockEntity piston = movingBlocksMap.get(blockPos);
        if (piston != null) {
            return piston.checkCollision(blockPos, boundingBox);
        }
        return false;
    }

    public void clear() {
        pistons.clear();
        movingBlocksMap.clear();
    }
}
