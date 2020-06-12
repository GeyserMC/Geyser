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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.window.ClickItemParam;
import com.github.steveice10.mc.protocol.data.game.window.ShiftClickItemParam;
import com.github.steveice10.mc.protocol.data.game.window.WindowAction;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.utils.InventoryUtils;

/**
 * Send a Left, Right or Shift+Click to the Downstream Server
 */
@Getter
@ToString
public class Click extends ConfirmAction {

    private final Type clickType;
    private final int javaSlot;

    @Setter
    private boolean refresh;

    public Click(Type clickType, int javaSlot, boolean refresh) {
        this.clickType = clickType;
        this.javaSlot = javaSlot;
        this.refresh = refresh;
    }

    public Click(Type clickType, int javaSlot) {
        this(clickType, javaSlot, false);
    }

    @Override
    public void execute() {
        super.execute();
        ItemStack clickedItem = transaction.getInventory().getItem(javaSlot);
        PlayerInventory playerInventory = transaction.getSession().getInventory();
        final ItemStack cursorItem = playerInventory.getCursor();

        ClientWindowActionPacket clickPacket;

        switch (clickType) {
            case LEFT:
                clickPacket = new ClientWindowActionPacket(transaction.getInventory().getId(),
                        id, javaSlot, refresh ? InventoryUtils.REFRESH_ITEM : clickedItem,
                        WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK);

                if (!InventoryUtils.canStack(cursorItem, clickedItem)) {
                    playerInventory.setCursor(clickedItem);
                    transaction.getInventory().setItem(javaSlot, cursorItem);
                } else {
                    playerInventory.setCursor(null);
                    transaction.getInventory().setItem(javaSlot, new ItemStack(clickedItem.getId(),
                            clickedItem.getAmount() + cursorItem.getAmount(), clickedItem.getNbt()));
                }
                transaction.getSession().sendDownstreamPacket(clickPacket);
                break;

            case RIGHT:
                clickPacket = new ClientWindowActionPacket(transaction.getInventory().getId(),
                        id, javaSlot, refresh ? InventoryUtils.REFRESH_ITEM : clickedItem,
                        WindowAction.CLICK_ITEM, ClickItemParam.RIGHT_CLICK);

                if (cursorItem == null && clickedItem != null) {
                    ItemStack halfItem = new ItemStack(clickedItem.getId(),
                            clickedItem.getAmount() / 2, clickedItem.getNbt());
                    transaction.getInventory().setItem(javaSlot, halfItem);
                    playerInventory.setCursor(new ItemStack(clickedItem.getId(),
                            clickedItem.getAmount() - halfItem.getAmount(), clickedItem.getNbt()));
                } else if (cursorItem != null && clickedItem == null) {
                    playerInventory.setCursor(new ItemStack(cursorItem.getId(),
                            cursorItem.getAmount() - 1, cursorItem.getNbt()));
                    transaction.getInventory().setItem(javaSlot, new ItemStack(cursorItem.getId(),
                            1, cursorItem.getNbt()));
                } else if (InventoryUtils.canStack(cursorItem, clickedItem)) {
                    playerInventory.setCursor(new ItemStack(cursorItem.getId(),
                            cursorItem.getAmount() - 1, cursorItem.getNbt()));
                    transaction.getInventory().setItem(javaSlot, new ItemStack(clickedItem.getId(),
                            clickedItem.getAmount() + 1, clickedItem.getNbt()));
                }
                transaction.getSession().sendDownstreamPacket(clickPacket);
                break;

            case SHIFT_CLICK:
                clickedItem = transaction.getInventory().getItem(javaSlot);

                ClientWindowActionPacket shiftClickPacket = new ClientWindowActionPacket(
                        transaction.getInventory().getId(),
                        id,
                        javaSlot, clickedItem,
                        WindowAction.SHIFT_CLICK_ITEM,
                        ShiftClickItemParam.LEFT_CLICK
                );
                transaction.getSession().sendDownstreamPacket(shiftClickPacket);
                break;
        }
    }

    public enum Type {
        LEFT,
        RIGHT,
        SHIFT_CLICK
    }
}
