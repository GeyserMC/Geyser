/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.session.cache;

import com.nukkitx.protocol.bedrock.data.inventory.ComponentItemData;
import com.nukkitx.protocol.bedrock.packet.StartGamePacket;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import org.geysermc.common.window.SimpleFormWindow;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.utils.ResourcePack;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class ResourcePackCache {

    private SimpleFormWindow form;

    private String resourcePackUrl;
    private String resourcePackHash;

    private ResourcePack bedrockResourcePack;
    private final List<StartGamePacket.ItemEntry> bedrockCustomItems = new ObjectArrayList<>();
    private final List<ComponentItemData> componentData = new ObjectArrayList<>();
    private final Int2ObjectMap<Int2IntMap> javaToCustomModelDataToBedrockId = new Int2ObjectOpenHashMap<>();
    /**
     * Used to reverse search for the item when translating to Java in ItemTranslator
     */
    private final Int2IntMap bedrockCustomIdToProperBedrockId = new Int2IntOpenHashMap();
    /**
     * Used to prevent concurrency issues in case javaToCustomModelDataToBedrockId is inputting items and the client is pre-resource-pack
     */
    private boolean customModelDataActive = false;

    public ResourcePackCache() {

    }

    public List<StartGamePacket.ItemEntry> getAllItems() {
        List<StartGamePacket.ItemEntry> items = new ArrayList<>(ItemRegistry.ITEMS);
        items.addAll(bedrockCustomItems);
        return items;
    }

}
