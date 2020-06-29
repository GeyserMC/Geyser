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

package org.geysermc.connector.network.translators.java.window;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.Objects;

@Translator(packet = ServerSetSlotPacket.class)
public class JavaSetSlotTranslator extends PacketTranslator<ServerSetSlotPacket> {

    @Override
    public void translate(ServerSetSlotPacket packet, GeyserSession session) {
        if (packet.getWindowId() == 255 && packet.getSlot() == -1) { //cursor
            if (session.getCraftSlot() != 0)
                return;

            session.getInventory().setCursor(packet.getItem());
            InventoryUtils.updateCursor(session);
            return;
        }

        Inventory inventory = session.getInventoryCache().getInventories().get(packet.getWindowId());
        if (inventory == null || (packet.getWindowId() != 0 && inventory.getWindowType() == null))
            return;

        InventoryTranslator translator = InventoryTranslator.INVENTORY_TRANSLATORS.get(inventory.getWindowType());
        if (translator != null) {
            inventory.setItem(packet.getSlot(), packet.getItem());
            translator.updateSlot(session, inventory, packet.getSlot());
        }
    }
}
