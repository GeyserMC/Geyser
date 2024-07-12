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

import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.MerchantContainer;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.protocol.java.inventory.JavaMerchantOffersTranslator;
import org.geysermc.geyser.util.InventoryUtils;

@Translator(packet = ContainerClosePacket.class)
public class BedrockContainerCloseTranslator extends PacketTranslator<ContainerClosePacket> {

    @Override
    public void translate(GeyserSession session, ContainerClosePacket packet) {
        byte bedrockId = packet.getId();

        //Client wants close confirmation
        session.sendUpstreamPacket(packet);
        session.setClosingInventory(false);

        if (bedrockId == -1 && session.getOpenInventory() instanceof MerchantContainer) {
            // 1.16.200 - window ID is always -1 sent from Bedrock
            bedrockId = (byte) session.getOpenInventory().getBedrockId();
        }

        Inventory openInventory = session.getOpenInventory();
        if (openInventory != null) {
            if (bedrockId == openInventory.getBedrockId()) {
                ServerboundContainerClosePacket closeWindowPacket = new ServerboundContainerClosePacket(openInventory.getJavaId());
                session.sendDownstreamGamePacket(closeWindowPacket);
                InventoryUtils.closeInventory(session, openInventory.getJavaId(), false);
            } else if (openInventory.isPending()) {
                InventoryUtils.displayInventory(session, openInventory);
                openInventory.setPending(false);

                if (openInventory instanceof MerchantContainer merchantContainer && merchantContainer.getPendingOffersPacket() != null) {
                    JavaMerchantOffersTranslator.openMerchant(session, merchantContainer.getPendingOffersPacket(), merchantContainer);
                }
            }
        }
    }
}
