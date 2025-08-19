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
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.BlockBreakStage;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockDestructionPacket;

@Translator(packet = ClientboundBlockDestructionPacket.class)
public class JavaBlockDestructionTranslator extends PacketTranslator<ClientboundBlockDestructionPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundBlockDestructionPacket packet) {
        LevelEventPacket levelEventPacket = new LevelEventPacket();
        levelEventPacket.setPosition(packet.getPosition().toFloat());

        if (packet.getStage() == BlockBreakStage.RESET) {
            // Invalidate the position now that it's not being broken anymore
            session.getBlockBreakHandler().getDestructionStageCache().invalidate(packet.getPosition());

            levelEventPacket.setType(LevelEvent.BLOCK_STOP_BREAK);
            levelEventPacket.setData(0);
            session.sendUpstreamPacket(levelEventPacket);
            return;
        }

        // Bedrock wants a total destruction time, not a stage - so we estimate!

        // First: Check if we know when the last packet for this position was sent - we'll use that for our estimation
        Pair<Integer, BlockBreakStage> lastUpdate = session.getBlockBreakHandler().getDestructionStageCache().getIfPresent(packet.getPosition());
        if (lastUpdate == null) {
            levelEventPacket.setType(LevelEvent.BLOCK_START_BREAK);
            levelEventPacket.setData(100000); // just a high value, we don't have any better one available
        } else {
            // Ticks since last update
            int ticksSince = session.getTicks() - lastUpdate.first();
            int stagesSince = packet.getStage().compareTo(lastUpdate.second());
            int ticksPerStage = stagesSince == 0 ? ticksSince : ticksSince / stagesSince;
            int remainingStages = 10 - packet.getStage().ordinal();

            levelEventPacket.setType(LevelEvent.BLOCK_UPDATE_BREAK);
            levelEventPacket.setData(Math.max(remainingStages * ticksPerStage, 0));
        }

        session.getBlockBreakHandler().getDestructionStageCache().put(packet.getPosition(), Pair.of(session.getTicks(), packet.getStage()));
        session.sendUpstreamPacket(levelEventPacket);
    }
}
