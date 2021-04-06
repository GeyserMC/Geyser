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

package org.geysermc.connector.network.translators.item.translators.nbt;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.*;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.ItemRemapper;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.network.translators.item.ItemTranslator;
import org.geysermc.connector.network.translators.item.NbtItemStackTranslator;

@ItemRemapper
public class CrossbowTranslator extends NbtItemStackTranslator {

    @Override
    public void translateToBedrock(GeyserSession session, CompoundTag itemTag, ItemEntry itemEntry) {
        if (itemTag.get("ChargedProjectiles") != null) {
            ListTag chargedProjectiles = itemTag.get("ChargedProjectiles");
            if (!chargedProjectiles.getValue().isEmpty()) {
                CompoundTag projectile = (CompoundTag) chargedProjectiles.getValue().get(0);

                ItemEntry projectileEntry = ItemRegistry.getItemEntry((String) projectile.get("id").getValue());
                if (projectileEntry == null) return;
                CompoundTag tag = projectile.get("tag");
                ItemStack itemStack = new ItemStack(itemEntry.getJavaId(), (byte) projectile.get("Count").getValue(), tag);
                ItemData itemData = ItemTranslator.translateToBedrock(session, itemStack);

                CompoundTag newProjectile = new CompoundTag("chargedItem");
                newProjectile.put(new ByteTag("Count", (byte) itemData.getCount()));
                newProjectile.put(new StringTag("Name", projectileEntry.getBedrockIdentifier()));

                newProjectile.put(new ShortTag("Damage", (short) itemData.getDamage()));

                itemTag.put(newProjectile);
            }
        }
    }

    @Override
    public void translateToJava(CompoundTag itemTag, ItemEntry itemEntry) {
        if (itemTag.get("chargedItem") != null) {
            CompoundTag chargedItem = itemTag.get("chargedItem");

            CompoundTag newProjectile = new CompoundTag("");
            newProjectile.put(new ByteTag("Count", (byte) chargedItem.get("Count").getValue()));
            newProjectile.put(new StringTag("id", (String) chargedItem.get("Name").getValue()));

            ListTag chargedProjectiles = new ListTag("ChargedProjectiles");
            chargedProjectiles.add(newProjectile);

            itemTag.put(chargedProjectiles);
        }
    }

    @Override
    public boolean acceptItem(ItemEntry itemEntry) {
        return "minecraft:crossbow".equals(itemEntry.getJavaIdentifier());
    }
}
