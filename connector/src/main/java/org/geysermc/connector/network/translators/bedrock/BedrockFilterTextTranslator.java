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

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientRenameItemPacket;
import com.nukkitx.protocol.bedrock.packet.FilterTextPacket;
import org.geysermc.connector.inventory.AnvilContainer;
import org.geysermc.connector.inventory.CartographyContainer;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

/**
 * Used to send strings to the server and filter out unwanted words.
 * Java doesn't care, so we don't care, and we approve all strings.
 */
@Translator(packet = FilterTextPacket.class)
public class BedrockFilterTextTranslator extends PacketTranslator<FilterTextPacket> {

    @Override
    public void translate(FilterTextPacket packet, GeyserSession session) {
        if (session.getOpenInventory() instanceof CartographyContainer) {
            // We don't want to be able to rename in the cartography table
            return;
        }
        packet.setFromServer(true);
        session.sendUpstreamPacket(packet);

        if (session.getOpenInventory() instanceof AnvilContainer) {
            // Java Edition sends a packet every time an item is renamed even slightly in GUI. Fortunately, this works out for us now
            ClientRenameItemPacket renameItemPacket = new ClientRenameItemPacket(packet.getText());
            session.sendDownstreamPacket(renameItemPacket);
        }
    }
}
