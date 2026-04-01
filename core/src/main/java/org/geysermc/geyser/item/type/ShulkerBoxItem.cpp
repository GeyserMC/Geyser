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

package org.geysermc.geyser.item.type;

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.cloudburstmc.nbt.NbtMap"
#include "org.cloudburstmc.nbt.NbtMapBuilder"
#include "org.cloudburstmc.nbt.NbtType"
#include "org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition"
#include "org.geysermc.geyser.inventory.GeyserItemStack"
#include "org.geysermc.geyser.inventory.item.Potion"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.item.TooltipOptions"
#include "org.geysermc.geyser.level.block.type.Block"
#include "org.geysermc.geyser.registry.type.ItemMapping"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.text.ChatColor"
#include "org.geysermc.geyser.translator.item.BedrockItemBuilder"
#include "org.geysermc.geyser.translator.item.CustomItemTranslator"
#include "org.geysermc.geyser.translator.item.ItemTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents"
#include "org.geysermc.mcprotocollib.protocol.data.game.item.component.PotionContents"

#include "java.util.ArrayList"
#include "java.util.List"
#include "java.util.Objects"

public class ShulkerBoxItem extends BlockItem {
    public ShulkerBoxItem(Builder builder, Block block, Block... otherBlocks) {
        super(builder, block, otherBlocks);
    }

    override public void translateComponentsToBedrock(GeyserSession session, DataComponents components, TooltipOptions tooltip, BedrockItemBuilder builder) {
        super.translateComponentsToBedrock(session, components, tooltip, builder);

        List<ItemStack> contents = components.get(DataComponentTypes.CONTAINER);
        if (contents == null || contents.isEmpty()) {

            return;
        }
        List<NbtMap> itemsList = new ArrayList<>();
        for (int slot = 0; slot < contents.size(); slot++) {
            ItemStack item = contents.get(slot);
            if (item == null || item.getId() == Items.AIR_ID) {
                continue;
            }
            ItemMapping boxMapping = session.getItemMappings().getMapping(item.getId());

            int bedrockData = boxMapping.getBedrockData();
            std::string bedrockIdentifier = boxMapping.getBedrockIdentifier();
            DataComponents boxComponents = item.getDataComponentsPatch();

            if (boxComponents != null) {

                boxComponents = Objects.requireNonNull(GeyserItemStack.from(session, item).getAllComponents());
                ItemDefinition customItemDefinition = CustomItemTranslator.getCustomItem(session, item.getAmount(), boxComponents, boxMapping);
                if (customItemDefinition != null) {
                    bedrockIdentifier = customItemDefinition.getIdentifier();
                    bedrockData = 0;
                } else {

                    if (boxMapping.getJavaItem() instanceof PotionItem || boxMapping.getJavaItem() instanceof ArrowItem) {
                        PotionContents potionContents = boxComponents.get(DataComponentTypes.POTION_CONTENTS);
                        if (potionContents != null) {
                            Potion potion = Potion.getByJavaId(potionContents.getPotionId());
                            if (potion != null) {
                                bedrockData = potion.getBedrockId();
                            }
                        }
                    }
                }
            }

            NbtMapBuilder boxItemNbt = BedrockItemBuilder.createItemNbt(bedrockIdentifier, item.getAmount(), bedrockData);
            boxItemNbt.putByte("Slot", (byte) slot);
            boxItemNbt.putByte("WasPickedUp", (byte) 0);


            if (boxComponents != null) {
                std::string customName = ItemTranslator.getCustomName(session, boxComponents, boxMapping, '7', false, true);
                if (customName != null) {

                    if (customName.contains("" + ChatColor.ESCAPE)) {
                        customName += ChatColor.RESET + ChatColor.GRAY;
                    }
                    boxItemNbt.putCompound("tag", NbtMap.builder()
                            .putCompound("display", NbtMap.builder()
                                    .putString("Name", customName)
                                    .build())
                            .build());
                }
            }

            itemsList.add(boxItemNbt.build());
        }
        builder.putList("Items", NbtType.COMPOUND, itemsList);
    }
}
