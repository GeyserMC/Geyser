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

/**
 * Translates the Crafter. Most important thing to know about this class is that
 * the result slot comes after the 3x3 grid AND the inventory. This means that the total size of the Crafter (10)
 * cannot be used to calculate the inventory slot indices. The Translator and the Updater must then
 * override any methods that use the size for such calculations
 */
public class CrafterInventoryTranslator extends AbstractBlockInventoryTranslator {

    public static final int JAVA_RESULT_SLOT = 45;
    public static final int BEDROCK_RESULT_SLOT = 50;
    public static final int GRID_SIZE = 9;

    // Properties
    private static final int SLOT_ENABLED = 0; // enabled slot value
    private static final int TRIGGERED_KEY = 9; // key of triggered state
    private static final int TRIGGERED = 1; // triggered value

    public CrafterInventoryTranslator() {
        super(10, Blocks.CRAFTER, org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.CRAFTER, CrafterInventoryUpdater.INSTANCE);
    }

    @Override
    public void updateProperty(GeyserSession session, Inventory inventory, int key, int value) {
        // the slot bits and triggered state are sent here rather than in a BlockEntityDataPacket. Yippee.
        CrafterContainer container = (CrafterContainer) inventory;

        if (key == TRIGGERED_KEY) {
            container.setTriggered(value == TRIGGERED);
        } else {
            // enabling and disabling slots of the 3x3 grid
            container.setSlot(key, value == SLOT_ENABLED);
        }

        // Unfortunately this will be called 10 times when a Crafter is opened
        // Kind of unavoidable because it must be invoked anytime an individual property is updated
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

        // grid slots 0-8
        if (slot < GRID_SIZE) {
            return slot;
        }

        // inventory and hotbar
        final int tmp = slot - GRID_SIZE;
        if (tmp < 27) {
            return tmp + 9;
        } else {
            return tmp - 27;
        }
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int javaSlot) {
        if (javaSlot == JAVA_RESULT_SLOT) {
            return new BedrockContainerSlot(ContainerSlotType.CRAFTER_BLOCK_CONTAINER, BEDROCK_RESULT_SLOT);
        }

        // grid slots 0-8
        if (javaSlot < GRID_SIZE) {
            return new BedrockContainerSlot(ContainerSlotType.LEVEL_ENTITY, javaSlot);
        }

        // inventory and hotbar
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
    public Inventory createInventory(String name, int windowId, ContainerType containerType, PlayerInventory playerInventory) {
        // Java sends the triggered and slot bits incrementally through properties, which we store here
        return new CrafterContainer(name, windowId, this.size, containerType, playerInventory);
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
        // just send some large amount since we don't know, and it'll be resent as 0 when java updates as not triggered
        tag.putInt("crafting_ticks_remaining", container.isTriggered() ? 10_000 : 0);
        tag.putShort("disabled_slots", container.getDisabledSlotsMask());

        BlockEntityUtils.updateBlockEntity(session, tag.build(), container.getHolderPosition());
    }
}
