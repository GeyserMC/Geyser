/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.mappings.definition;

import com.google.gson.JsonElement;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.util.MappingsUtil;
import org.geysermc.geyser.registry.mappings.util.NodeReader;

import java.util.function.BiConsumer;

public class GroupDefinitionReader implements ItemDefinitionReader {

    @Override
    public void readDefinition(JsonElement data, Identifier vanillaItem, Identifier parentModel,
                               BiConsumer<Identifier, CustomItemDefinition> consumer) throws InvalidCustomMappingsFileException {
        String context = "group item definition";

        // Read model of group if it's present, or default to the model of the parent group, if that's present
        // If the parent group model is not present (or there is no parent group), and this group also doesn't have a model, then it is expected the definitions supply their model themselves
        Identifier groupModel = MappingsUtil.readOrDefault(data, "model", NodeReader.IDENTIFIER, parentModel, context);

        // The method above should have already thrown a properly formatted error if data is not a JSON object
        JsonElement definitions = data.getAsJsonObject().get("definitions");

        // TODO could we make a definition node reader?
        if (definitions == null || !definitions.isJsonArray()) {
            throw new InvalidCustomMappingsFileException("reading item definitions in group", "group has no definitions key, or it wasn't an array", context);
        } else {
            for (JsonElement definition : definitions.getAsJsonArray()) {
                // Recursively read all the entries in the group - they can be more groups or definitions
                ItemDefinitionReaders.readDefinition(definition, vanillaItem, groupModel, consumer);
            }
        }
    }
}
