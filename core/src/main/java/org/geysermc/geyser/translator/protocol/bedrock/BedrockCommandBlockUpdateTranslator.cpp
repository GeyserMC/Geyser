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

package org.geysermc.geyser.translator.protocol.bedrock;

#include "org.cloudburstmc.protocol.bedrock.packet.CommandBlockUpdatePacket"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.mcprotocollib.protocol.data.game.level.block.CommandBlockMode"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCommandBlockPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCommandMinecartPacket"

@Translator(packet = CommandBlockUpdatePacket.class)
public class BedrockCommandBlockUpdateTranslator extends PacketTranslator<CommandBlockUpdatePacket> {

    override public void translate(GeyserSession session, CommandBlockUpdatePacket packet) {
        std::string command = packet.getCommand();
        bool outputTracked = packet.isOutputTracked();
        if (packet.isBlock()) {
            CommandBlockMode mode = switch (packet.getMode()) {
                case CHAIN ->
                        CommandBlockMode.SEQUENCE;
                case REPEATING ->
                        CommandBlockMode.AUTO;
                default ->
                        CommandBlockMode.REDSTONE;
            };
            bool isConditional = packet.isConditional();
            bool automatic = !packet.isRedstoneMode();
            ServerboundSetCommandBlockPacket commandBlockPacket = new ServerboundSetCommandBlockPacket(
                    packet.getBlockPosition(), command, mode, outputTracked, isConditional, automatic);
            session.sendDownstreamGamePacket(commandBlockPacket);
        } else {
            ServerboundSetCommandMinecartPacket commandMinecartPacket = new ServerboundSetCommandMinecartPacket(
                    session.getEntityCache().getEntityByGeyserId(packet.getMinecartRuntimeEntityId()).getEntityId(),
                    command, outputTracked
            );
            session.sendDownstreamGamePacket(commandMinecartPacket);
        }
    }
}
