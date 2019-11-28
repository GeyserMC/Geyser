package org.geysermc.connector.network.translators.inventory;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.data.ContainerType;
import com.nukkitx.protocol.bedrock.data.InventoryAction;
import com.nukkitx.protocol.bedrock.packet.ContainerOpenPacket;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;

public class CraftingTableInventoryTranslator extends ContainerInventoryTranslator {
    public CraftingTableInventoryTranslator() {
        super(10);
    }

    @Override
    public void prepareInventory(GeyserSession session, Inventory inventory) {

    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        ContainerOpenPacket containerOpenPacket = new ContainerOpenPacket();
        containerOpenPacket.setWindowId((byte) inventory.getId());
        containerOpenPacket.setType((byte) ContainerType.WORKBENCH.id());
        containerOpenPacket.setBlockPosition(inventory.getHolderPosition());
        containerOpenPacket.setUniqueEntityId(inventory.getHolderId());
        session.getUpstream().sendPacket(containerOpenPacket);
    }

    @Override
    public void closeInventory(GeyserSession session, Inventory inventory) {

    }

    @Override
    public int bedrockSlotToJava(InventoryAction action) {
        int slotnum = action.getSlot();
        if (action.getSource().getContainerId() == ContainerId.INVENTORY) {
            //hotbar
            if (slotnum >= 9) {
                return slotnum + this.size - 9;
            } else {
                return slotnum + this.size + 27;
            }
        } else {
            if (slotnum >= 32 && 42 >= slotnum) {
                return slotnum - 31;
            } else if (slotnum == 50) {
                return 0;
            }
            return slotnum;
        }
    }
}
