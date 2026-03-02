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
            // Invalidate the position now that it's not being broken anymore
            session.getBlockBreakHandler().getDestructionStageCache().invalidate(packet.getPosition());
            BlockUtils.sendBedrockStopBlockBreak(session, packet.getPosition().toFloat());
            return;
        }

        // Bedrock wants a total destruction time, not a stage - so we estimate!
        LevelEventPacket levelEventPacket = new LevelEventPacket();
        levelEventPacket.setPosition(packet.getPosition().toFloat());

        // First: Check if we know when the last packet for this position was sent - we'll use that for our estimation
        Pair<Long, BlockBreakStage> lastUpdate = session.getBlockBreakHandler().getDestructionStageCache().getIfPresent(packet.getPosition());
        if (lastUpdate == null) {
            levelEventPacket.setType(LevelEvent.BLOCK_START_BREAK);
            levelEventPacket.setData(65535 / 6000); // just a high value (5 mins), we'll update this once we get a new progress update
        } else {
            // Ticks since last update
            int ticksSince = (int) (session.getClientTicks() - lastUpdate.first());
            int stagesSince = packet.getStage().compareTo(lastUpdate.second());
            int ticksPerStage = stagesSince == 0 ? ticksSince : ticksSince / stagesSince;
            int remainingStages = 10 - packet.getStage().ordinal();

            levelEventPacket.setType(LevelEvent.BLOCK_UPDATE_BREAK);
            levelEventPacket.setData(65535 / Math.max(remainingStages, 1) * Math.max(ticksPerStage, 1));
        }

        session.getBlockBreakHandler().getDestructionStageCache().put(packet.getPosition(), Pair.of(session.getClientTicks(), packet.getStage()));
        session.sendUpstreamPacket(levelEventPacket);
    }
}
