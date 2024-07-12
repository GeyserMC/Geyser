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
import net.kyori.adventure.text.format.TextColor;
import org.cloudburstmc.protocol.bedrock.data.TrimMaterial;
import org.cloudburstmc.protocol.bedrock.data.TrimPattern;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemTagDescriptor;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.translator.text.MessageTranslator;

/**
 * Stores information on trim materials and patterns, including smithing armor hacks for pre-1.20.
 */
public final class TrimRecipe {
    // For CraftingDataPacket
    public static final String ID = "minecraft:smithing_armor_trim";
    public static final ItemDescriptorWithCount BASE = tagDescriptor("minecraft:trimmable_armors");
    public static final ItemDescriptorWithCount ADDITION = tagDescriptor("minecraft:trim_materials");
    public static final ItemDescriptorWithCount TEMPLATE = tagDescriptor("minecraft:trim_templates");

    public static TrimMaterial readTrimMaterial(RegistryEntryContext context) {
        String key = context.id().asMinimalString();

        // Color is used when hovering over the item
        // Find the nearest legacy color from the RGB Java gives us to work with
        // Also yes this is a COMPLETE hack but it works ok!!!!!
        String colorTag = context.data().getCompound("description").getString("color");
        TextColor color = TextColor.fromHexString(colorTag);
        String legacy = MessageTranslator.convertMessage(Component.space().color(color));

        String itemIdentifier = context.data().getString("ingredient");
        ItemMapping itemMapping = context.session().getItemMappings().getMapping(itemIdentifier);
        if (itemMapping == null) {
            // This should never happen so not sure what to do here.
            itemMapping = ItemMapping.AIR;
        }
        // Just pick out the resulting color code, without RESET in front.
        return new TrimMaterial(key, legacy.substring(2).trim(), itemMapping.getBedrockIdentifier());
    }

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

    private static ItemDescriptorWithCount tagDescriptor(String tag) {
        return new ItemDescriptorWithCount(new ItemTagDescriptor(tag), 1);
    }
}
