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

import com.google.common.base.Suppliers;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.predicate.context.item.ChargedProjectile;
import org.geysermc.geyser.api.predicate.context.item.ItemPredicateContext;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.custom.impl.predicates.GeyserChargedProjectile;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.translator.inventory.BundleInventoryTranslator;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.geyser.util.thirdparty.Fraction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ArmorTrim;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.CustomModelData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public record GeyserItemPredicateContext(Supplier<Identifier> dimensionSupplier, int count, Supplier<Integer> maxStackSizeSupplier,
                                         Supplier<Integer> damageSupplier, Supplier<Integer> maxDamageSupplier,
                                         Supplier<Boolean> fishingRodCastSupplier, Supplier<Boolean> unbreakableSupplier,
                                         Supplier<Float> bundleFullnessSupplier, Supplier<Identifier> trimMaterialSupplier, Supplier<List<ChargedProjectile>> chargedProjectilesSupplier,
                                         Supplier<List<Identifier>> componentsSupplier, Supplier<List<Boolean>> customModelDataFlagsSupplier, Supplier<List<String>> customModelDataStringsSupplier,
                                         Supplier<List<Float>> customModelDataFloatsSupplier) implements ItemPredicateContext {
    private static final CustomModelData EMPTY_CUSTOM_MODEL_DATA = new CustomModelData(List.of(), List.of(), List.of(), List.of());

    @Override
    public Identifier dimension() {
        return dimensionSupplier.get();
    }

    @Override
    public int maxStackSize() {
        return maxStackSizeSupplier.get();
    }

    @Override
    public int damage() {
        return damageSupplier.get();
    }

    @Override
    public int maxDamage() {
        return maxDamageSupplier.get();
    }

    @Override
    public boolean hasFishingRodCast() {
        return fishingRodCastSupplier.get();
    }

    @Override
    public boolean unbreakable() {
        return unbreakableSupplier.get();
    }

    @Override
    public float bundleFullness() {
        return bundleFullnessSupplier.get();
    }

    @Override
    public @Nullable Identifier trimMaterial() {
        return trimMaterialSupplier.get();
    }

    @Override
    public @NonNull List<ChargedProjectile> chargedProjectiles() {
        return chargedProjectilesSupplier.get();
    }

    @Override
    public @NonNull List<Identifier> components() {
        return componentsSupplier.get();
    }

    @Override
    public boolean customModelDataFlag(int index) {
        return getCustomBoolean(index);
    }

    @Override
    public String customModelDataString(int index) {
        return getSafeCustomModelData(customModelDataStringsSupplier.get(), index);
    }

    @Override
    public float customModelDataFloat(int index) {
        return getCustomFloat(index);
    }

    private boolean getCustomBoolean(int index) {
        Boolean b = getSafeCustomModelData(customModelDataFlagsSupplier.get(), index);
        return b != null && b;
    }

    private float getCustomFloat(int index) {
        Float f = getSafeCustomModelData(customModelDataFloatsSupplier.get(), index);
        return f == null ? 0.0F : f;
    }

    private static <T> T getSafeCustomModelData(List<T> data, int index) {
        if (index < 0) {
            return null;
        } else if (index < data.size()) {
            return data.get(index);
        }
        return null;
    }

    public static ItemPredicateContext create(GeyserSession session, int stackSize, DataComponents components) {
        Supplier<Identifier> dimension = Suppliers.memoize(() -> MinecraftKey.keyToIdentifier(JavaRegistries.DIMENSION_TYPE.key(session, session.getDimensionType())));

        Supplier<Integer> maxStackSize = Suppliers.memoize(() -> components.getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 64));
        Supplier<Integer> damage = Suppliers.memoize(() -> components.getOrDefault(DataComponentTypes.DAMAGE, 0));
        Supplier<Integer> maxDamage = Suppliers.memoize(() -> components.getOrDefault(DataComponentTypes.MAX_DAMAGE, 0));

        Supplier<Boolean> fishingRodCast = Suppliers.memoize(session::hasFishingRodCast);
        Supplier<Boolean> unbreakable = Suppliers.memoize((() -> components.get(DataComponentTypes.UNBREAKABLE) != null));

        Supplier<Float> bundleFullness = Suppliers.memoize(() -> {
            List<ItemStack> bundleStacks = components.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (bundleStacks != null) {
                Fraction fraction = BundleInventoryTranslator.calculateBundleWeight(bundleStacks.stream()
                    .map(stack -> GeyserItemStack.from(session, stack)).collect(Collectors.toList()));
                return fraction.floatValue();
            }
            return 0f;
        });

        Supplier<Identifier> trimMaterial = Suppliers.memoize(() -> {
            ArmorTrim trim = components.get(DataComponentTypes.TRIM);
            if (trim != null && !trim.material().isCustom()) {
                return MinecraftKey.keyToIdentifier(JavaRegistries.TRIM_MATERIAL.key(session, trim.material().id()).key());
            }
            return null;
        });

        Supplier<List<ChargedProjectile>> chargedProjectiles = Suppliers.memoize(() -> components.getOrDefault(DataComponentTypes.CHARGED_PROJECTILES, List.of()).stream()
            .map(GeyserItemPredicateContext::stackToProjectile).toList());

        Supplier<List<Identifier>> componentList = Suppliers.memoize(() -> components.getDataComponents().keySet().stream()
            .map(type -> MinecraftKey.keyToIdentifier(type.getKey())).toList());

        Supplier<CustomModelData> customModelData = Suppliers.memoize(() -> components.getOrDefault(DataComponentTypes.CUSTOM_MODEL_DATA, EMPTY_CUSTOM_MODEL_DATA));

        Supplier<List<Boolean>> flags = Suppliers.memoize(() -> customModelData.get().flags());
        Supplier<List<String>> strings = Suppliers.memoize(() -> customModelData.get().strings());
        Supplier<List<Float>> floats = Suppliers.memoize(() -> customModelData.get().floats());

        return new GeyserItemPredicateContext(dimension, stackSize, maxStackSize, damage, maxDamage, fishingRodCast, unbreakable,
            bundleFullness, trimMaterial, chargedProjectiles, componentList, flags, strings, floats);
    }

    private static ChargedProjectile stackToProjectile(ItemStack stack) {
        return stack.getId() == Items.FIREWORK_ROCKET.javaId()
            ? new GeyserChargedProjectile(ChargedProjectile.ChargeType.ROCKET, stack.getAmount())
            : new GeyserChargedProjectile(ChargedProjectile.ChargeType.ARROW, stack.getAmount());
    }
}
