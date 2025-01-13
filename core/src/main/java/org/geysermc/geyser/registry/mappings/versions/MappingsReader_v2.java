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
import org.geysermc.geyser.api.item.custom.v2.predicate.ConditionProperty;
import org.geysermc.geyser.api.item.custom.v2.predicate.CustomItemPredicate;
import org.geysermc.geyser.api.item.custom.v2.predicate.RangeDispatchProperty;
import org.geysermc.geyser.api.item.custom.v2.predicate.match.CustomModelDataString;
import org.geysermc.geyser.api.item.custom.v2.predicate.match.MatchPredicateProperty;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.components.DataComponentReaders;
import org.geysermc.geyser.registry.mappings.util.CustomBlockMapping;
import org.geysermc.geyser.registry.mappings.util.MappingsUtil;
import org.geysermc.geyser.registry.mappings.util.NodeReader;
import org.geysermc.geyser.util.MinecraftKey;

import java.nio.file.Path;
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
        JsonNode items = mappingsRoot.get("items");

        if (items != null && items.isObject()) {
            items.fields().forEachRemaining(entry -> {
                if (entry.getValue().isArray()) {
                    entry.getValue().forEach(data -> {
                        try {
                            readItemDefinitionEntry(data, entry.getKey(), null, consumer);
                        } catch (InvalidCustomMappingsFileException exception) {
                            GeyserImpl.getInstance().getLogger().error(
                                "Error reading definition for item " + entry.getKey() + " in custom mappings file: " + file.toString(), exception);
                        }
                    });
                } else {
                    GeyserImpl.getInstance().getLogger().error("Item definitions key " + entry.getKey() + " was not an array!");
                }
            });
        }
    }

    @Override
    public void readBlockMappings(Path file, JsonNode mappingsRoot, BiConsumer<String, CustomBlockMapping> consumer) {
        JsonNode blocksNode = mappingsRoot.get("blocks");
        if (blocksNode != null) {
            throw new UnsupportedOperationException("Unimplemented; use the v1 format of block mappings");
        }
    }

    private void readItemDefinitionEntry(JsonNode data, String itemIdentifier, Identifier model,
                                         BiConsumer<String, CustomItemDefinition> definitionConsumer) throws InvalidCustomMappingsFileException {
        String context = "item definition(s) for Java item " + itemIdentifier;

        String type = MappingsUtil.readOrDefault(data, "type", NodeReader.NON_EMPTY_STRING, "definition", context);
        if (type.equals("group")) {
            // Read model of group if it's present, or default to the model of the parent group, if that's present
            // If the parent group model is not present (or there is no parent group), and this group also doesn't have a model, then it is expected the definitions supply their model themselves
            Identifier groupModel = MappingsUtil.readOrDefault(data, "model", NodeReader.IDENTIFIER, model, context);
            JsonNode definitions = data.get("definitions");
            if (definitions == null || !definitions.isArray()) {
                throw new InvalidCustomMappingsFileException("reading item definitions in group", "group has no definitions key, or it wasn't an array", context);
            } else {
                for (JsonNode definition : definitions) {
                    // Recursively read all the entries in the group - they can be more groups or definitions
                    readItemDefinitionEntry(definition, itemIdentifier, groupModel, definitionConsumer);
                }
            }
        } else if (type.equals("definition")) {
            CustomItemDefinition customItemDefinition = readItemMappingEntry(model, data);
            definitionConsumer.accept(itemIdentifier, customItemDefinition);
        } else {
            throw new InvalidCustomMappingsFileException("reading item definition", "unknown definition type " + type, context);
        }
    }

    @Override
    public CustomItemDefinition readItemMappingEntry(Identifier parentModel, JsonNode node) throws InvalidCustomMappingsFileException {
        if (node == null || !node.isObject()) {
            throw new InvalidCustomMappingsFileException("Invalid item definition entry");
        }

        Identifier bedrockIdentifier = MappingsUtil.readOrThrow(node, "bedrock_identifier", NodeReader.IDENTIFIER, "item definition");
        // We now know the Bedrock identifier, make a base context so that the error can be easily located in the JSON file
        String context = "item definition (bedrock identifier=" + bedrockIdentifier + ")";

        Identifier model = MappingsUtil.readOrDefault(node, "model", NodeReader.IDENTIFIER, parentModel, context);

        if (model == null) {
            throw new InvalidCustomMappingsFileException("reading item model", "no model present", context);
        }

        if (bedrockIdentifier.namespace().equals(Key.MINECRAFT_NAMESPACE)) {
            bedrockIdentifier = new Identifier(Constants.GEYSER_CUSTOM_NAMESPACE, bedrockIdentifier.path()); // Use geyser_custom namespace when no namespace or the minecraft namespace was given
        }
        CustomItemDefinition.Builder builder = CustomItemDefinition.builder(bedrockIdentifier, model);

        MappingsUtil.readTextIfPresent(node, "display_name", builder::displayName, context);
        MappingsUtil.readIfPresent(node, "priority", builder::priority, NodeReader.INT, context);

        readPredicates(builder, node.get("predicate"), context);
        MappingsUtil.readIfPresent(node, "predicate_strategy", builder::predicateStrategy, NodeReader.PREDICATE_STRATEGY, context);

        builder.bedrockOptions(readBedrockOptions(node.get("bedrock_options"), context));

        JsonNode componentsNode = node.get("components");
        if (componentsNode != null) {
            if (componentsNode.isObject()) {
                for (Iterator<Map.Entry<String, JsonNode>> iterator = componentsNode.fields(); iterator.hasNext();) {
                    Map.Entry<String, JsonNode> entry = iterator.next();
                    DataComponentReaders.readDataComponent(builder, MinecraftKey.key(entry.getKey()), entry.getValue(), context);
                }
            } else {
                throw new InvalidCustomMappingsFileException("reading components", "expected components key to be an object", context);
            }
        }

        return builder.build();
    }

    private CustomItemBedrockOptions.Builder readBedrockOptions(JsonNode node, String baseContext) throws InvalidCustomMappingsFileException {
        CustomItemBedrockOptions.Builder builder = CustomItemBedrockOptions.builder();
        if (node == null || !node.isObject()) {
            return builder;
        }

        String[] context = {"bedrock options", baseContext};
        MappingsUtil.readTextIfPresent(node, "icon", builder::icon, context);
        MappingsUtil.readIfPresent(node, "allow_offhand", builder::allowOffhand, NodeReader.BOOLEAN, context);
        MappingsUtil.readIfPresent(node, "display_handheld", builder::displayHandheld, NodeReader.BOOLEAN, context);
        MappingsUtil.readIfPresent(node, "protection_value", builder::protectionValue, NodeReader.NON_NEGATIVE_INT, context);
        MappingsUtil.readIfPresent(node, "creative_category", builder::creativeCategory, NodeReader.CREATIVE_CATEGORY, context);
        MappingsUtil.readTextIfPresent(node, "creative_group", builder::creativeGroup, context);
        MappingsUtil.readIfPresent(node, "texture_size", builder::textureSize, NodeReader.POSITIVE_INT, context);
        MappingsUtil.readIfPresent(node, "render_offsets", builder::renderOffsets, MappingsReader::renderOffsetsFromJsonNode, context);

        if (node.get("tags") instanceof ArrayNode tags) {
            Set<String> tagsSet = new ObjectOpenHashSet<>();
            for (JsonNode tag : tags) {
                tagsSet.add(NodeReader.NON_EMPTY_STRING.read(tag, "reading tag", context));
            }
            builder.tags(tagsSet);
        }

        return builder;
    }

    private void readPredicates(CustomItemDefinition.Builder builder, JsonNode node, String context) throws InvalidCustomMappingsFileException {
        if (node == null) {
            return;
        }

        if (node.isObject()) {
            readPredicate(builder, node, context);
        } else if (node.isArray()) {
            for (JsonNode predicate : node) {
                readPredicate(builder, predicate, context);
            }
        } else {
            throw new InvalidCustomMappingsFileException("reading predicates", "expected predicate key to be a list of predicates or a predicate", context);
        }
    }

    private void readPredicate(CustomItemDefinition.Builder builder, @NonNull JsonNode node, String baseContext) throws InvalidCustomMappingsFileException {
        if (!node.isObject()) {
            throw new InvalidCustomMappingsFileException("reading predicate", "expected predicate to be an object", baseContext);
        }

        String type = MappingsUtil.readOrThrow(node, "type", NodeReader.NON_EMPTY_STRING, "predicate", baseContext);
        String[] context = {type + " predicate", baseContext};

        switch (type) {
            case "condition" -> {
                ConditionProperty conditionProperty = MappingsUtil.readOrThrow(node, "property", NodeReader.CONDITION_PROPERTY, context);
                boolean expected = MappingsUtil.readOrDefault(node, "expected", NodeReader.BOOLEAN, true, context);
                int index = MappingsUtil.readOrDefault(node, "index", NodeReader.NON_NEGATIVE_INT, 0, context);

                // Note that index is only used for the CUSTOM_MODEL_DATA property, but we allow specifying it for other properties anyway
                builder.predicate(CustomItemPredicate.condition(conditionProperty, expected, index));
            }
            case "match" -> {
                String property = MappingsUtil.readOrThrow(node, "property", NodeReader.NON_EMPTY_STRING, context);

                switch (property) {
                    case "charge_type" -> builder.predicate(CustomItemPredicate.match(MatchPredicateProperty.CHARGE_TYPE,
                        MappingsUtil.readOrThrow(node, "value", NodeReader.CHARGE_TYPE, context)));
                    case "trim_material" -> builder.predicate(CustomItemPredicate.match(MatchPredicateProperty.TRIM_MATERIAL,
                        MappingsUtil.readOrThrow(node, "value", NodeReader.IDENTIFIER, context)));
                    case "context_dimension" -> builder.predicate(CustomItemPredicate.match(MatchPredicateProperty.CONTEXT_DIMENSION,
                        MappingsUtil.readOrThrow(node, "value", NodeReader.IDENTIFIER, context)));
                    case "custom_model_data" -> builder.predicate(CustomItemPredicate.match(MatchPredicateProperty.CUSTOM_MODEL_DATA,
                        new CustomModelDataString(MappingsUtil.readOrThrow(node, "value", NodeReader.STRING, context),
                            MappingsUtil.readOrDefault(node, "index", NodeReader.NON_NEGATIVE_INT, 0, context))));
                    default -> throw new InvalidCustomMappingsFileException("reading match predicate", "unknown property " + property, context);
                }
            }
            case "range_dispatch" -> {
                RangeDispatchProperty property = MappingsUtil.readOrThrow(node, "property", NodeReader.RANGE_DISPATCH_PROPERTY, context);

                double threshold = MappingsUtil.readOrThrow(node, "threshold", NodeReader.DOUBLE, context);
                double scale = MappingsUtil.readOrDefault(node, "scale", NodeReader.DOUBLE, 1.0, context);
                boolean normalizeIfPossible = MappingsUtil.readOrDefault(node, "normalize", NodeReader.BOOLEAN, false, context);
                int index = MappingsUtil.readOrDefault(node, "index", NodeReader.NON_NEGATIVE_INT, 0, context);

                builder.predicate(CustomItemPredicate.rangeDispatch(property, threshold, scale, normalizeIfPossible, index));
            }
            default -> throw new InvalidCustomMappingsFileException("reading predicate", "unknown predicate type " + type, context);
        }
    }

    @Override
    public CustomBlockMapping readBlockMappingEntry(String identifier, JsonNode node) throws InvalidCustomMappingsFileException {
        throw new InvalidCustomMappingsFileException("Unimplemented; use the v1 format of block mappings");
    }
}
