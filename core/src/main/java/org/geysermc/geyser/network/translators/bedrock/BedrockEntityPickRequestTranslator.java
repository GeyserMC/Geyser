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

package org.geysermc.geyser.network.translators.bedrock;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.nukkitx.protocol.bedrock.packet.EntityPickRequestPacket;
import org.geysermc.geyser.entity.BoatEntity;
import org.geysermc.geyser.entity.Entity;
import org.geysermc.geyser.network.session.GeyserSession;
import org.geysermc.geyser.network.translators.PacketTranslator;
import org.geysermc.geyser.network.translators.Translator;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.utils.InventoryUtils;

import java.util.Locale;

/**
 * Called when the Bedrock user uses the pick block button on an entity
 */
@Translator(packet = EntityPickRequestPacket.class)
public class BedrockEntityPickRequestTranslator extends PacketTranslator<EntityPickRequestPacket> {

    @Override
    public void translate(GeyserSession session, EntityPickRequestPacket packet) {
        if (session.getGameMode() != GameMode.CREATIVE) return; // Apparently Java behavior
        Entity entity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
        if (entity == null) return;

        // Get the corresponding item
        String itemName;
        switch (entity.getDefinition().entityType()) {
            case BOAT -> {
                // Include type of boat in the name
                int variant = ((BoatEntity) entity).getVariant();
                String typeOfBoat = switch (variant) {
                    case 1 -> "spruce";
                    case 2 -> "birch";
                    case 3 -> "jungle";
                    case 4 -> "acacia";
                    case 5 -> "dark_oak";
                    default -> "oak";
                };
                itemName = typeOfBoat + "_boat";
            }
            case LEASH_KNOT -> itemName = "lead";
            case MINECART_CHEST, MINECART_COMMAND_BLOCK, MINECART_FURNACE, MINECART_HOPPER, MINECART_TNT ->
                    // The Bedrock identifier matches the item name which moves MINECART to the end of the name
                    // TODO test
                    itemName = entity.getDefinition().identifier();
            case MINECART_SPAWNER -> itemName = "minecart"; // Turns into a normal minecart
            //case ITEM_FRAME -> Not an entity in Bedrock Edition
            //case GLOW_ITEM_FRAME ->
            case ARMOR_STAND, END_CRYSTAL, MINECART, PAINTING ->
                    // No spawn egg, just an item
                    itemName = entity.getDefinition().entityType().toString().toLowerCase(Locale.ROOT);
            default -> itemName = entity.getDefinition().entityType().toString().toLowerCase(Locale.ROOT) + "_spawn_egg";
        }

        String fullItemName = "minecraft:" + itemName;
        ItemMapping mapping = session.getItemMappings().getMapping(fullItemName);
        // Verify it is, indeed, an item
        if (mapping == null) return;

        InventoryUtils.findOrCreateItem(session, fullItemName);
    }
}
