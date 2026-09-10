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

import lombok.Getter;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.geysermc.erosion.util.BlockPositionIterator;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.CollisionManager;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;

import java.util.concurrent.ThreadLocalRandom;

public class FishingHookEntity extends ProjectileEntity {

    private boolean hooked = false;
    private boolean castByPlayer = false;
    private boolean inWater = false;
    // Set from the Bedrock packet thread, read on the Java one
    private volatile boolean retrievedByClient = false;

    @Getter
    private final long bedrockOwnerId;
    @Getter
    private long bedrockTargetId;

    private final BoundingBox boundingBox;

    // The top lava block near the hook, or MIN_VALUE when there is none; see bedrockPosition
    private int lavaTopY = Integer.MIN_VALUE;
    private Vector3i lavaScanCell;
    private boolean lavaHoverActive = false;

    public FishingHookEntity(EntitySpawnContext context, PlayerEntity owner) {
        super(context.headYaw(0));

        this.boundingBox = new BoundingBox(0.125, 0.125, 0.125, 0.25, 0.25, 0.25);

        // In Java, the splash sound depends on the entity's velocity, but in Bedrock the volume doesn't change.
        // This splash can be confused with the sound from catching a fish. This silences the splash from Bedrock,
        // so that it can be handled by moveAbsoluteImmediate.
        setBoundingBoxHeight(128);

        this.bedrockOwnerId = owner.geyserId();
        this.metadata.put(EntityDataTypes.OWNER_EID, this.bedrockOwnerId);

        if (owner == session.getPlayerEntity()) {
            session.setFishingHook(this);
            castByPlayer = true;
        }

        updateLavaTop(position);
    }

    // The client kills the bobber if it touches lava, so we keep it hovering above it.
    // Lower may work but needs testing; 0.3 is safe for now.
    @Override
    public Vector3f bedrockPosition() {
        Vector3f bedrockPosition = super.bedrockPosition();
        if (lavaTopY == Integer.MIN_VALUE) {
            return bedrockPosition;
        }
        float lowest = lavaTopY + 1.3F;
        if (bedrockPosition.getY() >= lowest) {
            return bedrockPosition;
        }
        return Vector3f.from(bedrockPosition.getX(), lowest, bedrockPosition.getZ());
    }

    /**
     * Looks for lava around the hook, once per block the hook is in. The client also removes a hook
     * that is next to lava, so the neighbouring blocks count too. Hooks in water are skipped.
     */
    private void updateLavaTop(Vector3f position) {
        Vector3i cell = Vector3i.from(GenericMath.floor(position.getX()), GenericMath.floor(position.getY()), GenericMath.floor(position.getZ()));
        if (cell.equals(lavaScanCell)) {
            return;
        }
        lavaScanCell = cell;
        lavaTopY = Integer.MIN_VALUE;
        if (BlockStateValues.getFluid(session.getGeyser().getWorldManager().getBlockAt(session, cell)) == Fluid.WATER) {
            return;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int y = cell.getY() + 1; y >= cell.getY() - 1; y--) {
                    int state = session.getGeyser().getWorldManager().getBlockAt(session, cell.getX() + dx, y, cell.getZ() + dz);
                    if (BlockStateValues.getFluid(state) == Fluid.LAVA) {
                        lavaTopY = Math.max(lavaTopY, y);
                        break;
                    }
                }
            }
        }
        if (lavaTopY != Integer.MIN_VALUE && !lavaHoverActive) {
            // Take the hook away from the client's own simulation before that carries it into the lava
            lavaHoverActive = true;
            setFlag(EntityFlag.HAS_GRAVITY, false);
            if (isValid()) {
                updateBedrockMetadata();
                SetEntityMotionPacket motionPacket = new SetEntityMotionPacket();
                motionPacket.setRuntimeEntityId(geyserId);
                motionPacket.setMotion(Vector3f.ZERO);
                session.sendUpstreamPacket(motionPacket);
            }
        }
    }

    @Override
    public void despawnEntity() {
        if (castByPlayer) {
            session.setFishingHook(null);
        }
        super.despawnEntity();
    }

    /**
     * Called when the Bedrock client uses its rod while this hook is out. The client removes its
     * own hook right away and assumes the retrieve succeeds. When a plugin cancels the retrieve,
     * Java keeps the hook and never sends anything that brings it back, so the hook is respawned
     * on the next movement Java sends for it. A hook at rest only gets Java's periodic position
     * sync, so that can take up to three seconds. Geyser's own projectile ticking must not trigger
     * the respawn, since a successful retrieve's removal can still be in flight.
     */
    public void markRetrievedByClient() {
        if (castByPlayer) {
            retrievedByClient = true;
        }
    }

    /**
     * Spawns this hook again under a fresh Bedrock id, at the position the Java movement just applied.
     * The client treats the id it reeled in as gone and only links its rod and line to a hook it sees spawn.
     */
    private void respawnIfRetrievedByClient() {
        if (!retrievedByClient) {
            return;
        }
        retrievedByClient = false;
        geyserId = session.getEntityCache().reassignGeyserId(this);
        // The spawn packet only carries metadata not sent yet, so put back what the client needs from the
        // first spawn: the height that silences its splash, the owner the line is drawn to, and the hooked target
        metadata.put(EntityDataTypes.HEIGHT, getBoundingBoxHeight());
        metadata.put(EntityDataTypes.OWNER_EID, bedrockOwnerId);
        if (hooked) {
            metadata.put(EntityDataTypes.TARGET_EID, bedrockTargetId);
        }
        spawnEntity();
    }

    @Override
    public void moveRelativeRaw(double relX, double relY, double relZ, float yaw, float pitch, float headYaw, boolean isOnGround) {
        super.moveRelativeRaw(relX, relY, relZ, yaw, pitch, headYaw, isOnGround);
        respawnIfRetrievedByClient();
    }

    @Override
    public void moveAbsoluteRaw(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        super.moveAbsoluteRaw(position, yaw, pitch, headYaw, isOnGround, teleported);
        respawnIfRetrievedByClient();
    }

    public void setHookedEntity(IntEntityMetadata entityMetadata) {
        int hookedEntityId = entityMetadata.getPrimitiveValue() - 1;
        Entity entity = session.getEntityCache().getEntityByJavaId(hookedEntityId);
        if (entity != null) {
            bedrockTargetId = entity.geyserId();
            metadata.put(EntityDataTypes.TARGET_EID, bedrockTargetId);
            hooked = true;
        } else {
            hooked = false;
        }
    }

    @Override
    protected void moveAbsoluteImmediate(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        boundingBox.setMiddleX(position.getX());
        boundingBox.setMiddleY(position.getY() + boundingBox.getSizeY() / 2);
        boundingBox.setMiddleZ(position.getZ());

        boolean touchingWater = false;
        boolean collided = false;
        for (BlockPositionIterator iter = CollisionManager.collidableBlocksIterator(session, boundingBox); iter.hasNext(); iter.next()) {
            int blockID = session.getGeyser().getWorldManager().getBlockAt(session, iter.getX(), iter.getY(), iter.getZ());
            BlockCollision blockCollision = BlockUtils.getCollision(blockID);
            if (blockCollision != null) {
                if (blockCollision.checkIntersection(iter.getX(), iter.getY(), iter.getZ(), boundingBox)) {
                    // TODO Push bounding box out of collision to improve movement
                    collided = true;
                }
            }

            double waterHeight = BlockStateValues.getWaterHeight(blockID);
            if (waterHeight != -1 && position.getY() <= (iter.getY() + waterHeight)) {
                touchingWater = true;
            }
        }

        if (!inWater && touchingWater) {
            sendSplashSound(session);
        }
        inWater = touchingWater;

        updateLavaTop(position);
        if (!collided) {
            super.moveAbsoluteImmediate(position, yaw, pitch, headYaw, isOnGround, teleported);
        } else {
            super.moveAbsoluteImmediate(this.position, yaw, pitch, headYaw, true, true);
        }
    }

    private void sendSplashSound(GeyserSession session) {
        if (!silent) {
            float volume = (float) (0.2f * Math.sqrt(0.2 * (motion.getX() * motion.getX() + motion.getZ() * motion.getZ()) + motion.getY() * motion.getY()));
            if (volume > 1) {
                volume = 1;
            }
            PlaySoundPacket playSoundPacket = new PlaySoundPacket();
            playSoundPacket.setSound("random.splash");
            playSoundPacket.setPosition(bedrockPosition());
            playSoundPacket.setVolume(volume);
            playSoundPacket.setPitch(1f + ThreadLocalRandom.current().nextFloat() * 0.3f);
            session.sendUpstreamPacket(playSoundPacket);
        }
    }

    @Override
    public void tick() {
        if (removedInVoid() || vehicle != null) {
            return;
        }
        if (hooked || !isInAir() && !isInWater() || isOnGround()) {
            motion = Vector3f.ZERO;
            return;
        }
        float gravity = getGravity();
        motion = motion.down(gravity);

        moveAbsoluteImmediate(position.add(motion), getYaw(), getPitch(), getHeadYaw(), isOnGround(), false);

        float drag = getDrag();
        motion = motion.mul(drag);
    }

    @Override
    protected float getGravity() {
        if (!isInWater() && !isOnGround()) {
            return 0.03f;
        }
        return 0;
    }

    /**
     * @return true if this entity is currently in air.
     */
    protected boolean isInAir() {
        int block = session.getGeyser().getWorldManager().getBlockAt(session, position.toInt());
        return block == Block.JAVA_AIR_ID;
    }

    @Override
    protected float getDrag() {
        return 0.92f;
    }
}
