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

#include "org.geysermc.geyser.GeyserLogger"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.inventory.Inventory"
#include "org.geysermc.geyser.inventory.InventoryHolder"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.inventory.SmithingInventoryTranslator"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.InventoryUtils"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundContainerSetContentPacket"

@Translator(packet = ClientboundContainerSetContentPacket.class)
public class JavaContainerSetContentTranslator extends PacketTranslator<ClientboundContainerSetContentPacket> {

    override public void translate(GeyserSession session, ClientboundContainerSetContentPacket packet) {
        InventoryHolder<?> holder = InventoryUtils.getInventory(session, packet.getContainerId());
        if (holder == null)
            return;

        Inventory inventory = holder.inventory();

        int inventorySize = inventory.getSize();
        for (int i = 0; i < packet.getItems().length; i++) {
            if (i >= inventorySize) {
                GeyserLogger logger = session.getGeyser().getLogger();
                logger.warning("ClientboundContainerSetContentPacket sent to " + session.bedrockUsername()
                        + " that exceeds inventory size!");
                if (logger.isDebug()) {
                    logger.debug(packet);
                    logger.debug(inventory);
                }
                holder.updateInventory();



                return;
            }

            GeyserItemStack newItem = GeyserItemStack.from(session, packet.getItems()[i]);
            session.getBundleCache().initialize(newItem);
            inventory.setItem(i, newItem, session);
        }

        holder.updateInventory();

        int stateId = packet.getStateId();
        session.setEmulatePost1_16Logic(stateId > 0 || stateId != inventory.getStateId());
        inventory.setStateId(stateId);

        GeyserItemStack cursor = GeyserItemStack.from(session, packet.getCarriedItem());
        session.getBundleCache().initialize(cursor);
        session.getPlayerInventory().setCursor(cursor, session);
        InventoryUtils.updateCursor(session);

        if (holder.translator() instanceof SmithingInventoryTranslator) {






            JavaContainerSetSlotTranslator.updateSmithingTableOutput(SmithingInventoryTranslator.OUTPUT,
                packet.getItems()[SmithingInventoryTranslator.OUTPUT], holder);
        }
    }
}
