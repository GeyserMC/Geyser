/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.bedrock;

import org.cloudburstmc.protocol.bedrock.packet.EntityPickRequestPacket;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.InventoryUtils;

import java.util.Locale;

/**
 * Called when the Bedrock user uses the pick block button on an entity
 */
@Translator(packet = EntityPickRequestPacket.class)
public class BedrockEntityPickRequestTranslator extends PacketTranslator<EntityPickRequestPacket> {

    @Override
    public void translate(GeyserSession session, EntityPickRequestPacket packet) {
        if (!session.isInstabuild()) {
            // As of Java Edition 1.19.3
            return;
        }
        Entity entity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
        if (entity == null) return;

        if (entity instanceof BoatEntity boat) {
            InventoryUtils.findOrCreateItem(session, boat.getPickItem());
            return;
        }

        // Get the corresponding item
        String itemName;
        switch (entity.getDefinition().entityType()) {
            case LEASH_KNOT -> itemName = "lead";
            case CHEST_MINECART, COMMAND_BLOCK_MINECART, FURNACE_MINECART, HOPPER_MINECART, TNT_MINECART ->
                    // The Bedrock identifier matches the item name which moves MINECART to the end of the name
                    // TODO test
                    itemName = entity.getDefinition().identifier();
            case SPAWNER_MINECART -> itemName = "minecart"; // Turns into a normal minecart
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
