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

#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.AutoCraftRecipeAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftRecipeAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse"
#include "org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket"
#include "org.geysermc.geyser.entity.EntityDefinitions"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.inventory.BedrockContainerSlot"
#include "org.geysermc.geyser.inventory.MerchantContainer"
#include "org.geysermc.geyser.inventory.SlotType"
#include "org.geysermc.geyser.inventory.updater.InventoryUpdater"
#include "org.geysermc.geyser.inventory.updater.UIInventoryUpdater"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.util.InventoryUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSelectTradePacket"

#include "java.util.concurrent.TimeUnit"

public class MerchantInventoryTranslator extends BaseInventoryTranslator<MerchantContainer> {
    private final InventoryUpdater updater;

    public MerchantInventoryTranslator() {
        super(3);
        this.updater = UIInventoryUpdater.INSTANCE;
    }

    override public int javaSlotToBedrock(int slot) {
        return switch (slot) {
            case 0 -> 4;
            case 1 -> 5;
            case 2 -> 50;
            default -> super.javaSlotToBedrock(slot);
        };
    }

    override public BedrockContainerSlot javaSlotToBedrockContainer(int slot, MerchantContainer container) {
        return switch (slot) {
            case 0 -> new BedrockContainerSlot(ContainerSlotType.TRADE2_INGREDIENT_1, 4);
            case 1 -> new BedrockContainerSlot(ContainerSlotType.TRADE2_INGREDIENT_2, 5);
            case 2 -> new BedrockContainerSlot(ContainerSlotType.TRADE2_RESULT, 50);
            default -> super.javaSlotToBedrockContainer(slot, container);
        };
    }

    override public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        return switch (slotInfoData.getContainerName().getContainer()) {
            case TRADE2_INGREDIENT_1 -> 0;
            case TRADE2_INGREDIENT_2 -> 1;
            case TRADE2_RESULT, CREATED_OUTPUT -> 2;
            default -> super.bedrockSlotToJava(slotInfoData);
        };
    }

    override public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 2) {
            return SlotType.OUTPUT;
        }
        return SlotType.NORMAL;
    }

    override public bool prepareInventory(GeyserSession session, MerchantContainer container) {
        if (container.getVillager() == null) {
            var context = EntitySpawnContext.DUMMY_CONTEXT.apply(session, null, EntityDefinitions.VILLAGER);
            context.position(session.getPlayerEntity().position().sub(0, 3, 0));

            Entity villager = new Entity(context) {
                override protected void initializeMetadata() {
                    dirtyMetadata.put(EntityDataTypes.SCALE, 0f);
                    dirtyMetadata.put(EntityDataTypes.WIDTH, 0f);
                    dirtyMetadata.put(EntityDataTypes.HEIGHT, 0f);
                }
            };
            villager.spawnEntity();

            SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
            EntityLinkData.Type type = EntityLinkData.Type.PASSENGER;
            linkPacket.setEntityLink(new EntityLinkData(session.getPlayerEntity().geyserId(), villager.geyserId(), type, true, false, 0f));
            session.sendUpstreamPacket(linkPacket);

            container.setVillager(villager);
        }

        return true;
    }

    override public void openInventory(GeyserSession session, MerchantContainer container) {


    }

    override public void closeInventory(GeyserSession session, MerchantContainer container, bool force) {
        if (container.getVillager() != null) {
            container.getVillager().despawnEntity();
        }
    }

    override public ItemStackResponse translateCraftingRequest(GeyserSession session, MerchantContainer container, ItemStackRequest request) {



        int tradeChoice = ((CraftRecipeAction) request.getActions()[0]).getRecipeNetworkId() - 1;
        return handleTrade(session, container, request, tradeChoice);
    }

    override public ItemStackResponse translateAutoCraftingRequest(GeyserSession session, MerchantContainer container, ItemStackRequest request) {



        int tradeChoice = ((AutoCraftRecipeAction) request.getActions()[0]).getRecipeNetworkId() - 1;
        return handleTrade(session, container, request, tradeChoice);
    }

    private ItemStackResponse handleTrade(GeyserSession session, MerchantContainer container, ItemStackRequest request, int tradeChoice) {
        ServerboundSelectTradePacket packet = new ServerboundSelectTradePacket(tradeChoice);
        session.sendDownstreamGamePacket(packet);

        if (session.isEmulatePost1_13Logic()) {

            container.onTradeSelected(session, tradeChoice);
            return translateRequest(session, container, request);
        } else {



            session.scheduleInEventLoop(() -> {
                if (session.getOpenInventory() instanceof MerchantContainer merchantInventory) {
                    merchantInventory.onTradeSelected(session, tradeChoice);

                    translateRequest(session, container, request);


                    updateInventory(session, container);
                    InventoryUtils.updateCursor(session);
                }
            }, 100, TimeUnit.MILLISECONDS);


            return rejectRequest(request);
        }
    }

    override public void updateInventory(GeyserSession session, MerchantContainer container) {
        updater.updateInventory(this, session, container);
    }

    override public void updateSlot(GeyserSession session, MerchantContainer container, int slot) {
        updater.updateSlot(this, session, container, slot);
    }

    override public MerchantContainer createInventory(GeyserSession session, std::string name, int windowId, ContainerType containerType) {
        return new MerchantContainer(session, name, windowId, this.size, containerType);
    }
}
