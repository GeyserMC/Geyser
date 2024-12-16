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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.predicate.ConditionPredicate;
import org.geysermc.geyser.api.item.custom.v2.predicate.match.CustomModelDataString;
import org.geysermc.geyser.api.item.custom.v2.predicate.RangeDispatchPredicate;
import org.geysermc.geyser.api.item.custom.v2.predicate.match.ChargeType;
import org.geysermc.geyser.api.item.custom.v2.predicate.MatchPredicate;
import org.geysermc.geyser.api.item.custom.v2.predicate.match.MatchPredicateProperty;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.components.DataComponentReaders;
import org.geysermc.geyser.registry.mappings.util.CustomBlockMapping;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class MappingsReader_v2 extends MappingsReader {

    @Override
    public void readItemMappings(Path file, JsonNode mappingsRoot, BiConsumer<String, CustomItemDefinition> consumer) {
        readItemMappingsV2(file, mappingsRoot, consumer);
    }

    public void readItemMappingsV2(Path file, JsonNode mappingsRoot, BiConsumer<String, CustomItemDefinition> consumer) {
        // TODO - do we want to continue allowing type conversions, or do we want to enforce strict typing in JSON mappings?
        // E.g., do we want to allow reading "210" as 210 and reading 1 as true, or do we throw exceptions in that case?
        JsonNode itemModels = mappingsRoot.get("items");

        if (itemModels != null && itemModels.isObject()) {
            itemModels.fields().forEachRemaining(entry -> {
                if (entry.getValue().isArray()) {
                    entry.getValue().forEach(data -> {
                        try {
                            readItemDefinitionEntry(data, entry.getKey(), null, consumer);
                        } catch (InvalidCustomMappingsFileException exception) {
                            GeyserImpl.getInstance().getLogger().error(
                                "Error reading definition for item " + entry.getKey() + " in custom mappings file: " + file.toString(), exception);
                        }
                    });
                }
            });
        }
    }

    @Override
    public void readBlockMappings(Path file, JsonNode mappingsRoot, BiConsumer<String, CustomBlockMapping> consumer) {
        // TODO
    }

    private void readItemDefinitionEntry(JsonNode data, String itemIdentifier, String model,
                                         BiConsumer<String, CustomItemDefinition> definitionConsumer) throws InvalidCustomMappingsFileException {
        String type = readOrThrow(data, "type", JsonNode::asText, "Missing type key in definition");
        if (type.equals("group")) {
            // Read model of group if it's present, or default to the model of the parent group, if that's present
            // If the parent group model is not present (or there is no parent group), and this group also doesn't have a model, then it is expected the definitions supply their model themselves
            String groupModel = readOrDefault(data, "model", JsonNode::asText, model);
            JsonNode definitions = data.get("definitions");
            if (definitions == null || !definitions.isArray()) {
                throw new InvalidCustomMappingsFileException("An item entry group has no definitions key, or it wasn't an array");
            } else {
                for (JsonNode definition : definitions) {
                    readItemDefinitionEntry(definition, itemIdentifier, groupModel, definitionConsumer);
                }
            }
        } else if (type.equals("definition")) {
            CustomItemDefinition customItemDefinition = readItemMappingEntry(model, data);
            definitionConsumer.accept(itemIdentifier, customItemDefinition);
        } else {
            throw new InvalidCustomMappingsFileException("Unknown definition type " + type);
        }
    }

    @Override
    public CustomItemDefinition readItemMappingEntry(String itemModel, JsonNode node) throws InvalidCustomMappingsFileException {
        if (node == null || !node.isObject()) {
            throw new InvalidCustomMappingsFileException("Invalid item definition entry");
        }

        JsonNode bedrockIdentifierNode = node.get("bedrock_identifier");

        JsonNode modelNode = node.get("model");
        String model = itemModel != null || modelNode == null || !modelNode.isTextual() ? itemModel : modelNode.asText();

        if (bedrockIdentifierNode == null || !bedrockIdentifierNode.isTextual() || bedrockIdentifierNode.asText().isEmpty()) {
            throw new InvalidCustomMappingsFileException("An item definition has no bedrock identifier");
        }
        if (model == null || model.isEmpty()) {
            throw new InvalidCustomMappingsFileException("An item definition has no model");
        }

        Identifier bedrockIdentifier = new Identifier(bedrockIdentifierNode.asText());
        if (bedrockIdentifier.namespace().equals(Key.MINECRAFT_NAMESPACE)) {
            bedrockIdentifier = new Identifier(Constants.GEYSER_CUSTOM_NAMESPACE, bedrockIdentifier.path());
        }
        CustomItemDefinition.Builder builder = CustomItemDefinition.builder(bedrockIdentifier, new Identifier(model));

        // We now know the Bedrock identifier, put it in the exception message so that the error can be easily located in the JSON file
        try {
            readTextIfPresent(node, "display_name", builder::displayName);
            readIfPresent(node, "priority", builder::priority, JsonNode::asInt);

            readPredicates(builder, node.get("predicate"));

            builder.bedrockOptions(readBedrockOptions(node.get("bedrock_options")));

            JsonNode componentsNode = node.get("components");
            if (componentsNode != null && componentsNode.isObject()) {
                DataComponents components = new DataComponents(new HashMap<>()); // TODO faster map ?
                for (Iterator<Map.Entry<String, JsonNode>> iterator = componentsNode.fields(); iterator.hasNext();) {
                    Map.Entry<String, JsonNode> entry = iterator.next();
                    try {
                        DataComponentReaders.readDataComponent(components, MinecraftKey.key(entry.getKey()), entry.getValue());
                    } catch (InvalidCustomMappingsFileException exception) {
                        throw new InvalidCustomMappingsFileException("While reading data component " + entry.getKey() + ": " + exception.getMessage());
                    }
                }
                builder.components(components);
            }
        } catch (InvalidCustomMappingsFileException exception) {
            throw new InvalidCustomMappingsFileException("While reading item definition (bedrock identifier=" + bedrockIdentifier + "): " + exception.getMessage());
        }

        return builder.build();
    }

    private CustomItemBedrockOptions.Builder readBedrockOptions(JsonNode node) {
        CustomItemBedrockOptions.Builder builder = CustomItemBedrockOptions.builder();
        if (node == null || !node.isObject()) {
            return builder;
        }

        readTextIfPresent(node, "icon", builder::icon);
        readIfPresent(node, "creative_category", builder::creativeCategory, category -> CreativeCategory.fromName(category.asText()));
        readTextIfPresent(node, "creative_group", builder::creativeGroup);
        readIfPresent(node, "allow_offhand", builder::allowOffhand, JsonNode::asBoolean);
        readIfPresent(node, "display_handheld", builder::displayHandheld, JsonNode::asBoolean);
        readIfPresent(node, "texture_size", builder::textureSize, JsonNode::asInt);
        readIfPresent(node, "render_offsets", builder::renderOffsets, MappingsReader::renderOffsetsFromJsonNode);

        if (node.get("tags") instanceof ArrayNode tags) {
            Set<String> tagsSet = new ObjectOpenHashSet<>();
            tags.forEach(tag -> tagsSet.add(tag.asText()));
            builder.tags(tagsSet);
        }

        return builder;
    }

    private void readPredicates(CustomItemDefinition.Builder builder, JsonNode node) throws InvalidCustomMappingsFileException {
        if (node == null) {
            return;
        }

        if (node.isObject()) {
            readPredicate(builder, node);
        } else if (node.isArray()) {
            node.forEach(predicate -> {
                try {
                    readPredicate(builder, predicate);
                } catch (InvalidCustomMappingsFileException e) {
                    GeyserImpl.getInstance().getLogger().error("Error in reading predicate", e); // TODO log this better
                }
            });
        } else {
            throw new InvalidCustomMappingsFileException("Expected predicate key to be a list of predicates or a predicate");
        }
    }

    private void readPredicate(CustomItemDefinition.Builder builder, @NonNull JsonNode node) throws InvalidCustomMappingsFileException {
        if (!node.isObject()) {
            throw new InvalidCustomMappingsFileException("Expected predicate to be an object");
        }

        String type = readOrThrow(node, "type", JsonNode::asText, "Predicate requires type key");
        String property = readOrThrow(node, "property", JsonNode::asText, "Predicate requires property key");

        switch (type) {
            case "condition" -> {
                try {
                    ConditionPredicate.ConditionProperty conditionProperty = ConditionPredicate.ConditionProperty.valueOf(property.toUpperCase());
                    JsonNode expected = node.get("expected");

                    // Note that index is only used for the CUSTOM_MODEL_DATA property, but we allow specifying it for other properties anyway
                    builder.predicate(new ConditionPredicate(conditionProperty,
                        expected == null || expected.asBoolean(), readOrDefault(node, "index", JsonNode::asInt, 0)));
                } catch (IllegalArgumentException exception) {
                    throw new InvalidCustomMappingsFileException("Unknown property " + property);
                }
            }
            case "match" -> {
                String value = readOrThrow(node, "value", JsonNode::asText, "Predicate requires value key");

                switch (property) {
                    case "charge_type" -> {
                        try {
                            ChargeType chargeType = ChargeType.valueOf(value.toUpperCase());
                            builder.predicate(new MatchPredicate<>(MatchPredicateProperty.CHARGE_TYPE, chargeType));
                        } catch (IllegalArgumentException exception) {
                            throw new InvalidCustomMappingsFileException("Unknown charge type " + value);
                        }
                    }
                    case "trim_material" -> builder.predicate(new MatchPredicate<>(MatchPredicateProperty.TRIM_MATERIAL, new Identifier(value)));
                    case "context_dimension" -> builder.predicate(new MatchPredicate<>(MatchPredicateProperty.CONTEXT_DIMENSION, new Identifier(value)));
                    case "custom_model_data" -> {
                        builder.predicate(new MatchPredicate<>(MatchPredicateProperty.CUSTOM_MODEL_DATA,
                            new CustomModelDataString(value, readOrDefault(node, "index", JsonNode::asInt, 0))));
                    }
                    default -> throw new InvalidCustomMappingsFileException("Unknown property " + property);
                }
            }
            case "range_dispatch" -> {
                double threshold = readOrThrow(node, "threshold", JsonNode::asDouble, "Predicate requires threshold key");
                JsonNode scaleNode = node.get("scale");
                double scale = 1.0;
                if (scaleNode != null && scaleNode.isNumber()) {
                    scale = scaleNode.asDouble();
                }

                JsonNode normalizeNode = node.get("normalize");
                boolean normalizeIfPossible = normalizeNode != null && normalizeNode.booleanValue();

                int index = readOrDefault(node, "index", JsonNode::asInt, 0);

                try {
                    RangeDispatchPredicate.RangeDispatchProperty rangeDispatchProperty = RangeDispatchPredicate.RangeDispatchProperty.valueOf(property.toUpperCase());
                    builder.predicate(new RangeDispatchPredicate(rangeDispatchProperty, threshold, scale, normalizeIfPossible, index));
                } catch (IllegalArgumentException exception) {
                    throw new InvalidCustomMappingsFileException("Unknown property " + property);
                }
            }
            default -> throw new InvalidCustomMappingsFileException("Unknown predicate type " + type);
        }
    }

    @Override
    public CustomBlockMapping readBlockMappingEntry(String identifier, JsonNode node) throws InvalidCustomMappingsFileException {
        return null; // TODO
    }
}
