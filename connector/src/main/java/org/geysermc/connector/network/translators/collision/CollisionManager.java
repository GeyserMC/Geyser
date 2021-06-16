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
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlags;
import com.nukkitx.protocol.bedrock.packet.MovePlayerPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.collision.translators.BlockCollision;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    /**
     * Trims Y coordinates when jumping to prevent rounding issues being sent to the server.
     * The locale used is necessary so other regions don't use <code>,</code> as their decimal separator.
     */
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.#####", new DecimalFormatSymbols(Locale.ENGLISH));

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
            playerBoundingBox = new BoundingBox(playerPosition.getX(), playerPosition.getY() + 0.9, playerPosition.getZ(),
                    EntityType.PLAYER.getWidth(), EntityType.PLAYER.getHeight(), EntityType.PLAYER.getLength());
        } else {
            // According to the Minecraft Wiki, when sneaking:
            // - In Bedrock Edition, the height becomes 1.65 blocks, allowing movement through spaces as small as 1.75 (2 - 1⁄4) blocks high.
            // - In Java Edition, the height becomes 1.5 blocks.
            // Other instances have the player's bounding box become as small as 0.6 or 0.2.
            playerBoundingBox.setSizeY(session.getPlayerEntity().getMetadata().getFloat(EntityData.BOUNDING_BOX_HEIGHT));
        }
    }

    /**
     * Adjust the Bedrock position before sending to the Java server to account for inaccuracies in movement between
     * the two versions.
     *
     * @param bedrockPosition the current Bedrock position of the client
     * @param onGround whether the Bedrock player is on the ground
     * @return the position to send to the Java server, or null to cancel sending the packet
     */
    public Vector3d adjustBedrockPosition(Vector3f bedrockPosition, boolean onGround) {
        // We need to parse the float as a string since casting a float to a double causes us to
        // lose precision and thus, causes players to get stuck when walking near walls
        double javaY = bedrockPosition.getY() - EntityType.PLAYER.getOffset();

        Vector3d position = Vector3d.from(Double.parseDouble(Float.toString(bedrockPosition.getX())), javaY,
                Double.parseDouble(Float.toString(bedrockPosition.getZ())));

        updatePlayerBoundingBox(position);

        // Correct player position
        if (!correctPlayerPosition()) {
            // Cancel the movement if it needs to be cancelled
            recalculatePosition();
            return null;
        }

        position = Vector3d.from(playerBoundingBox.getMiddleX(),
                playerBoundingBox.getMiddleY() - (playerBoundingBox.getSizeY() / 2),
                playerBoundingBox.getMiddleZ());

        if (!onGround) {
            // Trim the position to prevent rounding errors that make Java think we are clipping into a block
            position = Vector3d.from(position.getX(), Double.parseDouble(DECIMAL_FORMAT.format(position.getY())), position.getZ());
        }

        return position;
    }

    // TODO: This makes the player look upwards for some reason, rotation values must be wrong
    public void recalculatePosition() {
        PlayerEntity entity = session.getPlayerEntity();
        // Gravity might need to be reset...
        SetEntityDataPacket entityDataPacket = new SetEntityDataPacket();
        entityDataPacket.setRuntimeEntityId(entity.getGeyserId());
        entityDataPacket.getMetadata().putAll(entity.getMetadata());
        session.sendUpstreamPacket(entityDataPacket);

        MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(entity.getGeyserId());
        movePlayerPacket.setPosition(entity.getPosition());
        movePlayerPacket.setRotation(entity.getBedrockRotation());
        movePlayerPacket.setMode(MovePlayerPacket.Mode.NORMAL);
        session.sendUpstreamPacket(movePlayerPacket);
    }

    public List<Vector3i> getCollidableBlocks(BoundingBox box) {
        List<Vector3i> blocks = new ArrayList<>();

        Vector3d position = Vector3d.from(box.getMiddleX(),
                box.getMiddleY() - (box.getSizeY() / 2),
                box.getMiddleZ());

        // Loop through all blocks that could collide
        int minCollisionX = (int) Math.floor(position.getX() - ((box.getSizeX() / 2) + COLLISION_TOLERANCE));
        int maxCollisionX = (int) Math.floor(position.getX() + (box.getSizeX() / 2) + COLLISION_TOLERANCE);

        // Y extends 0.5 blocks down because of fence hitboxes
        int minCollisionY = (int) Math.floor(position.getY() - 0.5);

        int maxCollisionY = (int) Math.floor(position.getY() + box.getSizeY());

        int minCollisionZ = (int) Math.floor(position.getZ() - ((box.getSizeZ() / 2) + COLLISION_TOLERANCE));
        int maxCollisionZ = (int) Math.floor(position.getZ() + (box.getSizeZ() / 2) + COLLISION_TOLERANCE);

        for (int y = minCollisionY; y < maxCollisionY + 1; y++) {
            for (int x = minCollisionX; x < maxCollisionX + 1; x++) {
                for (int z = minCollisionZ; z < maxCollisionZ + 1; z++) {
                    blocks.add(Vector3i.from(x, y, z));
                }
            }
        }

        return blocks;
    }

    public List<Vector3i> getPlayerCollidableBlocks() {
        return getCollidableBlocks(playerBoundingBox);
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

        updateScaffoldingFlags(true);

        return true;
    }

    /**
     * @return true if the block located at the player's floor position plus 1 would intersect with the player,
     * were they not sneaking
     */
    public boolean isUnderSlab() {
        Vector3i position = session.getPlayerEntity().getPosition().toInt();
        BlockCollision collision = CollisionTranslator.getCollisionAt(session, position.getX(), position.getY(), position.getZ());
        if (collision != null) {
            // Determine, if the player's bounding box *were* at full height, if it would intersect with the block
            // at the current location.
            playerBoundingBox.setSizeY(EntityType.PLAYER.getHeight());
            boolean result = collision.checkIntersection(playerBoundingBox);
            playerBoundingBox.setSizeY(session.getPlayerEntity().getMetadata().getFloat(EntityData.BOUNDING_BOX_HEIGHT));
            return result;
        }
        return false;
    }

    /**
     * @return if the player is currently in a water block
     */
    public boolean isPlayerInWater() {
        return session.getConnector().getWorldManager().getBlockAt(session, session.getPlayerEntity().getPosition().toInt()) == BlockTranslator.JAVA_WATER_ID;
    }

    /**
     * Updates scaffolding entity flags
     * Scaffolding needs to be checked per-move since it's a flag in Bedrock but Java does it client-side
     *
     * @param updateMetadata whether we should update metadata if something changed
     */
    public void updateScaffoldingFlags(boolean updateMetadata) {
        EntityFlags flags = session.getPlayerEntity().getMetadata().getFlags();
        boolean flagsChanged;
        boolean isSneakingWithScaffolding = (touchingScaffolding || onScaffolding) && session.isSneaking();

        flagsChanged = flags.getFlag(EntityFlag.FALL_THROUGH_SCAFFOLDING) != isSneakingWithScaffolding;
        flagsChanged |= flags.getFlag(EntityFlag.OVER_SCAFFOLDING) != isSneakingWithScaffolding;

        flags.setFlag(EntityFlag.FALL_THROUGH_SCAFFOLDING, isSneakingWithScaffolding);
        flags.setFlag(EntityFlag.OVER_SCAFFOLDING, isSneakingWithScaffolding);

        flagsChanged |= flags.getFlag(EntityFlag.IN_SCAFFOLDING) != touchingScaffolding;
        flags.setFlag(EntityFlag.IN_SCAFFOLDING, touchingScaffolding);

        if (flagsChanged && updateMetadata) {
            session.getPlayerEntity().updateBedrockMetadata(session);
        }
    }
}
