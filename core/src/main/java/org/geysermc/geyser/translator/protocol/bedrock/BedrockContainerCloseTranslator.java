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

package org.geysermc.geyser.translator.protocol.bedrock;

import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.MerchantContainer;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.MerchantInventoryTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.protocol.java.inventory.JavaMerchantOffersTranslator;
import org.geysermc.geyser.util.InventoryUtils;

import java.util.concurrent.TimeUnit;

import static org.geysermc.geyser.util.InventoryUtils.MAGIC_VIRTUAL_INVENTORY_HACK;

@Translator(packet = ContainerClosePacket.class)
public class BedrockContainerCloseTranslator extends PacketTranslator<ContainerClosePacket> {

    @Override
    public void translate(GeyserSession session, ContainerClosePacket packet) {
        GeyserImpl.getInstance().getLogger().debug(session, packet.toString());
        byte bedrockId = packet.getId();

        //Client wants close confirmation
        session.sendUpstreamPacket(packet);
        session.setClosingInventory(false);

        // 1.21.70: Bedrock can reject opening inventories - in those cases it replies with -1
        Inventory openInventory = session.getOpenInventory();
        if (bedrockId == -1 && openInventory != null) {
            // 1.16.200 - window ID is always -1 sent from Bedrock for merchant containers
            if (openInventory.getTranslator() instanceof MerchantInventoryTranslator) {
                bedrockId = (byte) openInventory.getBedrockId();
            } else if (openInventory.getBedrockId() == session.getPendingOrCurrentBedrockInventoryId()) {
                // If virtual inventories are opened too quickly, they can be occasionally rejected
                // We just try and queue a new one.
                // Before making another attempt to re-open, let's make sure we actually need this inventory open.
                if (session.getContainerOpenAttempts() < 3) {
                    openInventory.setPending(true);

                    session.scheduleInEventLoop(() -> {
                        NetworkStackLatencyPacket latencyPacket = new NetworkStackLatencyPacket();
                        latencyPacket.setFromServer(true);
                        latencyPacket.setTimestamp(MAGIC_VIRTUAL_INVENTORY_HACK);
                        session.sendUpstreamPacket(latencyPacket);
                        GeyserImpl.getInstance().getLogger().debug(session, "Unable to open a virtual inventory, sent another latency packet!");
                    }, 100, TimeUnit.MILLISECONDS);
                    return;
                } else {
                    GeyserImpl.getInstance().getLogger().debug(session, "Exceeded 3 attempts to open a virtual inventory!");
                    GeyserImpl.getInstance().getLogger().debug(session, packet + " " + session.getOpenInventory().getClass().getSimpleName());
                }
            }
        }

        session.setPendingOrCurrentBedrockInventoryId(-1);
        session.setContainerOpenAttempts(0);
        closeCurrentOrOpenPending(session, bedrockId, openInventory);
    }

    private void closeCurrentOrOpenPending(GeyserSession session, byte bedrockId, Inventory openInventory) {
        if (openInventory != null) {
            if (bedrockId == openInventory.getBedrockId()) {
                InventoryUtils.sendJavaContainerClose(session, openInventory);
                InventoryUtils.closeInventory(session, openInventory.getJavaId(), false);
            } else if (openInventory.isPending()) {
                InventoryUtils.displayInventory(session, openInventory);

                if (openInventory instanceof MerchantContainer merchantContainer && merchantContainer.getPendingOffersPacket() != null) {
                    JavaMerchantOffersTranslator.openMerchant(session, merchantContainer.getPendingOffersPacket(), merchantContainer);
                }
            }
        }
    }
}
