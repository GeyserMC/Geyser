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

package org.geysermc.connector.network.translators.java.entity.player;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerActionAckPacket;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.TranslatorsInit;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.utils.BlockUtils;
import org.geysermc.connector.utils.ChunkUtils;

public class JavaPlayerActionAckTranslator extends PacketTranslator<ServerPlayerActionAckPacket> {

    @Override
    public void translate(ServerPlayerActionAckPacket packet, GeyserSession session) {
        switch (packet.getAction()) {
            case FINISH_DIGGING:
                ChunkUtils.updateBlock(session, packet.getNewState(), packet.getPosition());
                break;

            case START_DIGGING: {
                LevelEventPacket levelEvent = new LevelEventPacket();
                levelEvent.setType(LevelEventType.BLOCK_START_BREAK);
                levelEvent.setPosition(Vector3f.from(
                        packet.getPosition().getX(),
                        packet.getPosition().getY(),
                        packet.getPosition().getZ()
                ));
                double blockHardness = BlockTranslator.JAVA_RUNTIME_ID_TO_HARDNESS.get(packet.getNewState().getId());
                PlayerInventory inventory = session.getInventory();
                ItemStack item = inventory.getItemInHand();
                ItemEntry itemEntry = null;
                CompoundTag nbtData = new CompoundTag("");
                if (item != null) {
                    itemEntry = TranslatorsInit.getItemTranslator().getItem(item);
                    nbtData = item.getNbt();
                    System.out.println("item.getNbt() = " + item.getNbt());
                }
                double breakTime = Math.ceil(BlockUtils.getBreakTime(blockHardness, packet.getNewState().getId(), itemEntry, nbtData, session.getPlayerEntity()) * 20);
                System.out.println("breakTime = " + breakTime);
                int data = (int) (65535 / breakTime);
                System.out.println("data = " + data);
                levelEvent.setData((int) (65535 / breakTime));
                session.getUpstream().sendPacket(levelEvent);
                break;
            }

            case CANCEL_DIGGING: {
                LevelEventPacket levelEvent = new LevelEventPacket();
                levelEvent.setType(LevelEventType.BLOCK_STOP_BREAK);
                levelEvent.setPosition(Vector3f.from(
                        packet.getPosition().getX(),
                        packet.getPosition().getY(),
                        packet.getPosition().getZ()
                ));
                levelEvent.setData(0);
                session.getUpstream().sendPacket(levelEvent);
                break;
            }
        }
    }
}


