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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.window.DropItemParam;
import com.github.steveice10.mc.protocol.data.game.window.WindowAction;
import com.github.steveice10.mc.protocol.data.game.world.block.BlockFace;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Send a Drop packet to the Downstream server
 */
@Getter
@ToString
@AllArgsConstructor
public class Drop extends ConfirmAction {

    private final Type dropType;
    private final int javaSlot;

    @Override
    public void execute() {
        super.execute();

        switch (dropType) {
            case DROP_ITEM:
            case DROP_STACK:
                ClientWindowActionPacket dropPacket = new ClientWindowActionPacket(
                        transaction.getInventory().getId(),
                        id,
                        javaSlot,
                        null,
                        WindowAction.DROP_ITEM,
                        dropType == Type.DROP_ITEM ? DropItemParam.DROP_FROM_SELECTED : DropItemParam.DROP_SELECTED_STACK
                );
                transaction.getSession().sendDownstreamPacket(dropPacket);
                break;
            case DROP_ITEM_HOTBAR:
            case DROP_STACK_HOTBAR:
                ClientPlayerActionPacket actionPacket = new ClientPlayerActionPacket(
                        dropType == Type.DROP_ITEM_HOTBAR ? PlayerAction.DROP_ITEM : PlayerAction.DROP_ITEM_STACK,
                        new Position(0, 0, 0),
                        BlockFace.DOWN
                );
                transaction.getSession().sendDownstreamPacket(actionPacket);
                break;
        }

        transaction.next();
    }

    public enum Type {
        DROP_ITEM,
        DROP_STACK,
        DROP_ITEM_HOTBAR,
        DROP_STACK_HOTBAR
    }
}
