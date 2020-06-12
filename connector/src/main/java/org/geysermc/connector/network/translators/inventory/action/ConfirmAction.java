/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.network.translators.inventory.action;

import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientConfirmTransactionPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.concurrent.TimeUnit;

public abstract class ConfirmAction extends BaseAction {

    protected int id;

    @Override
    public void execute() {
        id = transaction.getInventory().getTransactionId().getAndIncrement();
    }

    /**
     * Called when we received a server confirmation packet.
     */
    public void confirm(int id, boolean accepted) {
        if (id != this.id) {
            GeyserConnector.getInstance().getLogger().warning("Out of sequence Confirmation Packet with id: " + id);
            return;
        }

        if (!accepted) {
            // Downstream disagrees with what we think the slot is so we will update and accept it
            GeyserConnector.getInstance().getGeneralThreadPool().schedule(() -> {
                ClientConfirmTransactionPacket confirmPacket = new ClientConfirmTransactionPacket(transaction.getInventory().getId(),
                        id, true);
                transaction.getSession().sendDownstreamPacket(confirmPacket);

                transaction.next();
            }, 200, TimeUnit.MILLISECONDS);
            return;
        }

        transaction.next();
    }

}
