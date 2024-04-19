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
import com.github.steveice10.mc.protocol.data.game.item.ItemStack;
import com.github.steveice10.mc.protocol.data.game.item.component.DataComponentPatch;
import com.github.steveice10.mc.protocol.data.game.item.component.DataComponentType;
import com.github.steveice10.opennbt.tag.builtin.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.util.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class ShulkerBoxItem extends BlockItem {
    public ShulkerBoxItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public void translateComponentsToBedrock(@NonNull GeyserSession session, @NonNull DataComponentPatch components, @NonNull NbtMapBuilder builder) {
        super.translateComponentsToBedrock(session, components, builder);

        List<ItemStack> contents = components.get(DataComponentType.CONTAINER);
        if (contents == null || contents.isEmpty()) {
            // Empty shulker box
            return;
        }
        List<NbtMap> itemsList = new ArrayList<>();
        for (int slot = 0; slot < contents.size(); slot++) {
            ItemStack item = contents.get(slot);
            if (item.getId() == Items.AIR_ID) {
                continue;
            }
            NbtMapBuilder boxItemNbt = NbtMap.builder(); // Final item tag to add to the list
            boxItemNbt.putByte("Slot", (byte) slot);
            boxItemNbt.putByte("WasPickedUp", (byte) 0); // ???

            ItemMapping boxMapping = session.getItemMappings().getMapping(item.getId());

            boxItemNbt.putString("Name", boxMapping.getBedrockIdentifier());
            boxItemNbt.putShort("Damage", (short) boxMapping.getBedrockData());
            boxItemNbt.putByte("Count", (byte) item.getAmount());
            // Only the display name is what we have interest in, so just translate that if relevant
            DataComponentPatch boxComponents = item.getDataComponentPatch();
            if (boxComponents != null) {
                boxItemNbt.put(ItemTranslator.translateDisplayProperties(session, displayTag, boxMapping, '7'));
            }

            itemsList.add(boxItemNbt.build());
        }
        builder.putList("Items", NbtType.COMPOUND, itemsList);
    }

    @Override
    public void translateNbtToJava(@NonNull CompoundTag tag, @NonNull ItemMapping mapping) {
        super.translateNbtToJava(tag, mapping);

        // Remove any extraneous Bedrock tag and don't touch the Java one
        tag.remove("Items");
    }
}
