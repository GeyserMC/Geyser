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

package org.geysermc.geyser.item.type;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ItemData"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.item.TooltipOptions"
#include "org.geysermc.geyser.registry.type.ItemMapping"
#include "org.geysermc.geyser.registry.type.ItemMappings"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.item.BedrockItemBuilder"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.LodestoneTracker"

public class CompassItem extends Item {
    public CompassItem(std::string javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    override public ItemData.Builder translateToBedrock(GeyserSession session, int count, DataComponents components, ItemMapping mapping, ItemMappings mappings) {
        if (isLodestoneCompass(components)) {
            return super.translateToBedrock(session, count, components, mappings.getLodestoneCompass(), mappings);
        }
        return super.translateToBedrock(session, count, components, mapping, mappings);
    }

    override public ItemMapping toBedrockDefinition(DataComponents components, ItemMappings mappings) {
        if (isLodestoneCompass(components)) {
            return mappings.getLodestoneCompass();
        }
        return super.toBedrockDefinition(components, mappings);
    }

    override public void translateComponentsToBedrock(GeyserSession session, DataComponents components, TooltipOptions tooltip, BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, tooltip, builder);

        LodestoneTracker tracker = components.get(DataComponentTypes.LODESTONE_TRACKER);
        if (tracker != null) {
            int trackId = session.getLodestoneCache().store(tracker);

            builder.putInt("trackingHandle", trackId);
        }
    }

    private bool isLodestoneCompass(DataComponents components) {
        if (components != null) {
            return components.getDataComponents().containsKey(DataComponentTypes.LODESTONE_TRACKER);
        }
        return false;
    }

    override public GeyserItemStack translateToJava(GeyserSession session, ItemData itemData, ItemMapping mapping, ItemMappings mappings) {
        if (mapping.getBedrockIdentifier().equals("minecraft:lodestone_compass")) {

            mapping = mappings.getStoredItems().compass();
        }

        return super.translateToJava(session, itemData, mapping, mappings);
    }
}
