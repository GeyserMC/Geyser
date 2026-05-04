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

package org.geysermc.geyser.registry.mappings.util;

import com.google.gson.JsonPrimitive;
import org.geysermc.geyser.Constants;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaConsumable;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaEquippable;
import org.geysermc.geyser.api.predicate.PredicateStrategy;
import org.geysermc.geyser.api.predicate.context.item.ChargedProjectile;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.definition.ItemDefinitionReaders;
import org.geysermc.geyser.registry.mappings.predicate.ItemConditionProperty;
import org.geysermc.geyser.registry.mappings.predicate.ItemMatchProperty;
import org.geysermc.geyser.registry.mappings.predicate.ItemRangeDispatchProperty;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

@FunctionalInterface
public interface NodeReader<T> {

    NodeReader<Integer> INT = node -> {
        double i = node.getAsDouble();
        if (i == (int) i) { // Make sure the number is round
            return (int) i;
        }
        throw new InvalidCustomMappingsFileException("expected node to be an integer");
    };

    NodeReader<Integer> NON_NEGATIVE_INT = INT.validate(i -> i >= 0, "integer must be non-negative");

    NodeReader<Integer> POSITIVE_INT = INT.validate(i -> i > 0, "integer must be positive");

    NodeReader<Double> DOUBLE = JsonPrimitive::getAsDouble;

    NodeReader<Double> NON_NEGATIVE_DOUBLE = DOUBLE.validate(d -> d >= 0, "number must be non-negative");

    NodeReader<Double> POSITIVE_DOUBLE = DOUBLE.validate(d -> d > 0, "number must be positive");

    NodeReader<Float> FLOAT = DOUBLE.andThen(Double::floatValue);

    NodeReader<Boolean> BOOLEAN = node -> {
        // Not directly using getAsBoolean here since that doesn't convert integers and doesn't throw an error when the string is not "true" or "false"
        if (node.isString()) {
            String s = node.getAsString();
            if (s.equals("true")) {
                return true;
            } else if (s.equals("false")) {
                return false;
            }
        } else if (node.isNumber() && node.getAsNumber() instanceof Integer i) {
            if (i == 1) {
                return true;
            } else if (i == 0) {
                return false;
            }
        } else if (node.isBoolean()) {
            return node.getAsBoolean();
        }
        throw new InvalidCustomMappingsFileException("expected node to be a boolean");
    };

    NodeReader<String> STRING = JsonPrimitive::getAsString;

    NodeReader<String> NON_EMPTY_STRING = STRING.validate(s -> !s.isEmpty(), "string must not be empty");

    NodeReader<Identifier> IDENTIFIER = NON_EMPTY_STRING.andThen(Identifier::of);

    NodeReader<Identifier> GEYSER_IDENTIFIER = NON_EMPTY_STRING.validate(s -> !s.startsWith("minecraft:"), "namespace cannot be minecraft")
        .andThen(Identifier::of)
        .andThen(identifier -> {
            if (identifier.namespace().equals(Identifier.DEFAULT_NAMESPACE)) {
                return Identifier.of(Constants.GEYSER_CUSTOM_NAMESPACE, identifier.path());
            }
            return identifier;
        });

    NodeReader<Identifier> TAG = NON_EMPTY_STRING.andThen(s -> {
        if (s.startsWith("#")) {
            return s.replaceFirst("#", "");
        }
        throw new InvalidCustomMappingsFileException("tag must start with a #");
    }).andThen(Identifier::of);

    NodeReader<CreativeCategory> CREATIVE_CATEGORY = NON_EMPTY_STRING.andThen(CreativeCategory::fromName).validate(Objects::nonNull, "unknown creative category");

    NodeReader<ItemDefinitionReaders> ITEM_DEFINITION_READER = ofEnum(ItemDefinitionReaders.class);

    NodeReader<PredicateStrategy> PREDICATE_STRATEGY = ofEnum(PredicateStrategy.class);

    NodeReader<ItemConditionProperty> ITEM_CONDITION_PROPERTY = ofEnum(ItemConditionProperty.class);

    NodeReader<ItemMatchProperty> ITEM_MATCH_PROPERTY = ofEnum(ItemMatchProperty.class);

    NodeReader<ItemRangeDispatchProperty> ITEM_RANGE_DISPATCH_PROPERTY = ofEnum(ItemRangeDispatchProperty.class);

    NodeReader<ChargedProjectile.ChargeType> CHARGE_TYPE = ofEnum(ChargedProjectile.ChargeType.class);

    NodeReader<JavaConsumable.Animation> CONSUMABLE_ANIMATION = ofEnum(JavaConsumable.Animation.class);

    NodeReader<JavaEquippable.EquipmentSlot> EQUIPMENT_SLOT = ofEnum(JavaEquippable.EquipmentSlot.class);

    static <E extends Enum<E>> NodeReader<E> ofEnum(Class<E> clazz) {
        return NON_EMPTY_STRING.andThen(String::toUpperCase).andThen(s -> {
            try {
                return Enum.valueOf(clazz, s);
            } catch (IllegalArgumentException exception) {
                throw new InvalidCustomMappingsFileException("unknown element in enum " + clazz.getSimpleName() + ", must be one of ["
                    + String.join(", ", Arrays.stream(clazz.getEnumConstants()).map(E::toString).toArray(String[]::new)).toLowerCase() + "]");
            }
        });
    }

    static <T> NodeReader<T> ofMap(Map<String, T> map) {
        return NON_EMPTY_STRING.andThen(String::toLowerCase).andThen(s -> {
            T value = map.get(s);
            if (value == null) {
                throw new InvalidCustomMappingsFileException("unknown element, must be one of ["
                    + String.join(", ", map.keySet()).toLowerCase() + "]");
            }
            return value;
        });
    }

    static NodeReader<Integer> boundedInt(int min, int max) {
        return INT.validate(i -> i >= min && i <= max, "integer must be in range [" + min + ", " + max + "]");
    }

    static NodeReader<Double> boundedDouble(double min, double max) {
        return DOUBLE.validate(d -> d >= min && d <= max, "number must be in range [" + min + ", " + max + "]");
    }

    /**
     * {@link NodeReader#read(JsonPrimitive, String, String...)} is preferably used as that properly formats the error when one is thrown.
     */
    T read(JsonPrimitive node) throws InvalidCustomMappingsFileException;

    default T read(JsonPrimitive node, String task, String... context) throws InvalidCustomMappingsFileException {
        try {
            return read(node);
        } catch (Exception exception) {
            throw new InvalidCustomMappingsFileException(task, exception.getMessage() + " (node was " + node.toString() + ")", context);
        }
    }

    default <V> NodeReader<V> andThen(After<T, V> after) {
        return node -> after.apply(read(node));
    }

    default NodeReader<T> validate(Predicate<T> validator, String error) {
        return andThen(v -> {
            if (!validator.test(v)) {
                throw new InvalidCustomMappingsFileException(error);
            }
            return v;
        });
    }

    @FunctionalInterface
    interface After<V, T> {

        T apply(V value) throws InvalidCustomMappingsFileException;
    }
}
