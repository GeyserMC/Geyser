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

import com.github.steveice10.mc.protocol.data.game.ResourcePackStatus;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientResourcePackStatusPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerResourcePackSendPacket;
import org.geysermc.common.window.SimpleFormWindow;
import org.geysermc.common.window.button.FormButton;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.JavaResourcePackUtils;
import org.geysermc.connector.utils.LocaleUtils;

@Translator(packet = ServerResourcePackSendPacket.class)
public class JavaResourcePackSendTranslator extends PacketTranslator<ServerResourcePackSendPacket> {

    @Override
    public void translate(ServerResourcePackSendPacket packet, GeyserSession session) {
        if (packet.getHash().equals(session.getResourcePackCache().getResourcePackHash())) {
            // TODO: Do we need to also send ACCEPTED?
            // TODO: Compare the pack name as well? How does vanilla handle it?
            ClientResourcePackStatusPacket statusPacket = new ClientResourcePackStatusPacket(ResourcePackStatus.SUCCESSFULLY_LOADED);
            session.sendDownstreamPacket(statusPacket);
            return;
        }
        String language = session.getClientData().getLanguageCode();

        SimpleFormWindow window = new SimpleFormWindow(LocaleUtils.getLocaleString("addServer.resourcePack", language),
                LocaleUtils.getLocaleString("multiplayer.texturePrompt.line1", language) + "\n" +
                        LocaleUtils.getLocaleString("multiplayer.texturePrompt.line2", language));

        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("gui.yes", language)));
        window.getButtons().add(new FormButton(LocaleUtils.getLocaleString("gui.no", language)));

        session.getResourcePackCache().setResourcePackUrl(packet.getUrl());
        session.getResourcePackCache().setResourcePackHash(packet.getHash());
        session.getResourcePackCache().setForm(window);

        session.sendForm(window, JavaResourcePackUtils.WINDOW_ID);
    }
}
