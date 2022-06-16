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

package org.geysermc.geyser.translator.inventory.item;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.network.MinecraftProtocol;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.util.Collections;
import java.util.List;

@ItemRemapper
public class GoatHornTranslator extends ItemTranslator {

    private static final List<String> INSTRUMENTS = List.of(
            "ponder_goat_horn",
            "sing_goat_horn",
            "seek_goat_horn",
            "feel_goat_horn",
            "admire_goat_horn",
            "call_goat_horn",
            "yearn_goat_horn",
            "dream_goat_horn" // Called "Resist" on Bedrock 1.19.0 due to https://bugs.mojang.com/browse/MCPE-155059
    );

    @Override
    protected ItemData.Builder translateToBedrock(ItemStack itemStack, ItemMapping mapping, ItemMappings mappings) {
        ItemData.Builder builder = super.translateToBedrock(itemStack, mapping, mappings);
        if (itemStack.getNbt() != null && itemStack.getNbt().get("instrument") instanceof StringTag instrumentTag) {
            String instrument = instrumentTag.getValue();
            // Drop the Minecraft namespace if applicable
            if (instrument.startsWith("minecraft:")) {
                instrument = instrument.substring("minecraft:".length());
            }

            int damage = INSTRUMENTS.indexOf(instrument);
            if (damage == -1) {
                damage = 0;
                GeyserImpl.getInstance().getLogger().debug("Unknown goat horn instrument: " + instrumentTag.getValue());
            }
            builder.damage(damage);
        }
        return builder;
    }

    @Override
    public ItemStack translateToJava(ItemData itemData, ItemMapping mapping, ItemMappings mappings) {
        ItemStack itemStack = super.translateToJava(itemData, mapping, mappings);

        int damage = itemData.getDamage();
        if (damage < 0 || damage >= INSTRUMENTS.size()) {
            GeyserImpl.getInstance().getLogger().debug("Unknown goat horn instrument for damage: " + damage);
            damage = 0;
        }

        String instrument = INSTRUMENTS.get(damage);
        StringTag instrumentTag = new StringTag("instrument", "minecraft:" + instrument);
        itemStack.getNbt().put(instrumentTag);

        return itemStack;
    }

    @Override
    public List<ItemMapping> getAppliedItems() {
        return Collections.singletonList(
                Registries.ITEMS.forVersion(MinecraftProtocol.DEFAULT_BEDROCK_CODEC.getProtocolVersion())
                        .getMapping("minecraft:goat_horn")
        );
    }
}
