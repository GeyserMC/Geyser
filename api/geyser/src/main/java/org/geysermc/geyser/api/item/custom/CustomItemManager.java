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

package org.geysermc.geyser.api.item.custom;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;

import java.util.List;
import java.util.Map;

/**
 * This class is used to manage custom item data.
 */
public abstract class CustomItemManager {
    /**
     * Registers a custom item with a base java item. This is used for custom model data etc. Note that you can only register custom items in the {@link GeyserPreInitializeEvent},
     * and if add non-Bedrock items is not disabled in the config.
     *
     * @param identifier the base (java) item
     * @param customItemData the custom item data to register
     * @return if the item was registered
     */
    public abstract boolean registerCustomItem(@NonNull String identifier, @NonNull CustomItemData customItemData);

    /**
     * Registers a custom item with no base item. This is used for mods. Note that you can only register custom items in the {@link GeyserPreInitializeEvent},
     * and if add non-Bedrock items is not disabled in the config.
     *
     * @param customItemData the custom item data to register
     * @return if the item was registered
     */
    public abstract boolean registerCustomItem(@NonNull FullyCustomItemData customItemData);

    /**
     * Gets the custom item data for the given base item.
     *
     * @param identifier the base item
     * @return the custom item data
     */
    public abstract @NonNull List<CustomItemData> customItemData(@NonNull String identifier);

    /**
     * Gets all the custom item mappings.
     *
     * @return all the custom item mappings
     */
    public abstract @NonNull Map<String, List<CustomItemData>> itemMappings();
}
