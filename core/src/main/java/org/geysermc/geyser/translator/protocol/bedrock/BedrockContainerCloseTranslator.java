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

import java.util.concurrent.TimeUnit;
import org.cloudburstmc.protocol.bedrock.packet.ContainerClosePacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.InventoryHolder;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.MerchantInventoryTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.InventoryUtils;

@Translator(packet = ContainerClosePacket.class)
public class BedrockContainerCloseTranslator extends PacketTranslator<ContainerClosePacket> {

    @Override
    public void translate(GeyserSession session, ContainerClosePacket packet) {
        GeyserImpl.getInstance().getLogger().debug(session, packet.toString());
        byte bedrockId = packet.getId();

        //Client wants close confirmation
        session.sendUpstreamPacket(packet);
        session.setClosingInventory(false);

        
        InventoryHolder<? extends Inventory> holder = session.getInventoryHolder();
        if (bedrockId == -1 && holder != null) {
            
            if (holder.translator() instanceof MerchantInventoryTranslator) {
                bedrockId = (byte) holder.bedrockId();
            } else if (holder.bedrockId() == session.getPendingOrCurrentBedrockInventoryId()) {
                
                
                holder.inventory().setDisplayed(false);
                
                if (holder.containerOpenAttempts() < 7) {
                    holder.incrementContainerOpenAttempts();
                    holder.pending(true);

                    session.scheduleInEventLoop(() -> {
                        InventoryUtils.scheduleInventoryOpen(session);
                        GeyserImpl.getInstance().getLogger().debug(session, "Unable to open a virtual inventory, sent another latency packet!");
                    }, 150, TimeUnit.MILLISECONDS);
                    return;
                } else {
                    GeyserImpl.getInstance().getLogger().warning(session.bedrockUsername() + " exceeded 7 attempts to open a virtual inventory!");
                    GeyserImpl.getInstance().getLogger().debug(session, packet + " " + holder.inventory().getClass().getSimpleName());

                    
                    bedrockId = (byte) holder.bedrockId();
                }
            }
        }

        session.setPendingOrCurrentBedrockInventoryId(-1);

        if (holder != null) {
            
            if (bedrockId == holder.bedrockId()) {
                InventoryUtils.sendJavaContainerClose(holder);
                InventoryUtils.closeInventory(session, holder, false);
                return;
            }

            
            InventoryUtils.openPendingInventory(session);
        } else {
            
            
            session.getFormCache().resendAllForms();
        }
    }
}
