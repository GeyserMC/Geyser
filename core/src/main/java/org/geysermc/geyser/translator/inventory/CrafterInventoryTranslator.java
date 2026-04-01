/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.inventory;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.geysermc.geyser.inventory.*;
import org.geysermc.geyser.inventory.updater.CrafterInventoryUpdater;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.BlockEntityUtils;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;


public class CrafterInventoryTranslator extends AbstractBlockInventoryTranslator<CrafterContainer> {

    public static final int JAVA_RESULT_SLOT = 45;
    public static final int BEDROCK_RESULT_SLOT = 50;
    public static final int GRID_SIZE = 9;

    
    private static final int SLOT_ENABLED = 0; 
    private static final int TRIGGERED_KEY = 9; 
    private static final int TRIGGERED = 1; 

    public CrafterInventoryTranslator() {
        super(10, Blocks.CRAFTER, org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.CRAFTER, CrafterInventoryUpdater.INSTANCE);
    }

    @Override
    public void updateProperty(GeyserSession session, CrafterContainer container, int key, int value) {
        
        if (key == TRIGGERED_KEY) {
            container.setTriggered(value == TRIGGERED);
        } else {
            
            container.setSlot(key, value == SLOT_ENABLED);
        }

        
        
        updateBlockEntity(session, container);
    }

    @Override
    public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        int slot = slotInfoData.getSlot();
        switch (slotInfoData.getContainerName().getContainer()) {
            case HOTBAR_AND_INVENTORY, HOTBAR, INVENTORY -> {
                //hotbar
                if (slot >= 9) {
                    return slot + GRID_SIZE - 9;
                } else {
                    return slot + GRID_SIZE + 27;
                }
            }
        }
        return slot;
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        if (slot == JAVA_RESULT_SLOT) {
            return BEDROCK_RESULT_SLOT;
        }

        
        if (slot < GRID_SIZE) {
            return slot;
        }

        
        final int tmp = slot - GRID_SIZE;
        if (tmp < 27) {
            return tmp + 9;
        } else {
            return tmp - 27;
        }
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int javaSlot, CrafterContainer container) {
        if (javaSlot == JAVA_RESULT_SLOT) {
            return new BedrockContainerSlot(ContainerSlotType.CRAFTER_BLOCK_CONTAINER, BEDROCK_RESULT_SLOT);
        }

        
        if (javaSlot < GRID_SIZE) {
            return new BedrockContainerSlot(ContainerSlotType.LEVEL_ENTITY, javaSlot);
        }

        
        final int tmp = javaSlot - GRID_SIZE;
        if (tmp < 27) {
            return new BedrockContainerSlot(ContainerSlotType.INVENTORY, tmp + 9);
        } else {
            return new BedrockContainerSlot(ContainerSlotType.HOTBAR, tmp - 27);
        }
    }

    @Override
    public SlotType getSlotType(int javaSlot) {
        if (javaSlot == JAVA_RESULT_SLOT) {
            return SlotType.OUTPUT;
        }
        return SlotType.NORMAL;
    }

    @Override
    public CrafterContainer createInventory(GeyserSession session, String name, int windowId, ContainerType containerType) {
        
        return new CrafterContainer(session, name, windowId, this.size, containerType);
    }

    private static void updateBlockEntity(GeyserSession session, CrafterContainer container) {
        /*
        Here is an example of the tag sent by BDS 1.20.50.24
        It doesn't include the position or the block entity ID in the tag, for whatever reason.

        CLIENT BOUND BlockEntityDataPacket(blockPosition=(8, 110, 45), data={
            "crafting_ticks_remaining": 0i,
            "disabled_slots": 511s
        })
         */

        NbtMapBuilder tag = NbtMap.builder();
        
        tag.putInt("crafting_ticks_remaining", container.isTriggered() ? 10_000 : 0);
        tag.putShort("disabled_slots", container.getDisabledSlotsMask());

        BlockEntityUtils.updateBlockEntity(session, tag.build(), container.getHolderPosition());
    }

    @Override
    public org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType closeContainerType(CrafterContainer container) {
        return null;
    }
}
