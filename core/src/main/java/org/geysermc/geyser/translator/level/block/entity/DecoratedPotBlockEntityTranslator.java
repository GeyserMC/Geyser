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

package org.geysermc.geyser.translator.level.block.entity;

import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.item.type.Item;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.RegistryEntryContext;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;

@BlockEntity(type = BlockEntityType.DECORATED_POT)
public class DecoratedPotBlockEntityTranslator extends BlockEntityTranslator {
    private static final String POTTERY_PATTERN_COMPONENT = DataComponentTypes.PROVIDES_POTTERY_PATTERN.getKey().toString();
    // Use this pattern for all non-vanilla patterns
    public static final Key DEFAULT_PATTERN = MinecraftKey.key("minecraft:shelter_pottery_sherd");
    // Use this item for empty faces
    public static final String DEFAULT_BEDROCK_ITEM = "minecraft:brick";

    @Override
    public void translateTag(GeyserSession session, NbtMapBuilder bedrockNbt, NbtMap javaNbt, BlockState blockState) {
        if (javaNbt == null) {
            return;
        }

        // Java has them in a map of item stacks, bedrock in a list of item IDs (order back -> left -> right -> front)
        NbtMap sherds = javaNbt.getCompound("sherds");
        bedrockNbt.putList("sherds", NbtType.STRING,
            convertItemStackToBedrockItem(session, sherds.getCompound("back")),
            convertItemStackToBedrockItem(session, sherds.getCompound("left")),
            convertItemStackToBedrockItem(session, sherds.getCompound("right")),
            convertItemStackToBedrockItem(session, sherds.getCompound("front")));
    }

    private static String convertItemStackToBedrockItem(GeyserSession session, NbtMap stack) {
        if (stack.isEmpty()) {
            return DEFAULT_BEDROCK_ITEM;
        }

        // Check the component patch first: it may remove the provides_pottery_pattern component, or add it
        NbtMap components = stack.getCompound("components");
        if (!components.isEmpty()) {
            // Specifies component removal
            if (components.containsKey("!" + POTTERY_PATTERN_COMPONENT)) {
                return DEFAULT_BEDROCK_ITEM;
            }
            String pattern = components.getString(POTTERY_PATTERN_COMPONENT, null);
            if (pattern != null) {
                Key bedrockItem = JavaRegistries.DECORATED_POT_PATTERN.value(session, MinecraftKey.key(pattern));
                if (bedrockItem != null) {
                    return bedrockItem.toString();
                }
                return DEFAULT_BEDROCK_ITEM;
            }
        }

        // If no pattern was specified in the component patch, check the item's default components
        Item item = Registries.JAVA_ITEM_IDENTIFIERS.get(stack.getString("id"));
        if (item != null) {
            Integer patternId = item.getComponent(session.getComponentCache(), DataComponentTypes.PROVIDES_POTTERY_PATTERN);
            if (patternId != null) {
                Key bedrockItem = JavaRegistries.DECORATED_POT_PATTERN.value(session, patternId);
                if (bedrockItem != null) {
                    return bedrockItem.toString();
                }
                return DEFAULT_BEDROCK_ITEM;
            }
        }

        // Doubtful, but possible
        return DEFAULT_BEDROCK_ITEM;
    }

    public static Key readDecoratedPotPattern(RegistryEntryContext context) {
        // Connect the pattern to a bedrock item via asset ID - that way, if for some reason a non-vanilla pattern uses a vanilla asset ID, it'll still work
        Key assetId = MinecraftKey.key(context.dataAsMap().getString("asset_id"));
        return Registries.DECORATED_POT_ASSETS.getOrDefault(assetId, DEFAULT_PATTERN);
    }
}
