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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.opennbt.tag.builtin.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;

public class CrossbowItem extends Item {
    public CrossbowItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateNbtToBedrock(@NonNull GeyserSession session, @NonNull CompoundTag tag) {
        super.translateNbtToBedrock(session, tag);

        ListTag chargedProjectiles = tag.get("ChargedProjectiles");
        if (chargedProjectiles != null) {
            if (!chargedProjectiles.getValue().isEmpty()) {
                CompoundTag javaProjectileAsNbt = (CompoundTag) chargedProjectiles.getValue().get(0);

                ItemMapping projectileMapping = session.getItemMappings().getMapping((String) javaProjectileAsNbt.get("id").getValue());
                if (projectileMapping == null) return;
                @Nullable CompoundTag projectileTag = javaProjectileAsNbt.get("tag");
                ItemStack itemStack = new ItemStack(projectileMapping.getJavaItem().javaId(), (byte) javaProjectileAsNbt.get("Count").getValue(), projectileTag);
                ItemData itemData = ItemTranslator.translateToBedrock(session, itemStack);

                CompoundTag newProjectile = new CompoundTag("chargedItem");
                newProjectile.put(new ByteTag("Count", (byte) itemData.getCount()));
                newProjectile.put(new StringTag("Name", projectileMapping.getBedrockIdentifier()));

                newProjectile.put(new ShortTag("Damage", (short) itemData.getDamage()));

                tag.put(newProjectile);
            }
        }
    }

    @Override
    public void translateNbtToJava(@NonNull CompoundTag tag, @NonNull ItemMapping mapping) {
        super.translateNbtToJava(tag, mapping);

        if (tag.get("chargedItem") != null) {
            CompoundTag chargedItem = tag.get("chargedItem");

            CompoundTag newProjectile = new CompoundTag("");
            newProjectile.put(new ByteTag("Count", (byte) chargedItem.get("Count").getValue()));
            newProjectile.put(new StringTag("id", (String) chargedItem.get("Name").getValue()));

            ListTag chargedProjectiles = new ListTag("ChargedProjectiles");
            chargedProjectiles.add(newProjectile);

            tag.put(chargedProjectiles);
        }
    }
}
