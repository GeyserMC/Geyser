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
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerActionAckPacket;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.world.block.BlockTranslator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.utils.BlockUtils;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.ChunkUtils;

@Translator(packet = ServerPlayerActionAckPacket.class)
public class JavaPlayerActionAckTranslator extends PacketTranslator<ServerPlayerActionAckPacket> {

    @Override
    public void translate(ServerPlayerActionAckPacket packet, GeyserSession session) {
        LevelEventPacket levelEvent = new LevelEventPacket();
        switch (packet.getAction()) {
            case FINISH_DIGGING:
                double blockHardness = BlockTranslator.JAVA_RUNTIME_ID_TO_HARDNESS.get(session.getBreakingBlock());
                if (session.getGameMode() != GameMode.CREATIVE && blockHardness != 0) {
                    levelEvent.setType(LevelEventType.PARTICLE_DESTROY_BLOCK);
                    levelEvent.setPosition(Vector3f.from(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ()));
                    levelEvent.setData(BlockTranslator.getBedrockBlockId(session.getBreakingBlock()));
                    session.sendUpstreamPacket(levelEvent);
                    session.setBreakingBlock(0);
                }
                ChunkUtils.updateBlock(session, packet.getNewState(), packet.getPosition());
                break;
            case START_DIGGING:
                if (session.getGameMode() == GameMode.CREATIVE) {
                    break;
                }
                blockHardness = BlockTranslator.JAVA_RUNTIME_ID_TO_HARDNESS.get(packet.getNewState());
                levelEvent.setType(LevelEventType.BLOCK_START_BREAK);
                levelEvent.setPosition(Vector3f.from(
                        packet.getPosition().getX(),
                        packet.getPosition().getY(),
                        packet.getPosition().getZ()
                ));
                PlayerInventory inventory = session.getInventory();
                ItemStack item = inventory.getItemInHand();
                ItemEntry itemEntry = null;
                CompoundTag nbtData = new CompoundTag("");
                if (item != null) {
                    itemEntry = ItemRegistry.getItem(item);
                    nbtData = item.getNbt();
                }
                double breakTime = Math.ceil(BlockUtils.getBreakTime(blockHardness, packet.getNewState(), itemEntry, nbtData, session) * 20);
                levelEvent.setData((int) (65535 / breakTime));
                session.setBreakingBlock(packet.getNewState());
                session.sendUpstreamPacket(levelEvent);
                break;
            case CANCEL_DIGGING:
                levelEvent.setType(LevelEventType.BLOCK_STOP_BREAK);
                levelEvent.setPosition(Vector3f.from(
                        packet.getPosition().getX(),
                        packet.getPosition().getY(),
                        packet.getPosition().getZ()
                ));
                levelEvent.setData(0);
                session.setBreakingBlock(0);
                session.sendUpstreamPacket(levelEvent);
                break;
        }
    }
}