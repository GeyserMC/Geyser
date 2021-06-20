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

package org.geysermc.connector.network.translators.bedrock;

import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.packet.EntityPickRequestPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.item.ItemEntry;
import org.geysermc.connector.network.translators.item.ItemRegistry;
import org.geysermc.connector.utils.InventoryUtils;

/**
 * Called when the Bedrock user uses the pick block button on an entity
 */
@Translator(packet = EntityPickRequestPacket.class)
public class BedrockEntityPickRequestTranslator extends PacketTranslator<EntityPickRequestPacket> {

    @Override
    public void translate(EntityPickRequestPacket packet, GeyserSession session) {
        if (session.getGameMode() != GameMode.CREATIVE) return; // Apparently Java behavior
        Entity entity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
        if (entity == null) return;

        // Get the corresponding item
        String itemName;
        switch (entity.getEntityType()) {
            case BOAT:
                // Include type of boat in the name
                int variant = entity.getMetadata().getInt(EntityData.VARIANT);
                String typeOfBoat;
                switch (variant) {
                    case 1:
                        typeOfBoat = "spruce";
                        break;
                    case 2:
                        typeOfBoat = "birch";
                        break;
                    case 3:
                        typeOfBoat = "jungle";
                        break;
                    case 4:
                        typeOfBoat = "acacia";
                        break;
                    case 5:
                        typeOfBoat = "dark_oak";
                        break;
                    default:
                        typeOfBoat = "oak";
                        break;
                }
                itemName = typeOfBoat + "_boat";
                break;
            case LEASH_KNOT:
                itemName = "lead";
                break;
            case MINECART_CHEST:
            case MINECART_COMMAND_BLOCK:
            case MINECART_FURNACE:
            case MINECART_HOPPER:
            case MINECART_TNT:
                // Move MINECART to the end of the name
                itemName = entity.getEntityType().toString().toLowerCase().replace("minecart_", "") + "_minecart";
                break;
            case MINECART_SPAWNER:
                // Turns into a normal minecart
                itemName = "minecart";
                break;
            case ARMOR_STAND:
            case END_CRYSTAL:
            //case ITEM_FRAME: Not an entity in Bedrock Edition
            //case GLOW_ITEM_FRAME:
            case MINECART:
            case PAINTING:
                // No spawn egg, just an item
                itemName = entity.getEntityType().toString().toLowerCase();
                break;
            default:
                itemName = entity.getEntityType().toString().toLowerCase() + "_spawn_egg";
                break;
        }

        String fullItemName = "minecraft:" + itemName;
        ItemEntry entry = ItemRegistry.getItemEntry(fullItemName);
        // Verify it is, indeed, an item
        if (entry == null) return;

        InventoryUtils.findOrCreateItem(session, fullItemName);
    }
}
