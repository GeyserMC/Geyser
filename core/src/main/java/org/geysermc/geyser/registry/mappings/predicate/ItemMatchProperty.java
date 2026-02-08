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

package org.geysermc.geyser.registry.mappings.predicate;

import com.google.gson.JsonElement;
import org.geysermc.geyser.api.predicate.MatchPredicate;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.predicate.item.ItemMatchPredicate;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.util.MappingsUtil;
import org.geysermc.geyser.registry.mappings.util.NodeReader;

public enum ItemMatchProperty implements PredicateReader<ItemPredicateContext> {
    CHARGE_TYPE(ItemMatchPredicate::chargeType, NodeReader.CHARGE_TYPE),
    TRIM_MATERIAL(ItemMatchPredicate::trimMaterial, NodeReader.IDENTIFIER),
    CONTEXT_DIMENSION((element, context) -> MatchPredicate.dimension(readValue(element, NodeReader.IDENTIFIER, context))),
    CUSTOM_MODEL_DATA((element, context) -> {
        int index = MappingsUtil.readOrDefault(element, "index", NodeReader.NON_NEGATIVE_INT, 0, context);
        return ItemMatchPredicate.customModelData(index, readValue(element, NodeReader.STRING, context));
    });

    private final PredicateReader<? super ItemPredicateContext> reader;

    ItemMatchProperty(PredicateReader<? super ItemPredicateContext> reader) {
        this.reader = reader;
    }

    <T> ItemMatchProperty(PredicateCreator<? super ItemPredicateContext, T> creator, NodeReader<T> reader) {
        this((element, context) -> creator.create(readValue(element, reader, context)));
    }

    private static <T> T readValue(JsonElement element, NodeReader<T> reader, String... context) throws InvalidCustomMappingsFileException {
        return MappingsUtil.readOrThrow(element, "value", reader, context);
    }

    @Override
    public MinecraftPredicate<? super ItemPredicateContext> read(JsonElement element, String... context) throws InvalidCustomMappingsFileException {
        return reader.read(element, context);
    }
}
