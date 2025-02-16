/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.item;

import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.kyori.adventure.key.Key;
import org.geysermc.geyser.api.predicate.MinecraftPredicate;
import org.geysermc.geyser.api.predicate.PredicateStrategy;
import org.geysermc.geyser.api.predicate.context.ItemPredicateContext;
import org.geysermc.geyser.item.GeyserCustomMappingData;
import org.geysermc.geyser.item.custom.GeyserItemPredicateContext;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.Collection;

/**
 * This is only a separate class for testing purposes so we don't have to load in GeyserImpl in ItemTranslator.
 */
public final class CustomItemTranslator {
    private static final Key FALLBACK_MODEL = MinecraftKey.key("air");

    @Nullable
    public static ItemDefinition getCustomItem(GeyserSession session, int stackSize, DataComponents components, ItemMapping mapping) {
        if (components == null) {
            return null;
        }

        Multimap<Key, GeyserCustomMappingData> allCustomItems = mapping.getCustomItemDefinitions();
        if (allCustomItems == null) {
            return null;
        }

        Key itemModel = components.getOrDefault(DataComponentType.ITEM_MODEL, FALLBACK_MODEL);
        Collection<GeyserCustomMappingData> customItems = allCustomItems.get(itemModel);
        if (customItems.isEmpty()) {
            return null;
        }

        ItemPredicateContext context = GeyserItemPredicateContext.create(session, stackSize, components);

        // Cache predicate values so they're not recalculated every time when there are multiple item definitions using the same predicates
        Object2BooleanMap<MinecraftPredicate<? super ItemPredicateContext>> calculatedPredicates = new Object2BooleanOpenHashMap<>();
        for (GeyserCustomMappingData customMapping : customItems) {
            boolean needsOnlyOneMatch = customMapping.definition().predicateStrategy() == PredicateStrategy.OR;
            boolean allMatch = true;

            for (MinecraftPredicate<? super ItemPredicateContext> predicate : customMapping.definition().predicates()) {
                boolean value = calculatedPredicates.computeIfAbsent(predicate, x -> predicate.test(context));
                if (!value) {
                    allMatch = false;
                    break;
                } else if (needsOnlyOneMatch) {
                    break;
                }
            }
            if (allMatch) {
                return customMapping.itemDefinition();
            }
        }
        return null;
    }

    private CustomItemTranslator() {
    }
}
