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

package org.geysermc.geyser.translator.inventory.item.nbt;

import com.github.steveice10.opennbt.tag.builtin.ByteTag;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemRemapper;
import org.geysermc.geyser.translator.inventory.item.NbtItemStackTranslator;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.text.MinecraftLocale;

@ItemRemapper
public class AxolotlBucketTranslator extends NbtItemStackTranslator {

    @Override
    public void translateToBedrock(GeyserSession session, CompoundTag itemTag, ItemMapping mapping) {
        // Bedrock Edition displays the properties of the axolotl. Java does not.
        // To work around this, set the custom name to the Axolotl translation and it's displayed correctly
        itemTag.put(new ByteTag("AppendCustomName", (byte) 1));
        itemTag.put(new StringTag("CustomName", MinecraftLocale.getLocaleString("entity.minecraft.axolotl", session.getLocale())));
        // Boilerplate required so the nametag does not appear as "Bucket of "
        itemTag.put(new StringTag("ColorID", ""));
        itemTag.put(new StringTag("BodyID", ""));
    }

    @Override
    public boolean acceptItem(ItemMapping mapping) {
        return mapping.getJavaIdentifier().equals("minecraft:axolotl_bucket");
    }
}
