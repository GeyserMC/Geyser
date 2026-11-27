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

import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.BlockBreakHandler.DestructionProgress;
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

        DestructionProgress lastUpdate = session.getBlockBreakHandler().getDestructionStageCache().getIfPresent(packet.getPosition());
        int stagesSince = lastUpdate == null ? 0 : packet.getStage().compareTo(lastUpdate.stage());
        if (lastUpdate != null && stagesSince == 0) {
            return;
        }

        LevelEventPacket levelEventPacket = new LevelEventPacket();
        levelEventPacket.setPosition(packet.getPosition().toFloat());
        long tick = session.getClientTicks();
        double displayedProgress = 0;
        int rate;

        if (lastUpdate == null || stagesSince < 0) {
            levelEventPacket.setType(LevelEvent.BLOCK_START_BREAK);
            rate = 65535 / 6000; // just a high value (5 mins), we'll update this once we get a new progress update
        } else {
            long ticksSince = Math.max(0L, tick - lastUpdate.tick());
            displayedProgress = Math.clamp(lastUpdate.displayedProgress() + lastUpdate.rate() * (double) ticksSince / 65535, 0, 1);
            double ticksPerStage = Math.max(1, (double) ticksSince / stagesSince);
            double nextProgress = Math.min(1, (packet.getStage().ordinal() + 1) / 10D);

            // Catch up to the next Java stage over one estimated stage interval, including any initial visual delay.
            rate = (int) Math.clamp(Math.ceil((nextProgress - displayedProgress) * 65535 / ticksPerStage), 0, 65535);
            levelEventPacket.setType(LevelEvent.BLOCK_UPDATE_BREAK);
        }

        levelEventPacket.setData(rate);
        session.getBlockBreakHandler().getDestructionStageCache().put(packet.getPosition(), new DestructionProgress(tick, packet.getStage(), displayedProgress, rate));
        session.sendUpstreamPacket(levelEventPacket);
    }
}
