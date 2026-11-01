/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.util;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.inventory.GeyserItemStack;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable;

/**
 * Bedrock has no glider component; its client only glides with a vanilla elytra in the chest slot.
 * A player's custom glider is therefore shown to Bedrock as an elytra, so that the player can glide with it.
 */
public final class GliderUtils {

    private static final int ELYTRA_DURABILITY = 432;

    public static boolean isGlider(GeyserItemStack stack) {
        if (stack.isEmpty() || stack.getComponent(DataComponentTypes.GLIDER) == null) {
            return false;
        }
        Equippable equippable = stack.getComponent(DataComponentTypes.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHESTPLATE;
    }

    /**
     * @return the chest slot item as Bedrock should see it, an elytra if it is a glider that would not translate to one
     */
    public static ItemData toBedrockChest(GeyserSession session, GeyserItemStack stack, ItemData item) {
        if (!isGlider(stack)) {
            return item;
        }
        ItemDefinition elytra = session.getItemMappings().getMapping(Items.ELYTRA).getBedrockDefinition();
        if (item.getDefinition().getRuntimeId() == elytra.getRuntimeId()) {
            return item;
        }

        ItemData.Builder builder = item.toBuilder().definition(elytra);
        // Bedrock stops gliding when the elytra is about to break, so keep the durability ratio
        Integer maxDamage = stack.getComponent(DataComponentTypes.MAX_DAMAGE);
        NbtMap tag = item.getTag();
        if (maxDamage != null && maxDamage > 0 && maxDamage != ELYTRA_DURABILITY && tag != null && tag.containsKey("Damage")) {
            int damage = Math.round(tag.getInt("Damage") * (float) ELYTRA_DURABILITY / maxDamage);
            builder.tag(tag.toBuilder().putInt("Damage", Math.min(damage, ELYTRA_DURABILITY - 1)).build());
        }
        return builder.build();
    }

    private GliderUtils() {
    }
}
