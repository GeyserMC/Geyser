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

import org.cloudburstmc.protocol.bedrock.packet.PacketViolationWarningPacket;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = PacketViolationWarningPacket.class)
public class BedrockPacketViolationWarningTranslator extends PacketTranslator<PacketViolationWarningPacket> {

    @Override
    public void translate(GeyserSession session, PacketViolationWarningPacket packet) {
        // Not translated since this is something that the developers need to know
        GeyserLogger logger = session.getGeyser().getLogger();
        if (logger.isDebug()) {
            logger.error("Packet violation warning sent from client (%s): %s".formatted(session.bedrockUsername(), packet));
            return;
        }

        String message = packet.getContext();
        if (message.length() > 100) {
            message = message.substring(0, 97) + "...";
        }

        logger.warning("Received packet violation warning (type: %s, severity: %s, packetCauseId: %s) from client (%s): %s! Enable debug mode for the full message.".formatted(packet.getType(),
            packet.getSeverity(), packet.getPacketCauseId(), session.bedrockUsername(), sanitize(message)));
        session.disconnect("An error occurred! Please contact an administrator.");
    }

    private static String sanitize(String message) {
        char[] input = message.toCharArray();
        StringBuilder output = new StringBuilder(input.length);
        for (int i = 0; i < input.length; i++) {
            char c = input[i];
            if (c == '§') { // ChatColor.ESCAPE
                i++;
                continue;
            }
            if (c >= 32 && c <= 126) {
                output.append(c);
            }
        }
        return output.toString();
    }
}
