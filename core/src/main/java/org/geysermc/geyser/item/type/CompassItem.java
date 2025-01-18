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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.LodestoneTracker;

public class CompassItem extends Item {
    public CompassItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public ItemData.Builder translateToBedrock(GeyserSession session, int count, DataComponents components, ItemMapping mapping, ItemMappings mappings) {
        if (isLodestoneCompass(components)) {
            return super.translateToBedrock(session, count, components, mappings.getLodestoneCompass(), mappings);
        }
        return super.translateToBedrock(session, count, components, mapping, mappings);
    }

    @Override
    public ItemMapping toBedrockDefinition(DataComponents components, ItemMappings mappings) {
        if (isLodestoneCompass(components)) {
            return mappings.getLodestoneCompass();
        }
        return super.toBedrockDefinition(components, mappings);
    }

    @Override
    public void translateComponentsToBedrock(@NonNull GeyserSession session, @NonNull DataComponents components, @NonNull BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, builder);

        LodestoneTracker tracker = components.get(DataComponentType.LODESTONE_TRACKER);
        if (tracker != null) {
            int trackId = session.getLodestoneCache().store(tracker);
            // Set the bedrock tracking id - will return 0 if invalid
            builder.putInt("trackingHandle", trackId);
        }
    }

    private boolean isLodestoneCompass(@Nullable DataComponents components) {
        if (components != null) {
            return components.getDataComponents().containsKey(DataComponentType.LODESTONE_TRACKER);
        }
        return false;
    }

    @Override
    public @NonNull GeyserItemStack translateToJava(GeyserSession session, @NonNull ItemData itemData, @NonNull ItemMapping mapping, @NonNull ItemMappings mappings) {
        if (mapping.getBedrockIdentifier().equals("minecraft:lodestone_compass")) {
            // Revert the entry back to the compass
            mapping = mappings.getStoredItems().compass();
        }

        return super.translateToJava(session, itemData, mapping, mappings);
    }
}
