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

package org.geysermc.geyser.custommodeldata;

import com.fasterxml.jackson.databind.JsonNode;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.custommodeldata.CustomItemData;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

public class MappingsConfigReader {
    public static Path getCustomMappingsDirectory() {
        return GeyserImpl.getInstance().getBootstrap().getConfigFolder().resolve("custom_mappings");
    }

    public static File[] getCustomMappingsFiles() {
        File[] files = getCustomMappingsDirectory().toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return new File[0];
        }
        return files;
    }

    public static CustomItemData readItemMappingEntry(Map.Entry<String, JsonNode> data) {
        if (data == null) {
            return null;
        }

        JsonNode node = data.getValue();
        CustomItemData customItemData = null;

        if (node != null && node.isObject()) {
            if (node.has("name")) {
                Integer customModelData = Integer.parseInt(data.getKey());
                String name = node.get("name").asText();

                customItemData = new CustomItemData(customModelData, name);

                //The next entries are optional
                if (node.has("display_name")) {
                    customItemData.setDisplayName(node.get("display_name").asText());
                }

                if (node.has("is_tool")) {
                    customItemData.setIsTool(node.get("is_tool").asBoolean());
                }

                if (node.has("allow_offhand")) {
                    customItemData.setAllowOffhand(node.get("allow_offhand").asBoolean());
                }

                if (node.has("is_hat")) {
                    customItemData.setIsHat(node.get("is_hat").asBoolean());
                }

                if (node.has("texture_size")) {
                    customItemData.setTextureSize(node.get("texture_size").asInt());
                }
            }
        }

        return customItemData;
    }
}
