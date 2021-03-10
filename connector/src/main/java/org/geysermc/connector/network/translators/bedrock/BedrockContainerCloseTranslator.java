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

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientCloseWindowPacket;
import com.nukkitx.protocol.bedrock.packet.ContainerClosePacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.inventory.MerchantContainer;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.utils.InventoryUtils;

@Translator(packet = ContainerClosePacket.class)
public class BedrockContainerCloseTranslator extends PacketTranslator<ContainerClosePacket> {

    @Override
    public void translate(ContainerClosePacket packet, GeyserSession session) {
        session.addInventoryTask(() -> {
            byte windowId = packet.getId();

            //Client wants close confirmation
            session.sendUpstreamPacket(packet);
            session.setClosingInventory(false);

            if (windowId == -1 && session.getOpenInventory() instanceof MerchantContainer) {
                // 1.16.200 - window ID is always -1 sent from Bedrock
                windowId = (byte) session.getOpenInventory().getId();
            }

            Inventory openInventory = session.getOpenInventory();
            if (openInventory != null) {
                if (windowId == openInventory.getId()) {
                    ClientCloseWindowPacket closeWindowPacket = new ClientCloseWindowPacket(windowId);
                    session.sendDownstreamPacket(closeWindowPacket);
                    InventoryUtils.closeInventory(session, windowId, false);
                } else if (openInventory.isPending()) {
                    InventoryUtils.displayInventory(session, openInventory);
                    openInventory.setPending(false);
                }
            }
        });
    }
}
