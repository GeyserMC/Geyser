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

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import com.nukkitx.protocol.bedrock.packet.TextPacket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatTypeEntry;
import org.geysermc.geyser.text.TextDecoration;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.text.MessageTranslator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Translator(packet = ClientboundPlayerChatPacket.class)
public class JavaPlayerChatTranslator extends PacketTranslator<ClientboundPlayerChatPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundPlayerChatPacket packet) {
        ChatTypeEntry entry = session.getChatTypes().get(packet.getTypeId());

        TextPacket textPacket = new TextPacket();
        textPacket.setPlatformChatId("");
        textPacket.setSourceName("");
        textPacket.setXuid(session.getAuthData().xuid());
        textPacket.setType(entry.bedrockChatType());

        textPacket.setNeedsTranslation(false);
        Component message = packet.getUnsignedContent() == null ? packet.getSignedContent() : packet.getUnsignedContent();

        TextDecoration decoration = entry.textDecoration();
        if (decoration != null) {
            // As of 1.19 - do this to apply all the styling for signed messages
            // Though, Bedrock cannot care about the signed stuff.
            TranslatableComponent.Builder withDecoration = Component.translatable()
                    .key(decoration.translationKey())
                    .style(decoration.style());
            Set<TextDecoration.Parameter> parameters = decoration.parameters();
            List<Component> args = new ArrayList<>(3);
            if (parameters.contains(TextDecoration.Parameter.TEAM_NAME)) {
                args.add(packet.getSenderTeamName());
            }
            if (parameters.contains(TextDecoration.Parameter.SENDER)) {
                args.add(packet.getSenderName());
            }
            if (parameters.contains(TextDecoration.Parameter.CONTENT)) {
                args.add(message);
            }
            withDecoration.args(args);
            textPacket.setMessage(MessageTranslator.convertMessage(withDecoration.build(), session.getLocale()));
        } else {
            textPacket.setMessage(MessageTranslator.convertMessage(message, session.getLocale()));
        }

        session.sendUpstreamPacket(textPacket);
    }
}
