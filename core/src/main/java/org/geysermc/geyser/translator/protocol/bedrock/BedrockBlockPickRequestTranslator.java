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

package org.geysermc.geyser.translator.protocol.bedrock;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.packet.BlockPickRequestPacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.util.InventoryUtils;

@Translator(packet = BlockPickRequestPacket.class)
public class BedrockBlockPickRequestTranslator extends PacketTranslator<BlockPickRequestPacket> {

    @Override
    public void translate(GeyserSession session, BlockPickRequestPacket packet) {
        Vector3i vector = packet.getBlockPosition();
        int blockToPick = session.getGeyser().getWorldManager().getBlockAt(session, vector.getX(), vector.getY(), vector.getZ());
        
        // Block is air - chunk caching is probably off
        if (blockToPick == BlockStateValues.JAVA_AIR_ID) {
            // Check for an item frame since the client thinks that's a block when it's an entity in Java
            ItemFrameEntity entity = ItemFrameEntity.getItemFrameEntity(session, packet.getBlockPosition());
            if (entity != null) {
                // Check to see if the item frame has an item in it first
                if (entity.getHeldItem() != null && entity.getHeldItem().getId() != 0) {
                    // Grab the item in the frame
                    InventoryUtils.findOrCreateItem(session, entity.getHeldItem());
                } else {
                    // Grab the frame as the item
                    InventoryUtils.findOrCreateItem(session, entity.getDefinition() == EntityDefinitions.GLOW_ITEM_FRAME ? "minecraft:glow_item_frame" : "minecraft:item_frame");
                }
            }
            return;
        }

        InventoryUtils.findOrCreateItem(session, BlockRegistries.JAVA_BLOCKS.get(blockToPick).getPickItem());
    }
}
