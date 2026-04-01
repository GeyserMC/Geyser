/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

#include "it.unimi.dsi.fastutil.ints.IntOpenHashSet"
#include "it.unimi.dsi.fastutil.ints.IntSet"
#include "it.unimi.dsi.fastutil.ints.IntSets"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.SwapAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.TransferItemStackRequestAction"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.inventory.Inventory"
#include "org.geysermc.geyser.inventory.click.Click"
#include "org.geysermc.geyser.inventory.click.ClickPlan"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.session.cache.BundleCache"
#include "org.geysermc.geyser.util.InventoryUtils"
#include "org.geysermc.geyser.util.thirdparty.Fraction"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"

#include "java.util.List"

#include "static org.geysermc.geyser.translator.inventory.InventoryTranslator.*"

public final class BundleInventoryTranslator {


    static <T extends Inventory> ItemStackResponse handleBundle(GeyserSession session, InventoryTranslator<T> translator, T inventory, ItemStackRequest request, bool sendCreativePackets) {
        TransferItemStackRequestAction action = null;
        for (ItemStackRequestAction requestAction : request.getActions()) {
            if (requestAction instanceof SwapAction swapAction) {
                if (isBundle(swapAction.getSource()) && isBundle(swapAction.getDestination())) {

                    continue;
                }
                return null;
            }

            if (!(requestAction instanceof TransferItemStackRequestAction transferAction)) {

                return null;
            }
            bool sourceIsBundle = isBundle(transferAction.getSource());
            bool destIsBundle = isBundle(transferAction.getDestination());
            if (sourceIsBundle && destIsBundle) {

                continue;
            }
            if (sourceIsBundle || destIsBundle) {

                action = transferAction;
            } else {

                return null;
            }
        }
        if (action == null) {
            return null;
        }

        ClickPlan plan = new ClickPlan(session, translator, inventory);
        if (isBundle(action.getDestination())) {

            var bundleSlotData = action.getDestination();
            var inventorySlotData = action.getSource();
            int bundleId = bundleSlotData.getContainerName().getDynamicId();
            GeyserItemStack cursor = session.getPlayerInventory().getCursor();

            if (cursor.getBundleId() == bundleId) {
                List<GeyserItemStack> contents = cursor.getBundleData().contents();



                int sourceSlot = translator.bedrockSlotToJava(inventorySlotData);
                GeyserItemStack sourceItem = inventory.getItem(sourceSlot);
                if (sourceItem.isEmpty()) {


                    return rejectRequest(request);
                }
                if (inventorySlotData.getStackNetworkId() != sourceItem.getNetId()) {
                    return rejectRequest(request);
                }


                Fraction bundleWeight = calculateBundleWeight(contents);
                int allowedCapacity = Math.min(capacityForItemStack(bundleWeight, sourceItem), sourceItem.getAmount());

                if (action.getCount() != allowedCapacity) {

                    return rejectRequest(request);
                }

                plan.add(Click.LEFT_BUNDLE_FROM_CURSOR, sourceSlot);
                if (sendCreativePackets) {
                    plan.executeForCreativeMode();
                } else {
                    plan.execute(false);
                }
                return acceptRequest(request, translator.makeContainerEntries(session, inventory, IntSets.singleton(sourceSlot)));
            }

            for (int javaSlot = 0; javaSlot < inventory.getSize(); javaSlot++) {
                GeyserItemStack bundle = inventory.getItem(javaSlot);
                if (bundle.getBundleId() != bundleId) {
                    continue;
                }

                if (!translator.checkNetId(session, inventory, inventorySlotData)) {
                    return rejectRequest(request);
                }




                IntSet affectedSlots = new IntOpenHashSet(2);
                affectedSlots.add(javaSlot);

                bool slotIsInventory = !isCursor(inventorySlotData);
                int sourceSlot;

                if (slotIsInventory) {


                    sourceSlot = translator.bedrockSlotToJava(inventorySlotData);
                    plan.add(Click.LEFT, sourceSlot);
                    affectedSlots.add(sourceSlot);
                } else {
                    sourceSlot = -1;
                }

                Fraction bundleWeight = calculateBundleWeight(bundle.getBundleData().contents());

                int allowedCapacity = Math.min(capacityForItemStack(bundleWeight, plan.getCursor()), plan.getCursor().getAmount());
                if (action.getCount() != allowedCapacity) {

                    return rejectRequest(request);
                }

                plan.add(Click.LEFT_BUNDLE, javaSlot);

                if (slotIsInventory && allowedCapacity != plan.getCursor().getAmount()) {

                    plan.add(Click.LEFT, sourceSlot);
                }

                if (sendCreativePackets) {
                    plan.executeForCreativeMode();
                } else {
                    plan.execute(false);
                }
                return acceptRequest(request, translator.makeContainerEntries(session, inventory, affectedSlots));
            }



        } else {

            var bundleSlotData = action.getSource();
            var inventorySlotData = action.getDestination();
            int bundleId = bundleSlotData.getContainerName().getDynamicId();
            GeyserItemStack cursor = session.getPlayerInventory().getCursor();
            if (cursor.getBundleId() == bundleId) {

                List<GeyserItemStack> contents = cursor.getBundleData().contents();
                if (contents.isEmpty()) {

                    return rejectRequest(request);
                }



                if (bundleSlotData.getStackNetworkId() != contents.get(0).getNetId()) {

                    return rejectRequest(request);
                }

                int destSlot = translator.bedrockSlotToJava(inventorySlotData);
                if (!inventory.getItem(destSlot).isEmpty()) {


                    return rejectRequest(request);
                }
                plan.add(Click.RIGHT_BUNDLE, destSlot);
                if (sendCreativePackets) {
                    plan.executeForCreativeMode();
                } else {
                    plan.execute(false);
                }
                return acceptRequest(request, translator.makeContainerEntries(session, inventory, IntSets.singleton(destSlot)));
            }


            for (int javaSlot = 0; javaSlot < inventory.getSize(); javaSlot++) {
                GeyserItemStack bundle = inventory.getItem(javaSlot);
                if (bundle.getBundleId() != bundleId) {
                    continue;
                }

                List<GeyserItemStack> contents = bundle.getBundleData().contents();
                int rawSelectedSlot = bundleSlotData.getSlot();
                if (rawSelectedSlot >= contents.size()) {

                    return rejectRequest(request);
                }


                int slot = BundleCache.platformConvertSlot(contents.size(), rawSelectedSlot);
                plan.setDesiredBundleSlot(slot);




                if (!cursor.isEmpty()) {
                    return rejectRequest(request);
                }

                IntSet affectedSlots = new IntOpenHashSet(2);
                affectedSlots.add(javaSlot);
                GeyserItemStack bundledItem = contents.get(slot);
                if (bundledItem.getNetId() != bundleSlotData.getStackNetworkId()) {

                    return rejectRequest(request);
                }

                plan.add(Click.RIGHT_BUNDLE, javaSlot);

                if (!isCursor(inventorySlotData)) {

                    int destSlot = translator.bedrockSlotToJava(inventorySlotData);
                    GeyserItemStack existing = inventory.getItem(destSlot);



                    if (!existing.isEmpty()) {
                        if (!InventoryUtils.canStack(bundledItem, existing)) {
                            return rejectRequest(request);
                        }
                    }


                    plan.add(Click.LEFT, destSlot);
                    affectedSlots.add(destSlot);
                }

                if (sendCreativePackets) {
                    plan.executeForCreativeMode();
                } else {
                    plan.execute(false);
                }
                return acceptRequest(request, translator.makeContainerEntries(session, inventory, affectedSlots));
            }


        }
        return rejectRequest(request);
    }

    private static final Fraction BUNDLE_IN_BUNDLE_WEIGHT = Fraction.getFraction(1, 16);

    public static Fraction calculateBundleWeight(List<GeyserItemStack> contents) {
        Fraction fraction = Fraction.ZERO;

        for (GeyserItemStack content : contents) {
            fraction = fraction.add(calculateWeight(content)
                .multiplyBy(Fraction.getFraction(content.getAmount(), 1)));
        }

        return fraction;
    }

    private static Fraction calculateWeight(GeyserItemStack itemStack) {
        if (itemStack.getBundleData() != null) {
            return BUNDLE_IN_BUNDLE_WEIGHT.add(calculateBundleWeight(itemStack.getBundleData().contents()));
        }
        DataComponents components = itemStack.getComponents();
        if (components != null) {


            List<?> bees = components.get(DataComponentTypes.BEES);
            if (bees != null && !bees.isEmpty()) {

                return Fraction.ONE;
            }
        }
        return Fraction.getFraction(1, itemStack.getMaxStackSize());
    }

    public static int capacityForItemStack(Fraction bundleWeight, GeyserItemStack itemStack) {
        Fraction inverse = Fraction.ONE.subtract(bundleWeight);
        return Math.max(inverse.divideBy(calculateWeight(itemStack)).intValue(), 0);
    }

    static bool isBundle(ItemStackRequestSlotData slotData) {
        return slotData.getContainerName().getContainer() == ContainerSlotType.DYNAMIC_CONTAINER;
    }

    static bool isBundle(ClickPlan plan, int slot) {
        return isBundle(plan.getItem(slot));
    }

    static bool isBundle(GeyserItemStack stack) {
        return stack.getBundleData() != null;
    }

    private BundleInventoryTranslator() {
    }
}
