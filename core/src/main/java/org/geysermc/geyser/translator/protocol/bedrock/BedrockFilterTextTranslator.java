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

package org.geysermc.geyser.translator.protocol.bedrock;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundRenameItemPacket;
import com.nukkitx.protocol.bedrock.packet.FilterTextPacket;
import org.geysermc.geyser.inventory.AnvilContainer;
import org.geysermc.geyser.inventory.CartographyContainer;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.ItemUtils;

/**
 * Used to send strings to the server and filter out unwanted words.
 * Java doesn't care, so we don't care, and we approve all strings.
 */
@Translator(packet = FilterTextPacket.class)
public class BedrockFilterTextTranslator extends PacketTranslator<FilterTextPacket> {

    @Override
    public void translate(GeyserSession session, FilterTextPacket packet) {
        if (session.getOpenInventory() instanceof CartographyContainer) {
            // We don't want to be able to rename in the cartography table
            return;
        }
        packet.setFromServer(true);
        if (session.getOpenInventory() instanceof AnvilContainer anvilContainer) {
            anvilContainer.setNewName(packet.getText());

            String originalName = ItemUtils.getCustomName(anvilContainer.getInput().getNbt());

            String plainOriginalName = MessageTranslator.convertToPlainText(originalName, session.getLocale());
            String plainNewName = MessageTranslator.convertToPlainText(packet.getText(), session.getLocale());
            if (!plainOriginalName.equals(plainNewName)) {
                // Strip out formatting since Java Edition does not allow it
                packet.setText(plainNewName);
                // Java Edition sends a packet every time an item is renamed even slightly in GUI. Fortunately, this works out for us now
                ServerboundRenameItemPacket renameItemPacket = new ServerboundRenameItemPacket(plainNewName);
                session.sendDownstreamPacket(renameItemPacket);
            } else {
                // Restore formatting for item since we're not renaming
                packet.setText(MessageTranslator.convertMessageLenient(originalName));
                // Java Edition sends the original custom name when not renaming,
                // if there isn't a custom name an empty string is sent
                ServerboundRenameItemPacket renameItemPacket = new ServerboundRenameItemPacket(plainOriginalName);
                session.sendDownstreamPacket(renameItemPacket);
            }

            anvilContainer.setUseJavaLevelCost(false);
            session.getInventoryTranslator().updateSlot(session, anvilContainer, 1);
        }
        session.sendUpstreamPacket(packet);
    }
}
