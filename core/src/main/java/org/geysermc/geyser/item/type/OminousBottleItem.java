/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.registry.type.ItemMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

public class OminousBottleItem extends Item {
    public OminousBottleItem(String javaIdentifier, Builder builder) {
        super(javaIdentifier, builder);
    }

    @Override
    public ItemData.Builder translateToBedrock(GeyserSession session, int count, @Nullable DataComponents components, ItemMapping mapping, ItemMappings mappings) {
        var builder = super.translateToBedrock(session, count, components, mapping, mappings);
        if (components == null) {
            // Level 1 ominous bottle is null components - Java 1.21.
            return builder;
        }
        Integer amplifier = components.get(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER);
        if (amplifier != null) {
            builder.damage(amplifier);
        }
        return builder;
    }

    @Override
    public @NonNull GeyserItemStack translateToJava(GeyserSession session, @NonNull ItemData itemData, @NonNull ItemMapping mapping, @NonNull ItemMappings mappings) {
        // This item can be pulled from the creative inventory with amplifiers.
        GeyserItemStack itemStack = super.translateToJava(session, itemData, mapping, mappings);
        int damage = itemData.getDamage();
        if (damage == 0) {
            return itemStack;
        }
        itemStack.getOrCreateComponents().put(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER, damage);
        return itemStack;
    }

    @Override
    public boolean ignoreDamage() {
        return true;
    }
}
