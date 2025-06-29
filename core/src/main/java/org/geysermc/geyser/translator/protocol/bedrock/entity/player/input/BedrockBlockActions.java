/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.bedrock.entity.player.input;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.PlayerBlockActionData;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.bedrock.BedrockInventoryTransactionTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;

import java.util.List;

final class BedrockBlockActions {

    static void tickBlockBreaking(GeyserSession session, PlayerAuthInputPacket packet) {
        // Ensure we don't duplicate block breaking ticking
        if (!packet.getPlayerActions().isEmpty()) {
            translate(session, packet.getPlayerActions(), packet.getTick());
        } else {
            session.getBlockBreakHandler().tick(packet.getTick());
        }
    }

    static void translate(GeyserSession session, List<PlayerBlockActionData> playerActions, long tick) {
        // Send book update before any player action
        session.getBookEditCache().checkForSend();
        GameMode gameMode = session.getGameMode();

        for (PlayerBlockActionData blockActionData : playerActions) {
            PlayerActionType action = blockActionData.getAction();
            Vector3i vector = blockActionData.getBlockPosition();
            int blockFace = blockActionData.getFace();

            GeyserImpl.getInstance().getLogger().info(blockActionData.toString());

            switch (action) {
                case DROP_ITEM -> {
                    ServerboundPlayerActionPacket dropItemPacket = new ServerboundPlayerActionPacket(PlayerAction.DROP_ITEM,
                        vector, Direction.VALUES[blockFace], 0);
                    session.sendDownstreamGamePacket(dropItemPacket);
                }
                case START_BREAK -> {
                    if (gameMode == GameMode.CREATIVE) {
                        // Handled in other case
                        break;
                    }

                    Vector3f playerPosition = session.getPlayerEntity().getPosition();
                    playerPosition = playerPosition.down(EntityDefinitions.PLAYER.offset() - session.getEyeHeight());
                    if (!BedrockInventoryTransactionTranslator.canInteractWithBlock(session, playerPosition, vector)) {
                        BlockBreakHandler.stopBedrockBreaking(session, vector.toFloat());
                        break;
                    }

                    Entity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, vector);
                    if (itemFrameEntity != null) {
                        break;
                    }

                    if (BlockBreakHandler.restrictedBlockActions(session)) {
                        BlockBreakHandler.stopBedrockBreaking(session, vector.toFloat());
                        break;
                    }

                    int blockState = session.getGeyser().getWorldManager().getBlockAt(session, vector);
                    session.getBlockBreakHandler().startDestroying(BlockState.of(blockState), vector, Direction.VALUES[blockFace], tick);
                }
                case BLOCK_CONTINUE_DESTROY -> {
                    if (session.getGameMode() == GameMode.CREATIVE) {
                        break;
                    }

                    session.getBlockBreakHandler().continueDestroying(vector, Direction.VALUES[blockFace], tick, false);
                }
                case ABORT_BREAK -> session.getBlockBreakHandler().abortBlockBreaking(vector);
                // Handled in BedrockInventoryTransactionTranslator
                case BLOCK_PREDICT_DESTROY -> {
                    Vector3f playerPosition = session.getPlayerEntity().getPosition();
                    playerPosition = playerPosition.down(EntityDefinitions.PLAYER.offset() - session.getEyeHeight());
                    if (!BedrockInventoryTransactionTranslator.canInteractWithBlock(session, playerPosition, vector) || BlockBreakHandler.restrictedBlockActions(session)) {
                        BedrockInventoryTransactionTranslator.restoreCorrectBlock(session, vector);
                        BlockBreakHandler.stopBedrockBreaking(session, vector.toFloat());
                        break;
                    }

                    session.setLastBlockPlaced(null);
                    session.setLastBlockPlacePosition(null);

                    Entity itemFrameEntity = ItemFrameEntity.getItemFrameEntity(session, vector);
                    if (itemFrameEntity != null) {
                        break;
                    }

                    session.getBlockBreakHandler().continueDestroying(vector, Direction.VALUES[blockFace], tick, true);
                }
            }
        }
    }
}
