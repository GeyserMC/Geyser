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
import org.geysermc.geyser.api.predicate.item.CustomModelDataFloat;
import org.geysermc.geyser.api.predicate.item.ItemRangeDispatchPredicate;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.util.MappingsUtil;
import org.geysermc.geyser.registry.mappings.util.NodeReader;

import java.util.function.BiConsumer;

public class LegacyDefinitionReader implements ItemDefinitionReader {

    @Override
    public void readDefinition(JsonElement data, Identifier vanillaItem, Identifier parentModel,
                               BiConsumer<Identifier, CustomItemDefinition> consumer) throws InvalidCustomMappingsFileException {
        // TODO ehh code duplication...
        Identifier bedrockIdentifier = MappingsUtil.readOrThrow(data, "bedrock_identifier", NodeReader.GEYSER_IDENTIFIER, "single item definition");
        // We now know the Bedrock identifier, make a base context so that the error can be easily located in the JSON file
        String context = "item definition (bedrock identifier=" + bedrockIdentifier + ")";

        int customModelData = MappingsUtil.readOrThrow(data, "custom_model_data", NodeReader.INT, context);

        CustomItemDefinition.Builder builder = CustomItemDefinition.builder(bedrockIdentifier, vanillaItem);
        builder.predicate(ItemRangeDispatchPredicate.CUSTOM_MODEL_DATA.create(new CustomModelDataFloat(customModelData, 0)));
        SingleDefinitionReader.readDefinitionBase(builder, data, context);
        consumer.accept(vanillaItem, builder.build());
    }
}
