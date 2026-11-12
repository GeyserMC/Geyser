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

package org.geysermc.geyser.registry.mappings.versions.item;

import com.google.gson.JsonArray;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.MappingsReader;
import org.geysermc.geyser.registry.mappings.definition.ItemDefinitionReaders;

public class ItemMappingsReader_v2 implements MappingsReader<Identifier, CustomItemDefinition> {

    @Override
    public void read(Context<Identifier, CustomItemDefinition> context) {
        context.forEachMapping((key, value) -> {
            if (value instanceof JsonArray array) {
                array.forEach(definition -> {
                    try {
                        Identifier vanillaItem = Identifier.of(key);
                        ItemDefinitionReaders.readDefinition(definition, vanillaItem, null, context,
                            "item definition(s) for vanilla Java item " + vanillaItem);
                    } catch (InvalidCustomMappingsFileException exception) {
                        context.reportError("failed reading definition(s) for vanilla Java item " + key + ": " + exception.getMessage());
                    }
                });
            } else {
                context.reportError("item definitions key " + key + " was not an array!");
            }
        });
    }
}
