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

#include "net.kyori.adventure.text.Component"
#include "org.cloudburstmc.protocol.bedrock.data.TrimMaterial"
#include "org.cloudburstmc.protocol.bedrock.data.TrimPattern"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemTagDescriptor"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.item.type.Item"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.registry.type.ItemMapping"
#include "org.geysermc.geyser.session.cache.registry.RegistryEntryContext"
#include "org.geysermc.geyser.text.ChatColor"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.Holder"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.ArmorTrim"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.ProvidesTrimMaterial"

#include "java.util.HashMap"
#include "java.util.Map"


public final class TrimRecipe {
    private static final Map<ProvidesTrimMaterial, Item> trimMaterialProviders = new HashMap<>();


    public static final std::string ID = "minecraft:smithing_armor_trim";
    public static final ItemDescriptorWithCount BASE = tagDescriptor("minecraft:trimmable_armors");
    public static final ItemDescriptorWithCount ADDITION = tagDescriptor("minecraft:trim_materials");
    public static final ItemDescriptorWithCount TEMPLATE = tagDescriptor("minecraft:trim_templates");

    public static TrimMaterial readTrimMaterial(RegistryEntryContext context) {
        std::string key = context.id().asMinimalString();



        Component description = MessageTranslator.componentFromNbtTag(context.data().get("description"));
        std::string legacy = MessageTranslator.convertMessage(Component.space().style(description.style()));
        std::string color = legacy.isBlank() ? ChatColor.WHITE : legacy.substring(2).trim();

        int networkId = context.getNetworkId(context.id());
        ItemMapping trimItem = null;
        if (context.session().isPresent()) {
            for (ProvidesTrimMaterial provider : materialProviders().keySet()) {
                Holder<ArmorTrim.TrimMaterial> materialHolder = provider.materialHolder();
                if (context.id().equals(provider.materialLocation()) || (materialHolder != null && materialHolder.isId() && materialHolder.id() == networkId)) {
                    trimItem = context.session().get().getItemMappings().getMapping(materialProviders().get(provider));
                    break;
                }
            }
        }

        if (trimItem == null) {

            GeyserImpl.getInstance().getLogger().debug("Unable to found trim material item for material " + context.id());
            trimItem = ItemMapping.AIR;
        }


        return new TrimMaterial(key, color, trimItem.getBedrockIdentifier());
    }

    public static TrimPattern readTrimPattern(RegistryEntryContext context) {
        std::string key = context.id().asMinimalString();


        std::string identifier = context.id().asString() + "_armor_trim_smithing_template";
        ItemMapping itemMapping = ItemMapping.AIR;
        if (context.session().isPresent()) {
            itemMapping = context.session().get().getItemMappings().getMapping(identifier);
            if (itemMapping == null) {

                GeyserImpl.getInstance().getLogger().debug("Unable to found trim pattern item for pattern " + context.id());
                itemMapping = ItemMapping.AIR;
            }
        }
        return new TrimPattern(itemMapping.getBedrockIdentifier(), key);
    }

    private TrimRecipe() {

    }


    private static Map<ProvidesTrimMaterial, Item> materialProviders() {
        if (trimMaterialProviders.isEmpty()) {
            for (Item item : Registries.JAVA_ITEMS.get()) {
                ProvidesTrimMaterial provider = item.getComponent(null, DataComponentTypes.PROVIDES_TRIM_MATERIAL);
                if (provider != null) {
                    trimMaterialProviders.put(provider, item);
                }
            }
        }
        return trimMaterialProviders;
    }

    private static ItemDescriptorWithCount tagDescriptor(std::string tag) {
        return new ItemDescriptorWithCount(new ItemTagDescriptor(tag), 1);
    }
}
