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
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.item.GeyserCustomMappingData;
import org.geysermc.geyser.item.custom.GeyserItemPredicateContext;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.Collection;

/**
 * This is only a separate class for testing purposes so we don't have to load in GeyserImpl in ItemTranslator.
 */
public final class CustomItemTranslator {

    /**
     * Looks up whether a Java item is a custom Bedrock item on our end.
     * @param session the session
     * @param stackSize the stack size
     * @param components ALL components of the item; not just the patch
     * @param mapping the current Bedrock item mapping we have
     * @return the custom item definition, or null
     */
    @Nullable
    public static ItemDefinition getCustomItem(GeyserSession session, int stackSize, DataComponents components, ItemMapping mapping) {
        if (components == null) {
            return null;
        }

        Multimap<Key, GeyserCustomMappingData> allCustomItems = mapping.getCustomItemDefinitions();
        if (allCustomItems == null) {
            return null;
        }

        Key itemModel = components.get(DataComponentTypes.ITEM_MODEL);
        if (itemModel == null) {
            return null;
        }
        Collection<GeyserCustomMappingData> customItems = allCustomItems.get(itemModel);
        if (customItems.isEmpty()) {
            return null;
        }

        ItemPredicateContext context = GeyserItemPredicateContext.create(session, stackSize, components);

        // Cache predicate values so they're not recalculated every time when there are multiple item definitions using the same predicates
        // As with predicate conflict detection, this only works for common predicates that are backed using record classes in the API module, since they work with .equals().
        // JSON mappings use only these common predicates, extensions may not.
        Object2BooleanMap<MinecraftPredicate<? super ItemPredicateContext>> calculatedPredicates = new Object2BooleanOpenHashMap<>();
        for (GeyserCustomMappingData customMapping : customItems) {
            boolean needsOnlyOneMatch = customMapping.definition().predicateStrategy() == PredicateStrategy.OR;
            boolean allMatch = true;

            for (MinecraftPredicate<? super ItemPredicateContext> predicate : customMapping.definition().predicates()) {
                boolean value = calculatedPredicates.computeIfAbsent(predicate, x -> predicate.test(context));
                if (value) {
                    if (needsOnlyOneMatch) {
                        return customMapping.itemDefinition();
                    }
                } else {
                    allMatch = false;
                    // If we need everything to match, that is no longer possible, so break
                    if (!needsOnlyOneMatch) {
                        break;
                    }
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
