/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.inventory;

import com.nukkitx.protocol.bedrock.data.ContainerType;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.*;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.TranslatorsInit;

public class SingleChestInventoryTranslator extends BlockInventoryTranslator {
    public SingleChestInventoryTranslator(int size) {
        super(size, "minecraft:chest[facing=north,type=single,waterlogged=false]", ContainerType.CONTAINER);
    }

    @Override
    public void updateInventory(GeyserSession session, Inventory inventory) {
        //need to pad empty slots for 1x9 and 2x9
        ItemData[] bedrockItems = new ItemData[27];
        for (int i = 0; i < bedrockItems.length; i++) {
            if (i <= this.size) {
                bedrockItems[i] = TranslatorsInit.getItemTranslator().translateToBedrock(inventory.getItem(i));
            } else {
                bedrockItems[i] = ItemData.AIR;
            }
        }
        InventoryContentPacket contentPacket = new InventoryContentPacket();
        contentPacket.setContainerId(inventory.getId());
        contentPacket.setContents(bedrockItems);
        session.getUpstream().sendPacket(contentPacket);

        Inventory playerInventory = session.getInventory();
        for (int i = 0; i < 36; i++) {
            playerInventory.setItem(i + 9, inventory.getItem(i + this.size));
        }
        TranslatorsInit.getInventoryTranslators().get(playerInventory.getWindowType()).updateInventory(session, playerInventory);
    }
}
