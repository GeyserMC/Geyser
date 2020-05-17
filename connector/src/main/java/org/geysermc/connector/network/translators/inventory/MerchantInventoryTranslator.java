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

package org.geysermc.connector.network.translators.inventory;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.window.ClickItemParam;
import com.github.steveice10.mc.protocol.data.game.window.VillagerTrade;
import com.github.steveice10.mc.protocol.data.game.window.WindowAction;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientSelectTradePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.data.InventoryActionData;
import org.geysermc.connector.entity.living.merchant.VillagerEntity;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.Translators;
import org.geysermc.connector.network.translators.inventory.updater.CursorInventoryUpdater;
import org.geysermc.connector.network.translators.inventory.updater.InventoryUpdater;

import java.util.List;

public class MerchantInventoryTranslator extends BaseInventoryTranslator {

    private final InventoryUpdater updater;

    public MerchantInventoryTranslator() {
        super(3);
        this.updater = new CursorInventoryUpdater();
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        switch (slot) {
            case 0:
                return 4;
            case 1:
                return 5;
            case 2:
                return 50;
        }
        return super.javaSlotToBedrock(slot);
    }

    @Override
    public int bedrockSlotToJava(InventoryActionData action) {
        if (action.getSource().getContainerId() == ContainerId.CURSOR) {
            switch (action.getSlot()) {
                case 4:
                    return 0;
                case 5:
                    return 1;
                case 50:
                    return 2;
            }
        }
        return super.bedrockSlotToJava(action);
    }

    @Override
    public void prepareInventory(GeyserSession session, Inventory inventory) {

    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {

    }

    @Override
    public void closeInventory(GeyserSession session, Inventory inventory) {
        session.setLastInteractedVillagerEid(-1);
        session.setFirstTradeSlot(null);
        session.setSecondTradeSlot(null);
    }

    @Override
    public void updateInventory(GeyserSession session, Inventory inventory) {
        updater.updateInventory(this, session, inventory);
    }

    @Override
    public void updateSlot(GeyserSession session, Inventory inventory, int slot) {
        updater.updateSlot(this, session, inventory, slot);
    }

    @Override
    public void translateActions(GeyserSession session, Inventory inventory, List<InventoryActionData> actions) {
        InventoryActionData result = null;

        VillagerEntity villager = (VillagerEntity) session.getEntityCache().getEntityByGeyserId(session.getLastInteractedVillagerEid());
        if (villager == null) {
            session.getConnector().getLogger().debug("Could not find villager with entity id: " + session.getLastInteractedVillagerEid());
            return;
        }

        // We need to store the trade slot data in the session itself as data
        // needs to persist beyond this translateActions method since the client
        // sends multiple packets for this
        for (InventoryActionData data : actions) {
            if (data.getSlot() == 4 && session.getFirstTradeSlot() == null && data.getSource().getContainerId() == ContainerId.CURSOR) {
                session.setFirstTradeSlot(Translators.getItemTranslator().translateToJava(session, data.getToItem()));
            }

            if (data.getSlot() == 5 && session.getSecondTradeSlot() == null && data.getToItem() != null && data.getSource().getContainerId() == ContainerId.CURSOR) {
                session.setSecondTradeSlot(Translators.getItemTranslator().translateToJava(session, data.getToItem()));
            }
            if (data.getSlot() == 50 && result == null) {
                result = data;
            }
        }

        if (result == null || session.getFirstTradeSlot() == null) {
            super.translateActions(session, inventory, actions);
            return;
        }

        ItemStack resultSlot = Translators.getItemTranslator().translateToJava(session, result.getToItem());
        for (int i = 0; i < villager.getVillagerTrades().length; i++) {
            VillagerTrade trade = villager.getVillagerTrades()[i];
            if (!Translators.getItemTranslator().equals(session.getFirstTradeSlot(), trade.getFirstInput(), true, true, false) || !Translators.getItemTranslator().equals(resultSlot, trade.getOutput(), true, false, false)) {
                continue;
            }

            if (session.getSecondTradeSlot() != null && trade.getSecondInput() != null && !Translators.getItemTranslator().equals(session.getSecondTradeSlot(), trade.getSecondInput(), true, false, false)) {
                continue;
            }

            ClientSelectTradePacket selectTradePacket = new ClientSelectTradePacket(i);
            session.sendDownstreamPacket(selectTradePacket);

            ClientWindowActionPacket tradeAction = new ClientWindowActionPacket(
                    inventory.getId(),
                    inventory.getTransactionId().getAndIncrement(),
                    this.bedrockSlotToJava(result),
                    null,
                    WindowAction.CLICK_ITEM,
                    ClickItemParam.LEFT_CLICK
            );
            session.sendDownstreamPacket(tradeAction);
            break;

        }

        super.translateActions(session, inventory, actions);
    }
}
