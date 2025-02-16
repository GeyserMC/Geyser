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

package org.geysermc.geyser.item.custom;

import org.geysermc.geyser.api.predicate.context.ItemPredicateContext;
import org.geysermc.geyser.api.predicate.context.item.ChargedProjectile;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ArmorTrim;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.CustomModelData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.List;

public record GeyserItemPredicateContext(Identifier dimension, int count, int maxStackSize, int damage, int maxDamage,
                                         boolean unbreakable, int bundleFullness, Identifier trimMaterial, List<ChargedProjectile> chargedProjectiles,
                                         List<Identifier> components, List<Boolean> customModelDataFlags, List<String> customModelDataStrings, List<Float> customModelDataFloats) implements ItemPredicateContext {
    private static final CustomModelData EMPTY_CUSTOM_MODEL_DATA = new CustomModelData(List.of(), List.of(), List.of(), List.of());

    @Override
    public boolean customModelDataFlag(int index) {
        return getCustomBoolean(index);
    }

    @Override
    public String customModelDataString(int index) {
        return getSafeCustomModelData(customModelDataStrings, index);
    }

    @Override
    public float customModelDataFloat(int index) {
        return getCustomFloat(index);
    }

    private boolean getCustomBoolean(int index) {
        Boolean b = getSafeCustomModelData(customModelDataFlags, index);
        return b != null && b;
    }

    private float getCustomFloat(int index) {
        Float f = getSafeCustomModelData(customModelDataFloats, index);
        return f == null ? 0.0F : f;
    }

    private static <T> T getSafeCustomModelData(List<T> data, int index) {
        if (index < 0) {
            return null;
        }
        if (index < data.size()) {
            return data.get(index);
        }
        return null;
    }

    public static ItemPredicateContext create(GeyserSession session, int stackSize, DataComponents components) {
        Identifier dimension = MinecraftKey.keyToIdentifier(session.getRegistryCache().dimensions().entryByValue(session.getDimensionType()).key());

        int maxStackSize = components.getOrDefault(DataComponentType.MAX_STACK_SIZE, 64);
        int damage = components.getOrDefault(DataComponentType.DAMAGE, 0);
        int maxDamage = components.getOrDefault(DataComponentType.MAX_DAMAGE, 0);

        boolean unbreakable = components.get(DataComponentType.UNBREAKABLE) != null;

        List<ItemStack> bundleStacks = components.get(DataComponentType.BUNDLE_CONTENTS);
        int bundleFullness = 0;
        if (bundleStacks != null) {
            for (ItemStack stack : bundleStacks) {
                bundleFullness += stack.getAmount();
            }
        }

        Identifier trimMaterial = null;
        ArmorTrim trim = components.get(DataComponentType.TRIM);
        if (trim != null && !trim.material().isCustom()) {
            trimMaterial = MinecraftKey.keyToIdentifier(session.getRegistryCache().trimMaterials().entryById(trim.material().id()).key());
        }

        List<ChargedProjectile> chargedProjectiles = components.getOrDefault(DataComponentType.CHARGED_PROJECTILES, List.of()).stream()
            .map(GeyserItemPredicateContext::stackToProjectile).toList();

        List<Identifier> componentList = components.getDataComponents().keySet().stream()
            .map(type -> MinecraftKey.keyToIdentifier(type.getKey())).toList();

        CustomModelData customModelData = components.getOrDefault(DataComponentType.CUSTOM_MODEL_DATA, EMPTY_CUSTOM_MODEL_DATA);

        return new GeyserItemPredicateContext(dimension, stackSize, maxStackSize, damage, maxDamage, unbreakable, bundleFullness,
            trimMaterial, chargedProjectiles, componentList, customModelData.flags(), customModelData.strings(), customModelData.floats());
    }

    private static ChargedProjectile stackToProjectile(ItemStack stack) {
        return stack.getId() == Items.FIREWORK_ROCKET.javaId()
            ? new ChargedProjectile(ChargedProjectile.ChargeType.ROCKET, stack.getAmount())
            : new ChargedProjectile(ChargedProjectile.ChargeType.ARROW, stack.getAmount());
    }
}
