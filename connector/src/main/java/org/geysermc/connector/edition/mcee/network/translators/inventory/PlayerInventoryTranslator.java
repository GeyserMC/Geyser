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

package org.geysermc.connector.edition.mcee.network.translators.inventory;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientCreativeInventoryActionPacket;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.inventory.InventorySource;
import lombok.ToString;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.utils.InventoryUtils;

import java.util.List;

@ToString
public class PlayerInventoryTranslator extends org.geysermc.connector.network.translators.inventory.PlayerInventoryTranslator {

    @Override
    public int bedrockSlotToJava(InventoryActionData action) {
        int slotnum = action.getSlot();
        switch (action.getSource().getContainerId()) {
            case ContainerId.CRAFTING_ADD_INGREDIENT:
            case  ContainerId.DROP_CONTENTS:
                return slotnum+1;
        }
        return super.bedrockSlotToJava(action);
    }

    @Override
    public void translateActions(GeyserSession session, Inventory inventory, List<InventoryActionData> actions) {
        if (session.getGameMode() == GameMode.CREATIVE) {
            //crafting grid is not visible in creative mode in java edition
            for (InventoryActionData action : actions) {
                if (action.getSource().getContainerId() == ContainerId.CRAFTING_ADD_INGREDIENT && (action.getSlot() >= 0 && 5 >= action.getSlot())) {
                    updateInventory(session, inventory);
                    InventoryUtils.updateCursor(session);
                    return;
                }
            }

            ItemStack javaItem;
            for (InventoryActionData action : actions) {
                switch (action.getSource().getContainerId()) {
                    case ContainerId.INVENTORY:
                    case ContainerId.ARMOR:
                    case ContainerId.OFFHAND:
                        int javaSlot = bedrockSlotToJava(action);
                        if (action.getToItem().getId() == 0) {
                            javaItem = new ItemStack(-1, 0, null);
                        } else {
                            javaItem = ItemTranslator.translateToJava(action.getToItem());
                        }
                        ClientCreativeInventoryActionPacket creativePacket = new ClientCreativeInventoryActionPacket(javaSlot, javaItem);
                        session.sendDownstreamPacket(creativePacket);
                        inventory.setItem(javaSlot, javaItem);
                        break;
                    case ContainerId.UI:
                        if (action.getSlot() == 0) {
                            session.getInventory().setCursor(ItemTranslator.translateToJava(action.getToItem()));
                        }
                        break;
                    case ContainerId.NONE:
                        if (action.getSource().getType() == InventorySource.Type.WORLD_INTERACTION
                                && action.getSource().getFlag() == InventorySource.Flag.DROP_ITEM) {
                            javaItem = ItemTranslator.translateToJava(action.getToItem());
                            ClientCreativeInventoryActionPacket creativeDropPacket = new ClientCreativeInventoryActionPacket(-1, javaItem);
                            session.sendDownstreamPacket(creativeDropPacket);
                        }
                        break;
                }
            }
            return;
        }

        // Remove Useless Packet
        if (actions.stream().anyMatch(a -> a.getSource().getContainerId() == ContainerId.CRAFTING_USE_INGREDIENT)) {
            return;
        }

        super.translateActions(session, inventory, actions);
    }
}
