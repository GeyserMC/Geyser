/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 *  @author GeyserMC
 *  @link https://github.com/GeyserMC/Geyser
 *
 */

package org.geysermc.connector.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.nukkitx.protocol.bedrock.data.CraftingData;
import com.nukkitx.protocol.bedrock.data.ItemData;
import com.nukkitx.protocol.bedrock.packet.CraftingDataPacket;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.network.session.GeyserSession;

import java.io.InputStream;
import java.util.Iterator;
import java.util.UUID;

public class RecipeUtils {

    public static void sendCustomRecipes(GeyserSession session) {

        InputStream stream = FileUtils.getResource("recipes.json");

        JsonNode items;
        try {
            items = GeyserConnector.JSON_MAPPER.readTree(stream);
        } catch (Exception e) {
            throw new AssertionError("Unable to load Java runtime item IDs", e);
        }

        CraftingDataPacket packet = new CraftingDataPacket();
        packet.setCleanRecipes(true);

        for (JsonNode entry : items) {
            UUID uuid = UUID.randomUUID();
            ItemData output = itemFromJson(entry.get("output"));
            Iterator<JsonNode> inputIterator = entry.get("inputs").iterator();
            ItemData[] inputs = new ItemData[entry.get("inputs").size()];
            int i = 0;
            while (inputIterator.hasNext()) {
                JsonNode jsonInput = inputIterator.next();
                inputs[i] = itemFromJson(jsonInput);
                i++;
            }
            if (entry.get("shapeless").asBoolean()) {
                packet.getCraftingData().add(CraftingData.fromShapeless(uuid.toString(), inputs, new ItemData[]{output}, uuid, "crafting_table", 0));
            } else {
                packet.getCraftingData().add(CraftingData.fromShaped(
                        uuid.toString(), entry.get("width").asInt(), entry.get("height").asInt(), inputs, new ItemData[]{output}, uuid, "crafting_table", 0));
            }
        }
        session.sendUpstreamPacket(packet);
    }

    private static ItemData itemFromJson(JsonNode node) {
        return ItemData.of(node.get("id").asInt(), (short) node.get("damage").asInt(), node.get("count").asInt());
    }

}
