/*
 * Copyright (c) 2019-2021 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.item.translators;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.connector.network.BedrockProtocol;
import org.geysermc.connector.network.translators.ItemRemapper;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.registry.Registries;
import org.geysermc.connector.registry.type.ItemMapping;
import org.geysermc.connector.utils.LoadstoneTracker;

import java.util.List;
import java.util.stream.Collectors;

@ItemRemapper
public class CompassTranslator extends ItemTranslator {

    private final List<ItemMapping> appliedItems;

    public CompassTranslator() {
        appliedItems = Registries.ITEMS.forVersion(BedrockProtocol.DEFAULT_BEDROCK_CODEC.getProtocolVersion())
                .getItems()
                .values()
                .stream()
                .filter(entry -> entry.getJavaIdentifier().endsWith("compass"))
                .collect(Collectors.toList());
    }

    @Override
    public ItemData.Builder translateToBedrock(ItemStack itemStack, ItemMapping mapping, int protocolVersion) {
        if (itemStack.getNbt() == null) return super.translateToBedrock(itemStack, mapping, protocolVersion);

        Tag lodestoneTag = itemStack.getNbt().get("LodestoneTracked");
        if (lodestoneTag instanceof ByteTag) {
            // Get the fake lodestonecompass entry
            mapping = Registries.ITEMS.forVersion(protocolVersion).getStored("minecraft:lodestone_compass");

            // Get the loadstone pos
            CompoundTag loadstonePos = itemStack.getNbt().get("LodestonePos");
            if (loadstonePos != null) {
                // Get all info needed for tracking
                int x = ((IntTag) loadstonePos.get("X")).getValue();
                int y = ((IntTag) loadstonePos.get("Y")).getValue();
                int z = ((IntTag) loadstonePos.get("Z")).getValue();
                String dim = ((StringTag) itemStack.getNbt().get("LodestoneDimension")).getValue();

                // Store the info
                int trackID = LoadstoneTracker.store(x, y, z, dim);

                // Set the bedrock tracking id
                itemStack.getNbt().put(new IntTag("trackingHandle", trackID));
            } else {
                // The loadstone was removed just set the tracking id to 0
                itemStack.getNbt().put(new IntTag("trackingHandle", 0));
            }
        }

        return super.translateToBedrock(itemStack, mapping, protocolVersion);
    }

    @Override
    public ItemStack translateToJava(ItemData itemData, ItemMapping mapping, int protocolVersion) {
        boolean isLoadstone = false;
        if (mapping.getBedrockIdentifier().equals("minecraft:lodestone_compass")) {
            // Revert the entry back to the compass
            mapping = Registries.ITEMS.forVersion(protocolVersion).getStored("minecraft:compass");

            isLoadstone = true;
        }

        ItemStack itemStack = super.translateToJava(itemData, mapping, protocolVersion);

        if (isLoadstone) {
            // Get the tracking id
            int trackingID = ((IntTag) itemStack.getNbt().get("trackingHandle")).getValue();

            // Fetch the tracking info from the id
            LoadstoneTracker.LoadstonePos pos = LoadstoneTracker.getPos(trackingID);
            if (pos != null) {
                // Build the new NBT data for the fetched tracking info
                itemStack.getNbt().put(new StringTag("LodestoneDimension", pos.getDimension()));

                CompoundTag posTag = new CompoundTag("LodestonePos");
                posTag.put(new IntTag("X", pos.getX()));
                posTag.put(new IntTag("Y", pos.getY()));
                posTag.put(new IntTag("Z", pos.getZ()));

                itemStack.getNbt().put(posTag);
            }
        }

        return itemStack;
    }

    @Override
    public List<ItemMapping> getAppliedItems() {
        return appliedItems;
    }
}
