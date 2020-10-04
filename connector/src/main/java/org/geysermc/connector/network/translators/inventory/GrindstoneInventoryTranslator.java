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

import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.inventory.InventorySource;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.updater.CursorInventoryUpdater;

import java.util.List;
import java.util.stream.Collectors;

public class GrindstoneInventoryTranslator extends BlockInventoryTranslator {

    public GrindstoneInventoryTranslator() {
        super(3, "minecraft:grindstone[face=floor,facing=north]", ContainerType.GRINDSTONE, new CursorInventoryUpdater());
    }

    @Override
    public int bedrockSlotToJava(InventoryActionData action) {
        final int slot = super.bedrockSlotToJava(action);
        if (action.getSource().getContainerId() == ContainerId.UI) {
            switch (slot) {
                case 16:
                    return 0;
                case 17:
                    return 1;
                default:
                    return slot;
            }
        }
        if (action.getSource().getContainerId() == ContainerId.ANVIL_MATERIAL) {
            return 2;
        }
        return slot;
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        switch (slot) {
            case 0:
                return 16;
            case 1:
                return 17;
            case 2:
                return 50;
        }
        return super.javaSlotToBedrock(slot);
    }

    @Override
    public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 2)
            return SlotType.OUTPUT;
        return SlotType.NORMAL;
    }

    @Override
    public void translateActions(GeyserSession session, Inventory inventory, List<InventoryActionData> actions) {
        InventoryActionData anvilResult = null;
        for (InventoryActionData action : actions) {
            if (action.getSource().getContainerId() == ContainerId.ANVIL_MATERIAL) {
                anvilResult = action;
            }
        }
        if (anvilResult != null) {
            //Strip unnecessary actions
            List<InventoryActionData> strippedActions = actions.stream()
                    .filter(action -> action.getSource().getContainerId() == ContainerId.ANVIL_MATERIAL
                            || (action.getSource().getType() == InventorySource.Type.CONTAINER
                            && !(action.getSource().getContainerId() == ContainerId.UI && action.getSlot() != 0)))
                    .collect(Collectors.toList());
            super.translateActions(session, inventory, strippedActions);
            return;
        }

        super.translateActions(session, inventory, actions);
    }

}
