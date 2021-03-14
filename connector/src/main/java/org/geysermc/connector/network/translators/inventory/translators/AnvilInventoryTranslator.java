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

package org.geysermc.connector.network.translators.inventory.translators;

import com.github.steveice10.mc.protocol.data.game.window.WindowType;
import com.github.steveice10.mc.protocol.packet.ingame.client.window.ClientRenameItemPacket;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.protocol.bedrock.data.inventory.*;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.CraftResultsDeprecatedStackRequestActionData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.StackRequestActionData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.StackRequestActionType;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import org.geysermc.connector.inventory.AnvilContainer;
import org.geysermc.connector.inventory.GeyserItemStack;
import org.geysermc.connector.inventory.Inventory;
import org.geysermc.connector.inventory.PlayerInventory;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.inventory.BedrockContainerSlot;
import org.geysermc.connector.network.translators.inventory.updater.UIInventoryUpdater;
import org.geysermc.connector.network.translators.item.ItemTranslator;

public class AnvilInventoryTranslator extends AbstractBlockInventoryTranslator {
    public AnvilInventoryTranslator() {
        super(3, "minecraft:anvil[facing=north]", ContainerType.ANVIL, UIInventoryUpdater.INSTANCE,
                "minecraft:chipped_anvil", "minecraft:damaged_anvil");
    }

    /* 1.16.100 support start */
    @Override
    @Deprecated
    public boolean shouldHandleRequestFirst(StackRequestActionData action, Inventory inventory) {
        return action.getType() == StackRequestActionType.CRAFT_NON_IMPLEMENTED_DEPRECATED;
    }

    @Override
    @Deprecated
    public ItemStackResponsePacket.Response translateSpecialRequest(GeyserSession session, Inventory inventory, ItemStackRequest request) {
        if (!(request.getActions()[1] instanceof CraftResultsDeprecatedStackRequestActionData)) {
            // Just silently log an error
            session.getConnector().getLogger().debug("Something isn't quite right with taking an item out of an anvil.");
            return translateRequest(session, inventory, request);
        }
        CraftResultsDeprecatedStackRequestActionData actionData = (CraftResultsDeprecatedStackRequestActionData) request.getActions()[1];
        ItemData resultItem = actionData.getResultItems()[0];
        if (resultItem.getTag() != null) {
            NbtMap displayTag = resultItem.getTag().getCompound("display");
            if (displayTag != null && displayTag.containsKey("Name")) {
                ItemData sourceSlot = inventory.getItem(0).getItemData(session);

                if (sourceSlot.getTag() != null) {
                    NbtMap oldDisplayTag = sourceSlot.getTag().getCompound("display");
                    if (oldDisplayTag != null && oldDisplayTag.containsKey("Name")) {
                        if (!displayTag.getString("Name").equals(oldDisplayTag.getString("Name"))) {
                            // Name has changed
                            sendRenamePacket(session, inventory, resultItem, displayTag.getString("Name"));
                        }
                    } else {
                        // No display tag on the old item
                        sendRenamePacket(session, inventory, resultItem, displayTag.getString("Name"));
                    }
                } else {
                    // New NBT tag
                    sendRenamePacket(session, inventory, resultItem, displayTag.getString("Name"));
                }
            }
        }
        return translateRequest(session, inventory, request);
    }

    private void sendRenamePacket(GeyserSession session, Inventory inventory, ItemData outputItem, String name) {
        session.sendDownstreamPacket(new ClientRenameItemPacket(name));
        inventory.setItem(2, GeyserItemStack.from(ItemTranslator.translateToJava(outputItem)), session);
    }

    /* 1.16.100 support end */

    @Override
    public int bedrockSlotToJava(StackRequestSlotInfoData slotInfoData) {
        switch (slotInfoData.getContainer()) {
            case ANVIL_INPUT:
                return 0;
            case ANVIL_MATERIAL:
                return 1;
            case ANVIL_RESULT:
            case CREATIVE_OUTPUT:
                return 2;
        }
        return super.bedrockSlotToJava(slotInfoData);
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        switch (slot) {
            case 0:
                return new BedrockContainerSlot(ContainerSlotType.ANVIL_INPUT, 1);
            case 1:
                return new BedrockContainerSlot(ContainerSlotType.ANVIL_MATERIAL, 2);
            case 2:
                return new BedrockContainerSlot(ContainerSlotType.ANVIL_RESULT, 50);
        }
        return super.javaSlotToBedrockContainer(slot);
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        switch (slot) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 50;
        }
        return super.javaSlotToBedrock(slot);
    }

    @Override
    public Inventory createInventory(String name, int windowId, WindowType windowType, PlayerInventory playerInventory) {
        return new AnvilContainer(name, windowId, this.size, windowType, playerInventory);
    }
}
