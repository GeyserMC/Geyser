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

import org.cloudburstmc.protocol.bedrock.packet.CommandRequestPacket;
import org.geysermc.geyser.api.util.PlatformType;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.command.CommandRegistry;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.text.MessageTranslator;

@Translator(packet = CommandRequestPacket.class)
public class BedrockCommandRequestTranslator extends PacketTranslator<CommandRequestPacket> {

    @Override
    public void translate(GeyserSession session, CommandRequestPacket packet) {
        String command = MessageTranslator.convertToPlainText(packet.getCommand());
        String strippedCommand = command.substring(1);

        if (session.getGeyser().getPlatformType() == PlatformType.STANDALONE) {
            // try to handle the command within the standalone command manager

            String[] args = strippedCommand.split(" ");
            if (args.length > 0) {
                String root = args[0];

                // todo cloud do we want to pass the command to the server
                //  if cloud gives a NoSuchCommandException? might be more accurate.
                CommandRegistry registry = GeyserImpl.getInstance().commandRegistry();
                if (registry.cloud().rootCommands().contains(root)) {
                    // todo cloud might not like the trailing whitespace either
                    registry.runCommand(session, strippedCommand);
                    return; // don't pass the command to the java server
                }
            }
        }

        if (MessageTranslator.isTooLong(command, session)) {
            return;
        }

        // running commands via Bedrock's command select menu adds a trailing whitespace which Java doesn't like
        // https://github.com/GeyserMC/Geyser/issues/3877
        session.sendCommand(strippedCommand.stripTrailing());
    }
}
