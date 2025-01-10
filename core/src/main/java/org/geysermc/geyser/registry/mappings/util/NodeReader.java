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

import com.fasterxml.jackson.databind.JsonNode;
import org.geysermc.geyser.api.item.custom.v2.predicate.ConditionProperty;
import org.geysermc.geyser.api.item.custom.v2.predicate.PredicateStrategy;
import org.geysermc.geyser.api.item.custom.v2.predicate.RangeDispatchProperty;
import org.geysermc.geyser.api.item.custom.v2.predicate.match.ChargeType;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable;

import java.util.Objects;
import java.util.function.Predicate;

@FunctionalInterface
public interface NodeReader<T> {

    NodeReader<Integer> INT = node -> {
        if (!node.isTextual() && !node.isIntegralNumber()) {
            throw new InvalidCustomMappingsFileException("expected node to be an integer");
        }
        return node.isTextual() ? Integer.parseInt(node.textValue()) : node.intValue(); // Not using asInt because that catches the exception parseInt throws, which we don't want
    };

    NodeReader<Integer> NON_NEGATIVE_INT = INT.validate(i -> i >= 0, "integer must be non-negative");

    NodeReader<Integer> POSITIVE_INT = INT.validate(i -> i > 0, "integer must be positive");

    NodeReader<Double> DOUBLE = node -> {
        if (!node.isTextual() && !node.isNumber()) {
            throw new InvalidCustomMappingsFileException("expected node to be a number");
        }
        return node.isTextual() ? Double.parseDouble(node.textValue()) : node.doubleValue(); // Not using asDouble because that catches the exception parseDouble throws, which we don't want
    };

    NodeReader<Double> NON_NEGATIVE_DOUBLE = DOUBLE.validate(d -> d >= 0, "number must be non-negative");

    NodeReader<Double> POSITIVE_DOUBLE = DOUBLE.validate(d -> d > 0, "number must be positive");

    NodeReader<Boolean> BOOLEAN = node -> {
        if (node.isTextual()) {
            String s = node.textValue();
            if (s.equals("true")) {
                return true;
            } else if (s.equals("false")) {
                return false;
            }
        } else if (node.isIntegralNumber()) {
            int i = node.intValue();
            if (i == 1) {
                return true;
            } else if (i == 0) {
                return false;
            }
        } else if (node.isBoolean()) {
            return node.booleanValue();
        }
        throw new InvalidCustomMappingsFileException("expected node to be a boolean");
    };

    NodeReader<String> STRING = node -> {
        if (!node.isTextual() && !node.isNumber() && !node.isBoolean()) {
            throw new InvalidCustomMappingsFileException("expected node to be a string");
        }
        return node.asText();
    };

    NodeReader<String> NON_EMPTY_STRING = STRING.validate(s -> !s.isEmpty(), "string must not be empty");

    NodeReader<Identifier> IDENTIFIER = NON_EMPTY_STRING.andThen(Identifier::new);

    NodeReader<CreativeCategory> CREATIVE_CATEGORY = NON_EMPTY_STRING.andThen(CreativeCategory::fromName).validate(Objects::nonNull, "unknown creative category");

    NodeReader<PredicateStrategy> PREDICATE_STRATEGY = ofEnum(PredicateStrategy.class);

    NodeReader<ConditionProperty> CONDITION_PROPERTY = ofEnum(ConditionProperty.class);

    NodeReader<ChargeType> CHARGE_TYPE = ofEnum(ChargeType.class);

    NodeReader<RangeDispatchProperty> RANGE_DISPATCH_PROPERTY = ofEnum(RangeDispatchProperty.class);

    NodeReader<Consumable.ItemUseAnimation> ITEM_USE_ANIMATION = ofEnum(Consumable.ItemUseAnimation.class);

    static <E extends Enum<E>> NodeReader<E> ofEnum(Class<E> clazz) {
        return NON_EMPTY_STRING.andThen(String::toUpperCase).andThen(s -> {
            try {
                return Enum.valueOf(clazz, s);
            } catch (IllegalArgumentException exception) {
                throw new InvalidCustomMappingsFileException("unknown element");
            }
        });
    }

    static NodeReader<Integer> boundedInt(int min, int max) {
        return INT.validate(i -> i >= min && i <= max, "integer must be in range [" + min + ", " + max + "]");
    }

    T read(JsonNode node) throws InvalidCustomMappingsFileException;

    default T read(JsonNode node, String task, String... context) throws InvalidCustomMappingsFileException {
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
