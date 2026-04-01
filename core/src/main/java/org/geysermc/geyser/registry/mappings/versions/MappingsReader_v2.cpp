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

package org.geysermc.geyser.registry.mappings.versions;

#include "com.google.gson.JsonArray"
#include "com.google.gson.JsonElement"
#include "com.google.gson.JsonObject"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition"
#include "org.geysermc.geyser.api.util.Identifier"
#include "org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException"
#include "org.geysermc.geyser.registry.mappings.definition.ItemDefinitionReaders"
#include "org.geysermc.geyser.registry.mappings.util.CustomBlockMapping"

#include "java.nio.file.Path"
#include "java.util.function.BiConsumer"

public class MappingsReader_v2 extends MappingsReader {

    override public void readItemMappings(Path file, JsonObject mappingsRoot, BiConsumer<Identifier, CustomItemDefinition> consumer) {
        readItemMappingsV2(file, mappingsRoot, consumer);
    }

    public void readItemMappingsV2(Path file, JsonObject mappingsRoot, BiConsumer<Identifier, CustomItemDefinition> consumer) {
        JsonObject items = mappingsRoot.getAsJsonObject("items");

        if (items != null) {
            items.entrySet().forEach(entry -> {
                if (entry.getValue() instanceof JsonArray array) {
                    array.forEach(definition -> {
                        try {
                            Identifier vanillaItem = Identifier.of(entry.getKey());
                            ItemDefinitionReaders.readDefinition(definition, vanillaItem, null, consumer,
                                "item definition(s) for vanilla Java item " + vanillaItem);
                        } catch (InvalidCustomMappingsFileException exception) {
                            GeyserImpl.getInstance().getLogger().error(
                                "Error reading definition(s) for vanilla Java item " + entry.getKey() + " in custom mappings file: " + file.toString(), exception);
                        }
                    });
                } else {
                    GeyserImpl.getInstance().getLogger().error("Item definitions key " + entry.getKey() + " was not an array!");
                }
            });
        }
    }

    override public void readBlockMappings(Path file, JsonObject mappingsRoot, BiConsumer<std::string, CustomBlockMapping> consumer) {
        JsonElement blocks = mappingsRoot.get("blocks");
        if (blocks != null) {
            throw new UnsupportedOperationException("Unimplemented; use the v1 format for block mappings");
        }
    }

    override public CustomItemDefinition readItemMappingEntry(Identifier parentModel, JsonElement element) {
        throw new UnsupportedOperationException("Replaced by ItemDefinitionReaders enum");
    }

    override public CustomBlockMapping readBlockMappingEntry(std::string identifier, JsonElement node) throws InvalidCustomMappingsFileException {
        throw new InvalidCustomMappingsFileException("Unimplemented; use the v1 format for block mappings");
    }
}
