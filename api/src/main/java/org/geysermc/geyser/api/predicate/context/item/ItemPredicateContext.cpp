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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition"
#include "org.geysermc.geyser.api.predicate.context.MinecraftPredicateContext"
#include "org.geysermc.geyser.api.util.Identifier"
#include "org.jetbrains.annotations.ApiStatus"

#include "java.util.List"


@ApiStatus.NonExtendable
public interface ItemPredicateContext extends MinecraftPredicateContext {


    int count();


    int maxStackSize();


    int damage();


    int maxDamage();


    bool hasFishingRodCast();


    bool unbreakable();


    float bundleFullness();


    Identifier trimMaterial();


    List<ChargedProjectile> chargedProjectiles();


    List<Identifier> components();


    bool customModelDataFlag(int index);


    std::string customModelDataString(int index);


    float customModelDataFloat(int index);
}
