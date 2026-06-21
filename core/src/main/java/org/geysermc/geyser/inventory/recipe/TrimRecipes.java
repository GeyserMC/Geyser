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
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemTagDescriptor;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.session.cache.registry.RegistryEntryData;
import org.geysermc.geyser.text.ChatColor;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ArmorTrim;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores information on trim materials and patterns, including smithing armor hacks for pre-1.20.
 */
@Accessors(fluent = true)
public final class TrimRecipes {
    // For CraftingDataPacket
    public static final String ID = "minecraft:smithing_armor_trim";
    public static final ItemDescriptorWithCount BASE = tagDescriptor("minecraft:trimmable_armors");
    public static final ItemDescriptorWithCount ADDITION = tagDescriptor("minecraft:trim_materials");
    public static final ItemDescriptorWithCount TEMPLATE = tagDescriptor("minecraft:trim_templates");

    @Getter
    private final List<TrimMaterial> bedrockTrimMaterials = new ObjectArrayList<>();
    @Getter
    private final List<TrimPattern> bedrockTrimPatterns = new ObjectArrayList<>();

    public void initializeBedrockTrimRecipes(GeyserSession session) {
        bedrockTrimMaterials.clear();
        bedrockTrimPatterns.clear();

        Map<Holder<ArmorTrim.TrimMaterial>, Item> trimMaterialProviders = getTrimMaterialProviders(session);

        session.getRegistryCache().registry(JavaRegistries.TRIM_MATERIAL).forEach(material -> bedrockTrimMaterials.add(translateJavaTrimMaterial(session, material, trimMaterialProviders)));
        session.getRegistryCache().registry(JavaRegistries.TRIM_PATTERN).forEach(pattern -> bedrockTrimPatterns.add(translateJavaTrimPattern(session, pattern)));
    }

    private static TrimMaterial translateJavaTrimMaterial(GeyserSession session, RegistryEntryData<ArmorTrim.TrimMaterial> java, Map<Holder<ArmorTrim.TrimMaterial>, Item> trimMaterialProviders) {
        String key = java.key().asMinimalString();

        // Color is used when hovering over the item
        // Find the nearest legacy color from the style Java gives us to work with
        String legacy = MessageTranslator.convertMessage(Component.space().style(java.data().description().style()));
        String color = legacy.isBlank() ? ChatColor.WHITE : legacy.substring(2).trim();

        ItemMapping trimItem = null;
        for (Holder<ArmorTrim.TrimMaterial> provider : trimMaterialProviders.keySet()) {
            if ((provider.isCustom() && java.data().assetBase().equals(provider.custom().assetBase())) || (provider.isId() && provider.id() == java.id())) {
                trimItem = session.getItemMappings().getMapping(trimMaterialProviders.get(provider));
                break;
            }
        }

        if (trimItem == null) {
            // This happens in testing and for custom trim materials, not sure what to do for the latter.
            GeyserImpl.getInstance().getLogger().debug("Unable to found trim material item for material " + java.key());
            trimItem = ItemMapping.AIR;
        }

        // Just pick out the resulting color code, without RESET in front.
        return new TrimMaterial(key, color, trimItem.getBedrockIdentifier());
    }

    private static TrimPattern translateJavaTrimPattern(GeyserSession session, RegistryEntryData<ArmorTrim.TrimPattern> java) {
        String key = java.key().asMinimalString();

        // Not ideal, Java edition also gives us a translatable description... Bedrock wants the template item
        String identifier = java.key().asString() + "_armor_trim_smithing_template";
        ItemMapping itemMapping = ItemMapping.AIR;
        itemMapping = session.getItemMappings().getMapping(identifier);
        if (itemMapping == null) {
            // This should never happen so not sure what to do here.
            GeyserImpl.getInstance().getLogger().debug("Unable to found trim pattern item for pattern " + java.key());
            itemMapping = ItemMapping.AIR;
        }
        return new TrimPattern(itemMapping.getBedrockIdentifier(), key);
    }

    public static ArmorTrim.TrimMaterial readTrimMaterial(RegistryEntryContext context) {
        // Not parsing override_armor_assets as we don't use it and can safely pass an empty map instead
        return new ArmorTrim.TrimMaterial(context.data().getString("asset_name"), Map.of(), MessageTranslator.componentFromNbtTag(context.data().get("description")));
    }

    public static ArmorTrim.TrimPattern readTrimPattern(RegistryEntryContext context) {
        return new ArmorTrim.TrimPattern(MinecraftKey.key(context.data().getString("asset_id")), MessageTranslator.componentFromNbtTag(context.data().get("description")), context.data().getBoolean("decal", false));
    }

    private static Map<Holder<ArmorTrim.TrimMaterial>, Item> getTrimMaterialProviders(GeyserSession session) {
        Map<Holder<ArmorTrim.TrimMaterial>, Item> trimMaterialProviders = new HashMap<>();
        for (Item item : Registries.JAVA_ITEMS.get()) {
            Holder<ArmorTrim.TrimMaterial> provider = item.getComponent(session.getComponentCache(), DataComponentTypes.PROVIDES_TRIM_MATERIAL);
            if (provider != null) {
                trimMaterialProviders.put(provider, item);
            }
        }
        return trimMaterialProviders;
    }

    private static ItemDescriptorWithCount tagDescriptor(String tag) {
        return new ItemDescriptorWithCount(new ItemTagDescriptor(tag), 1);
    }
}
