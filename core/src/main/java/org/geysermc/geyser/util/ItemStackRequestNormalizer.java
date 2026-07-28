/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.util;

import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ConsumeAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.DestroyAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.DropAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.ItemStackRequestAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.PlaceAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.SwapAction;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.TakeAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Pre-v712 Bedrock fills {@link ItemStackRequestSlotData#getContainer()} and leaves
 * {@link ItemStackRequestSlotData#getContainerName()} null. Modern Geyser code only reads
 * {@code containerName}, which NPEs on 1.20.x clients. Normalize once at the inventory entrypoint.
 */
public final class ItemStackRequestNormalizer {

    private ItemStackRequestNormalizer() {
    }

    public static List<ItemStackRequest> normalize(List<ItemStackRequest> requests) {
        List<ItemStackRequest> out = new ArrayList<>(requests.size());
        for (ItemStackRequest request : requests) {
            out.add(normalize(request));
        }
        return out;
    }

    public static ItemStackRequest normalize(ItemStackRequest request) {
        ItemStackRequestAction[] actions = request.getActions();
        if (actions == null || actions.length == 0) {
            return request;
        }

        boolean changed = false;
        ItemStackRequestAction[] normalized = new ItemStackRequestAction[actions.length];
        for (int i = 0; i < actions.length; i++) {
            ItemStackRequestAction action = normalizeAction(actions[i]);
            normalized[i] = action;
            if (action != actions[i]) {
                changed = true;
            }
        }

        if (!changed) {
            return request;
        }
        return new ItemStackRequest(request.getRequestId(), normalized, request.getFilterStrings(),
            request.getTextProcessingEventOrigin());
    }

    private static ItemStackRequestAction normalizeAction(ItemStackRequestAction action) {
        if (action instanceof TakeAction take) {
            ItemStackRequestSlotData source = normalizeSlot(take.getSource());
            ItemStackRequestSlotData destination = normalizeSlot(take.getDestination());
            if (source == take.getSource() && destination == take.getDestination()) {
                return take;
            }
            return new TakeAction(take.getCount(), source, destination);
        }
        if (action instanceof PlaceAction place) {
            ItemStackRequestSlotData source = normalizeSlot(place.getSource());
            ItemStackRequestSlotData destination = normalizeSlot(place.getDestination());
            if (source == place.getSource() && destination == place.getDestination()) {
                return place;
            }
            return new PlaceAction(place.getCount(), source, destination);
        }
        if (action instanceof SwapAction swap) {
            ItemStackRequestSlotData source = normalizeSlot(swap.getSource());
            ItemStackRequestSlotData destination = normalizeSlot(swap.getDestination());
            if (source == swap.getSource() && destination == swap.getDestination()) {
                return swap;
            }
            return new SwapAction(source, destination);
        }
        if (action instanceof DropAction drop) {
            ItemStackRequestSlotData source = normalizeSlot(drop.getSource());
            if (source == drop.getSource()) {
                return drop;
            }
            return new DropAction(drop.getCount(), source, drop.isRandomly());
        }
        if (action instanceof DestroyAction destroy) {
            ItemStackRequestSlotData source = normalizeSlot(destroy.getSource());
            if (source == destroy.getSource()) {
                return destroy;
            }
            return new DestroyAction(destroy.getCount(), source);
        }
        if (action instanceof ConsumeAction consume) {
            ItemStackRequestSlotData source = normalizeSlot(consume.getSource());
            if (source == consume.getSource()) {
                return consume;
            }
            return new ConsumeAction(consume.getCount(), source);
        }
        return action;
    }

    public static ItemStackRequestSlotData normalizeSlot(ItemStackRequestSlotData slot) {
        if (slot == null || slot.getContainerName() != null) {
            return slot;
        }
        return new ItemStackRequestSlotData(
            slot.getContainer(),
            slot.getSlot(),
            slot.getStackNetworkId(),
            new FullContainerName(slot.getContainer(), null)
        );
    }
}
