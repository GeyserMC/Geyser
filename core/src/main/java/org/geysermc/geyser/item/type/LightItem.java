/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.BlockStateProperties;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

public class LightItem extends BlockItem {

    public LightItem(Builder builder, Block block, Block... otherBlocks) {
        super(builder, block, otherBlocks);
    }

    @Override
    public ItemData.Builder translateToBedrock(GeyserSession session, int count, DataComponents components, ItemMapping mapping, ItemMappings mappings)  {
        ItemMapping lightLevelMapping = getLightLevelMapping(components, mappings);
        if (lightLevelMapping != null) {
            return super.translateToBedrock(session, count, components, lightLevelMapping, mappings);
        }
        return super.translateToBedrock(session, count, components, mapping, mappings);
    }

    @Override
    public ItemMapping toBedrockDefinition(DataComponents components, ItemMappings mappings) {
        ItemMapping lightLevelMapping = getLightLevelMapping(components, mappings);
        if (lightLevelMapping != null) {
            return lightLevelMapping;
        }
        return super.toBedrockDefinition(components, mappings);
    }


    private static ItemMapping getLightLevelMapping(DataComponents components, ItemMappings mappings) {
        String lightLevel = "15";
        if (components != null) {
            BlockStateProperties blockStateProperties = components.get(DataComponentType.BLOCK_STATE);

            if (blockStateProperties != null) {
                lightLevel = blockStateProperties.getProperties().get(Properties.LEVEL.name());
            }
        }
        ItemDefinition definition = mappings.getDefinition("minecraft:light_block_" + lightLevel);
        if (definition != null) {
            return mappings.getLightBlocks().get(definition.getRuntimeId());
        }
        return null;
    }
}
