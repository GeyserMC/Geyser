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

import com.github.steveice10.mc.protocol.data.game.Identifier;
import com.github.steveice10.opennbt.tag.builtin.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.item.ItemTranslator;
import org.geysermc.geyser.util.MathUtils;

public class ShulkerBoxItem extends BlockItem {
    public ShulkerBoxItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateNbtToBedrock(@NonNull GeyserSession session, @NonNull CompoundTag tag) {
        super.translateNbtToBedrock(session, tag);

        CompoundTag blockEntityTag = tag.get("BlockEntityTag");
        if (blockEntityTag == null) {
            // Empty shulker box
            return;
        }
        if (blockEntityTag.get("Items") == null) return;
        ListTag itemsList = new ListTag("Items");
        for (Tag item : (ListTag) blockEntityTag.get("Items")) {
            CompoundTag itemData = (CompoundTag) item; // Information about the item
            CompoundTag boxItemTag = new CompoundTag(""); // Final item tag to add to the list
            boxItemTag.put(new ByteTag("Slot", (byte) (MathUtils.getNbtByte(itemData.get("Slot").getValue()) & 255)));
            boxItemTag.put(new ByteTag("WasPickedUp", (byte) 0)); // ???

            ItemMapping boxMapping = session.getItemMappings().getMapping(Identifier.formalize(((StringTag) itemData.get("id")).getValue()));

            if (boxMapping == null) {
                // If invalid ID
                continue;
            }

            boxItemTag.put(new StringTag("Name", boxMapping.getBedrockIdentifier()));
            boxItemTag.put(new ShortTag("Damage", (short) boxMapping.getBedrockData()));
            boxItemTag.put(new ByteTag("Count", MathUtils.getNbtByte(itemData.get("Count").getValue())));
            // Only the display name is what we have interest in, so just translate that if relevant
            CompoundTag displayTag = itemData.get("tag");
            if (displayTag == null && boxMapping.hasTranslation()) {
                displayTag = new CompoundTag("tag");
            }
            if (displayTag != null) {
                boxItemTag.put(ItemTranslator.translateDisplayProperties(session, displayTag, boxMapping, '7'));
            }

            itemsList.add(boxItemTag);
        }
        tag.put(itemsList);

        // Strip the BlockEntityTag from the chests contents
        // sent to the client. The client does not parse this
        // or use it for anything, as this tag is fully
        // server-side, so we remove it to reduce bandwidth and
        // solve potential issues with very large tags.

        // There was a problem in the past where this would strip
        // NBT data in creative mode, however with the new server
        // authoritative inventories, this is no longer a concern.
        tag.remove("BlockEntityTag");
    }

    @Override
    public void translateNbtToJava(@NonNull CompoundTag tag, @NonNull ItemMapping mapping) {
        super.translateNbtToJava(tag, mapping);

        // Remove any extraneous Bedrock tag and don't touch the Java one
        tag.remove("Items");
    }
}
