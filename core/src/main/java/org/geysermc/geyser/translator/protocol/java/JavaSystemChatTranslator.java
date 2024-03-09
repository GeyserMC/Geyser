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

package org.geysermc.geyser.translator.protocol.java;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import net.kyori.adventure.text.TranslatableComponent;
import org.cloudburstmc.protocol.bedrock.packet.TextPacket;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.text.MessageTranslator;

@Translator(packet = ClientboundSystemChatPacket.class)
public class JavaSystemChatTranslator extends PacketTranslator<ClientboundSystemChatPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundSystemChatPacket packet) {
        if (packet.getContent() instanceof TranslatableComponent component && component.key().equals("chat.disabled.missingProfileKey")) {
            // We likely got this message as a response to a player trying to chat
            // As there SHOULD be no false flags for this, print every time it shows up in chat.
            if (Boolean.parseBoolean(System.getProperty("Geyser.PrintSecureChatInformation", "true"))) {
                session.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.chat.secure_info_1", session.locale()));
                session.sendMessage(GeyserLocale.getPlayerLocaleString("geyser.chat.secure_info_2", session.locale(), "https://geysermc.link/secure-chat"));
            }
        }

        TextPacket textPacket = new TextPacket();
        textPacket.setPlatformChatId("");
        textPacket.setSourceName("");
        textPacket.setXuid(session.getAuthData().xuid());
        textPacket.setType(packet.isOverlay() ? TextPacket.Type.JUKEBOX_POPUP : TextPacket.Type.SYSTEM);

        textPacket.setNeedsTranslation(false);
        if (packet.isOverlay()) {
            textPacket.setMessage(ChatColor.WHITE + MessageTranslator.convertMessage(packet.getContent(), session.locale()));
        } else {
            textPacket.setMessage(MessageTranslator.convertMessage(packet.getContent(), session.locale()));
        }

        if (session.isSentSpawnPacket()) {
            session.sendUpstreamPacket(textPacket);
        } else {
            session.getUpstream().queuePostStartGamePacket(textPacket);
        }
    }
}
