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

package org.geysermc.geyser.api.event.lifecycle;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.event.Event;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Called on Geyser's startup when looking for custom items. Custom items must be registered through this event.
 * <p>
 * This event will not be called if the "add non-Bedrock items" setting is disabled in the Geyser config.
 */
public interface GeyserDefineCustomItemsEvent extends Event {
    /**
     * Gets a multimap of all the already registered custom items indexed by the item's extended java item's identifier.
     *
     * @return a multimap of all the already registered custom items
     */
    @NonNull
    Map<String, Collection<CustomItemData>> getExistingCustomItems();

    /**
     * Gets the list of the already registered non-vanilla custom items.
     *
     * @return the list of the already registered non-vanilla custom items
     */
    @NonNull
    List<NonVanillaCustomItemData> getExistingNonVanillaCustomItems();

    /**
     * Registers a custom item with a base Java item. This is used to register items with custom textures and properties
     * based on NBT data.
     *
     * @param identifier the base (java) item
     * @param customItemData the custom item data to register
     * @return if the item was registered
     */
    boolean register(@NonNull String identifier, @NonNull CustomItemData customItemData);

    /**
     * Registers a custom item with no base item. This is used for mods.
     *
     * @param customItemData the custom item data to register
     * @return if the item was registered
     */
    boolean register(@NonNull NonVanillaCustomItemData customItemData);
}