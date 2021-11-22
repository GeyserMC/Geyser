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

package org.geysermc.geyser.translator.protocol.java.inventory;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.geyser.text.MinecraftLocale;

@Translator(packet = ClientboundOpenScreenPacket.class)
public class JavaOpenScreenTranslator extends PacketTranslator<ClientboundOpenScreenPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundOpenScreenPacket packet) {
        if (packet.getContainerId() == 0) {
            return;
        }

        InventoryTranslator newTranslator = InventoryTranslator.INVENTORY_TRANSLATORS.get(packet.getType());
        Inventory openInventory = session.getOpenInventory();
        // No translator exists for this window type. Close all windows and return.
        if (newTranslator == null) {
            if (openInventory != null) {
                InventoryUtils.closeInventory(session, openInventory.getId(), true);
            }
            ServerboundContainerClosePacket closeWindowPacket = new ServerboundContainerClosePacket(packet.getContainerId());
            session.sendDownstreamPacket(closeWindowPacket);
            return;
        }

        String name = MessageTranslator.convertMessageLenient(packet.getName(), session.getLocale());
        name = MinecraftLocale.getLocaleString(name, session.getLocale());

        Inventory newInventory = newTranslator.createInventory(name, packet.getContainerId(), packet.getType(), session.getPlayerInventory());
        if (openInventory != null) {
            // If the window type is the same, don't close.
            // In rare cases, inventories can do funny things where it keeps the same window type up but change the contents.
            if (openInventory.getContainerType() != packet.getType()) {
                // Sometimes the server can double-open an inventory with the same ID - don't confirm in that instance.
                InventoryUtils.closeInventory(session, openInventory.getId(), openInventory.getId() != packet.getContainerId());
            }
        }

        session.setInventoryTranslator(newTranslator);
        InventoryUtils.openInventory(session, newInventory);
    }
}
