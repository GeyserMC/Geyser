/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerType;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerButtonClickPacket;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemStackRequest;
import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.CraftResultsDeprecatedStackRequestActionData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.StackRequestActionData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.StackRequestActionType;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import it.unimi.dsi.fastutil.ints.IntList;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.inventory.StonecutterContainer;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.inventory.BedrockContainerSlot;
import org.geysermc.geyser.inventory.SlotType;
import org.geysermc.geyser.inventory.updater.UIInventoryUpdater;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;

public class StonecutterInventoryTranslator extends AbstractBlockInventoryTranslator {
    public StonecutterInventoryTranslator() {
        super(2, "minecraft:stonecutter[facing=north]", com.nukkitx.protocol.bedrock.data.inventory.ContainerType.STONECUTTER, UIInventoryUpdater.INSTANCE);
    }

    @Override
    public boolean shouldHandleRequestFirst(StackRequestActionData action, Inventory inventory) {
        return action.getType() == StackRequestActionType.CRAFT_NON_IMPLEMENTED_DEPRECATED;
    }

    @Override
    public ItemStackResponsePacket.Response translateSpecialRequest(GeyserSession session, Inventory inventory, ItemStackRequest request) {
        // TODO: Also surely to change in the future
        StackRequestActionData data = request.getActions()[1];
        if (!(data instanceof CraftResultsDeprecatedStackRequestActionData craftData)) {
            return rejectRequest(request);
        }

        StonecutterContainer container = (StonecutterContainer) inventory;
        // Get the ID of the item we are cutting
        int id = inventory.getItem(0).getJavaId();
        // Look up all possible options of cutting from this ID
        IntList results = session.getStonecutterRecipes().get(id);
        if (results == null) {
            return rejectRequest(request);
        }

        ItemStack javaOutput = ItemTranslator.translateToJava(craftData.getResultItems()[0], session.getItemMappings());
        int button = results.indexOf(javaOutput.getId());
        // If we've already pressed the button with this item, no need to press it again!
        if (container.getStonecutterButton() != button) {
            // Getting the index of the item in the Java stonecutter list
            ServerboundContainerButtonClickPacket packet = new ServerboundContainerButtonClickPacket(inventory.getId(), button);
            session.sendDownstreamPacket(packet);
            container.setStonecutterButton(button);
            if (inventory.getItem(1).getJavaId() != javaOutput.getId()) {
                // We don't know there is an output here, so we tell ourselves that there is
                inventory.setItem(1, GeyserItemStack.from(javaOutput), session);
            }
        }

        return translateRequest(session, inventory, request);
    }

    @Override
    public int bedrockSlotToJava(StackRequestSlotInfoData slotInfoData) {
        return switch (slotInfoData.getContainer()) {
            case STONECUTTER_INPUT -> 0;
            case STONECUTTER_RESULT, CREATIVE_OUTPUT -> 1;
            default -> super.bedrockSlotToJava(slotInfoData);
        };
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        if (slot == 0) {
            return new BedrockContainerSlot(ContainerSlotType.STONECUTTER_INPUT, 3);
        }
        if (slot == 1) {
            return new BedrockContainerSlot(ContainerSlotType.STONECUTTER_RESULT, 50);
        }
        return super.javaSlotToBedrockContainer(slot);
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        if (slot == 0) {
            return 3;
        }
        if (slot == 1) {
            return 50;
        }
        return super.javaSlotToBedrock(slot);
    }

    @Override
    public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 1) {
            return SlotType.OUTPUT;
        }
        return super.getSlotType(javaSlot);
    }

    @Override
    public Inventory createInventory(String name, int windowId, ContainerType containerType, PlayerInventory playerInventory) {
        return new StonecutterContainer(name, windowId, this.size, containerType, playerInventory);
    }
}
