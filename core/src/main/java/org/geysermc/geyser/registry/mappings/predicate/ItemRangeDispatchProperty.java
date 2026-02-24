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
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.predicate.item.ItemRangeDispatchPredicate;
import org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException;
import org.geysermc.geyser.registry.mappings.util.MappingsUtil;
import org.geysermc.geyser.registry.mappings.util.NodeReader;

public enum ItemRangeDispatchProperty implements PredicateReader<ItemPredicateContext> {
    BUNDLE_FULLNESS(ItemRangeDispatchPredicate::bundleFullness),
    DAMAGE(ItemRangeDispatchPredicate::damage, ItemRangeDispatchPredicate::normalizedDamage),
    COUNT(ItemRangeDispatchPredicate::count, ItemRangeDispatchPredicate::normalizedCount),
    CUSTOM_MODEL_DATA((element, context) -> {
        int index = MappingsUtil.readOrDefault(element, "index", NodeReader.NON_NEGATIVE_INT, 0, context);
        return ItemRangeDispatchPredicate.customModelData(index, (float) readThreshold(element, context));
    });

    private final PredicateReader<? super ItemPredicateContext> reader;

    ItemRangeDispatchProperty(PredicateReader<? super ItemPredicateContext> reader) {
        this.reader = reader;
    }

    ItemRangeDispatchProperty(PredicateCreator<ItemPredicateContext, Double> creator) {
        this((element, context) -> creator.create(readThreshold(element, context)));
    }

    ItemRangeDispatchProperty(PredicateCreator<ItemPredicateContext, Integer> creator, PredicateCreator<ItemPredicateContext, Double> normalized) {
        this((element, context) -> {
            double threshold = readThreshold(element, context);
            return normalized(element, context) ? normalized.create(threshold) : creator.create((int) threshold);
        });
    }

    private static double readThreshold(JsonElement element, String... context) throws InvalidCustomMappingsFileException {
        double threshold = MappingsUtil.readOrThrow(element, "threshold", NodeReader.DOUBLE, context);
        double scale = MappingsUtil.readOrDefault(element, "scale", NodeReader.DOUBLE, 1.0, context);
        // Scale is a property from the Java resourcepack range dispatch predicates, and is applied to the property value. Since it's applied to the threshold here,
        // we have to divide the threshold instead of multiply it.
        return threshold / scale;
    }

    private static boolean normalized(JsonElement element, String... context) throws InvalidCustomMappingsFileException {
        return MappingsUtil.readOrDefault(element, "normalize", NodeReader.BOOLEAN, false, context);
    }

    @Override
    public MinecraftPredicate<? super ItemPredicateContext> read(JsonElement element, String... context) throws InvalidCustomMappingsFileException {
        return reader.read(element, context);
    }
}
