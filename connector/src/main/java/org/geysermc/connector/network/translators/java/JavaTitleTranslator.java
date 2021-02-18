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

package org.geysermc.connector.network.translators.java;

import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.chat.MessageTranslator;

import com.github.steveice10.mc.protocol.packet.ingame.server.ServerTitlePacket;
import com.nukkitx.protocol.bedrock.packet.SetTitlePacket;

@Translator(packet = ServerTitlePacket.class)
public class JavaTitleTranslator extends PacketTranslator<ServerTitlePacket> {

    @Override
    public void translate(ServerTitlePacket packet, GeyserSession session) {
        SetTitlePacket titlePacket = new SetTitlePacket();
        String locale = session.getLocale();

        String text;
        if (packet.getTitle() == null) {
            text = " ";
        } else {
            text = MessageTranslator.convertMessage(packet.getTitle(), locale);
        }

        switch (packet.getAction()) {
            case TITLE:
                titlePacket.setType(SetTitlePacket.Type.TITLE);
                titlePacket.setText(text);
                break;
            case SUBTITLE:
                titlePacket.setType(SetTitlePacket.Type.SUBTITLE);
                titlePacket.setText(text);
                break;
            case CLEAR:
            case RESET:
                titlePacket.setType(SetTitlePacket.Type.CLEAR);
                titlePacket.setText("");
                break;
            case ACTION_BAR:
                titlePacket.setType(SetTitlePacket.Type.ACTIONBAR);
                titlePacket.setText(text);
                break;
            case TIMES:
                titlePacket.setType(SetTitlePacket.Type.TIMES);
                titlePacket.setFadeInTime(packet.getFadeIn());
                titlePacket.setFadeOutTime(packet.getFadeOut());
                titlePacket.setStayTime(packet.getStay());
                titlePacket.setText("");
                break;
        }

        session.sendUpstreamPacket(titlePacket);
    }
}
