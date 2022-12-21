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

package org.geysermc.geyser.translator.protocol.java.inventory;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.translator.inventory.PlayerInventoryTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.InventoryUtils;

@Translator(packet = ClientboundContainerSetContentPacket.class)
public class JavaContainerSetContentTranslator extends PacketTranslator<ClientboundContainerSetContentPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundContainerSetContentPacket packet) {
        Inventory inventory = InventoryUtils.getInventory(session, packet.getContainerId());
        if (inventory == null)
            return;

        int inventorySize = inventory.getSize();
        for (int i = 0; i < packet.getItems().length; i++) {
            if (i >= inventorySize) {
                GeyserImpl geyser = session.getGeyser();
                geyser.getLogger().warning("ClientboundContainerSetContentPacket sent to " + session.bedrockUsername()
                        + " that exceeds inventory size!");
                if (geyser.getConfig().isDebugMode()) {
                    geyser.getLogger().debug(packet);
                    geyser.getLogger().debug(inventory);
                }
                updateInventory(session, inventory, packet.getContainerId());
                // 1.18.1 behavior: the previous items will be correctly set, but the state ID and carried item will not
                // as this produces a stack trace on the client.
                // If Java processes this correctly in the future, we can revert this behavior
                return;
            }

            GeyserItemStack newItem = GeyserItemStack.from(packet.getItems()[i]);
            inventory.setItem(i, newItem, session);
        }

        updateInventory(session, inventory, packet.getContainerId());

        int stateId = packet.getStateId();
        session.setEmulatePost1_16Logic(stateId > 0 || stateId != inventory.getStateId());
        inventory.setStateId(stateId);

        session.getPlayerInventory().setCursor(GeyserItemStack.from(packet.getCarriedItem()), session);
        InventoryUtils.updateCursor(session);
    }

    private void updateInventory(GeyserSession session, Inventory inventory, int containerId) {
        InventoryTranslator translator = session.getInventoryTranslator();
        if (containerId == 0 && !(translator instanceof PlayerInventoryTranslator)) {
            // In rare cases, the window ID can still be 0 but Java treats it as valid
            InventoryTranslator.PLAYER_INVENTORY_TRANSLATOR.updateInventory(session, inventory);
        } else if (translator != null) {
            translator.updateInventory(session, inventory);
        }
    }
}
