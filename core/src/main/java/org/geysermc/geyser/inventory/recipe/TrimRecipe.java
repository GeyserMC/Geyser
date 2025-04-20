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

import net.kyori.adventure.text.Component;
import org.cloudburstmc.protocol.bedrock.data.TrimMaterial;
import org.cloudburstmc.protocol.bedrock.data.TrimPattern;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemTagDescriptor;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ArmorTrim;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ProvidesTrimMaterial;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores information on trim materials and patterns, including smithing armor hacks for pre-1.20.
 */
public final class TrimRecipe {
    private static final Map<ProvidesTrimMaterial, Item> trimMaterialProviders = new HashMap<>();

    // For CraftingDataPacket
    public static final String ID = "minecraft:smithing_armor_trim";
    public static final ItemDescriptorWithCount BASE = tagDescriptor("minecraft:trimmable_armors");
    public static final ItemDescriptorWithCount ADDITION = tagDescriptor("minecraft:trim_materials");
    public static final ItemDescriptorWithCount TEMPLATE = tagDescriptor("minecraft:trim_templates");

    public static TrimMaterial readTrimMaterial(RegistryEntryContext context) {
        String key = context.id().asMinimalString();

        // Color is used when hovering over the item
        // Find the nearest legacy color from the style Java gives us to work with
        Component description = MessageTranslator.componentFromNbtTag(context.data().get("description"));
        String legacy = MessageTranslator.convertMessage(Component.space().style(description.style()));

        int networkId = context.getNetworkId(context.id());
        ItemMapping trimItem = null;
        for (ProvidesTrimMaterial provider : materialProviders().keySet()) {
            Holder<ArmorTrim.TrimMaterial> materialHolder = provider.materialHolder();
            if (context.id().equals(provider.materialLocation()) || (materialHolder != null && materialHolder.isId() && materialHolder.id() == networkId)) {
                trimItem = context.session().getItemMappings().getMapping(materialProviders().get(provider));
                break;
            }
        }

        if (trimItem == null) {
            // This happens for custom trim materials, not sure what to do here.
            GeyserImpl.getInstance().getLogger().debug("Unable to found trim material item for material " + context.id());
            trimItem = ItemMapping.AIR;
        }

        // Just pick out the resulting color code, without RESET in front.
        return new TrimMaterial(key, legacy.substring(2).trim(), trimItem.getBedrockIdentifier());
    }

    // TODO this is WRONG. this changed. FIXME in 1.21.5
    public static TrimPattern readTrimPattern(RegistryEntryContext context) {
        String key = context.id().asMinimalString();

        String itemIdentifier = context.data().getString("template_item");
        ItemMapping itemMapping = context.session().getItemMappings().getMapping(itemIdentifier);
        if (itemMapping == null) {
            // This should never happen so not sure what to do here.
            itemMapping = ItemMapping.AIR;
        }
        return new TrimPattern(itemMapping.getBedrockIdentifier(), key);
    }

    private TrimRecipe() {
        //no-op
    }

    // Lazy initialise
    private static Map<ProvidesTrimMaterial, Item> materialProviders() {
        if (trimMaterialProviders.isEmpty()) {
            for (Item item : Registries.JAVA_ITEMS.get()) {
                ProvidesTrimMaterial provider = item.getComponent(DataComponentTypes.PROVIDES_TRIM_MATERIAL);
                if (provider != null) {
                    trimMaterialProviders.put(provider, item);
                }
            }
        }
        return trimMaterialProviders;
    }

    private static ItemDescriptorWithCount tagDescriptor(String tag) {
        return new ItemDescriptorWithCount(new ItemTagDescriptor(tag), 1);
    }
}
