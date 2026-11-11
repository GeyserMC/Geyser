/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.inventory.recipe;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.protocol.bedrock.data.TrimMaterial;
import org.cloudburstmc.protocol.bedrock.data.TrimPattern;
import org.cloudburstmc.protocol.bedrock.packet.CraftingDataPacket;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.java.BuiltInJavaRegistries;
import org.geysermc.geyser.registry.java.JavaRegistries;
import org.geysermc.geyser.registry.java.RegistryEntryData;
import org.geysermc.geyser.registry.java.reader.JavaRegistryReader;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ArmorTrim;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.RecipeDisplayEntry;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.SmithingRecipeDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.ItemStackSlotDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.recipe.display.slot.SmithingTrimDemoSlotDisplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stores information on trim materials and patterns, including smithing armor hacks for pre-1.20.
 */
@Accessors(fluent = true)
public final class TrimRecipes {
    private static final String JAVA_TRIM_PATTERN_TEMPLATE_SUFFIX = "_armor_trim_smithing_template";

    private final List<GeyserSmithingRecipe.Transform> smithingTransformRecipes = new ArrayList<>();
    private final List<GeyserSmithingRecipe.Trim> smithingTrimRecipes = new ObjectArrayList<>();
    @Getter
    private final List<TrimMaterial> bedrockTrimMaterials = new ObjectArrayList<>();
    @Getter
    private final List<TrimPattern> bedrockTrimPatterns = new ObjectArrayList<>();

    public void initializeBedrockTrimRecipes(GeyserSession session) {
        smithingTransformRecipes.clear();
        smithingTrimRecipes.clear();
        bedrockTrimMaterials.clear();
        bedrockTrimPatterns.clear();

        Map<Holder<ArmorTrim.TrimMaterial>, Item> trimMaterialProviders = getTrimMaterialProviders(session);

        session.javaRegistries().registry(JavaRegistries.TRIM_MATERIAL).forEachEntry(material -> bedrockTrimMaterials.add(translateJavaTrimMaterial(session, material, trimMaterialProviders)));
        session.javaRegistries().registry(JavaRegistries.TRIM_PATTERN).forEachEntry(pattern -> bedrockTrimPatterns.add(translateJavaTrimPattern(session, pattern)));
    }

    public GeyserSmithingRecipe<?> addSmithingRecipe(RecipeDisplayEntry entry, SmithingRecipeDisplay recipe, int netId) {
        if (recipe.result() instanceof SmithingTrimDemoSlotDisplay) {
            GeyserSmithingRecipe.Trim trimRecipe = GeyserSmithingRecipe.Trim.of(entry.id(), netId, recipe);
            smithingTrimRecipes.add(trimRecipe);
            return trimRecipe;
        } else {
            GeyserSmithingRecipe.Transform transformRecipe = GeyserSmithingRecipe.Transform.of(entry.id(), netId, recipe);
            smithingTransformRecipes.add(transformRecipe);
            return transformRecipe;
        }
    }

    public Optional<GeyserSmithingRecipe<?>> addDiscoveredRecipe(GeyserSession session, GeyserItemStack template, GeyserItemStack input,
                                                                 GeyserItemStack material, ItemStack discoveredOutput) {
        boolean isVanillaTrimPattern = template.asItem().javaKey().value().endsWith(JAVA_TRIM_PATTERN_TEMPLATE_SUFFIX);
        if (!isVanillaTrimPattern && !template.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
            // Technically we should probably also do this for custom items, but last I checked Bedrock doesn't even support that.
            return Optional.empty();
        }

        if (isVanillaTrimPattern) {
            for (GeyserSmithingRecipe.Trim recipe : smithingTrimRecipes) {
                if (recipe.matches(session, template, input, material)) {
                    // The client already recognizes this item.
                    return Optional.empty();
                }
            }

            GeyserSmithingRecipe.Trim trimRecipe = new GeyserSmithingRecipe.Trim(
                ThreadLocalRandom.current().nextInt(),
                session.getLastRecipeNetId().incrementAndGet(),
                template.asIngredient(),
                input.asIngredient(),
                material.asIngredient()
            );
            smithingTrimRecipes.add(trimRecipe);
            return Optional.of(trimRecipe);
        } else {
            for (GeyserSmithingRecipe.Transform recipe : smithingTransformRecipes) {
                if (recipe.matches(session, template, input, material)) {
                    // The client already recognizes this item.
                    return Optional.empty();
                }
            }

            GeyserSmithingRecipe.Transform transformRecipe = new GeyserSmithingRecipe.Transform(
                ThreadLocalRandom.current().nextInt(),
                session.getLastRecipeNetId().incrementAndGet(),
                template.asIngredient(),
                input.asIngredient(),
                material.asIngredient(),
                new ItemStackSlotDisplay(discoveredOutput)
            );
            smithingTransformRecipes.add(transformRecipe);
            return Optional.of(transformRecipe);
        }
    }

    public void addAllSmithingRecipes(GeyserSession session, CraftingDataPacket craftingDataPacket) {
        smithingTransformRecipes.forEach(transform -> transform.addToPacket(session, craftingDataPacket));
        smithingTrimRecipes.forEach(trim -> trim.addToPacket(session, craftingDataPacket));
    }

    private static TrimMaterial translateJavaTrimMaterial(GeyserSession session, RegistryEntryData<ArmorTrim.TrimMaterial> java, Map<Holder<ArmorTrim.TrimMaterial>, Item> trimMaterialProviders) {
        String key = java.key().asMinimalString();

        // Color is used when hovering over the item
        // Find the nearest legacy color from the style Java gives us to work with
        String legacy = MessageTranslator.convertMessage(Component.space().style(java.data().description().style()));
        // Just pick out the resulting color code, without RESET in front.
        String color = legacy.isBlank() ? ChatColor.WHITE : legacy.substring(2).trim();

        ItemMapping trimItem = null;
        for (Holder<ArmorTrim.TrimMaterial> provider : trimMaterialProviders.keySet()) {
            if ((provider.isCustom() && java.data().paletteId().equals(provider.custom().paletteId())) || (provider.isId() && provider.id() == java.id())) {
                trimItem = session.getItemMappings().getMapping(trimMaterialProviders.get(provider));
                break;
            }
        }

        if (trimItem == null) {
            // This happens in testing and for custom trim materials, not sure what to do for the latter.
            GeyserImpl.getInstance().getLogger().debug("Unable to found trim material item for material " + java.key());
            trimItem = ItemMapping.AIR;
        }

        return new TrimMaterial(key, color, trimItem.getBedrockIdentifier());
    }

    private static TrimPattern translateJavaTrimPattern(GeyserSession session, RegistryEntryData<ArmorTrim.TrimPattern> java) {
        String key = java.key().asMinimalString();

        // Not ideal, Java edition also gives us a translatable description... Bedrock wants the template item
        String identifier = java.key().asString() + JAVA_TRIM_PATTERN_TEMPLATE_SUFFIX;
        ItemMapping itemMapping = session.getItemMappings().getMapping(identifier);
        if (itemMapping == null) {
            // This happens for custom trim patterns, not sure what to do here.
            GeyserImpl.getInstance().getLogger().debug("Unable to found trim pattern item for pattern " + java.key());
            itemMapping = ItemMapping.AIR;
        }
        return new TrimPattern(itemMapping.getBedrockIdentifier(), key);
    }

    public static ArmorTrim.TrimMaterial readTrimMaterial(JavaRegistryReader.Context context) {
        return new ArmorTrim.TrimMaterial(MinecraftKey.key(context.dataAsMap().getString("palette_id")),
            context.parseDescription());
    }

    public static ArmorTrim.TrimPattern readTrimPattern(JavaRegistryReader.Context context) {
        return new ArmorTrim.TrimPattern(MinecraftKey.key(context.dataAsMap().getString("asset_id")),
            context.parseDescription(),
            context.dataAsMap().getBoolean("decal", false));
    }

    private static Map<Holder<ArmorTrim.TrimMaterial>, Item> getTrimMaterialProviders(GeyserSession session) {
        Map<Holder<ArmorTrim.TrimMaterial>, Item> trimMaterialProviders = new HashMap<>();
        for (Item item : BuiltInJavaRegistries.ITEM) {
            Holder<ArmorTrim.TrimMaterial> provider = item.getComponent(session.getComponentCache(), DataComponentTypes.PROVIDES_TRIM_MATERIAL);
            if (provider != null) {
                trimMaterialProviders.put(provider, item);
            }
        }
        return trimMaterialProviders;
    }
}
