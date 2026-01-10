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

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.AutoCraftRecipeAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftRecipeAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityLinkPacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.inventory.BedrockContainerSlot;
import org.geysermc.geyser.inventory.MerchantContainer;
import org.geysermc.geyser.inventory.SlotType;
import org.geysermc.geyser.inventory.updater.InventoryUpdater;
import org.geysermc.geyser.inventory.updater.UIInventoryUpdater;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSelectTradePacket;

import java.util.concurrent.TimeUnit;

public class MerchantInventoryTranslator extends BaseInventoryTranslator<MerchantContainer> {
    private final InventoryUpdater updater;

    public MerchantInventoryTranslator() {
        super(3);
        this.updater = UIInventoryUpdater.INSTANCE;
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        return switch (slot) {
            case 0 -> 4;
            case 1 -> 5;
            case 2 -> 50;
            default -> super.javaSlotToBedrock(slot);
        };
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot, MerchantContainer container) {
        return switch (slot) {
            case 0 -> new BedrockContainerSlot(ContainerSlotType.TRADE2_INGREDIENT_1, 4);
            case 1 -> new BedrockContainerSlot(ContainerSlotType.TRADE2_INGREDIENT_2, 5);
            case 2 -> new BedrockContainerSlot(ContainerSlotType.TRADE2_RESULT, 50);
            default -> super.javaSlotToBedrockContainer(slot, container);
        };
    }

    @Override
    public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        return switch (slotInfoData.getContainerName().getContainer()) {
            case TRADE2_INGREDIENT_1 -> 0;
            case TRADE2_INGREDIENT_2 -> 1;
            case TRADE2_RESULT, CREATED_OUTPUT -> 2;
            default -> super.bedrockSlotToJava(slotInfoData);
        };
    }

    @Override
    public SlotType getSlotType(int javaSlot) {
        if (javaSlot == 2) {
            return SlotType.OUTPUT;
        }
        return SlotType.NORMAL;
    }

    @Override
    public boolean prepareInventory(GeyserSession session, MerchantContainer container) {
        if (container.getVillager() == null) {
            var context = EntitySpawnContext.DUMMY_CONTEXT.apply(session, null, EntityDefinitions.VILLAGER);
            context.position(session.getPlayerEntity().position().sub(0, 3, 0));

            Entity villager = new Entity(context) {
                @Override
                protected void initializeMetadata() {
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

    @Override
    public void openInventory(GeyserSession session, MerchantContainer container) {
        //Handled in JavaMerchantOffersTranslator
        //TODO: send a blank inventory here in case the villager doesn't send a TradeList packet
    }

    @Override
    public void closeInventory(GeyserSession session, MerchantContainer container, boolean force) {
        if (container.getVillager() != null) {
            container.getVillager().despawnEntity();
        }
    }

    @Override
    public ItemStackResponse translateCraftingRequest(GeyserSession session, MerchantContainer container, ItemStackRequest request) {
        // Behavior as of 1.18.10.
        // We set the net ID to the trade index + 1. This doesn't appear to cause issues and means we don't have to
        // store a map of net ID to trade index on our end.
        int tradeChoice = ((CraftRecipeAction) request.getActions()[0]).getRecipeNetworkId() - 1;
        return handleTrade(session, container, request, tradeChoice);
    }

    @Override
    public ItemStackResponse translateAutoCraftingRequest(GeyserSession session, MerchantContainer container, ItemStackRequest request) {
        // 1.18.10 update - seems impossible to call without consoles/controller input
        // We set the net ID to the trade index + 1. This doesn't appear to cause issues and means we don't have to
        // store a map of net ID to trade index on our end.
        int tradeChoice = ((AutoCraftRecipeAction) request.getActions()[0]).getRecipeNetworkId() - 1;
        return handleTrade(session, container, request, tradeChoice);
    }

    private ItemStackResponse handleTrade(GeyserSession session, MerchantContainer container, ItemStackRequest request, int tradeChoice) {
        ServerboundSelectTradePacket packet = new ServerboundSelectTradePacket(tradeChoice);
        session.sendDownstreamGamePacket(packet);

        if (session.isEmulatePost1_13Logic()) {
            // 1.18 Java cooperates nicer than older versions
            container.onTradeSelected(session, tradeChoice);
            return translateRequest(session, container, request);
        } else {
            // 1.18 servers works fine without a workaround, but ViaVersion needs to work around 1.13 servers,
            // so we need to work around that with the delay. Specifically they force a window refresh after a
            // trade packet has been sent.
            session.scheduleInEventLoop(() -> {
                if (session.getOpenInventory() instanceof MerchantContainer merchantInventory) {
                    merchantInventory.onTradeSelected(session, tradeChoice);
                    // Ignore output since we don't want to send a delayed response packet back to the client
                    translateRequest(session, container, request);

                    // Resync items once more
                    updateInventory(session, container);
                    InventoryUtils.updateCursor(session);
                }
            }, 100, TimeUnit.MILLISECONDS);

            // Revert this request, for now
            return rejectRequest(request);
        }
    }

    @Override
    public void updateInventory(GeyserSession session, MerchantContainer container) {
        updater.updateInventory(this, session, container);
    }

    @Override
    public void updateSlot(GeyserSession session, MerchantContainer container, int slot) {
        updater.updateSlot(this, session, container, slot);
    }

    @Override
    public MerchantContainer createInventory(GeyserSession session, String name, int windowId, ContainerType containerType) {
        return new MerchantContainer(session, name, windowId, this.size, containerType);
    }
}
