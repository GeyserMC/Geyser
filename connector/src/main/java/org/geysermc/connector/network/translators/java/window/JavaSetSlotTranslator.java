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

package org.geysermc.connector.network.translators.java.window;

import com.github.steveice10.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import org.geysermc.connector.inventory.GeyserItemStack;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.inventory.InventoryTranslator;
import org.geysermc.connector.utils.InventoryUtils;

@Translator(packet = ServerSetSlotPacket.class)
public class JavaSetSlotTranslator extends PacketTranslator<ServerSetSlotPacket> {

    @Override
    public void translate(ServerSetSlotPacket packet, GeyserSession session) {
        System.out.println(packet.toString());
        session.addInventoryTask(() -> {
            if (packet.getWindowId() == 255) { //cursor
                GeyserItemStack newItem = GeyserItemStack.from(packet.getItem());
                session.getPlayerInventory().setCursor(newItem, session);
                InventoryUtils.updateCursor(session);
                return;
            }

            //TODO: support window id -2, should update player inventory
            Inventory inventory = InventoryUtils.getInventory(session, packet.getWindowId());
            if (inventory == null)
                return;

            InventoryTranslator translator = session.getInventoryTranslator();
            if (translator != null) {
                GeyserItemStack newItem = GeyserItemStack.from(packet.getItem());
                inventory.setItem(packet.getSlot(), newItem, session);
                translator.updateSlot(session, inventory, packet.getSlot());
            }
        });
    }
}
