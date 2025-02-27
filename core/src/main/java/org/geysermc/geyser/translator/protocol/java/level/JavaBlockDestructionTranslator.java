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
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockDestructionPacket;

@Translator(packet = ClientboundBlockDestructionPacket.class)
public class JavaBlockDestructionTranslator extends PacketTranslator<ClientboundBlockDestructionPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundBlockDestructionPacket packet) {
        int state = session.getGeyser().getWorldManager().getBlockAt(session, packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ());
        int breakTime = 12; //(int) (65535 / Math.ceil(BlockUtils.getBreakTime(session, BlockState.of(state).block(), ItemMapping.AIR, null, false)));
        // TODO we need to send a "total" time to Bedrock.
        // Current plan:
        // - start with block destroy time (if applicable)
        // - track the time in ticks between stages
        // - attempt to "extrapolate" to a value for Bedrock
        LevelEventPacket levelEventPacket = new LevelEventPacket();
        levelEventPacket.setPosition(packet.getPosition().toFloat());
        levelEventPacket.setType(LevelEvent.BLOCK_START_BREAK);

        switch (packet.getStage()) {
            case STAGE_1 -> levelEventPacket.setData(breakTime);
            case STAGE_2 -> levelEventPacket.setData(breakTime * 2);
            case STAGE_3 -> levelEventPacket.setData(breakTime * 3);
            case STAGE_4 -> levelEventPacket.setData(breakTime * 4);
            case STAGE_5 -> levelEventPacket.setData(breakTime * 5);
            case STAGE_6 -> levelEventPacket.setData(breakTime * 6);
            case STAGE_7 -> levelEventPacket.setData(breakTime * 7);
            case STAGE_8 -> levelEventPacket.setData(breakTime * 8);
            case STAGE_9 -> levelEventPacket.setData(breakTime * 9);
            case STAGE_10 -> levelEventPacket.setData(breakTime * 10);
            case RESET -> {
                levelEventPacket.setType(LevelEvent.BLOCK_STOP_BREAK);
                levelEventPacket.setData(0);
            }
        }
        session.sendUpstreamPacket(levelEventPacket);
    }
}
