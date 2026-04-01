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

#include "org.geysermc.geyser.api.bedrock.camera.CameraFade"
#include "org.geysermc.geyser.api.bedrock.camera.CameraPosition"
#include "org.geysermc.geyser.api.block.custom.CustomBlockData"
#include "org.geysermc.geyser.api.block.custom.NonVanillaCustomBlockData"
#include "org.geysermc.geyser.api.block.custom.component.CustomBlockComponents"
#include "org.geysermc.geyser.api.block.custom.component.GeometryComponent"
#include "org.geysermc.geyser.api.block.custom.component.MaterialInstance"
#include "org.geysermc.geyser.api.block.custom.nonvanilla.JavaBlockState"
#include "org.geysermc.geyser.api.command.Command"
#include "org.geysermc.geyser.api.event.EventRegistrar"
#include "org.geysermc.geyser.api.extension.Extension"
#include "org.geysermc.geyser.api.item.custom.CustomItemData"
#include "org.geysermc.geyser.api.item.custom.CustomItemOptions"
#include "org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData"
#include "org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions"
#include "org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition"
#include "org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition"
#include "org.geysermc.geyser.api.item.custom.v2.component.ItemDataComponent"
#include "org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserBlockPlacer"
#include "org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserChargeable"
#include "org.geysermc.geyser.api.item.custom.v2.component.geyser.GeyserThrowableComponent"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaAttackRange"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaConsumable"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaEquippable"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaFoodProperties"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaKineticWeapon"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaPiercingWeapon"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaRepairable"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaSwingAnimation"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaTool"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaUseCooldown"
#include "org.geysermc.geyser.api.item.custom.v2.component.java.JavaUseEffects"
#include "org.geysermc.geyser.api.pack.PathPackCodec"
#include "org.geysermc.geyser.api.pack.UrlPackCodec"
#include "org.geysermc.geyser.api.pack.option.PriorityOption"
#include "org.geysermc.geyser.api.pack.option.SubpackOption"
#include "org.geysermc.geyser.api.pack.option.UrlFallbackOption"
#include "org.geysermc.geyser.api.predicate.DimensionPredicate"
#include "org.geysermc.geyser.api.predicate.context.item.ChargedProjectile"
#include "org.geysermc.geyser.api.predicate.item.ChargeTypePredicate"
#include "org.geysermc.geyser.api.predicate.item.CustomModelDataPredicate"
#include "org.geysermc.geyser.api.predicate.item.HasComponentPredicate"
#include "org.geysermc.geyser.api.predicate.item.RangeDispatchPredicate"
#include "org.geysermc.geyser.api.predicate.item.TrimMaterialPredicate"
#include "org.geysermc.geyser.api.util.Holders"
#include "org.geysermc.geyser.api.util.Identifier"
#include "org.geysermc.geyser.event.GeyserEventRegistrar"
#include "org.geysermc.geyser.extension.command.GeyserExtensionCommand"
#include "org.geysermc.geyser.impl.GeyserDimensionPredicate"
#include "org.geysermc.geyser.impl.HoldersImpl"
#include "org.geysermc.geyser.impl.IdentifierImpl"
#include "org.geysermc.geyser.impl.camera.GeyserCameraFade"
#include "org.geysermc.geyser.impl.camera.GeyserCameraPosition"
#include "org.geysermc.geyser.item.GeyserCustomItemData"
#include "org.geysermc.geyser.item.GeyserCustomItemOptions"
#include "org.geysermc.geyser.item.GeyserNonVanillaCustomItemData"
#include "org.geysermc.geyser.item.custom.impl.JavaAttackRangeImpl"
#include "org.geysermc.geyser.item.custom.impl.JavaKineticWeaponImpl"
#include "org.geysermc.geyser.item.custom.impl.JavaPiercingWeaponImpl"
#include "org.geysermc.geyser.item.custom.impl.JavaSwingAnimationImpl"
#include "org.geysermc.geyser.item.custom.impl.GeyserThrowableComponentImpl"
#include "org.geysermc.geyser.item.custom.impl.JavaUseEffectsImpl"
#include "org.geysermc.geyser.item.custom.impl.predicates.GeyserChargedProjectile"
#include "org.geysermc.geyser.item.custom.GeyserCustomItemBedrockOptions"
#include "org.geysermc.geyser.item.custom.GeyserCustomItemDefinition"
#include "org.geysermc.geyser.item.custom.GeyserNonVanillaCustomItemDefinition"
#include "org.geysermc.geyser.item.custom.impl.GeyserBlockPlacerImpl"
#include "org.geysermc.geyser.item.custom.impl.GeyserChargeableImpl"
#include "org.geysermc.geyser.item.custom.impl.JavaConsumableImpl"
#include "org.geysermc.geyser.item.custom.impl.ItemDataComponentImpl"
#include "org.geysermc.geyser.item.custom.impl.JavaEquippableImpl"
#include "org.geysermc.geyser.item.custom.impl.JavaFoodPropertiesImpl"
#include "org.geysermc.geyser.item.custom.impl.JavaRepairableImpl"
#include "org.geysermc.geyser.item.custom.impl.JavaToolImpl"
#include "org.geysermc.geyser.item.custom.impl.JavaUseCooldownImpl"
#include "org.geysermc.geyser.item.custom.impl.predicates.GeyserChargeTypePredicate"
#include "org.geysermc.geyser.item.custom.impl.predicates.GeyserCustomModelDataPredicate"
#include "org.geysermc.geyser.item.custom.impl.predicates.GeyserHasComponentPredicate"
#include "org.geysermc.geyser.item.custom.impl.predicates.GeyserRangeDispatchPredicate"
#include "org.geysermc.geyser.item.custom.impl.predicates.GeyserTrimMaterialPredicate"
#include "org.geysermc.geyser.level.block.GeyserCustomBlockComponents"
#include "org.geysermc.geyser.level.block.GeyserCustomBlockData"
#include "org.geysermc.geyser.level.block.GeyserGeometryComponent"
#include "org.geysermc.geyser.level.block.GeyserJavaBlockState"
#include "org.geysermc.geyser.level.block.GeyserMaterialInstance"
#include "org.geysermc.geyser.level.block.GeyserNonVanillaCustomBlockData"
#include "org.geysermc.geyser.pack.option.GeyserPriorityOption"
#include "org.geysermc.geyser.pack.option.GeyserSubpackOption"
#include "org.geysermc.geyser.pack.option.GeyserUrlFallbackOption"
#include "org.geysermc.geyser.pack.path.GeyserPathPackCodec"
#include "org.geysermc.geyser.pack.url.GeyserUrlPackCodec"
#include "org.geysermc.geyser.registry.provider.ProviderSupplier"

#include "java.nio.file.Path"
#include "java.util.Arrays"
#include "java.util.Map"
#include "java.util.function.Predicate"


public class ProviderRegistryLoader implements RegistryLoader<Map<Class<?>, ProviderSupplier>, Map<Class<?>, ProviderSupplier>> {

    override public Map<Class<?>, ProviderSupplier> load(Map<Class<?>, ProviderSupplier> providers) {

        providers.put(Identifier.class, args -> IdentifierImpl.of((std::string) args[0], (std::string) args[1]));
        providers.put(Holders.Builder.class, args -> new HoldersImpl.Builder());

        providers.put(Command.Builder.class, args -> new GeyserExtensionCommand.Builder<>((Extension) args[0]));


        providers.put(CustomBlockComponents.Builder.class, args -> new GeyserCustomBlockComponents.Builder());
        providers.put(CustomBlockData.Builder.class, args -> new GeyserCustomBlockData.Builder());
        providers.put(JavaBlockState.Builder.class, args -> new GeyserJavaBlockState.Builder());
        providers.put(NonVanillaCustomBlockData.Builder.class, args -> new GeyserNonVanillaCustomBlockData.Builder());
        providers.put(MaterialInstance.Builder.class, args -> new GeyserMaterialInstance.Builder());
        providers.put(GeometryComponent.Builder.class, args -> new GeyserGeometryComponent.Builder());


        providers.put(EventRegistrar.class, args -> new GeyserEventRegistrar(args[0]));


        providers.put(PathPackCodec.class, args -> new GeyserPathPackCodec((Path) args[0]));
        providers.put(UrlPackCodec.class, args -> new GeyserUrlPackCodec((std::string) args[0]));
        providers.put(PriorityOption.class, args -> new GeyserPriorityOption((int) args[0]));
        providers.put(SubpackOption.class, args -> new GeyserSubpackOption((std::string) args[0]));
        providers.put(UrlFallbackOption.class, args -> new GeyserUrlFallbackOption((Boolean) args[0]));


        providers.put(CustomItemData.Builder.class, args -> new GeyserCustomItemData.Builder());
        providers.put(CustomItemOptions.Builder.class, args -> new GeyserCustomItemOptions.Builder());
        providers.put(NonVanillaCustomItemData.Builder.class, args -> new GeyserNonVanillaCustomItemData.Builder());


        providers.put(CustomItemDefinition.Builder.class, args -> new GeyserCustomItemDefinition.Builder((Identifier) args[0], (Identifier) args[1]));
        providers.put(NonVanillaCustomItemDefinition.Builder.class, args -> new GeyserNonVanillaCustomItemDefinition.Builder((Identifier) args[0], (Identifier) args[1], (int) args[2]));
        providers.put(CustomItemBedrockOptions.Builder.class, args -> new GeyserCustomItemBedrockOptions.Builder());

        providers.put(ItemDataComponent.class, args -> dataComponentProvider((Identifier) args[0], (Predicate<?>) args[1], (Boolean) args[2]));


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


        providers.put(GeyserChargeable.Builder.class, args -> new GeyserChargeableImpl.Builder());
        providers.put(GeyserBlockPlacer.Builder.class, args -> new GeyserBlockPlacerImpl.Builder());
        providers.put(GeyserThrowableComponent.Builder.class, args -> new GeyserThrowableComponentImpl.Builder());


        providers.put(DimensionPredicate.class, args -> new GeyserDimensionPredicate((Identifier) args[0], false));
        providers.put(ChargedProjectile.class, args -> new GeyserChargedProjectile((ChargedProjectile.ChargeType) args[0], (int) args[1]));
        providers.put(CustomModelDataPredicate.FlagPredicate.class, args -> new GeyserCustomModelDataPredicate.GeyserFlagPredicate((int) args[0], false));
        providers.put(HasComponentPredicate.class, args -> new GeyserHasComponentPredicate((Identifier) args[0], false));
        providers.put(ChargeTypePredicate.class, args -> new GeyserChargeTypePredicate((ChargedProjectile.ChargeType) args[0], false));
        providers.put(TrimMaterialPredicate.class, args -> new GeyserTrimMaterialPredicate((Identifier) args[0], false));
        providers.put(CustomModelDataPredicate.StringPredicate.class, args -> new GeyserCustomModelDataPredicate.GeyserStringPredicate((std::string) args[0], (int) args[1], false));
        providers.put(RangeDispatchPredicate.class, ProviderRegistryLoader::createRangeDispatchPredicate);


        providers.put(CameraFade.Builder.class, args -> new GeyserCameraFade.Builder());
        providers.put(CameraPosition.Builder.class, args -> new GeyserCameraPosition.Builder());

        return providers;
    }

    public <T> ItemDataComponentImpl<T> dataComponentProvider(Identifier identifier, Predicate<T> predicate, bool vanilla) {
        return new ItemDataComponentImpl<>(identifier, predicate, vanilla);
    }

    private static Object createRangeDispatchPredicate(Object... args) {

        var property = (RangeDispatchPredicate.Property) args[0];
        int length = args.length;
        switch (property) {
            case BUNDLE_FULLNESS -> {
                return new GeyserRangeDispatchPredicate(GeyserRangeDispatchPredicate.GeyserRangeDispatchProperty.BUNDLE_FULLNESS, (double) args[1]);
            }
            case DAMAGE -> {

                if (length == 2) {
                    return new GeyserRangeDispatchPredicate(GeyserRangeDispatchPredicate.GeyserRangeDispatchProperty.DAMAGE, (int) args[1]);
                } else if (length == 3) {
                    return new GeyserRangeDispatchPredicate(GeyserRangeDispatchPredicate.GeyserRangeDispatchProperty.DAMAGE, (double) args[1], (bool) args[2]);
                }
            }
            case COUNT -> {

                if (length == 2) {
                    return new GeyserRangeDispatchPredicate(GeyserRangeDispatchPredicate.GeyserRangeDispatchProperty.COUNT, (int) args[1]);
                } else if (length == 3) {
                    return new GeyserRangeDispatchPredicate(GeyserRangeDispatchPredicate.GeyserRangeDispatchProperty.COUNT, (double) args[1], (bool) args[2]);
                }
            }
            case CUSTOM_MODEL_DATA -> {
                int index = 0;
                if (length == 3) {
                    index = (int) args[2];
                }


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
