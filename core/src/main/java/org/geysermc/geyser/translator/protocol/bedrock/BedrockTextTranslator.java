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

import com.nukkitx.protocol.bedrock.packet.TextPacket;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.text.MessageTranslator;

@Translator(packet = TextPacket.class)
public class BedrockTextTranslator extends PacketTranslator<TextPacket> {

    @Override
    public void translate(GeyserSession session, TextPacket packet) {
        String message = MessageTranslator.convertToPlainText(packet.getMessage());

        if (message.isBlank()) {
            // Java Edition (as of 1.17.1) just doesn't pass on these messages, so... we won't either!
            return;
        }

        if (MessageTranslator.isTooLong(message, session)) {
            return;
        }

        if (session.getWorldCache().getChatWarningSent() == TriState.FALSE) {
            if (Boolean.parseBoolean(System.getProperty("Geyser.PrintSecureChatInformation", "true"))) {
                session.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.chat.secure_info_1", session.locale()));
                session.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.chat.secure_info_2", session.locale(), "https://geysermc.link/secure-chat"));
            }
            // Never send this message again for this session.
            session.getWorldCache().setChatWarningSent(TriState.TRUE);
        }

        session.sendChat(message);
    }
}
