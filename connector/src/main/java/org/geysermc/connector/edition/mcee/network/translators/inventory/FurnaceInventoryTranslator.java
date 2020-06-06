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

import com.github.steveice10.mc.protocol.data.game.window.WindowType;
import com.nukkitx.protocol.bedrock.data.ContainerId;
import com.nukkitx.protocol.bedrock.data.ContainerType;
import com.nukkitx.protocol.bedrock.data.InventoryActionData;
import com.nukkitx.protocol.bedrock.packet.ContainerSetDataPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.edition.mcee.network.translators.inventory.action.InventoryActionDataTranslator;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.BlockInventoryTranslator;
import org.geysermc.connector.network.translators.inventory.SlotType;
import org.geysermc.connector.network.translators.inventory.updater.ContainerInventoryUpdater;

import java.util.ArrayList;
import java.util.List;

public class FurnaceInventoryTranslator extends BlockInventoryTranslator {

    private final InventoryActionDataTranslator actionTranslator;

    public FurnaceInventoryTranslator(InventoryActionDataTranslator actionTranslator) {
        super(3, "minecraft:furnace[facing=north,lit=false]", ContainerType.FURNACE, new ContainerInventoryUpdater());

        this.actionTranslator = actionTranslator;
    }

    @Override
    public void updateProperty(GeyserSession session, Inventory inventory, int key, int value) {
        ContainerSetDataPacket dataPacket = new ContainerSetDataPacket();
        dataPacket.setWindowId((byte) inventory.getId());
        switch (key) {
            case 0:
                dataPacket.setProperty(ContainerSetDataPacket.FURNACE_LIT_TIME);
                break;
            case 1:
                dataPacket.setProperty(ContainerSetDataPacket.FURNACE_LIT_DURATION);
                break;
            case 2:
                dataPacket.setProperty(ContainerSetDataPacket.FURNACE_TICK_COUNT);
                if (inventory.getWindowType() == WindowType.BLAST_FURNACE || inventory.getWindowType() == WindowType.SMOKER) {
                    value *= 2;
                }
                break;
            default:
                return;
        }
        dataPacket.setValue(value);
        session.sendUpstreamPacket(dataPacket);
    }

    @Override
    public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 2)
            return SlotType.FURNACE_OUTPUT;
        return SlotType.NORMAL;
    }

    @Override
    public void translateActions(GeyserSession session, Inventory inventory, List<InventoryActionData> actions) {
        List<InventoryActionData> fromActions = new ArrayList<>();
        List<InventoryActionData> toActions = new ArrayList<>();

        for(InventoryActionData action : actions) {
            switch(action.getSource().getType()) {
                case UNTRACKED_INTERACTION_UI:
                case NON_IMPLEMENTED_TODO:
                case CONTAINER:
                case WORLD_INTERACTION:
                    switch(action.getSource().getContainerId()) {
                        // Container, Inventory, Crafting Input, Crafting Output
                        case ContainerId.CURSOR:
                        case ContainerId.INVENTORY:
                        case ContainerId.CRAFTING_ADD_INGREDIENT:
                        case ContainerId.CRAFTING_RESULT:
                        case ContainerId.NONE:
                        case ContainerId.DROP_CONTENTS:
                        case ContainerId.FIRST:
                        case 2: // FURNACE_INPUT
                        case 3: // FURNACE_OUTPUT
                        case 6: // FURNACE_???
                            if (action.getFromItem().getCount() > action.getToItem().getCount()) {
                                fromActions.add(action);
                            } else {
                                toActions.add(action);
                            }
                            break;

                        // We are not interested in these
                        case ContainerId.CRAFTING_USE_INGREDIENT:
                            return;
                        default:
                            GeyserConnector.getInstance().getLogger().warning("Unknown ContainerID: " + action.getSource().getContainerId());
                    }
                    break;
                default:
                    GeyserConnector.getInstance().getLogger().warning("Unknown Source: " + action.getSource().getType());
            }
        }

        if (!fromActions.isEmpty() && !toActions.isEmpty()) {
            actionTranslator.execute(this, session, inventory, fromActions, toActions);
        }
    }
}
