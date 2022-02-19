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

import com.github.steveice10.mc.protocol.data.game.inventory.ContainerType;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSelectTradePacket;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityLinkData;
import com.nukkitx.protocol.bedrock.data.inventory.ContainerSlotType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemStackRequest;
import com.nukkitx.protocol.bedrock.data.inventory.StackRequestSlotInfoData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.AutoCraftRecipeStackRequestActionData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.CraftRecipeStackRequestActionData;
import com.nukkitx.protocol.bedrock.packet.ItemStackResponsePacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityLinkPacket;
import com.nukkitx.protocol.bedrock.v486.Bedrock_v486;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.MerchantContainer;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.inventory.BedrockContainerSlot;
import org.geysermc.geyser.inventory.SlotType;
import org.geysermc.geyser.inventory.updater.InventoryUpdater;
import org.geysermc.geyser.inventory.updater.UIInventoryUpdater;
import org.geysermc.geyser.util.InventoryUtils;

import java.util.concurrent.TimeUnit;

public class MerchantInventoryTranslator extends BaseInventoryTranslator {
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
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot) {
        return switch (slot) {
            case 0 -> new BedrockContainerSlot(ContainerSlotType.TRADE2_INGREDIENT1, 4);
            case 1 -> new BedrockContainerSlot(ContainerSlotType.TRADE2_INGREDIENT2, 5);
            case 2 -> new BedrockContainerSlot(ContainerSlotType.TRADE2_RESULT, 50);
            default -> super.javaSlotToBedrockContainer(slot);
        };
    }

    @Override
    public int bedrockSlotToJava(StackRequestSlotInfoData slotInfoData) {
        return switch (slotInfoData.getContainer()) {
            case TRADE2_INGREDIENT1 -> 0;
            case TRADE2_INGREDIENT2 -> 1;
            case TRADE2_RESULT, CREATIVE_OUTPUT -> 2;
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
    public void prepareInventory(GeyserSession session, Inventory inventory) {
        MerchantContainer merchantInventory = (MerchantContainer) inventory;
        if (merchantInventory.getVillager() == null) {
            long geyserId = session.getEntityCache().getNextEntityId().incrementAndGet();
            Vector3f pos = session.getPlayerEntity().getPosition().sub(0, 3, 0);

            Entity villager = new Entity(session, 0, geyserId, null, EntityDefinitions.VILLAGER, pos, Vector3f.ZERO, 0f, 0f, 0f) {
                @Override
                protected void initializeMetadata() {
                    dirtyMetadata.put(EntityData.SCALE, 0f);
                    dirtyMetadata.put(EntityData.BOUNDING_BOX_WIDTH, 0f);
                    dirtyMetadata.put(EntityData.BOUNDING_BOX_HEIGHT, 0f);
                }
            };
            villager.spawnEntity();

            SetEntityLinkPacket linkPacket = new SetEntityLinkPacket();
            EntityLinkData.Type type = EntityLinkData.Type.PASSENGER;
            linkPacket.setEntityLink(new EntityLinkData(session.getPlayerEntity().getGeyserId(), geyserId, type, true, false));
            session.sendUpstreamPacket(linkPacket);

            merchantInventory.setVillager(villager);
        }
    }

    @Override
    public void openInventory(GeyserSession session, Inventory inventory) {
        //Handled in JavaMerchantOffersTranslator
        //TODO: send a blank inventory here in case the villager doesn't send a TradeList packet
    }

    @Override
    public void closeInventory(GeyserSession session, Inventory inventory) {
        MerchantContainer merchantInventory = (MerchantContainer) inventory;
        if (merchantInventory.getVillager() != null) {
            merchantInventory.getVillager().despawnEntity();
        }
    }

    @Override
    public ItemStackResponsePacket.Response translateCraftingRequest(GeyserSession session, Inventory inventory, ItemStackRequest request) {
        if (session.getUpstream().getProtocolVersion() < Bedrock_v486.V486_CODEC.getProtocolVersion()) {
            return super.translateCraftingRequest(session, inventory, request);
        }

        // Behavior as of 1.18.10.
        // We set the net ID to the trade index + 1. This doesn't appear to cause issues and means we don't have to
        // store a map of net ID to trade index on our end.
        int tradeChoice = ((CraftRecipeStackRequestActionData) request.getActions()[0]).getRecipeNetworkId() - 1;
        return handleTrade(session, inventory, request, tradeChoice);
    }

    @Override
    public ItemStackResponsePacket.Response translateAutoCraftingRequest(GeyserSession session, Inventory inventory, ItemStackRequest request) {
        if (session.getUpstream().getProtocolVersion() < Bedrock_v486.V486_CODEC.getProtocolVersion()) {
            // We're not crafting here
            // Called at least by consoles when pressing a trade option button
            return translateRequest(session, inventory, request);
        }

        // 1.18.10 update - seems impossible to call without consoles/controller input
        // We set the net ID to the trade index + 1. This doesn't appear to cause issues and means we don't have to
        // store a map of net ID to trade index on our end.
        int tradeChoice = ((AutoCraftRecipeStackRequestActionData) request.getActions()[0]).getRecipeNetworkId() - 1;
        return handleTrade(session, inventory, request, tradeChoice);
    }

    private ItemStackResponsePacket.Response handleTrade(GeyserSession session, Inventory inventory, ItemStackRequest request, int tradeChoice) {
        ServerboundSelectTradePacket packet = new ServerboundSelectTradePacket(tradeChoice);
        session.sendDownstreamPacket(packet);

        if (session.isEmulatePost1_14Logic()) {
            // 1.18 Java cooperates nicer than older versions
            if (inventory instanceof MerchantContainer merchantInventory) {
                merchantInventory.onTradeSelected(session, tradeChoice);
            }
            return translateRequest(session, inventory, request);
        } else {
            // 1.18 servers works fine without a workaround, but ViaVersion needs to work around 1.13 servers,
            // so we need to work around that with the delay. Specifically they force a window refresh after a
            // trade packet has been sent.
            session.scheduleInEventLoop(() -> {
                if (inventory instanceof MerchantContainer merchantInventory) {
                    merchantInventory.onTradeSelected(session, tradeChoice);
                    // Ignore output since we don't want to send a delayed response packet back to the client
                    translateRequest(session, inventory, request);

                    // Resync items once more
                    updateInventory(session, inventory);
                    InventoryUtils.updateCursor(session);
                }
            }, 100, TimeUnit.MILLISECONDS);

            // Revert this request, for now
            return rejectRequest(request);
        }
    }

    @Override
    public void updateInventory(GeyserSession session, Inventory inventory) {
        updater.updateInventory(this, session, inventory);
    }

    @Override
    public void updateSlot(GeyserSession session, Inventory inventory, int slot) {
        updater.updateSlot(this, session, inventory, slot);
    }

    @Override
    public Inventory createInventory(String name, int windowId, ContainerType containerType, PlayerInventory playerInventory) {
        return new MerchantContainer(name, windowId, this.size, containerType, playerInventory);
    }
}
