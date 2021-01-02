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

package org.geysermc.connector.network.translators.collision;

import com.nukkitx.math.vector.Vector3d;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlags;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.collision.translators.BlockCollision;

import java.util.ArrayList;
import java.util.List;

public class CollisionManager {

    private final GeyserSession session;

    @Getter
    private BoundingBox playerBoundingBox;

    /**
     * Whether the player is inside scaffolding
     */
    @Setter
    private boolean touchingScaffolding;

    /**
     * Whether the player is on top of scaffolding
     */
    @Setter
    private boolean onScaffolding;

    /**
     * Additional space where blocks are checked, which is helpful for fixing NoCheatPlus's Passable check.
     * This check doesn't allow players right up against the block, so they must be pushed slightly away.
     */
    public static final double COLLISION_TOLERANCE = 0.00001;

    public CollisionManager(GeyserSession session) {
        this.session = session;
        this.playerBoundingBox = new BoundingBox(0, 0, 0, 0.6, 1.8, 0.6);
    }

    /**
     * Updates the stored bounding box
     * @param position The new position of the player
     */
    public void updatePlayerBoundingBox(Vector3f position) {
        updatePlayerBoundingBox(position.toDouble());
    }

    /**
     * Updates the stored bounding box
     * @param position The new position of the player
     */
    public void updatePlayerBoundingBox(Vector3d position) {
        updatePlayerBoundingBox();

        playerBoundingBox.setMiddleX(position.getX());
        playerBoundingBox.setMiddleY(position.getY() + (playerBoundingBox.getSizeY() / 2));
        playerBoundingBox.setMiddleZ(position.getZ());
    }

    /**
     * Updates the stored bounding box without passing a position, which currently just changes the height depending on if the player is sneaking.
     */
    public void updatePlayerBoundingBox() {
        if (playerBoundingBox == null) {
            Vector3f playerPosition;
            if (session.getPlayerEntity() == null) {
                // Temporary position to prevent NullPointerException
                playerPosition = Vector3f.ZERO;
            } else {
                playerPosition = session.getPlayerEntity().getPosition();
            }
            playerBoundingBox = new BoundingBox(playerPosition.getX(), playerPosition.getY() + 0.9, playerPosition.getZ(), 0.6, 1.8, 0.6);
        } else {
            // According to the Minecraft Wiki, when sneaking:
            // - In Bedrock Edition, the height becomes 1.65 blocks, allowing movement through spaces as small as 1.75 (2 - 1⁄4) blocks high.
            // - In Java Edition, the height becomes 1.5 blocks.
            if (session.isSneaking()) {
                playerBoundingBox.setSizeY(1.5);
            } else {
                playerBoundingBox.setSizeY(1.8);
            }
        }
    }

    public List<Vector3i> getPlayerCollidableBlocks() {
        List<Vector3i> blocks = new ArrayList<>();

        Vector3d position = Vector3d.from(playerBoundingBox.getMiddleX(),
                playerBoundingBox.getMiddleY() - (playerBoundingBox.getSizeY() / 2),
                playerBoundingBox.getMiddleZ());

        // Loop through all blocks that could collide with the player
        int minCollisionX = (int) Math.floor(position.getX() - ((playerBoundingBox.getSizeX() / 2) + COLLISION_TOLERANCE));
        int maxCollisionX = (int) Math.floor(position.getX() + (playerBoundingBox.getSizeX() / 2) + COLLISION_TOLERANCE);

        // Y extends 0.5 blocks down because of fence hitboxes
        int minCollisionY = (int) Math.floor(position.getY() - 0.5);

        int maxCollisionY = (int) Math.floor(position.getY() + playerBoundingBox.getSizeY());

        int minCollisionZ = (int) Math.floor(position.getZ() - ((playerBoundingBox.getSizeZ() / 2) + COLLISION_TOLERANCE));
        int maxCollisionZ = (int) Math.floor(position.getZ() + (playerBoundingBox.getSizeZ() / 2) + COLLISION_TOLERANCE);

        for (int y = minCollisionY; y < maxCollisionY + 1; y++) {
            for (int x = minCollisionX; x < maxCollisionX + 1; x++) {
                for (int z = minCollisionZ; z < maxCollisionZ + 1; z++) {
                    blocks.add(Vector3i.from(x, y, z));
                }
            }
        }

        return blocks;
    }

    /**
     * Returns false if the movement is invalid, and in this case it shouldn't be sent to the server and should be
     * cancelled
     * See {@link BlockCollision#correctPosition(GeyserSession, BoundingBox)} for more info
     */
    public boolean correctPlayerPosition() {

        // These may be set to true by the correctPosition method in ScaffoldingCollision
        touchingScaffolding = false;
        onScaffolding = false;

        List<Vector3i> collidableBlocks = getPlayerCollidableBlocks();

        // Used when correction code needs to be run before the main correction
        for (Vector3i blockPos : collidableBlocks) {
            BlockCollision blockCollision = CollisionTranslator.getCollisionAt(
                    session, blockPos.getX(), blockPos.getY(), blockPos.getZ()
            );
            if (blockCollision != null) {
                blockCollision.beforeCorrectPosition(playerBoundingBox);
            }
        }

        // Main correction code
        for (Vector3i blockPos : collidableBlocks) {
            BlockCollision blockCollision = CollisionTranslator.getCollisionAt(
                    session, blockPos.getX(), blockPos.getY(), blockPos.getZ()
            );
            if (blockCollision != null) {
                if (!blockCollision.correctPosition(session, playerBoundingBox)) {
                    return false;
                }
            }
        }

        updateScaffoldingFlags();

        return true;
    }

    /**
     * Updates scaffolding entity flags
     * Scaffolding needs to be checked per-move since it's a flag in Bedrock but Java does it client-side
     */
    public void updateScaffoldingFlags() {
        EntityFlags flags = session.getPlayerEntity().getMetadata().getFlags();
        boolean flagsChanged;
        boolean isSneakingWithScaffolding = (touchingScaffolding || onScaffolding) && session.isSneaking();

        flagsChanged = flags.getFlag(EntityFlag.FALL_THROUGH_SCAFFOLDING) != isSneakingWithScaffolding;
        flagsChanged |= flags.getFlag(EntityFlag.OVER_SCAFFOLDING) != isSneakingWithScaffolding;

        flags.setFlag(EntityFlag.FALL_THROUGH_SCAFFOLDING, isSneakingWithScaffolding);
        flags.setFlag(EntityFlag.OVER_SCAFFOLDING, isSneakingWithScaffolding);

        flagsChanged |= flags.getFlag(EntityFlag.IN_SCAFFOLDING) != touchingScaffolding;
        flags.setFlag(EntityFlag.IN_SCAFFOLDING, touchingScaffolding);

        if (flagsChanged) {
            session.getPlayerEntity().updateBedrockMetadata(session);
        }
    }
}
