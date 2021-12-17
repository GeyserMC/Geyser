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

package org.geysermc.geyser.translator.protocol.bedrock;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.level.block.CommandBlockMode;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetCommandMinecartPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetCommandBlockPacket;
import com.nukkitx.protocol.bedrock.packet.CommandBlockUpdatePacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = CommandBlockUpdatePacket.class)
public class BedrockCommandBlockUpdateTranslator extends PacketTranslator<CommandBlockUpdatePacket> {

    @Override
    public void translate(GeyserSession session, CommandBlockUpdatePacket packet) {
        String command = packet.getCommand();
        boolean outputTracked = packet.isOutputTracked();
        if (packet.isBlock()) {
            CommandBlockMode mode = switch (packet.getMode()) {
                case CHAIN -> // The green one
                        CommandBlockMode.SEQUENCE;
                case REPEATING -> // The purple one
                        CommandBlockMode.AUTO;
                default -> // NORMAL, the orange one
                        CommandBlockMode.REDSTONE;
            };
            boolean isConditional = packet.isConditional();
            boolean automatic = !packet.isRedstoneMode(); // Automatic = Always Active option in Java
            ServerboundSetCommandBlockPacket commandBlockPacket = new ServerboundSetCommandBlockPacket(
                    new Position(packet.getBlockPosition().getX(), packet.getBlockPosition().getY(), packet.getBlockPosition().getZ()),
                    command, mode, outputTracked, isConditional, automatic);
            session.sendDownstreamPacket(commandBlockPacket);
        } else {
            ServerboundSetCommandMinecartPacket commandMinecartPacket = new ServerboundSetCommandMinecartPacket(
                    (int) session.getEntityCache().getEntityByGeyserId(packet.getMinecartRuntimeEntityId()).getEntityId(),
                    command, outputTracked
            );
            session.sendDownstreamPacket(commandMinecartPacket);
        }
    }
}
