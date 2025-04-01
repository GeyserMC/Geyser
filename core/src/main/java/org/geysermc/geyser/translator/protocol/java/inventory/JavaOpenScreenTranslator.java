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

import net.kyori.adventure.text.Component;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.translator.inventory.OldSmithingTableTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;

@Translator(packet = ClientboundOpenScreenPacket.class)
public class JavaOpenScreenTranslator extends PacketTranslator<ClientboundOpenScreenPacket> {

    private static final Component SMITHING_TABLE_COMPONENT = Component.translatable("container.upgrade");

    @Override
    public void translate(GeyserSession session, ClientboundOpenScreenPacket packet) {
        GeyserImpl.getInstance().getLogger().debug(session, packet.toString());
        if (packet.getContainerId() == 0) {
            return;
        }

        InventoryTranslator newTranslator;
        Inventory openInventory = session.getOpenInventory();

        // Hack: ViaVersion translates the old (pre 1.20) smithing table to a anvil (does not work for Bedrock). We can detect this and translate it back to a smithing table.
        // (Implementation note: used to be a furnace. Was changed sometime before 1.21.2)
        if (session.isOldSmithingTable() && packet.getType() == ContainerType.ANVIL && packet.getTitle().equals(SMITHING_TABLE_COMPONENT)) {
            newTranslator = OldSmithingTableTranslator.INSTANCE;
        } else {
            newTranslator = InventoryTranslator.inventoryTranslator(packet.getType());
        }

        // No translator exists for this window type. Close all windows and return.
        if (newTranslator == null) {
            if (openInventory != null) {
                InventoryUtils.closeInventory(session, openInventory.getJavaId(), true);
            }

            ServerboundContainerClosePacket closeWindowPacket = new ServerboundContainerClosePacket(packet.getContainerId());
            session.sendDownstreamGamePacket(closeWindowPacket);
            return;
        }

        String name = MessageTranslator.convertMessage(packet.getTitle(), session.locale());

        Inventory newInventory = newTranslator.createInventory(session, name, packet.getContainerId(), packet.getType(), session.getPlayerInventory());
        if (openInventory != null) {
            // Attempt to re-use existing open inventories, if possible.
            // Pending inventories are also considered, as a Java server can re-request the same inventory.
            if (newTranslator.canReuseInventory(session, newInventory, openInventory)) {
                // Use the same Bedrock id
                newInventory.setBedrockId(openInventory.getBedrockId());

                // Also mirror other properties - in case we're e.g. dealing with a pending virtual inventory
                boolean pending = openInventory.isPending();
                newInventory.setDisplayed(openInventory.isDisplayed());
                newInventory.setPending(pending);
                newInventory.setHolderPosition(openInventory.getHolderPosition());
                newInventory.setHolderId(openInventory.getHolderId());
                session.setOpenInventory(newInventory);

                GeyserImpl.getInstance().getLogger().debug(session, "Able to reuse current inventory. Is current pending? %s", pending);

                // If the current inventory is still pending, it'll be updated once open
                if (newInventory.isDisplayed()) {
                    newTranslator.updateInventory(session, newInventory);
                }

                return;
            }

            InventoryUtils.closeInventory(session, openInventory.getJavaId(), true);
        }

        InventoryUtils.openInventory(session, newInventory);
    }
}
