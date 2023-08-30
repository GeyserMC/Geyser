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
import org.geysermc.geyser.api.pack.PathPackCodec;
import org.geysermc.geyser.command.GeyserCommandManager;
import org.geysermc.geyser.event.GeyserEventRegistrar;
import org.geysermc.geyser.item.GeyserCustomItemData;
import org.geysermc.geyser.item.GeyserCustomItemOptions;
import org.geysermc.geyser.item.GeyserNonVanillaCustomItemData;
import org.geysermc.geyser.level.block.GeyserCustomBlockComponents;
import org.geysermc.geyser.level.block.GeyserCustomBlockData;
import org.geysermc.geyser.level.block.GeyserGeometryComponent;
import org.geysermc.geyser.level.block.GeyserJavaBlockState;
import org.geysermc.geyser.level.block.GeyserMaterialInstance;
import org.geysermc.geyser.level.block.GeyserNonVanillaCustomBlockData;
import org.geysermc.geyser.pack.path.GeyserPathPackCodec;
import org.geysermc.geyser.registry.provider.ProviderSupplier;

import java.nio.file.Path;
import java.util.Map;

/**
 * Registers the provider data from the provider.
 */
public class ProviderRegistryLoader implements RegistryLoader<Map<Class<?>, ProviderSupplier>, Map<Class<?>, ProviderSupplier>> {

    @Override
    public Map<Class<?>, ProviderSupplier> load(Map<Class<?>, ProviderSupplier> providers) {
        // misc
        providers.put(Command.Builder.class, args -> new GeyserCommandManager.CommandBuilder<>((Extension) args[0]));

        providers.put(CustomBlockComponents.Builder.class, args -> new GeyserCustomBlockComponents.CustomBlockComponentsBuilder());
        providers.put(CustomBlockData.Builder.class, args -> new GeyserCustomBlockData.CustomBlockDataBuilder());
        providers.put(JavaBlockState.Builder.class, args -> new GeyserJavaBlockState.JavaBlockStateBuilder());
        providers.put(NonVanillaCustomBlockData.Builder.class, args -> new GeyserNonVanillaCustomBlockData.NonVanillaCustomBlockDataBuilder());
        providers.put(MaterialInstance.Builder.class, args -> new GeyserMaterialInstance.MaterialInstanceBuilder());
        providers.put(GeometryComponent.Builder.class, args -> new GeyserGeometryComponent.GeometryComponentBuilder());

        providers.put(EventRegistrar.class, args -> new GeyserEventRegistrar(args[0]));
        providers.put(PathPackCodec.class, args -> new GeyserPathPackCodec((Path) args[0]));

        // items
        providers.put(CustomItemData.Builder.class, args -> new GeyserCustomItemData.CustomItemDataBuilder());
        providers.put(CustomItemOptions.Builder.class, args -> new GeyserCustomItemOptions.CustomItemOptionsBuilder());
        providers.put(NonVanillaCustomItemData.Builder.class, args -> new GeyserNonVanillaCustomItemData.NonVanillaCustomItemDataBuilder());

        return providers;
    }
}
