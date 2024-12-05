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

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.TransferItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.response.ItemStackResponse;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.inventory.Inventory;
import org.geysermc.geyser.inventory.click.Click;
import org.geysermc.geyser.inventory.click.ClickPlan;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.BundleCache;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.geyser.util.thirdparty.Fraction;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.List;

import static org.geysermc.geyser.translator.inventory.InventoryTranslator.*;

public final class BundleInventoryTranslator {
    /**
     * @return a processed bundle interaction, or null to resume normal transaction handling.
     */
    @Nullable
    static ItemStackResponse handleBundle(GeyserSession session, InventoryTranslator translator, Inventory inventory, ItemStackRequest request, boolean sendCreativePackets) {
        TransferItemStackRequestAction action = null;
        for (ItemStackRequestAction requestAction : request.getActions()) {
            if (!(requestAction instanceof TransferItemStackRequestAction transferAction)) {
                // No known bundle action that does not use transfer actions
                return null;
            }
            boolean sourceIsBundle = isBundle(transferAction.getSource());
            boolean destIsBundle = isBundle(transferAction.getDestination());
            if (sourceIsBundle && destIsBundle) {
                // The client is rearranging the bundle inventory; we're going to ignore translating these actions.
                continue;
            }
            if (sourceIsBundle || destIsBundle) {
                // This action is moving to a bundle or moving out of a bundle. This is the one we want to track
                action = transferAction;
            } else {
                // Neither location is a bundle location. We don't need to deal with this here.
                return null;
            }
        }
        if (action == null) {
            return null;
        }

        ClickPlan plan = new ClickPlan(session, translator, inventory);
        if (isBundle(action.getDestination())) {
            // Placing into bundle
            var bundleSlotData = action.getDestination();
            var inventorySlotData = action.getSource();
            int bundleId = bundleSlotData.getContainerName().getDynamicId();
            GeyserItemStack cursor = session.getPlayerInventory().getCursor();

            if (cursor.getBundleId() == bundleId) {
                List<GeyserItemStack> contents = cursor.getBundleData().contents();
                // Placing items into bundles can mean their contents are empty

                // We are currently holding the bundle and trying to pick an item up.
                int sourceSlot = translator.bedrockSlotToJava(inventorySlotData);
                GeyserItemStack sourceItem = inventory.getItem(sourceSlot);
                if (sourceItem.isEmpty()) {
                    // This would be treated as just... plumping the bundle down,
                    // and that should not be called here.
                    return rejectRequest(request);
                }
                if (inventorySlotData.getStackNetworkId() != sourceItem.getNetId()) {
                    return rejectRequest(request);
                }

                // Note that this is also called in ClickPlan. Not ideal...
                Fraction bundleWeight = calculateBundleWeight(contents);
                int allowedCapacity = Math.min(capacityForItemStack(bundleWeight, sourceItem), sourceItem.getAmount());

                if (action.getCount() != allowedCapacity) {
                    // Might trigger if bundle weight is different between Java and Bedrock (see calculateBundleWeight)
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

                // Placing items into bundles can mean their contents are empty
                // Bundle slot does not matter; Java always appends an item to the beginning of a bundle inventory

                IntSet affectedSlots = new IntOpenHashSet(2);
                affectedSlots.add(javaSlot);

                boolean slotIsInventory = !isCursor(inventorySlotData);
                int sourceSlot;
                // If source is cursor, logic lines up better with Java.
                if (slotIsInventory) {
                    // Simulate picking up the item and adding it to our cursor,
                    // which is what Java would expect
                    sourceSlot = translator.bedrockSlotToJava(inventorySlotData);
                    plan.add(Click.LEFT, sourceSlot);
                    affectedSlots.add(sourceSlot);
                } else {
                    sourceSlot = -1;
                }

                Fraction bundleWeight = calculateBundleWeight(bundle.getBundleData().contents());
                // plan.getCursor() covers if we just picked up the item above from a slot
                int allowedCapacity = Math.min(capacityForItemStack(bundleWeight, plan.getCursor()), plan.getCursor().getAmount());
                if (action.getCount() != allowedCapacity) {
                    // Might trigger if bundle weight is different between Java and Bedrock (see calculateBundleWeight)
                    return rejectRequest(request);
                }

                plan.add(Click.LEFT_BUNDLE, javaSlot);

                if (slotIsInventory && allowedCapacity != plan.getCursor().getAmount()) {
                    // We will need to place the item back in its original slot.
                    plan.add(Click.LEFT, sourceSlot);
                }

                if (sendCreativePackets) {
                    plan.executeForCreativeMode();
                } else {
                    plan.execute(false);
                }
                return acceptRequest(request, translator.makeContainerEntries(session, inventory, affectedSlots));
            }

            // Could not find bundle in inventory

        } else {
            // Taking from bundle
            var bundleSlotData = action.getSource();
            var inventorySlotData = action.getDestination();
            int bundleId = bundleSlotData.getContainerName().getDynamicId();
            GeyserItemStack cursor = session.getPlayerInventory().getCursor();
            if (cursor.getBundleId() == bundleId) {
                // We are currently holding the bundle
                List<GeyserItemStack> contents = cursor.getBundleData().contents();
                if (contents.isEmpty()) {
                    // Nothing would be ejected?
                    return rejectRequest(request);
                }

                // Can't select bundle slots while holding bundle in any version; don't set desired bundle slot

                if (bundleSlotData.getStackNetworkId() != contents.get(0).getNetId()) {
                    // We're pulling out the first item; if something mismatches, wuh oh.
                    return rejectRequest(request);
                }

                int destSlot = translator.bedrockSlotToJava(inventorySlotData);
                if (!inventory.getItem(destSlot).isEmpty()) {
                    // Illegal action to place an item down on an existing stack, even if
                    // the bundle contains the item.
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

            // We need context of what slot the bundle is in.
            for (int javaSlot = 0; javaSlot < inventory.getSize(); javaSlot++) {
                GeyserItemStack bundle = inventory.getItem(javaSlot);
                if (bundle.getBundleId() != bundleId) {
                    continue;
                }

                List<GeyserItemStack> contents = bundle.getBundleData().contents();
                int rawSelectedSlot = bundleSlotData.getSlot();
                if (rawSelectedSlot >= contents.size()) {
                    // Illegal?
                    return rejectRequest(request);
                }

                // Bedrock's indexes are flipped around - first item shown to it is the last index.
                int slot = BundleCache.platformConvertSlot(contents.size(), rawSelectedSlot);
                plan.setDesiredBundleSlot(slot);

                // We'll need it even if the final destination isn't the cursor.
                // I can't think of a situation where we shouldn't reject it and use a temp slot,
                // but we will see.
                if (!cursor.isEmpty()) {
                    return rejectRequest(request);
                }

                IntSet affectedSlots = new IntOpenHashSet(2);
                affectedSlots.add(javaSlot);
                GeyserItemStack bundledItem = contents.get(slot);
                if (bundledItem.getNetId() != bundleSlotData.getStackNetworkId()) {
                    // !!!
                    return rejectRequest(request);
                }

                plan.add(Click.RIGHT_BUNDLE, javaSlot);
                // If false, simple logic that matches nicely with Java Edition
                if (!isCursor(inventorySlotData)) {
                    // Alas, two-click time.
                    int destSlot = translator.bedrockSlotToJava(inventorySlotData);
                    GeyserItemStack existing = inventory.getItem(destSlot);

                    // Empty slot is good, but otherwise let's just check that
                    // the two can stack...
                    if (!existing.isEmpty()) {
                        if (!InventoryUtils.canStack(bundledItem, existing)) {
                            return rejectRequest(request);
                        }
                    }

                    // Copy the full stack to the new slot.
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

            // Could not find bundle in inventory
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
            // NOTE: this seems to be Java-only, so it can technically cause a bundle weight desync,
            // but it'll be so rare we can probably ignore it.
            List<?> bees = components.get(DataComponentType.BEES);
            if (bees != null && !bees.isEmpty()) {
                // Bees be heavy, I guess.
                return Fraction.ONE;
            }
        }
        return Fraction.getFraction(1, itemStack.asItem().maxStackSize());
    }

    public static int capacityForItemStack(Fraction bundleWeight, GeyserItemStack itemStack) {
        Fraction inverse = Fraction.ONE.subtract(bundleWeight);
        return Math.max(inverse.divideBy(calculateWeight(itemStack)).intValue(), 0);
    }

    static boolean isBundle(ItemStackRequestSlotData slotData) {
        return slotData.getContainerName().getContainer() == ContainerSlotType.DYNAMIC_CONTAINER;
    }

    static boolean isBundle(ClickPlan plan, int slot) {
        return isBundle(plan.getItem(slot));
    }

    static boolean isBundle(GeyserItemStack stack) {
        return stack.getBundleData() != null;
    }

    private BundleInventoryTranslator() {
    }
}
