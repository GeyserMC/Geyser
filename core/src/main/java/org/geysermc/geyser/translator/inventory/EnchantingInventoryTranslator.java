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

import it.unimi.dsi.fastutil.ints.IntSets;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.EnchantOptionData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.CraftRecipeAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestActionType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.cloudburstmc.protocol.bedrock.packet.PlayerEnchantOptionsPacket;
import org.geysermc.geyser.inventory.*;
import org.geysermc.geyser.inventory.updater.UIInventoryUpdater;
import org.geysermc.geyser.item.enchantment.Enchantment;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerButtonClickPacket;

import java.util.Arrays;

public class EnchantingInventoryTranslator extends AbstractBlockInventoryTranslator<EnchantingContainer> {
    public EnchantingInventoryTranslator() {
        super(2, Blocks.ENCHANTING_TABLE, org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType.ENCHANTMENT, UIInventoryUpdater.INSTANCE);
    }

    @Override
    public void updateProperty(GeyserSession session, EnchantingContainer container, int key, int value) {
        int slotToUpdate;
        boolean shouldUpdate = false;
        switch (key) {
            case 0:
            case 1:
            case 2:
                // Experience required
                slotToUpdate = key;
                container.getGeyserEnchantOptions()[slotToUpdate].setXpCost(value);
                break;
            case 4:
            case 5:
            case 6:
                // Enchantment type
                slotToUpdate = key - 4;
                // "value" here is the Java enchant ordinal, so that does not need to be changed
                // The Bedrock index might need changed, so let's look it up and see.
                int bedrockIndex = value;
                if (bedrockIndex != -1) {
                    Enchantment enchantment = session.getRegistryCache().registry(JavaRegistries.ENCHANTMENT).byId(value);
                    if (enchantment != null && enchantment.bedrockEnchantment() != null) {
                        // Convert the Java enchantment index to Bedrock's
                        bedrockIndex = enchantment.bedrockEnchantment().ordinal();
                    } else {
                        // There is no Bedrock enchantment equivalent
                        bedrockIndex = -1;
                    }
                }
                container.getGeyserEnchantOptions()[slotToUpdate].setEnchantIndex(bedrockIndex);
                break;
            case 7:
            case 8:
            case 9:
                // Enchantment level
                slotToUpdate = key - 7;
                container.getGeyserEnchantOptions()[slotToUpdate].setEnchantLevel(value);
                shouldUpdate = true; // Java sends each property as its own packet, so let's only update after all properties have been sent
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

    @Override
    protected boolean shouldHandleRequestFirst(ItemStackRequestAction action, EnchantingContainer container) {
        return action.getType() == ItemStackRequestActionType.CRAFT_RECIPE;
    }

    @Override
    public ItemStackResponse translateSpecialRequest(GeyserSession session, EnchantingContainer container, ItemStackRequest request) {
        // Client has requested an item to be enchanted
        CraftRecipeAction craftRecipeData = (CraftRecipeAction) request.getActions()[0];
        int javaSlot = -1;
        for (int i = 0; i < container.getEnchantOptions().length; i++) {
            EnchantOptionData enchantData = container.getEnchantOptions()[i];
            if (enchantData != null) {
                if (craftRecipeData.getRecipeNetworkId() == enchantData.getEnchantNetId()) {
                    // Enchant net ID is how we differentiate between what item Bedrock wants
                    javaSlot = container.getGeyserEnchantOptions()[i].getJavaIndex();
                    break;
                }
            }
        }
        if (javaSlot == -1) {
            // Slot should be determined as 0, 1, or 2
            return rejectRequest(request);
        }
        ServerboundContainerButtonClickPacket packet = new ServerboundContainerButtonClickPacket(container.getJavaId(), javaSlot);
        session.sendDownstreamGamePacket(packet);
        return acceptRequest(request, makeContainerEntries(session, container, IntSets.emptySet()));
    }

    @Override
    public int bedrockSlotToJava(ItemStackRequestSlotData slotInfoData) {
        if (slotInfoData.getContainerName().getContainer() == ContainerSlotType.ENCHANTING_INPUT) {
            return 0;
        }
        if (slotInfoData.getContainerName().getContainer() == ContainerSlotType.ENCHANTING_MATERIAL) {
            return 1;
        }
        return super.bedrockSlotToJava(slotInfoData);
    }

    @Override
    public BedrockContainerSlot javaSlotToBedrockContainer(int slot, EnchantingContainer container) {
        if (slot == 0) {
            return new BedrockContainerSlot(ContainerSlotType.ENCHANTING_INPUT, 14);
        }
        if (slot == 1) {
            return new BedrockContainerSlot(ContainerSlotType.ENCHANTING_MATERIAL, 15);
        }
        return super.javaSlotToBedrockContainer(slot, container);
    }

    @Override
    public int javaSlotToBedrock(int slot) {
        if (slot == 0) {
            return 14;
        }
        if (slot == 1) {
            return 15;
        }
        return super.javaSlotToBedrock(slot);
    }

    @Override
    public EnchantingContainer createInventory(GeyserSession session, String name, int windowId, ContainerType containerType) {
        return new EnchantingContainer(session, name, windowId, this.size, containerType);
    }

    @Override
    public org.cloudburstmc.protocol.bedrock.data.inventory.@Nullable ContainerType closeContainerType(EnchantingContainer container) {
        return null;
    }
}
