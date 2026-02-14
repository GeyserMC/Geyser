/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.registry.loader;

import org.geysermc.geyser.api.bedrock.camera.CameraFade;
import org.geysermc.geyser.api.bedrock.camera.CameraPosition;
import org.geysermc.geyser.api.block.custom.CustomBlockData;
import org.geysermc.geyser.api.block.custom.NonVanillaCustomBlockData;
import org.geysermc.geyser.api.block.custom.component.CustomBlockComponents;
import org.geysermc.geyser.api.block.custom.component.GeometryComponent;
import org.geysermc.geyser.api.block.custom.component.MaterialInstance;
import org.geysermc.geyser.api.block.custom.nonvanilla.JavaBlockState;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserBlockPlacer;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserChargeable;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserThrowableComponent;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaAttackRange;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaConsumable;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaEquippable;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaFoodProperties;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaKineticWeapon;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaPiercingWeapon;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaRepairable;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaSwingAnimation;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaTool;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaUseCooldown;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaUseEffects;
import org.geysermc.geyser.api.pack.PathPackCodec;
import org.geysermc.geyser.api.pack.UrlPackCodec;
import org.geysermc.geyser.api.pack.option.PriorityOption;
import org.geysermc.geyser.api.pack.option.SubpackOption;
import org.geysermc.geyser.api.pack.option.UrlFallbackOption;
import org.geysermc.geyser.api.predicate.DimensionPredicate;
import org.geysermc.geyser.api.predicate.context.item.ChargedProjectile;
import org.geysermc.geyser.api.predicate.item.ChargeTypePredicate;
import org.geysermc.geyser.api.predicate.item.CustomModelDataPredicate;
import org.geysermc.geyser.api.predicate.item.HasComponentPredicate;
import org.geysermc.geyser.api.predicate.item.RangeDispatchPredicate;
import org.geysermc.geyser.api.predicate.item.TrimMaterialPredicate;
import org.geysermc.geyser.api.util.Holders;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.event.GeyserEventRegistrar;
import org.geysermc.geyser.extension.command.GeyserExtensionCommand;
import org.geysermc.geyser.impl.GeyserDimensionPredicate;
import org.geysermc.geyser.impl.HoldersImpl;
import org.geysermc.geyser.impl.IdentifierImpl;
import org.geysermc.geyser.impl.camera.GeyserCameraFade;
import org.geysermc.geyser.impl.camera.GeyserCameraPosition;
import org.geysermc.geyser.item.GeyserCustomItemData;
import org.geysermc.geyser.item.GeyserCustomItemOptions;
import org.geysermc.geyser.item.GeyserNonVanillaCustomItemData;
import org.geysermc.geyser.item.custom.impl.JavaAttackRangeImpl;
import org.geysermc.geyser.item.custom.impl.JavaKineticWeaponImpl;
import org.geysermc.geyser.item.custom.impl.JavaPiercingWeaponImpl;
import org.geysermc.geyser.item.custom.impl.JavaSwingAnimationImpl;
import org.geysermc.geyser.item.custom.impl.GeyserThrowableComponentImpl;
import org.geysermc.geyser.item.custom.impl.JavaUseEffectsImpl;
import org.geysermc.geyser.item.custom.impl.predicates.GeyserChargedProjectile;
import org.geysermc.geyser.item.custom.GeyserCustomItemBedrockOptions;
import org.geysermc.geyser.item.custom.GeyserCustomItemDefinition;
import org.geysermc.geyser.item.custom.GeyserNonVanillaCustomItemDefinition;
import org.geysermc.geyser.item.custom.impl.GeyserBlockPlacerImpl;
import org.geysermc.geyser.item.custom.impl.GeyserChargeableImpl;
import org.geysermc.geyser.item.custom.impl.JavaConsumableImpl;
import org.geysermc.geyser.item.custom.impl.ItemDataComponentImpl;
import org.geysermc.geyser.item.custom.impl.JavaEquippableImpl;
import org.geysermc.geyser.item.custom.impl.JavaFoodPropertiesImpl;
import org.geysermc.geyser.item.custom.impl.JavaRepairableImpl;
import org.geysermc.geyser.item.custom.impl.JavaToolImpl;
import org.geysermc.geyser.item.custom.impl.JavaUseCooldownImpl;
import org.geysermc.geyser.item.custom.impl.predicates.GeyserChargeTypePredicate;
import org.geysermc.geyser.item.custom.impl.predicates.GeyserCustomModelDataPredicate;
import org.geysermc.geyser.item.custom.impl.predicates.GeyserHasComponentPredicate;
import org.geysermc.geyser.item.custom.impl.predicates.GeyserRangeDispatchPredicate;
import org.geysermc.geyser.item.custom.impl.predicates.GeyserTrimMaterialPredicate;
import org.geysermc.geyser.level.block.GeyserCustomBlockComponents;
import org.geysermc.geyser.level.block.GeyserCustomBlockData;
import org.geysermc.geyser.level.block.GeyserGeometryComponent;
import org.geysermc.geyser.level.block.GeyserJavaBlockState;
import org.geysermc.geyser.level.block.GeyserMaterialInstance;
import org.geysermc.geyser.level.block.GeyserNonVanillaCustomBlockData;
import org.geysermc.geyser.pack.option.GeyserPriorityOption;
import org.geysermc.geyser.pack.option.GeyserSubpackOption;
import org.geysermc.geyser.pack.option.GeyserUrlFallbackOption;
import org.geysermc.geyser.pack.path.GeyserPathPackCodec;
import org.geysermc.geyser.pack.url.GeyserUrlPackCodec;
import org.geysermc.geyser.registry.provider.ProviderSupplier;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Registers the provider data from the provider.
 */
public class ProviderRegistryLoader implements RegistryLoader<Map<Class<?>, ProviderSupplier>, Map<Class<?>, ProviderSupplier>> {

    @Override
    public Map<Class<?>, ProviderSupplier> load(Map<Class<?>, ProviderSupplier> providers) {
        // misc
        providers.put(Identifier.class, args -> IdentifierImpl.of((String) args[0], (String) args[1]));
        providers.put(Holders.Builder.class, args -> new HoldersImpl.Builder());
        // commands
        providers.put(Command.Builder.class, args -> new GeyserExtensionCommand.Builder<>((Extension) args[0]));

        // custom blocks
        providers.put(CustomBlockComponents.Builder.class, args -> new GeyserCustomBlockComponents.Builder());
        providers.put(CustomBlockData.Builder.class, args -> new GeyserCustomBlockData.Builder());
        providers.put(JavaBlockState.Builder.class, args -> new GeyserJavaBlockState.Builder());
        providers.put(NonVanillaCustomBlockData.Builder.class, args -> new GeyserNonVanillaCustomBlockData.Builder());
        providers.put(MaterialInstance.Builder.class, args -> new GeyserMaterialInstance.Builder());
        providers.put(GeometryComponent.Builder.class, args -> new GeyserGeometryComponent.Builder());

        // misc
        providers.put(EventRegistrar.class, args -> new GeyserEventRegistrar(args[0]));

        // packs
        providers.put(PathPackCodec.class, args -> new GeyserPathPackCodec((Path) args[0]));
        providers.put(UrlPackCodec.class, args -> new GeyserUrlPackCodec((String) args[0]));
        providers.put(PriorityOption.class, args -> new GeyserPriorityOption((int) args[0]));
        providers.put(SubpackOption.class, args -> new GeyserSubpackOption((String) args[0]));
        providers.put(UrlFallbackOption.class, args -> new GeyserUrlFallbackOption((Boolean) args[0]));

        // items
        providers.put(CustomItemData.Builder.class, args -> new GeyserCustomItemData.Builder());
        providers.put(CustomItemOptions.Builder.class, args -> new GeyserCustomItemOptions.Builder());
        providers.put(NonVanillaCustomItemData.Builder.class, args -> new GeyserNonVanillaCustomItemData.Builder());

        // items v2
        providers.put(CustomItemDefinition.Builder.class, args -> new GeyserCustomItemDefinition.Builder((Identifier) args[0], (Identifier) args[1]));
        providers.put(NonVanillaCustomItemDefinition.Builder.class, args -> new GeyserNonVanillaCustomItemDefinition.Builder((Identifier) args[0], (Identifier) args[1], (int) args[2]));
        providers.put(CustomItemBedrockOptions.Builder.class, args -> new GeyserCustomItemBedrockOptions.Builder());

        providers.put(ItemDataComponent.class, args -> dataComponentProvider((Identifier) args[0], (Predicate<?>) args[1], (Boolean) args[2]));

        // item components
        providers.put(JavaAttackRange.Builder.class, args -> new JavaAttackRangeImpl.Builder());
        providers.put(JavaConsumable.Builder.class, args -> new JavaConsumableImpl.Builder());
        providers.put(JavaEquippable.Builder.class, args -> new JavaEquippableImpl.Builder());
        providers.put(JavaFoodProperties.Builder.class, args -> new JavaFoodPropertiesImpl.Builder());
        providers.put(JavaKineticWeapon.Builder.class, args -> new JavaKineticWeaponImpl.Builder());
        providers.put(JavaKineticWeapon.Condition.Builder.class, args -> new JavaKineticWeaponImpl.ConditionImpl.Builder((Integer) args[0]));
        providers.put(JavaPiercingWeapon.class, args -> JavaPiercingWeaponImpl.INSTANCE);
        providers.put(JavaRepairable.Builder.class, args -> new JavaRepairableImpl.Builder());
        providers.put(JavaSwingAnimation.Builder.class, args -> new JavaSwingAnimationImpl.Builder());
        providers.put(JavaTool.Builder.class, args -> new JavaToolImpl.Builder());
        providers.put(JavaTool.Rule.Builder.class, args -> new JavaToolImpl.RuleImpl.Builder());
        providers.put(JavaUseCooldown.Builder.class, args -> new JavaUseCooldownImpl.Builder());
        providers.put(JavaUseEffects.Builder.class, args -> new JavaUseEffectsImpl.Builder());

        // geyser components
        providers.put(GeyserChargeable.Builder.class, args -> new GeyserChargeableImpl.Builder());
        providers.put(GeyserBlockPlacer.Builder.class, args -> new GeyserBlockPlacerImpl.Builder());
        providers.put(GeyserThrowableComponent.Builder.class, args -> new GeyserThrowableComponentImpl.Builder());

        // predicates
        providers.put(DimensionPredicate.class, args -> new GeyserDimensionPredicate((Identifier) args[0], false));
        providers.put(ChargedProjectile.class, args -> new GeyserChargedProjectile((ChargedProjectile.ChargeType) args[0], (int) args[1]));
        providers.put(CustomModelDataPredicate.FlagPredicate.class, args -> new GeyserCustomModelDataPredicate.GeyserFlagPredicate((int) args[0], false));
        providers.put(HasComponentPredicate.class, args -> new GeyserHasComponentPredicate((Identifier) args[0], false));
        providers.put(ChargeTypePredicate.class, args -> new GeyserChargeTypePredicate((ChargedProjectile.ChargeType) args[0], false));
        providers.put(TrimMaterialPredicate.class, args -> new GeyserTrimMaterialPredicate((Identifier) args[0], false));
        providers.put(CustomModelDataPredicate.StringPredicate.class, args -> new GeyserCustomModelDataPredicate.GeyserStringPredicate((String) args[0], (int) args[1], false));
        providers.put(RangeDispatchPredicate.class, ProviderRegistryLoader::createRangeDispatchPredicate);

        // cameras
        providers.put(CameraFade.Builder.class, args -> new GeyserCameraFade.Builder());
        providers.put(CameraPosition.Builder.class, args -> new GeyserCameraPosition.Builder());

        return providers;
    }

    public <T> ItemDataComponentImpl<T> dataComponentProvider(Identifier identifier, Predicate<T> predicate, boolean vanilla) {
        return new ItemDataComponentImpl<>(identifier, predicate, vanilla);
    }

    private static Object createRangeDispatchPredicate(Object... args) {
        // Enforcing a few things here :)
        var property = (RangeDispatchPredicate.Property) args[0];
        int length = args.length;
        switch (property) {
            case BUNDLE_FULLNESS -> {
                return new GeyserRangeDispatchPredicate(GeyserRangeDispatchPredicate.GeyserRangeDispatchProperty.BUNDLE_FULLNESS, (int) args[1]);
            }
            case DAMAGE -> {
                // One with, one without normalization
                if (length == 2) {
                    return new GeyserRangeDispatchPredicate(GeyserRangeDispatchPredicate.GeyserRangeDispatchProperty.DAMAGE, (int) args[1]);
                } else if (length == 3) {
                    return new GeyserRangeDispatchPredicate(GeyserRangeDispatchPredicate.GeyserRangeDispatchProperty.DAMAGE, (double) args[1], (boolean) args[2]);
                }
            }
            case COUNT -> {
                // One with, one without normalization
                if (length == 2) {
                    return new GeyserRangeDispatchPredicate(GeyserRangeDispatchPredicate.GeyserRangeDispatchProperty.COUNT, (int) args[1]);
                } else if (length == 3) {
                    return new GeyserRangeDispatchPredicate(GeyserRangeDispatchPredicate.GeyserRangeDispatchProperty.COUNT, (double) args[1], (boolean) args[2]);
                }
            }
            case CUSTOM_MODEL_DATA -> {
                int index = 0;
                if (length == 3) {
                    index = (int) args[2];
                }

                // Threshold is passed as either integer or float in API
                double threshold;
                if (args[1] instanceof Integer i) {
                    threshold = i.doubleValue();
                } else {
                    threshold = ((Float) args[1]).doubleValue();
                }
                return new GeyserRangeDispatchPredicate(GeyserRangeDispatchPredicate.GeyserRangeDispatchProperty.CUSTOM_MODEL_DATA, threshold, index);
            }
        }
        throw new IllegalStateException("Unexpected property: " + property.name() + " with args " + Arrays.toString(args));
    }
}
