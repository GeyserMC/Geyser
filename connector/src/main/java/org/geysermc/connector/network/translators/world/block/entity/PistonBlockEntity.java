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

package org.geysermc.connector.network.translators.world.block.entity;

import com.github.steveice10.mc.protocol.data.game.world.block.value.PistonValue;
import com.github.steveice10.mc.protocol.data.game.world.block.value.PistonValueType;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.packet.BlockEntityDataPacket;
import lombok.Getter;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.world.block.BlockStateValues;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Getter
public class PistonBlockEntity {
    private final GeyserSession session;
    private final Vector3i position;
    private final PistonValue direction;
    private final boolean sticky;

    private PistonValueType action;

    /**
     * A flattened array of the positions of attached blocks, stored in XYZ order.
     */
    private int[] attachedBlocks = new int[0];
    /**
     * A flattened array of the positions of blocks broken, stored in XYZ order.
     */
    private int[] breakBlocks = new int[0];

    /**
     * The position of the piston head
     */
    private float progress = 0.0f;
    private float lastProgress = 0.0f;

    private ScheduledFuture<?> updater;

    public PistonBlockEntity(GeyserSession session, Vector3i position, PistonValue direction) {
        this.session = session;
        this.position = position;
        this.direction = direction;

        if (session.getConnector().getConfig().isCacheChunks()) {
            int blockId = session.getConnector().getWorldManager().getBlockAt(session, position);
            sticky = BlockStateValues.isStickyPiston(blockId);
            boolean extended = BlockStateValues.getPistonValues().get(blockId);
            if (extended) {
                this.action = PistonValueType.PUSHING;
                this.progress = 1.0f;
            } else {
                this.action = PistonValueType.PULLING;
                this.progress = 0.0f;
            }
            this.lastProgress = progress;
        } else {
            sticky = false;
        }
    }

    /**
     * Set whether the piston is pulling or pushing blocks
     * @param action Pulling or Pushing
     */
    public void setAction(PistonValueType action) {
        System.out.println(action);
        this.action = action;

        if (action == PistonValueType.CANCELLED_MID_PUSH) {
            progress = 1.0f;
            lastProgress = 1.0f;
        }

        if (updater != null) {
            updater.cancel(true);
        }
    }

    /**
     * Send Block Entity Data packets to update the position of the piston head
     */
    public void sendUpdate() {
        System.out.println("Update" + action + progress + isDone());
        BlockEntityDataPacket blockEntityDataPacket = new BlockEntityDataPacket();
        blockEntityDataPacket.setBlockPosition(position);
        blockEntityDataPacket.setData(buildPistonTag());
        session.sendUpstreamPacket(blockEntityDataPacket);
        if (!isDone()) {
            updateProgress();
            updater = session.getConnector().getGeneralThreadPool().schedule(this::sendUpdate, 50, TimeUnit.MILLISECONDS);
        } else {
            attachedBlocks = new int[0];
            breakBlocks = new int[0];
        }
    }

    /**
     * Get the Bedrock state of the piston
     * @return 0 - Fully retracted, 1 - Extending, 2 - Fully extended, 3 - Retracting
     */
    private byte getState() {
        switch (action) {
            case PUSHING:
                return (byte) (isDone() ? 2 : 1);
            case PULLING:
                return (byte) (isDone() ? 0 : 3);
            default:
                return (byte) (isDone() ? 0 : 2);
        }
    }

    private void updateProgress() {
        switch (action) {
            case PUSHING:
                lastProgress = progress;
                progress += 0.5f;
                if (progress >= 1.0f) {
                    progress = 1.0f;
                }
                break;
            case CANCELLED_MID_PUSH:
            case PULLING:
                lastProgress = progress;
                progress -= 0.5f;
                if (progress <= 0.0f) {
                    progress = 0.0f;
                }
                break;
        }
    }

    /**
     * @return True if the piston has finished it's movement, otherwise false
     */
    public boolean isDone() {
        switch (action) {
            case PUSHING:
                return progress == 1.0f && lastProgress == 1.0f;
            case PULLING:
            case CANCELLED_MID_PUSH:
                return progress == 0.0f && lastProgress == 0.0f;
        }
        return true;
    }

    /**
     * Create a the piston data tag with the data in this block entity
     * @return A piston data tag
     */
    private NbtMap buildPistonTag() {
        NbtMapBuilder builder = NbtMap.builder()
                .putString("id", "PistonArm")
                .putFloat("Progress", progress)
                .putFloat("LastProgress", lastProgress)
                .putByte("NewState", getState())
                .putByte("State", getState())
                .putByte("Sticky", (byte) (sticky ? 1 : 0))
                .putByte("isMovable", (byte) 0)
                .putInt("x", position.getX())
                .putInt("y", position.getY())
                .putInt("z", position.getZ());
        if (isDone()) {
            builder.putIntArray("AttachedBlocks", new int[0])
                    .putIntArray("BreakBlocks", new int[0]);
        } else {
            builder.putIntArray("AttachedBlocks", attachedBlocks)
                    .putIntArray("BreakBlocks", breakBlocks);
        }
        return builder.build();
    }

    /**
     * Create a piston data tag that has fully extended/retracted
     * @param position The position for the base of the piston
     * @param extended If the piston is extended or not
     * @param sticky If the piston is a sticky piston or not
     * @return A piston data tag for a fully extended/retracted piston
     */
    public static NbtMap buildStaticPistonTag(Vector3i position, boolean extended, boolean sticky) {
        NbtMapBuilder builder = NbtMap.builder()
                .putString("id", "PistonArm")
                .putFloat("Progress", extended ? 1.0f : 0.0f)
                .putFloat("LastProgress", extended ? 1.0f : 0.0f)
                .putByte("NewState", (byte) (extended ? 2 : 0))
                .putByte("State", (byte) (extended ? 2 : 0))
                .putByte("Sticky", (byte) (sticky ? 1 : 0))
                .putByte("isMovable", (byte) 0)
                .putInt("x", position.getX())
                .putInt("y", position.getY())
                .putInt("z", position.getZ());
        return builder.build();
    }

    private NbtMap buildMovingBlockTag(Vector3i position, NbtMap movingBlock, NbtMap movingBlockExtra, Vector3i pistonPosition) {
        //BlockTranslator.BLOCKS.get(BlockTranslator.getBedrockBlockId(blockId));
        NbtMapBuilder builder = NbtMap.builder()
                .putString("id", "MovingBlock")
                .putCompound("movingBlock", movingBlock)
                .putCompound("movingBlockExtra", movingBlockExtra)
                .putByte("isMovable", (byte) 1)
                .putInt("pistonPosX", pistonPosition.getX())
                .putInt("pistonPosY", pistonPosition.getY())
                .putInt("pistonPosZ", pistonPosition.getZ())
                .putInt("x", position.getX())
                .putInt("y", position.getY())
                .putInt("z", position.getZ());
        return builder.build();
    }
}
