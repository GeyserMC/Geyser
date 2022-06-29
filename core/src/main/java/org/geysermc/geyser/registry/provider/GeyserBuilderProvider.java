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

package org.geysermc.geyser.registry.provider;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.item.custom.CustomItemData;
import org.geysermc.geyser.api.item.custom.CustomItemOptions;
import org.geysermc.geyser.api.item.custom.NonVanillaCustomItemData;
import org.geysermc.geyser.api.provider.BuilderProvider;
import org.geysermc.geyser.command.GeyserCommandManager;
import org.geysermc.geyser.item.GeyserCustomItemData;
import org.geysermc.geyser.item.GeyserCustomItemOptions;
import org.geysermc.geyser.item.GeyserNonVanillaCustomItemData;
import org.geysermc.geyser.registry.ProviderRegistries;
import org.geysermc.geyser.registry.SimpleMappedRegistry;

import java.util.Map;

public class GeyserBuilderProvider extends AbstractProvider implements BuilderProvider {
    public static GeyserBuilderProvider INSTANCE = new GeyserBuilderProvider();

    private GeyserBuilderProvider() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerProviders(Map<Class<?>, ProviderSupplier> providers) {
        providers.put(Command.Builder.class, args -> new GeyserCommandManager.CommandBuilder<>((Class<? extends CommandSource>) args[0]));


        providers.put(CustomItemData.Builder.class, args -> new GeyserCustomItemData.CustomItemDataBuilder());
        providers.put(CustomItemOptions.Builder.class, args -> new GeyserCustomItemOptions.CustomItemOptionsBuilder());
        providers.put(NonVanillaCustomItemData.Builder.class, args -> new GeyserNonVanillaCustomItemData.NonVanillaCustomItemDataBuilder());
    }

    @Override
    public SimpleMappedRegistry<Class<?>, ProviderSupplier> providerRegistry() {
        return ProviderRegistries.BUILDERS;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <B extends T, T> @NonNull B provideBuilder(@NonNull Class<T> builderClass, @Nullable Object... args) {
        return (B) this.providerRegistry().get(builderClass).create(args);
    }
}
