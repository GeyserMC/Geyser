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

package org.geysermc.geyser.custom.items;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.geyser.api.custom.items.CustomItemData;
import org.geysermc.geyser.api.custom.items.CustomItemManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeyserCustomItemManager extends CustomItemManager {
    private Map<String, List<CustomItemData>> customMappings = new HashMap<>();
    private Int2ObjectMap<String> customIdMappings = new Int2ObjectOpenHashMap<>();

    @Override
    public void registerCustomItem(String baseItem, CustomItemData customItemData) {
        if (this.customMappings.containsKey(baseItem)) {
            this.customMappings.get(baseItem).add(customItemData);
        } else {
            this.customMappings.put(baseItem, new ArrayList<>(List.of(customItemData)));
        }

        Map<Integer, String> out = CustomItemsRegistryPopulator.addToRegistry(baseItem, customItemData);
        if (out != null) {
            this.customIdMappings.putAll(out);
        }
    }

    @Override
    public List<CustomItemData> customItemData(String baseItem) {
        return this.customMappings.get(baseItem);
    }

    @Override
    public Map<String, List<CustomItemData>> customMappings() {
        return this.customMappings;
    }

    @Override
    public String itemStringFromId(Integer id) {
        return this.customIdMappings.get(id);
    }
}
