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

import net.kyori.adventure.key.Key;
import org.cloudburstmc.protocol.bedrock.data.TrimMaterial;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.predicate.CustomItemPredicate;
import org.geysermc.geyser.api.item.custom.v2.predicate.ItemPredicateType;
import org.geysermc.geyser.api.item.custom.v2.predicate.data.ConditionPredicateData;
import org.geysermc.geyser.api.item.custom.v2.predicate.data.match.ChargeType;
import org.geysermc.geyser.api.item.custom.v2.predicate.data.match.MatchPredicateData;
import org.geysermc.geyser.api.item.custom.v2.predicate.data.match.MatchPredicateProperty;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.RegistryEntryData;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ArmorTrim;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import it.unimi.dsi.fastutil.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.geysermc.geyser.registry.type.ItemMapping;

import java.util.List;

/**
 * This is only a separate class for testing purposes so we don't have to load in GeyserImpl in ItemTranslator.
 */
public final class CustomItemTranslator {

    @Nullable
    public static ItemDefinition getCustomItem(GeyserSession session, DataComponents components, ItemMapping mapping) {
        if (components == null) {
            return null;
        }

        List<Pair<CustomItemDefinition, ItemDefinition>> customItems = mapping.getCustomItemDefinitions();
        if (customItems.isEmpty()) {
            return null;
        }

        Key itemModel = components.getOrDefault(DataComponentType.ITEM_MODEL, MinecraftKey.key("air")); // TODO fallback onto default item model (when thats done by chris)

        // TODO check if definitions/predicates are in the correct order
        for (Pair<CustomItemDefinition, ItemDefinition> customModel : customItems) { // TODO Predicates
            if (customModel.first().model().equals(itemModel)) {
                boolean allMatch = true;
                for (CustomItemPredicate<?> predicate : customModel.first().predicates()) {
                    if (!predicateMatches(session, predicate, components)) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch) {
                    return customModel.second();
                }
            }
        }
        return null;
    }

    private static boolean predicateMatches(GeyserSession session, CustomItemPredicate<?> predicate, DataComponents components) {
        if (predicate.type() == ItemPredicateType.CONDITION) {
            ConditionPredicateData data = (ConditionPredicateData) predicate.data();
            return switch (data.property()) {
                case BROKEN -> nextDamageWillBreak(components);
                case DAMAGED -> isDamaged(components);
                case CUSTOM_MODEL_DATA -> false; // TODO 1.21.4
            };
        } else if (predicate.type() == ItemPredicateType.MATCH) {
            MatchPredicateData<?> data = (MatchPredicateData<?>) predicate.data();

            if (data.property() == MatchPredicateProperty.CHARGE_TYPE) {
                ChargeType expected = (ChargeType) data.data();
                List<ItemStack> charged = components.get(DataComponentType.CHARGED_PROJECTILES);
                if (charged == null) {
                    return expected == ChargeType.NONE;
                } else if (expected == ChargeType.ROCKET) {
                    for (ItemStack projectile : charged) {
                        if (projectile.getId() == Items.FIREWORK_ROCKET.javaId()) {
                            return true;
                        }
                    }
                    return false;
                }
                return true;
            } else if (data.property() == MatchPredicateProperty.TRIM_MATERIAL) {
                Key material = (Key) data.data();
                ArmorTrim trim = components.get(DataComponentType.TRIM);
                if (trim == null || trim.material().isCustom()) {
                    return false;
                }
                RegistryEntryData<TrimMaterial> registered = session.getRegistryCache().trimMaterials().entryById(trim.material().id());
                return registered != null && registered.key().equals(material);
            } else if (data.property() == MatchPredicateProperty.CONTEXT_DIMENSION) {
                Key dimension = (Key) data.data();
                RegistryEntryData<JavaDimension> registered = session.getRegistryCache().dimensions().entryByValue(session.getDimensionType());
                return registered != null && dimension.equals(registered.key()); // TODO check if this works
            } else if (data.property() == MatchPredicateProperty.CUSTOM_MODEL_DATA) {
                // TODO 1.21.4
                return false;
            }
        }

        throw new IllegalStateException("Unimplemented predicate type");
    }

    /* These three functions are based off their Mojmap equivalents from 1.21.3 */

    private static boolean nextDamageWillBreak(DataComponents components) {
        return isDamageableItem(components) && components.getOrDefault(DataComponentType.DAMAGE, 0) >= components.getOrDefault(DataComponentType.MAX_DAMAGE, 0) - 1;
    }

    private static boolean isDamaged(DataComponents components) {
        return isDamageableItem(components) && components.getOrDefault(DataComponentType.DAMAGE, 0) > 0;
    }

    private static boolean isDamageableItem(DataComponents components) {
        return components.getOrDefault(DataComponentType.UNBREAKABLE, false) && components.getOrDefault(DataComponentType.MAX_DAMAGE, 0) > 0;
    }

    private CustomItemTranslator() {
    }
}
