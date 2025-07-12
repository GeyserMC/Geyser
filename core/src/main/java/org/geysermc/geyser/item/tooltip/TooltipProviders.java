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

package org.geysermc.geyser.item.tooltip;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class TooltipProviders {
    private static final Map<DataComponentType<?>, ComponentTooltipProvider<?>> NAME_PROVIDERS = new Reference2ObjectOpenHashMap<>();
    private static final Map<DataComponentType<?>, ComponentTooltipProvider<?>> PROVIDERS = new Reference2ObjectOpenHashMap<>();

    private static <T> void register(DataComponentType<T> component, ComponentTooltipProvider<T> provider) {
        internalRegister(PROVIDERS, component, provider);
    }

    private static <T> void registerName(DataComponentType<T> component, ComponentTooltipProvider<T> provider) {
        internalRegister(NAME_PROVIDERS, component, provider);
    }

    private static <T> void internalRegister(Map<DataComponentType<?>, ComponentTooltipProvider<?>> map,
                                             DataComponentType<T> component, ComponentTooltipProvider<T> provider) {
        if (map.containsKey(component)) {
            throw new IllegalArgumentException("Component " + component + " already has a tooltip provider registered!");
        }
        map.put(component, provider);
    }

    static {
        // TODO charged projectiles (requires recursive)
    }

    @Nullable
    public static <T> ComponentTooltipProvider<T> getNameTooltipProvider(DataComponentType<T> component) {
        return (ComponentTooltipProvider<T>) NAME_PROVIDERS.get(component);
    }

    @Nullable
    public static <T> ComponentTooltipProvider<T> getTooltipProvider(DataComponentType<T> component) {
        return (ComponentTooltipProvider<T>) PROVIDERS.get(component);
    }

    // TODO tooltips for default components?
    public static void addNameTooltips(TooltipContext context, DataComponents componentPatch, Consumer<String> adder) {
        // TODO what's more efficient? looping over components or looping over providers?
        for (DataComponentType<?> component : NAME_PROVIDERS.keySet()) {
            tryAddTooltip(context, componentPatch, component, adder, TooltipProviders::getNameTooltipProvider);
        }
    }

    public static void addTooltips(TooltipContext context, DataComponents componentPatch, Consumer<String> adder) {
        for (DataComponentType<?> component : NAME_PROVIDERS.keySet()) {
            tryAddTooltip(context, componentPatch, component, adder, TooltipProviders::getTooltipProvider);
        }
    }

    private static <T> void tryAddTooltip(TooltipContext context, DataComponents componentPatch, DataComponentType<T> component, Consumer<String> adder,
                                          Function<DataComponentType<T>, ComponentTooltipProvider<T>> providerGetter) {
        T value = componentPatch.get(component);
        if (value != null) {
            ComponentTooltipProvider<T> provider = providerGetter.apply(component);
            if (provider != null) {
                provider.addTooltip(context, tooltip -> adder.accept(MessageTranslator.convertMessage(tooltip, context.session().locale())), value);
            }
        }
    }
}
