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

package org.geysermc.connector.network.translators.java;

import com.github.steveice10.mc.protocol.data.message.TranslationMessage;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.nukkitx.protocol.bedrock.packet.TextPacket;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.MessageUtils;

import java.util.List;

@Translator(packet = ServerChatPacket.class)
public class JavaChatTranslator extends PacketTranslator<ServerChatPacket> {

    @Override
    public void translate(ServerChatPacket packet, GeyserSession session) {
        TextPacket textPacket = new TextPacket();
        textPacket.setPlatformChatId("");
        textPacket.setSourceName("");
        textPacket.setXuid(session.getAuthData().getXboxUUID());
        switch (packet.getType()) {
            case CHAT:
                textPacket.setType(TextPacket.Type.CHAT);
                break;
            case SYSTEM:
                textPacket.setType(TextPacket.Type.SYSTEM);
                break;
            case NOTIFICATION:
                textPacket.setType(TextPacket.Type.TIP);
                break;
            default:
                textPacket.setType(TextPacket.Type.RAW);
                break;
        }

        String locale = session.getClientData().getLanguageCode();

        if (packet.getMessage() instanceof TranslationMessage) {
            textPacket.setType(TextPacket.Type.TRANSLATION);
            textPacket.setNeedsTranslation(true);

            List<String> paramsTranslated = MessageUtils.getTranslationParams(((TranslationMessage) packet.getMessage()).getWith(), locale);
            textPacket.setParameters(paramsTranslated);

            textPacket.setMessage(MessageUtils.insertParams(MessageUtils.getTranslatedBedrockMessage(packet.getMessage(), locale, true), paramsTranslated));
        } else {
            textPacket.setNeedsTranslation(false);

            textPacket.setMessage(MessageUtils.getTranslatedBedrockMessage(packet.getMessage(), locale, false));
        }

        session.sendUpstreamPacket(textPacket);
    }
}
