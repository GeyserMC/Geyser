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
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.DefaultComponentSerializer;

import java.util.HashSet;
import java.util.Map;
import java.util.function.BiConsumer;

public class SingleDefinitionReader implements ItemDefinitionReader {

    @Override
    public void readDefinition(JsonElement data, Identifier vanillaItem, Identifier parentModel,
                               BiConsumer<Identifier, CustomItemDefinition> consumer) throws InvalidCustomMappingsFileException {
        Identifier bedrockIdentifier = ItemDefinitionReader.readBedrockIdentifier(data, "single item definition");
        // We now know the Bedrock identifier, make a base context so that the error can be easily located in the JSON file
        String context = "item definition (bedrock identifier=" + bedrockIdentifier + ")";

        Identifier model = MappingsUtil.readOrDefault(data, "model", NodeReader.IDENTIFIER, parentModel, context);
        if (model == null) {
            throw new InvalidCustomMappingsFileException("reading item model", "no model present", context);
        }

        CustomItemDefinition.Builder builder = CustomItemDefinition.builder(bedrockIdentifier, model);
        readDefinitionBase(builder, data, context);
        consumer.accept(vanillaItem, builder.build());
    }

    public static void readDefinitionBase(CustomItemDefinition.Builder builder, JsonElement data, String context) throws InvalidCustomMappingsFileException {
        MappingsUtil.readIfPresent(data, "priority", builder::priority, NodeReader.INT, context);

        // Mappings util method used above already threw a properly formatted error if the element is not a JSON object
        JsonObject definition = data.getAsJsonObject();

        JsonElement displayName = definition.get("display_name");
        if (displayName != null) {
            try {
                builder.displayName(MessageTranslator.convertMessage(DefaultComponentSerializer.get().deserializeFromTree(displayName), GeyserLocale.getDefaultLocale()));
            } catch (Exception exception) {
                throw new InvalidCustomMappingsFileException("reading display name", "error while reading: " + exception.getMessage(), context);
            }
        }

        readPredicates(builder, definition, context);
        readBedrockOptions(builder, definition, context);
        readComponents(builder, definition, context);
    }

    private static void readPredicates(CustomItemDefinition.Builder builder, JsonObject definition, String context) throws InvalidCustomMappingsFileException {
        MappingsUtil.readIfPresent(definition, "predicate_strategy", builder::predicateStrategy, NodeReader.PREDICATE_STRATEGY, context);
        JsonElement predicates = definition.get("predicate");
        if (predicates == null) {
            return;
        }

        if (predicates.isJsonObject()) {
            readPredicate(builder, predicates, context);
        } else if (predicates.isJsonArray()) {
            for (JsonElement predicate : predicates.getAsJsonArray()) {
                readPredicate(builder, predicate, context);
            }
        } else {
            throw new InvalidCustomMappingsFileException("reading predicates", "expected predicate key to be a list of predicates or a predicate", context);
        }
    }

    private static void readPredicate(CustomItemDefinition.Builder builder, @NonNull JsonElement element, String baseContext) throws InvalidCustomMappingsFileException {
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

    private static void readBedrockOptions(CustomItemDefinition.Builder definitionBuilder, JsonObject definition, String baseContext) throws InvalidCustomMappingsFileException {
        CustomItemBedrockOptions.Builder builder = CustomItemBedrockOptions.builder();
        JsonElement bedrockOptions = definition.get("bedrock_options");
        if (bedrockOptions == null) {
            return;
        }

        String[] context = {"bedrock options", baseContext};
        MappingsUtil.readIfPresent(bedrockOptions, "icon", builder::icon, NodeReader.NON_EMPTY_STRING, context);
        MappingsUtil.readIfPresent(bedrockOptions, "allow_offhand", builder::allowOffhand, NodeReader.BOOLEAN, context);
        MappingsUtil.readIfPresent(bedrockOptions, "display_handheld", builder::displayHandheld, NodeReader.BOOLEAN, context);
        MappingsUtil.readIfPresent(bedrockOptions, "protection_value", builder::protectionValue, NodeReader.NON_NEGATIVE_INT, context);
        MappingsUtil.readIfPresent(bedrockOptions, "creative_category", builder::creativeCategory, NodeReader.CREATIVE_CATEGORY, context);
        MappingsUtil.readIfPresent(bedrockOptions, "creative_group", builder::creativeGroup, NodeReader.NON_EMPTY_STRING, context);
        MappingsUtil.readArrayIfPresent(bedrockOptions, "tags", tags -> builder.tags(new HashSet<>(tags)), NodeReader.IDENTIFIER, context);

        definitionBuilder.bedrockOptions(builder);
    }

    private static void readComponents(CustomItemDefinition.Builder builder, JsonObject definition, String context) throws InvalidCustomMappingsFileException {
        JsonElement componentsElement = definition.get("components");
        if (componentsElement != null) {
            if (componentsElement instanceof JsonObject components) {
                for (Map.Entry<String, JsonElement> entry : components.entrySet()) {
                    DataComponentReaders.readDataComponent(builder, entry.getKey(), entry.getValue(), context);
                }
            } else {
                throw new InvalidCustomMappingsFileException("reading components", "components key must be an object", context);
            }
        }
    }
}
