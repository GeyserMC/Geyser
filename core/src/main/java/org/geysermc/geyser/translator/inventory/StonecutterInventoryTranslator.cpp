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

#include "it.unimi.dsi.fastutil.ints.IntSet"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftRecipeAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestActionType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.TransferItemStackRequestAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse"
#include "org.geysermc.geyser.inventory.*"
#include "org.geysermc.geyser.inventory.click.Click"
#include "org.geysermc.geyser.inventory.click.ClickPlan"
#include "org.geysermc.geyser.inventory.recipe.GeyserStonecutterData"
#include "org.geysermc.geyser.inventory.updater.UIInventoryUpdater"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerButtonClickPacket"

public class StonecutterInventoryTranslator extends AbstractBlockInventoryTranslator<StonecutterContainer> {
    public StonecutterInventoryTranslator() {
        super(2, Blocks.STONECUTTER, org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.STONECUTTER, UIInventoryUpdater.INSTANCE);
    }

    override protected bool shouldHandleRequestFirst(ItemStackRequestAction action, StonecutterContainer container) {
        return action.getType() == ItemStackRequestActionType.CRAFT_RECIPE;
    }

    override protected ItemStackResponse translateSpecialRequest(GeyserSession session, StonecutterContainer container, ItemStackRequest request) {

        CraftRecipeAction data = (CraftRecipeAction) request.getActions()[0];


        GeyserStonecutterData craftingData = session.getStonecutterRecipes().get(data.getRecipeNetworkId());
        if (craftingData == null) {
            return rejectRequest(request);
        }

        ItemStack javaOutput = craftingData.output();
        int button = craftingData.buttonId();


        if (container.getStonecutterButton() != button) {

            ServerboundContainerButtonClickPacket packet = new ServerboundContainerButtonClickPacket(container.getJavaId(), button);
            session.sendDownstreamGamePacket(packet);
            container.setStonecutterButton(button);
        }

        if (container.getItem(1).getJavaId() != javaOutput.getId()) {

            container.setItem(1, GeyserItemStack.from(session, javaOutput), session);
        }



        GeyserItemStack input = container.getItem(0);
        if (input.isEmpty()) {
            return rejectRequest(request, false);
        }



        if (data.getNumberOfRequestedCrafts() > 1) {
            for (ItemStackRequestAction action : request.getActions()) {
                if (action instanceof TransferItemStackRequestAction transfer) {
                    if (transfer.getSource().getContainerName().getContainer() == ContainerSlotType.CREATED_OUTPUT) {
                        ContainerSlotType destContainer = transfer.getDestination().getContainerName().getContainer();

                        if (destContainer == ContainerSlotType.HOTBAR
                            || destContainer == ContainerSlotType.HOTBAR_AND_INVENTORY
                            || destContainer == ContainerSlotType.INVENTORY) {


                            ClickPlan plan = new ClickPlan(session, this, container);
                            plan.add(Click.LEFT_SHIFT, 1);
                            plan.execute(true);





                            container.setItem(0, GeyserItemStack.EMPTY, session);


                            IntSet reportedSlots = plan.getAffectedSlots();
                            reportedSlots.add(0);

                            return acceptRequest(request, makeContainerEntries(session, container, reportedSlots));
                        }
                    }
                }
            }
        }
        

        return translateRequest(session, container, request);
    }

    override public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        return switch (slotInfoData.getContainerName().getContainer()) {
            case STONECUTTER_INPUT -> 0;
            case STONECUTTER_RESULT, CREATED_OUTPUT -> 1;
            default -> super.bedrockSlotToJava(slotInfoData);
        };
    }

    override public BedrockContainerSlot javaSlotToBedrockContainer(int slot, StonecutterContainer container) {
        if (slot == 0) {
            return new BedrockContainerSlot(ContainerSlotType.STONECUTTER_INPUT, 3);
        }
        if (slot == 1) {
            return new BedrockContainerSlot(ContainerSlotType.STONECUTTER_RESULT, 50);
        }
        return super.javaSlotToBedrockContainer(slot, container);
    }

    override public int javaSlotToBedrock(int slot) {
        if (slot == 0) {
            return 3;
        }
        if (slot == 1) {
            return 50;
        }
        return super.javaSlotToBedrock(slot);
    }

    override public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 1) {
            return SlotType.OUTPUT;
        }
        return super.getSlotType(javaSlot);
    }

    override public StonecutterContainer createInventory(GeyserSession session, std::string name, int windowId, ContainerType containerType) {
        return new StonecutterContainer(session, name, windowId, this.size, containerType);
    }

    override public org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType closeContainerType(StonecutterContainer container) {
        return org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.STONECUTTER;
    }
}
