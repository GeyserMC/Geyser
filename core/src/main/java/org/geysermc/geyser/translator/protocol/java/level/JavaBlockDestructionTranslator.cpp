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

#include "it.unimi.dsi.fastutil.Pair"
#include "org.cloudburstmc.protocol.bedrock.data.LevelEvent"
#include "org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.BlockUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.player.BlockBreakStage"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockDestructionPacket"

@Translator(packet = ClientboundBlockDestructionPacket.class)
public class JavaBlockDestructionTranslator extends PacketTranslator<ClientboundBlockDestructionPacket> {

    override public void translate(GeyserSession session, ClientboundBlockDestructionPacket packet) {
        if (packet.getStage() == BlockBreakStage.RESET) {

            session.getBlockBreakHandler().getDestructionStageCache().invalidate(packet.getPosition());
            BlockUtils.sendBedrockStopBlockBreak(session, packet.getPosition().toFloat());
            return;
        }


        LevelEventPacket levelEventPacket = new LevelEventPacket();
        levelEventPacket.setPosition(packet.getPosition().toFloat());


        Pair<Long, BlockBreakStage> lastUpdate = session.getBlockBreakHandler().getDestructionStageCache().getIfPresent(packet.getPosition());
        if (lastUpdate == null) {
            levelEventPacket.setType(LevelEvent.BLOCK_START_BREAK);
            levelEventPacket.setData(65535 / 6000);
        } else {

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
