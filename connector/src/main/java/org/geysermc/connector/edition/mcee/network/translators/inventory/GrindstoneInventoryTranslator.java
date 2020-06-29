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

package org.geysermc.connector.edition.mcee.network.translators.inventory;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerType;
import com.nukkitx.protocol.bedrock.data.inventory.InventoryActionData;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.BlockInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.updater.ContainerInventoryUpdater;

import java.util.List;
import java.util.stream.Collectors;

public class GrindstoneInventoryTranslator extends BlockInventoryTranslator {

    public GrindstoneInventoryTranslator() {
        super(3, "minecraft:grindstone[face=floor,facing=north]", ContainerType.GRINDSTONE, new ContainerInventoryUpdater());
    }

    @Override
    public int bedrockSlotToJava(InventoryActionData action) {
        int slotnum = action.getSlot();
        switch (action.getSource().getContainerId()) {
            case ContainerId.CONTAINER_INPUT:
            case ContainerId.DROP_CONTENTS:
            case ContainerId.ANVIL_MATERIAL:
                return slotnum;
            case ContainerId.ANVIL_RESULT:
                return 2;
        }
        return super.bedrockSlotToJava(action);
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        if (slot < size) {
            return slot;
        }
        return super.javaSlotToBedrock(slot);
    }

    @Override
    public boolean isOutput(InventoryActionData action) {
        return action.getSource().getContainerId() == ContainerId.ANVIL_RESULT;
    }

    @Override
    public void translateActions(GeyserSession session, Inventory inventory, List<InventoryActionData> actions) {
        // If we have an anvil_result then we filter out anvil_material and container_input
        if (actions.stream().anyMatch(this::isOutput)) {
            actions = actions.stream()
                    .filter(a -> a.getSource().getContainerId() != ContainerId.ANVIL_MATERIAL)
                    .filter(a -> a.getSource().getContainerId() != ContainerId.CONTAINER_INPUT)
                    .collect(Collectors.toList());
        }

        super.translateActions(session, inventory, actions);
    }
}
