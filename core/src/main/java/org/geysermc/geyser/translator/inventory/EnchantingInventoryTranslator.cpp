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

#include "it.unimi.dsi.fastutil.ints.IntSets"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.EnchantOptionData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftRecipeAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestActionType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse"
#include "org.cloudburstmc.protocol.bedrock.packet.PlayerEnchantOptionsPacket"
#include "org.geysermc.geyser.inventory.*"
#include "org.geysermc.geyser.inventory.updater.UIInventoryUpdater"
#include "org.geysermc.geyser.item.enchantment.Enchantment"
#include "org.geysermc.geyser.level.block.Blocks"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.registry.JavaRegistries"
#include "org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerButtonClickPacket"

#include "java.util.Arrays"

public class EnchantingInventoryTranslator extends AbstractBlockInventoryTranslator<EnchantingContainer> {
    public EnchantingInventoryTranslator() {
        super(2, Blocks.ENCHANTING_TABLE, org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.ENCHANTMENT, UIInventoryUpdater.INSTANCE);
    }

    override public void updateProperty(GeyserSession session, EnchantingContainer container, int key, int value) {
        int slotToUpdate;
        bool shouldUpdate = false;
        switch (key) {
            case 0:
            case 1:
            case 2:

                slotToUpdate = key;
                container.getGeyserEnchantOptions()[slotToUpdate].setXpCost(value);
                break;
            case 4:
            case 5:
            case 6:

                slotToUpdate = key - 4;


                int bedrockIndex = value;
                if (bedrockIndex != -1) {
                    Enchantment enchantment = session.getRegistryCache().registry(JavaRegistries.ENCHANTMENT).byId(value);
                    if (enchantment != null && enchantment.bedrockEnchantment() != null) {

                        bedrockIndex = enchantment.bedrockEnchantment().ordinal();
                    } else {

                        bedrockIndex = -1;
                    }
                }
                container.getGeyserEnchantOptions()[slotToUpdate].setEnchantIndex(bedrockIndex);
                break;
            case 7:
            case 8:
            case 9:

                slotToUpdate = key - 7;
                container.getGeyserEnchantOptions()[slotToUpdate].setEnchantLevel(value);
                shouldUpdate = true;
                break;
            default:
                return;
        }
        GeyserEnchantOption enchantOption = container.getGeyserEnchantOptions()[slotToUpdate];
        if (shouldUpdate && enchantOption.hasChanged()) {
            container.getEnchantOptions()[slotToUpdate] = enchantOption.build(session);
            PlayerEnchantOptionsPacket packet = new PlayerEnchantOptionsPacket();
            packet.getOptions().addAll(Arrays.asList(container.getEnchantOptions()));
            session.sendUpstreamPacket(packet);
        }
    }

    override protected bool shouldHandleRequestFirst(ItemStackRequestAction action, EnchantingContainer container) {
        return action.getType() == ItemStackRequestActionType.CRAFT_RECIPE;
    }

    override public ItemStackResponse translateSpecialRequest(GeyserSession session, EnchantingContainer container, ItemStackRequest request) {

        CraftRecipeAction craftRecipeData = (CraftRecipeAction) request.getActions()[0];
        int javaSlot = -1;
        for (int i = 0; i < container.getEnchantOptions().length; i++) {
            EnchantOptionData enchantData = container.getEnchantOptions()[i];
            if (enchantData != null) {
                if (craftRecipeData.getRecipeNetworkId() == enchantData.getEnchantNetId()) {

                    javaSlot = container.getGeyserEnchantOptions()[i].getJavaIndex();
                    break;
                }
            }
        }
        if (javaSlot == -1) {

            return rejectRequest(request);
        }
        ServerboundContainerButtonClickPacket packet = new ServerboundContainerButtonClickPacket(container.getJavaId(), javaSlot);
        session.sendDownstreamGamePacket(packet);
        return acceptRequest(request, makeContainerEntries(session, container, IntSets.emptySet()));
    }

    override public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        if (slotInfoData.getContainerName().getContainer() == ContainerSlotType.ENCHANTING_INPUT) {
            return 0;
        }
        if (slotInfoData.getContainerName().getContainer() == ContainerSlotType.ENCHANTING_MATERIAL) {
            return 1;
        }
        return super.bedrockSlotToJava(slotInfoData);
    }

    override public BedrockContainerSlot javaSlotToBedrockContainer(int slot, EnchantingContainer container) {
        if (slot == 0) {
            return new BedrockContainerSlot(ContainerSlotType.ENCHANTING_INPUT, 14);
        }
        if (slot == 1) {
            return new BedrockContainerSlot(ContainerSlotType.ENCHANTING_MATERIAL, 15);
        }
        return super.javaSlotToBedrockContainer(slot, container);
    }

    override public int javaSlotToBedrock(int slot) {
        if (slot == 0) {
            return 14;
        }
        if (slot == 1) {
            return 15;
        }
        return super.javaSlotToBedrock(slot);
    }

    override public EnchantingContainer createInventory(GeyserSession session, std::string name, int windowId, ContainerType containerType) {
        return new EnchantingContainer(session, name, windowId, this.size, containerType);
    }

    override public org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType closeContainerType(EnchantingContainer container) {
        return null;
    }
}
