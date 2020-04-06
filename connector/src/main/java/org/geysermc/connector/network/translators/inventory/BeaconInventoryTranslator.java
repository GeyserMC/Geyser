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

package org.geysermc.connector.network.translators.inventory;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.Position;
import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.data.ContainerType;
import com.nukkitx.protocol.bedrock.data.InventoryActionData;
import com.nukkitx.protocol.bedrock.packet.ContainerSetDataPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.block.BlockTranslator;
import org.geysermc.connector.network.translators.inventory.updater.ContainerInventoryUpdater;
import org.geysermc.connector.utils.ChunkUtils;

import java.util.List;


public class BeaconInventoryTranslator extends BlockInventoryTranslator {
    public BeaconInventoryTranslator() {
        super(1, "minecraft:beacon", ContainerType.BEACON, new ContainerInventoryUpdater());
    }

    @Override
    public void translateActions(GeyserSession session, Inventory inventory, List<InventoryActionData> actions) {
        for (InventoryActionData action: actions) {
            System.out.println("Slot: " + action.getSlot());
            System.out.println(action.toString());
        }
        super.translateActions(session, inventory, actions);
    }

//    @Override
//    public int bedrockSlotToJava(InventoryActionData action) {
//        int slotnum = action.getSlot();
//        if (action.getSource().getContainerId() == ContainerId.INVENTORY) {
//            //hotbar
//            if (slotnum == 27) {
//                return 0;
//            } if (slotnum >= 9) {
//                return slotnum + this.size - 9;
//            } else {
//                return slotnum + this.size + 27;
//            }
//        }
//        return slotnum;
//    }
//
//    @Override
//    public int javaSlotToBedrock(int slot) {
//        if (slot >= this.size) {
//            final int tmp = slot - this.size;
//            if (slot == 0) {
//                return 27;
//            } if (tmp < 27) {
//                return tmp + 9;
//            } else {
//                return tmp - 27;
//            }
//        }
//        return slot;
//    }


    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        System.out.println(inventory.getHolderId());
        System.out.println(inventory.getHolderPosition());
        ChunkUtils.updateBlock(session, BlockTranslator.getJavaBlockState("minecraft:beacon"), new Position(156, 67, 103));
        super.openInventory(session, inventory);
    }

    @Override
    public void updateProperty(GeyserSession session, Inventory inventory, int key, int value) {
        ContainerSetDataPacket dataPacket = new ContainerSetDataPacket();
        dataPacket.setWindowId((byte) inventory.getId());
        // Key 0: Value means the level of the beacon
        System.out.println("Property is being updated!! ooo");
        System.out.println("Key: " + key);
        System.out.println("Value: " + value);
        switch (key) {
            case 0:
                dataPacket.setProperty(1);
                break;
            case 1:
                dataPacket.setProperty(0);
                break;
            default:
                System.out.println("New key! " + key);
                return;
        }
        dataPacket.setValue(value);
        session.getUpstream().sendPacket(dataPacket);

    }
}
