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
import com.google.gson.JsonObject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.components.DataComponentReaders;
import org.geysermc.geyser.registry.mappings.predicate.ItemConditionProperty;
import org.geysermc.geyser.registry.mappings.predicate.ItemMatchProperty;
import org.geysermc.geyser.registry.mappings.predicate.ItemRangeDispatchProperty;
import org.geysermc.geyser.registry.mappings.util.MappingsUtil;
import org.geysermc.geyser.registry.mappings.util.NodeReader;

import java.util.HashSet;
import java.util.Map;
import java.util.function.BiConsumer;

public class SingleDefinitionReader implements ItemDefinitionReader {

    @Override
    public void readDefinition(JsonElement data, Identifier vanillaItem, Identifier parentModel,
                               BiConsumer<Identifier, CustomItemDefinition> consumer) throws InvalidCustomMappingsFileException {
        Identifier bedrockIdentifier = MappingsUtil.readOrThrow(data, "bedrock_identifier", NodeReader.GEYSER_IDENTIFIER, "single item definition");
        // We now know the Bedrock identifier, make a base context so that the error can be easily located in the JSON file
        String context = "item definition (bedrock identifier=" + bedrockIdentifier + ")";

        Identifier model = MappingsUtil.readOrDefault(data, "model", NodeReader.IDENTIFIER, parentModel, context);

        if (model == null) {
            throw new InvalidCustomMappingsFileException("reading item model", "no model present", context);
        }

        CustomItemDefinition.Builder builder = CustomItemDefinition.builder(bedrockIdentifier, model);

        // TODO JSON text component
        MappingsUtil.readIfPresent(data, "display_name", builder::displayName, NodeReader.NON_EMPTY_STRING, context);
        MappingsUtil.readIfPresent(data, "priority", builder::priority, NodeReader.INT, context);

        // Mappings util methods used above already threw a properly formatted error if the element is not a JSON object
        readPredicates(builder, data.getAsJsonObject().get("predicate"), context);
        MappingsUtil.readIfPresent(data, "predicate_strategy", builder::predicateStrategy, NodeReader.PREDICATE_STRATEGY, context);

        builder.bedrockOptions(readBedrockOptions(data.getAsJsonObject().get("bedrock_options"), context));

        JsonElement componentsElement = data.getAsJsonObject().get("components");
        if (componentsElement != null) {
            if (componentsElement instanceof JsonObject components) {
                for (Map.Entry<String, JsonElement> entry : components.entrySet()) {
                    DataComponentReaders.readDataComponent(builder, entry.getKey(), entry.getValue(), context);
                }
            } else {
                throw new InvalidCustomMappingsFileException("reading components", "components key must be an object", context);
            }
        }

        consumer.accept(vanillaItem, builder.build());
    }

    private CustomItemBedrockOptions.Builder readBedrockOptions(JsonElement element, String baseContext) throws InvalidCustomMappingsFileException {
        CustomItemBedrockOptions.Builder builder = CustomItemBedrockOptions.builder();
        if (element == null) {
            return builder;
        }

        String[] context = {"bedrock options", baseContext};
        MappingsUtil.readIfPresent(element, "icon", builder::icon, NodeReader.NON_EMPTY_STRING, context);
        MappingsUtil.readIfPresent(element, "allow_offhand", builder::allowOffhand, NodeReader.BOOLEAN, context);
        MappingsUtil.readIfPresent(element, "display_handheld", builder::displayHandheld, NodeReader.BOOLEAN, context);
        MappingsUtil.readIfPresent(element, "protection_value", builder::protectionValue, NodeReader.NON_NEGATIVE_INT, context);
        MappingsUtil.readIfPresent(element, "creative_category", builder::creativeCategory, NodeReader.CREATIVE_CATEGORY, context);
        MappingsUtil.readIfPresent(element, "creative_group", builder::creativeGroup, NodeReader.NON_EMPTY_STRING, context);
        MappingsUtil.readArrayIfPresent(element, "tags", tags -> builder.tags(new HashSet<>(tags)), NodeReader.IDENTIFIER, context);

        return builder;
    }

    private void readPredicates(CustomItemDefinition.Builder builder, JsonElement element, String context) throws InvalidCustomMappingsFileException {
        if (element == null) {
            return;
        }

        if (element.isJsonObject()) {
            readPredicate(builder, element, context);
        } else if (element.isJsonArray()) {
            for (JsonElement predicate : element.getAsJsonArray()) {
                readPredicate(builder, predicate, context);
            }
        } else {
            throw new InvalidCustomMappingsFileException("reading predicates", "expected predicate key to be a list of predicates or a predicate", context);
        }
    }

    private void readPredicate(CustomItemDefinition.Builder builder, @NonNull JsonElement element, String baseContext) throws InvalidCustomMappingsFileException {
        String type = MappingsUtil.readOrThrow(element, "type", NodeReader.NON_EMPTY_STRING, "predicate", baseContext);
        String[] context = {type + " predicate", baseContext};

        switch (type) {
            case "condition" -> {
                ItemConditionProperty conditionProperty = MappingsUtil.readOrThrow(element, "property", NodeReader.ITEM_CONDITION_PROPERTY, context);
                boolean expected = MappingsUtil.readOrDefault(element, "expected", NodeReader.BOOLEAN, true, context);
                MinecraftPredicate<? super ItemPredicateContext> predicate = conditionProperty.read(element, context);

                if (!expected) {
                    predicate = predicate.negate();
                }
                builder.predicate(predicate);
            }
            case "match" -> {
                ItemMatchProperty matchProperty = MappingsUtil.readOrThrow(element, "property", NodeReader.ITEM_MATCH_PROPERTY, context);
                builder.predicate(matchProperty.read(element, context));
            }
            case "range_dispatch" -> {
                ItemRangeDispatchProperty rangeDispatchProperty = MappingsUtil.readOrThrow(element, "property", NodeReader.ITEM_RANGE_DISPATCH_PROPERTY, context);
                builder.predicate(rangeDispatchProperty.read(element, context));
            }
            default -> throw new InvalidCustomMappingsFileException("reading predicate", "unknown predicate type " + type, context);
        }
    }
}
