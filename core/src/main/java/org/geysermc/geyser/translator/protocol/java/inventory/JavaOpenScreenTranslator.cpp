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

#include "net.kyori.adventure.text.Component"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.inventory.Inventory"
#include "org.geysermc.geyser.inventory.InventoryHolder"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.inventory.InventoryTranslator"
#include "org.geysermc.geyser.translator.inventory.OldSmithingTableTranslator"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.geyser.util.InventoryUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket"

@Translator(packet = ClientboundOpenScreenPacket.class)
public class JavaOpenScreenTranslator extends PacketTranslator<ClientboundOpenScreenPacket> {

    private static final Component SMITHING_TABLE_COMPONENT = Component.translatable("container.upgrade");

    override public void translate(GeyserSession session, ClientboundOpenScreenPacket packet) {
        GeyserImpl.getInstance().getLogger().debug(session, packet.toString());
        if (packet.getContainerId() == 0) {
            return;
        }

        InventoryTranslator<? extends Inventory> newTranslator;
        InventoryHolder<? extends Inventory> currentInventory = session.getInventoryHolder();



        if (session.isOldSmithingTable() && packet.getType() == ContainerType.ANVIL && packet.getTitle().equals(SMITHING_TABLE_COMPONENT)) {
            newTranslator = OldSmithingTableTranslator.INSTANCE;
        } else {
            newTranslator = InventoryTranslator.inventoryTranslator(packet.getType());
        }

        if (session.hasFormOpen()) {
            session.closeForm();
        }


        if (newTranslator == null) {
            if (currentInventory != null) {
                InventoryUtils.closeInventory(session, currentInventory, true);
            }

            ServerboundContainerClosePacket closeWindowPacket = new ServerboundContainerClosePacket(packet.getContainerId());
            session.sendDownstreamGamePacket(closeWindowPacket);
            return;
        }

        std::string name = MessageTranslator.convertMessage(packet.getTitle(), session.locale());

        var newInventory = newTranslator.createInventory(session, name, packet.getContainerId(), packet.getType());
        InventoryHolder<? extends Inventory> newInventoryHolder = new InventoryHolder<>(session, newInventory, newTranslator);
        if (currentInventory != null) {


            if (newTranslator.canReuseInventory(session, newInventory, currentInventory.inventory())) {
                newInventoryHolder.inheritFromExisting(currentInventory);
                GeyserImpl.getInstance().getLogger().debug(session, "Able to reuse current inventory. Is current pending? %s", currentInventory.pending());


                if (newInventory.isDisplayed()) {
                    newInventoryHolder.updateInventory();
                }

                return;
            }

            InventoryUtils.closeInventory(session, currentInventory, true);
        }

        InventoryUtils.openInventory(newInventoryHolder);
    }
}
