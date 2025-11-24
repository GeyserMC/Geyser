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
import org.geysermc.geyser.api.item.custom.v2.component.DataComponent;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.BlockPlacer;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.Chargeable;
import org.geysermc.geyser.api.item.custom.v2.component.geyser.ThrowableComponent;
import org.geysermc.geyser.api.item.custom.v2.component.java.Consumable;
import org.geysermc.geyser.api.item.custom.v2.component.java.Equippable;
import org.geysermc.geyser.api.item.custom.v2.component.java.FoodProperties;
import org.geysermc.geyser.api.item.custom.v2.component.java.Repairable;
import org.geysermc.geyser.api.item.custom.v2.component.java.ToolProperties;
import org.geysermc.geyser.api.item.custom.v2.component.java.UseCooldown;
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
import org.geysermc.geyser.item.custom.impl.ThrowableComponentImpl;
import org.geysermc.geyser.item.custom.impl.predicates.GeyserChargedProjectile;
import org.geysermc.geyser.item.custom.GeyserCustomItemBedrockOptions;
import org.geysermc.geyser.item.custom.GeyserCustomItemDefinition;
import org.geysermc.geyser.item.custom.GeyserNonVanillaCustomItemDefinition;
import org.geysermc.geyser.item.custom.impl.BlockPlacerImpl;
import org.geysermc.geyser.item.custom.impl.ChargeableImpl;
import org.geysermc.geyser.item.custom.impl.ConsumableImpl;
import org.geysermc.geyser.item.custom.impl.DataComponentImpl;
import org.geysermc.geyser.item.custom.impl.EquippableImpl;
import org.geysermc.geyser.item.custom.impl.FoodPropertiesImpl;
import org.geysermc.geyser.item.custom.impl.RepairableImpl;
import org.geysermc.geyser.item.custom.impl.ToolPropertiesImpl;
import org.geysermc.geyser.item.custom.impl.UseCooldownImpl;
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

        providers.put(DataComponent.class, args -> dataComponentProvider((Identifier) args[0], (Predicate<?>) args[1], (Boolean) args[2]));

        // item components
        providers.put(Consumable.Builder.class, args -> new ConsumableImpl.Builder());
        providers.put(Equippable.Builder.class, args -> new EquippableImpl.Builder());
        providers.put(FoodProperties.Builder.class, args -> new FoodPropertiesImpl.Builder());
        providers.put(Repairable.Builder.class, args -> new RepairableImpl.Builder());
        providers.put(ToolProperties.Builder.class, args -> new ToolPropertiesImpl.Builder());
        providers.put(ToolProperties.Rule.Builder.class, args -> new ToolPropertiesImpl.RuleImpl.Builder());
        providers.put(UseCooldown.Builder.class, args -> new UseCooldownImpl.Builder());

        // geyser components
        providers.put(Chargeable.Builder.class, args -> new ChargeableImpl.Builder());
        providers.put(BlockPlacer.Builder.class, args -> new BlockPlacerImpl.Builder());
        providers.put(ThrowableComponent.Builder.class, args -> new ThrowableComponentImpl.Builder());

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

    public <T> DataComponentImpl<T> dataComponentProvider(Identifier identifier, Predicate<T> predicate, boolean vanilla) {
        return new DataComponentImpl<>(identifier, predicate, vanilla);
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
                    return new GeyserRangeDispatchPredicate(GeyserRangeDispatchPredicate.GeyserRangeDispatchProperty.DAMAGE, (int) args[1], (boolean) args[2]);
                }
            }
            case COUNT -> {
                // One with, one without normalization
                if (length == 2) {
                    return new GeyserRangeDispatchPredicate(GeyserRangeDispatchPredicate.GeyserRangeDispatchProperty.COUNT, (int) args[1]);
                } else if (length == 3) {
                    return new GeyserRangeDispatchPredicate(GeyserRangeDispatchPredicate.GeyserRangeDispatchProperty.COUNT, (int) args[1], (boolean) args[2]);
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
