/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.inventory.action;

import com.github.steveice10.mc.protocol.data.game.window.ClickItemParam;
import com.github.steveice10.mc.protocol.data.game.window.WindowAction;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientConfirmTransactionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import lombok.Getter;
import lombok.ToString;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.concurrent.TimeUnit;

/**
 * Send an invalid click to refresh all slots
 *
 * We will filter out repeat refreshes and ensre our executation happens last in the plan
 */
@Getter
@ToString
public class Refresh extends ConfirmAction {

    private final int weight = 10;

    @Override
    public void execute() {
        super.execute();

        ClientWindowActionPacket clickPacket = new ClientWindowActionPacket(transaction.getInventory().getId(),
                id, -1, InventoryUtils.REFRESH_ITEM,
                WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK);

        transaction.getSession().sendDownstreamPacket(clickPacket);
    }


    @Override
    public void confirm(int id, boolean accepted) {
        if (id != this.id) {
            GeyserConnector.getInstance().getLogger().warning("Out of sequence Confirmation Packet with id: " + id);
            return;
        }

        // We always reject the packet, but we will wait a little bit for a resync
        GeyserConnector.getInstance().getGeneralThreadPool().schedule(() -> {
            InventoryUtils.updateCursor(transaction.getSession());
            transaction.getTranslator().updateInventory(transaction.getSession(), transaction.getInventory());


            ClientConfirmTransactionPacket confirmPacket = new ClientConfirmTransactionPacket(transaction.getInventory().getId(),
                    id, false);
            transaction.getSession().sendDownstreamPacket(confirmPacket);

            transaction.next();
        }, 200, TimeUnit.MILLISECONDS);
    }
}
