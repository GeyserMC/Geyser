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

package org.geysermc.geyser.translator.inventory.item;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.MinecraftProtocol;
import org.geysermc.geyser.inventory.item.Potion;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.util.List;
import java.util.stream.Collectors;

@ItemRemapper
public class PotionTranslator extends ItemTranslator {

    private final List<ItemMapping> appliedItems;

    public PotionTranslator() {
        appliedItems = Registries.ITEMS.forVersion(MinecraftProtocol.DEFAULT_BEDROCK_CODEC.getProtocolVersion())
                .getItems()
                .values()
                .stream()
                .filter(entry -> entry.getJavaIdentifier().endsWith("potion"))
                .collect(Collectors.toList());
    }

    @Override
    public ItemData.Builder translateToBedrock(ItemStack itemStack, ItemMapping mapping, ItemMappings mappings) {
        if (itemStack.getNbt() == null) return super.translateToBedrock(itemStack, mapping, mappings);
        Tag potionTag = itemStack.getNbt().get("Potion");
        if (potionTag instanceof StringTag) {
            Potion potion = Potion.getByJavaIdentifier(((StringTag) potionTag).getValue());
            if (potion != null) {
                return ItemData.builder()
                        .id(mapping.getBedrockId())
                        .damage(potion.getBedrockId())
                        .count(itemStack.getAmount())
                        .tag(translateNbtToBedrock(itemStack.getNbt()));
            }
            GeyserImpl.getInstance().getLogger().debug("Unknown Java potion: " + potionTag.getValue());
        }
        return super.translateToBedrock(itemStack, mapping, mappings);
    }

    @Override
    public ItemStack translateToJava(ItemData itemData, ItemMapping mapping, ItemMappings mappings) {
        Potion potion = Potion.getByBedrockId(itemData.getDamage());
        ItemStack itemStack = super.translateToJava(itemData, mapping, mappings);
        if (potion != null) {
            StringTag potionTag = new StringTag("Potion", potion.getJavaIdentifier());
            itemStack.getNbt().put(potionTag);
        }
        return itemStack;
    }

    @Override
    public List<ItemMapping> getAppliedItems() {
        return appliedItems;
    }
}
