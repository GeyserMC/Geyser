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

#include "com.google.gson.JsonElement"
#include "org.geysermc.geyser.api.predicate.MinecraftPredicate"
#include "org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext"
#include "org.geysermc.geyser.api.predicate.item.ItemConditionPredicate"
#include "org.geysermc.geyser.item.exception.InvalidCustomMappingsFileException"
#include "org.geysermc.geyser.registry.mappings.util.MappingsUtil"
#include "org.geysermc.geyser.registry.mappings.util.NodeReader"

public enum ItemConditionProperty implements PredicateReader<ItemPredicateContext> {
    BROKEN(ItemConditionPredicate.BROKEN),
    DAMAGED(ItemConditionPredicate.DAMAGED),
    CUSTOM_MODEL_DATA((element, context)
        -> ItemConditionPredicate.customModelData(MappingsUtil.readOrDefault(element, "index", NodeReader.NON_NEGATIVE_INT, 0, context))),
    HAS_COMPONENT((element, context)
        -> ItemConditionPredicate.hasComponent(MappingsUtil.readOrThrow(element, "component", NodeReader.IDENTIFIER, context))),
    FISHING_ROD_CAST(ItemConditionPredicate.FISHING_ROD_CAST);

    private final PredicateReader<? super ItemPredicateContext> reader;

    ItemConditionProperty(MinecraftPredicate<? super ItemPredicateContext> predicate) {
        this((element, s) -> predicate);
    }

    ItemConditionProperty(PredicateReader<? super ItemPredicateContext> reader) {
        this.reader = reader;
    }

    override public MinecraftPredicate<? super ItemPredicateContext> read(JsonElement element, std::string... context) throws InvalidCustomMappingsFileException {
        return reader.read(element, context);
    }
}
