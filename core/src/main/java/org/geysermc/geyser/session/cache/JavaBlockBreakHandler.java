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

package org.geysermc.geyser.session.cache;

import java.util.Objects;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;

public class JavaBlockBreakHandler extends BlockBreakHandler {

    private int destroyDelay = 0;
    private Direction direction = null;
    private float currentProgress = 0;

    public JavaBlockBreakHandler(GeyserSession session) {
        super(session);
    }

    @Override
    public void handleBlockBreaking(PlayerAuthInputPacket packet) {
        if (packet.getInputData().contains(PlayerAuthInputData.PERFORM_BLOCK_ACTIONS) &&
            !packet.getPlayerActions().isEmpty()) {
            handleBlockBreakActions(packet);
        } else {
            tick(packet.getTick());
        }

        restoredBlocks.clear();
    }

    public void tick(long tick) {
        if (this.currentBlockPos != null) {
            continueDestroying(currentBlockPos, direction, tick, false);
        } else if (destroyDelay > 0) {
            destroyDelay--;
        }
    }

    @Override
    protected void startBreaking(Vector3i position, int blockFace, long tick) {
        this.direction = Direction.VALUES[blockFace]; // will get cleared if block is broken!
        super.startBreaking(position, blockFace, tick);
    }

    @Override
    protected float calculateBreakProgress(BlockState state, Vector3i vector, GeyserItemStack stack) {
        float result = super.calculateBreakProgress(state, vector, stack);
        this.currentProgress = result; // this is fine, only start_break will call this due to our overrides
        return result;
    }

    @Override
    protected boolean canBreak(Vector3i vector) {
        if (this.destroyDelay > 0) {
            this.destroyDelay--;
            return false;
        }

        return super.canBreak(vector);
    }

    @Override
    protected void handleContinueBreaking(Vector3i vector, int blockFace, long tick) {
        this.continueDestroying(vector, direction, tick, false);
    }

    @Override
    protected void handleBlockBreaking(Vector3i vector, int blockFace, long tick) {
        this.continueDestroying(vector, direction, tick, true);
    }

    public void continueDestroying(Vector3i blockPosition, Direction direction, long tick, boolean bedrockDestroyed) {
        if (session.getGameMode() == GameMode.CREATIVE) {
            this.destroyDelay = 5;
            this.currentBlockPos = blockPosition;
            this.currentBlockState = session.getGeyser().getWorldManager().blockAt(session, blockPosition);
            //sendBlockAction(PlayerAction.START_DIGGING, direction);

            if (canDestroyBlock(currentBlockState)) {
                BlockUtils.spawnBlockBreakParticles(session, direction, currentBlockPos, currentBlockState);

                LevelEventPacket effectPacket = new LevelEventPacket();
                effectPacket.setPosition(currentBlockPos.toFloat());
                effectPacket.setType(LevelEvent.PARTICLE_DESTROY_BLOCK);
                effectPacket.setData(session.getBlockMappings().getBedrockBlockId(currentBlockState.javaId()));
                session.sendUpstreamPacket(effectPacket);
            } else {
                BlockUtils.sendBedrockStopBlockBreak(session, blockPosition.toFloat());
                BlockUtils.restoreCorrectBlock(session, blockPosition);
            }

            clearVariables();
        } else if (Objects.equals(this.currentBlockPos, blockPosition)) {
            final float currentProgress = BlockUtils.getBlockMiningProgressPerTick(session, this.currentBlockState.block(), session.getPlayerInventory().getItemInHand());
            this.currentProgress = this.currentProgress + currentProgress;
            if (this.currentProgress >= 1.0F) {
                if (canDestroyBlock(currentBlockState)) {
                    BlockUtils.spawnBlockBreakParticles(session, direction, currentBlockPos, currentBlockState);

                    LevelEventPacket effectPacket = new LevelEventPacket();
                    effectPacket.setPosition(currentBlockPos.toFloat());
                    effectPacket.setType(LevelEvent.PARTICLE_DESTROY_BLOCK);
                    effectPacket.setData(session.getBlockMappings().getBedrockBlockId(currentBlockState.javaId()));
                    session.sendUpstreamPacket(effectPacket);
                } else {
                    BlockUtils.sendBedrockStopBlockBreak(session, currentBlockPos.toFloat());
                    BlockUtils.restoreCorrectBlock(session, currentBlockPos);
                }
                //sendBlockAction(PlayerAction.FINISH_DIGGING, direction);

                clearVariables();
                return;
            }

            // Prevent the Bedrock client from destroying blocks quicker than Java allows.
            if (bedrockDestroyed) {
                // TODO test
                //stopBedrockBreaking(session, blockPosition.toFloat());
                BlockUtils.restoreCorrectBlock(session, blockPosition);
            }

            LevelEventPacket updateBreak = new LevelEventPacket();
            updateBreak.setType(LevelEvent.BLOCK_UPDATE_BREAK);
            updateBreak.setPosition(blockPosition.toFloat());
            updateBreak.setData((int) (65535 / BlockUtils.reciprocal(currentProgress)));
            session.sendUpstreamPacket(updateBreak);

            BlockUtils.spawnBlockBreakParticles(session, direction, blockPosition, currentBlockState);
        } else {
            // Add check here for previous block - is that gone????
            if (this.currentBlockPos != null) {
                // TODO
                GeyserImpl.getInstance().getLogger().info("STILL DESTROYING!!!");
            }
            super.startBreaking(blockPosition, direction.ordinal(), tick);
        }
    }


    @Override
    protected void clearVariables() {
        super.clearVariables();
        this.direction = null;
        this.currentProgress = 0.0F;
    }
}
