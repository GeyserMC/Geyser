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

package org.geysermc.connector.network.translators.java.world;

import com.github.steveice10.mc.protocol.packet.ingame.server.world.ServerBlockBreakAnimPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.utils.BlockUtils;

@Translator(packet = ServerBlockBreakAnimPacket.class)
public class JavaBlockBreakAnimTranslator extends PacketTranslator<ServerBlockBreakAnimPacket> {

    @Override
    public void translate(ServerBlockBreakAnimPacket packet, GeyserSession session) {
        int state = session.getConnector().getWorldManager().getBlockAt(session, packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ());
        int breakTime = (int) (65535 / Math.ceil(BlockUtils.getBreakTime(session, BlockTranslator.getBlockMapping(state), ItemEntry.AIR, new CompoundTag(""), false) * 20));
        LevelEventPacket levelEventPacket = new LevelEventPacket();
        levelEventPacket.setPosition(Vector3f.from(
                packet.getPosition().getX(),
                packet.getPosition().getY(),
                packet.getPosition().getZ()
        ));
        levelEventPacket.setType(LevelEventType.BLOCK_START_BREAK);

        switch (packet.getStage()) {
            case STAGE_1:
                levelEventPacket.setData(breakTime);
                break;
            case STAGE_2:
                levelEventPacket.setData(breakTime * 2);
                break;
            case STAGE_3:
                levelEventPacket.setData(breakTime * 3);
                break;
            case STAGE_4:
                levelEventPacket.setData(breakTime * 4);
                break;
            case STAGE_5:
                levelEventPacket.setData(breakTime * 5);
                break;
            case STAGE_6:
                levelEventPacket.setData(breakTime * 6);
                break;
            case STAGE_7:
                levelEventPacket.setData(breakTime * 7);
                break;
            case STAGE_8:
                levelEventPacket.setData(breakTime * 8);
                break;
            case STAGE_9:
                levelEventPacket.setData(breakTime * 9);
                break;
            case STAGE_10:
                levelEventPacket.setData(breakTime * 10);
                break;
            case RESET:
                levelEventPacket.setType(LevelEventType.BLOCK_STOP_BREAK);
                levelEventPacket.setData(0);
                break;
        }
        session.sendUpstreamPacket(levelEventPacket);
    }
}
