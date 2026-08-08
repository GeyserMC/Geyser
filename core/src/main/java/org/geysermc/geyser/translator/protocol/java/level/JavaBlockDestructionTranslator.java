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

package org.geysermc.geyser.translator.protocol.java.level;

import it.unimi.dsi.fastutil.Pair;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.BlockBreakStage;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockDestructionPacket;

@Translator(packet = ClientboundBlockDestructionPacket.class)
public class JavaBlockDestructionTranslator extends PacketTranslator<ClientboundBlockDestructionPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundBlockDestructionPacket packet) {
        if (packet.getStage() == BlockBreakStage.RESET) {
            if (!session.getBlockBreakHandler().clearDestructionAt(packet.getPosition())) {
                BlockUtils.sendBedrockStopBlockBreak(session, packet.getPosition().toFloat());
            }
            return;
        }

        // Local mining already has a per-tick speed from BlockBreakHandler.
        // Translating the server echo as well would make two animations compete.
        if (packet.getPosition().equals(session.getBlockBreakHandler().getCurrentBlockPos())) {
            return;
        }

        // Bedrock wants a total destruction time, not a stage - so we estimate!
        LevelEventPacket levelEventPacket = new LevelEventPacket();
        levelEventPacket.setPosition(packet.getPosition().toFloat());

        // First: Check if we know when the last packet for this position was sent - we'll use that for our estimation
        long currentTick = session.getTicks();
        Pair<Long, BlockBreakStage> lastUpdate = session.getBlockBreakHandler().getDestructionStageCache().getIfPresent(packet.getPosition());
        if (lastUpdate != null && packet.getStage().compareTo(lastUpdate.second()) < 0) {
            // A new mining sequence can start at the same position before a
            // RESET packet arrives. Stop the old overlay instead of treating
            // every stage of the new sequence as out-of-order.
            session.getBlockBreakHandler().clearDestructionAt(packet.getPosition());
            lastUpdate = null;
        }
        if (lastUpdate == null) {
            levelEventPacket.setType(LevelEvent.BLOCK_START_BREAK);
            levelEventPacket.setData(65535 / 6000); // just a high value (5 mins), we'll update this once we get a new progress update
        } else {
            int ticksSince = (int) (currentTick - lastUpdate.first());
            int stagesSince = packet.getStage().compareTo(lastUpdate.second());
            if (ticksSince <= 0 || stagesSince <= 0) {
                return;
            }

            Integer stableSpeed = session.getBlockBreakHandler().getDestructionSpeedCache().getIfPresent(packet.getPosition());
            if (stableSpeed == null) {
                // The Bedrock animation started close to zero. Estimate the
                // server's stage rate once, then choose one stable speed that
                // catches up and reaches 100% at the same time as Java.
                float serverProgressPerTick = stagesSince / (10.0f * ticksSince);
                float serverProgress = Math.clamp(packet.getStage().ordinal() / 10.0f, 0.0f, 0.9f);
                int remainingTicks = Math.max(1, Math.round((1.0f - serverProgress) / serverProgressPerTick));
                stableSpeed = Math.clamp(Math.round(65535.0f / remainingTicks), 1, 65535);
                session.getBlockBreakHandler().getDestructionSpeedCache().put(packet.getPosition(), stableSpeed);
            }

            levelEventPacket.setType(LevelEvent.BLOCK_UPDATE_BREAK);
            levelEventPacket.setData(stableSpeed);
        }

        session.getBlockBreakHandler().getDestructionStageCache().put(packet.getPosition(), Pair.of(currentTick, packet.getStage()));
        session.sendUpstreamPacket(levelEventPacket);
    }
}
