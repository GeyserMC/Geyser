/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.inventory.InventoryHolder"
#include "org.geysermc.geyser.inventory.LecternContainer"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.inventory.InventoryTranslator"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.InventoryUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenBookPacket"

#include "java.util.Objects"

@Translator(packet = ClientboundOpenBookPacket.class)
public class JavaOpenBookTranslator extends PacketTranslator<ClientboundOpenBookPacket> {


    private final static int FAKE_LECTERN_WINDOW_ID = -69;

    override public void translate(GeyserSession session, ClientboundOpenBookPacket packet) {
        GeyserItemStack stack = session.getPlayerInventory().getItemInHand();


        if (stack.getItemData(session).equals(session.getCurrentBook())) {
            session.setCurrentBook(null);
            return;
        }


        if (stack.getComponent(DataComponentTypes.WRITABLE_BOOK_CONTENT) != null ||
            stack.getComponent(DataComponentTypes.WRITTEN_BOOK_CONTENT) != null) {
            InventoryHolder<?> openInventory = session.getInventoryHolder();
            if (openInventory != null) {
                InventoryUtils.sendJavaContainerClose(openInventory);
                InventoryUtils.closeInventory(session, openInventory, true);
            }

            if (session.hasFormOpen()) {
                session.closeForm();
            }


            InventoryTranslator<LecternContainer> translator = (InventoryTranslator<LecternContainer>) InventoryTranslator.inventoryTranslator(ContainerType.LECTERN);
            Objects.requireNonNull(translator, "could not find lectern inventory translator!");
            LecternContainer container = translator.createInventory(session, "", FAKE_LECTERN_WINDOW_ID, ContainerType.LECTERN);
            container.setVirtualLecternBook(stack, session);
            InventoryUtils.openInventory(new InventoryHolder<>(session, container, translator));
        }
    }
}
