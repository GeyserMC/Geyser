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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.geysermc.geyser.item.tooltip.providers.ArmorTrimTooltip;
import org.geysermc.geyser.item.tooltip.providers.AttributeModifiersTooltip;
import org.geysermc.geyser.item.tooltip.providers.BannerPatternLayersTooltip;
import org.geysermc.geyser.item.tooltip.providers.BeesTooltip;
import org.geysermc.geyser.item.tooltip.providers.BlockStatePropertiesTooltip;
import org.geysermc.geyser.item.tooltip.providers.ChargedProjectilesTooltip;
import org.geysermc.geyser.item.tooltip.providers.ContainerContentsTooltip;
import org.geysermc.geyser.item.tooltip.providers.ContainerLootTooltip;
import org.geysermc.geyser.item.tooltip.providers.DyedItemColorTooltip;
import org.geysermc.geyser.item.tooltip.providers.FireworkExplosionTooltip;
import org.geysermc.geyser.item.tooltip.providers.FireworksTooltip;
import org.geysermc.geyser.item.tooltip.providers.InstrumentTooltip;
import org.geysermc.geyser.item.tooltip.providers.ItemEnchantmentsTooltip;
import org.geysermc.geyser.item.tooltip.providers.JukeboxPlayableTooltip;
import org.geysermc.geyser.item.tooltip.providers.LoreTooltip;
import org.geysermc.geyser.item.tooltip.providers.MapTooltip;
import org.geysermc.geyser.item.tooltip.providers.OminousBottleTooltip;
import org.geysermc.geyser.item.tooltip.providers.PotDecorationsTooltip;
import org.geysermc.geyser.item.tooltip.providers.PotionContentsTooltip;
import org.geysermc.geyser.item.tooltip.providers.SuspiciousStewTooltip;
import org.geysermc.geyser.item.tooltip.providers.TropicalFishPatternTooltip;
import org.geysermc.geyser.item.tooltip.providers.WrittenBookTooltip;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class TooltipProviders {
    private static final Component UNBREAKABLE = Component.translatable("item.unbreakable").color(NamedTextColor.BLUE);

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
        register(DataComponentTypes.TROPICAL_FISH_PATTERN, new TropicalFishPatternTooltip());
        register(DataComponentTypes.INSTRUMENT, new InstrumentTooltip());
        register(DataComponentTypes.MAP_ID, new MapTooltip());
        register(DataComponentTypes.BEES, new BeesTooltip());
        register(DataComponentTypes.CONTAINER_LOOT, new ContainerLootTooltip());
        register(DataComponentTypes.CONTAINER, new ContainerContentsTooltip());
        register(DataComponentTypes.BANNER_PATTERNS, new BannerPatternLayersTooltip());
        register(DataComponentTypes.POT_DECORATIONS, new PotDecorationsTooltip());
        register(DataComponentTypes.WRITTEN_BOOK_CONTENT, new WrittenBookTooltip());
        register(DataComponentTypes.CHARGED_PROJECTILES, new ChargedProjectilesTooltip());
        register(DataComponentTypes.FIREWORKS, new FireworksTooltip());
        register(DataComponentTypes.FIREWORK_EXPLOSION, new FireworkExplosionTooltip());
        register(DataComponentTypes.POTION_CONTENTS, new PotionContentsTooltip());
        register(DataComponentTypes.JUKEBOX_PLAYABLE, new JukeboxPlayableTooltip());
        register(DataComponentTypes.TRIM, new ArmorTrimTooltip());
        register(DataComponentTypes.STORED_ENCHANTMENTS, new ItemEnchantmentsTooltip());
        register(DataComponentTypes.ENCHANTMENTS, new ItemEnchantmentsTooltip());
        register(DataComponentTypes.DYED_COLOR, new DyedItemColorTooltip());
        register(DataComponentTypes.LORE, new LoreTooltip());
        register(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleTooltip());
        register(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, new SuspiciousStewTooltip());
        register(DataComponentTypes.BLOCK_STATE, new BlockStatePropertiesTooltip());
        register(DataComponentTypes.ATTRIBUTE_MODIFIERS, new AttributeModifiersTooltip());
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
        // TODO
    }

    public static void addTooltips(TooltipContext context, Consumer<Component> adder) {
        tryAddTooltip(context, DataComponentTypes.TROPICAL_FISH_PATTERN, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.INSTRUMENT, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.MAP_ID, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.BEES, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.CONTAINER_LOOT, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.CONTAINER, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.BANNER_PATTERNS, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.POT_DECORATIONS, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.WRITTEN_BOOK_CONTENT, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.CHARGED_PROJECTILES, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.FIREWORKS, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.FIREWORK_EXPLOSION, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.POTION_CONTENTS, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.JUKEBOX_PLAYABLE, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.TRIM, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.STORED_ENCHANTMENTS, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.ENCHANTMENTS, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.DYED_COLOR, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.LORE, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.ATTRIBUTE_MODIFIERS, adder, TooltipProviders::getTooltipProvider);
        if (context.components().get(DataComponentTypes.UNBREAKABLE) != null && context.options().showInTooltip(DataComponentTypes.UNBREAKABLE)) {
            adder.accept(UNBREAKABLE);
        }

        tryAddTooltip(context, DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, adder, TooltipProviders::getTooltipProvider);
        tryAddTooltip(context, DataComponentTypes.BLOCK_STATE, adder, TooltipProviders::getTooltipProvider);
        // TODO spawner
        // TODO can break/can place

        if (context.advanced()) {
            addAdvancedTooltips(context, adder);
        }

        // TODO op warning
    }

    private static <T> void tryAddTooltip(TooltipContext context, DataComponentType<T> component, Consumer<Component> adder,
                                          Function<DataComponentType<T>, ComponentTooltipProvider<T>> providerGetter) {
        if (context.options().showInTooltip(component)) {
            T value = context.components().get(component);
            if (value != null) {
                ComponentTooltipProvider<T> provider = providerGetter.apply(component);
                if (provider != null) {
                    provider.addTooltip(context, adder, value);
                }
            }
        }
    }

    private static void addAdvancedTooltips(TooltipContext context, Consumer<Component> adder) {
        int maxDamage = context.components().getOrDefault(DataComponentTypes.MAX_DAMAGE, 0);
        int damage = context.components().getOrDefault(DataComponentTypes.DAMAGE, 0);
        if (maxDamage > 0 && damage > 0 && context.options().showInTooltip(DataComponentTypes.DAMAGE)) {
            adder.accept(Component.translatable("item.durability", Component.text(maxDamage - damage), Component.text(maxDamage)));
        }

        adder.accept(Component.text(context.item().javaKey().asString()).color(NamedTextColor.DARK_GRAY));

        int components = context.components().getDataComponents().size();
        if (components > 0) {
            adder.accept(Component.translatable("item.components", Component.text(components)).color(NamedTextColor.DARK_GRAY));
        }
    }
}
