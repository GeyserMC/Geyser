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

package org.geysermc.geyser.api.predicate.context.item;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.predicate.context.MinecraftPredicateContext;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * Item context. Used for predicates in {@link CustomItemDefinition}s.
 * @since 2.9.3
 */
@ApiStatus.NonExtendable
public interface ItemPredicateContext extends MinecraftPredicateContext {

    /**
     * @return the stack size
     * @since 2.9.3
     */
    int count();

    /**
     * @return the max stack size
     * @since 2.9.3
     */
    int maxStackSize();

    /**
     * @return the value of the {@code minecraft:damage} component, or 0 if not present
     * @since 2.9.3
     */
    int damage();

    /**
     * @return the value of the {@code minecraft:max_damage} component, or 0 if not present
     * @since 2.9.3
     */
    int maxDamage();

    /**
     * @return if the session player is holding a fishing rod cast
     * @since 2.9.3
     */
    boolean hasFishingRodCast();

    /**
     * @return true if the {@code minecraft:unbreakable} component is present
     * @since 2.9.3
     */
    boolean unbreakable();

    /**
     * @return the bundle's fullness from the {@code minecraft:bundle_contents} component, or 0 if not present
     * @since 2.9.3
     */
    float bundleFullness();

    /**
     * @return the identifier of the item's trim material, or null if no trim is present
     * @since 2.9.3
     */
    @Nullable Identifier trimMaterial();

    /**
     * @return all the charged projectiles in the {@code minecraft:charged_projectiles} component
     * @since 2.9.3
     */
    @NonNull List<@NonNull ChargedProjectile> chargedProjectiles();

    /**
     * @return a list of all the components present on the item, including default components
     * @since 2.9.3
     */
    @NonNull List<@NonNull Identifier> components();

    /**
     * @param index the flag index
     * @return a flag of the item's custom model data, defaults to false
     * @since 2.9.3
     */
    boolean customModelDataFlag(int index);

    /**
     * @param index the string index
     * @return a string of the item's custom model data. Returns null if index is out of range
     * @since 2.9.3
     */
    @Nullable String customModelDataString(int index);

    /**
     * @param index the float index
     * @return a float of the item's custom model data, defaults to 0.0
     * @since 2.9.3
     */
    float customModelDataFloat(int index);
}
