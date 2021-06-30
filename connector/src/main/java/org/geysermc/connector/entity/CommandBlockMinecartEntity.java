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

package org.geysermc.connector.entity;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.EntityMetadata;
import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import net.kyori.adventure.text.Component;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.chat.MessageTranslator;

public class CommandBlockMinecartEntity extends DefaultBlockMinecartEntity {

    public CommandBlockMinecartEntity(long entityId, long geyserId, EntityType entityType, Vector3f position, Vector3f motion, Vector3f rotation) {
        super(entityId, geyserId, entityType, position, motion, rotation);
        // Required, or else the GUI will not open
        metadata.put(EntityData.CONTAINER_TYPE, (byte) 16);
        metadata.put(EntityData.CONTAINER_BASE_SIZE, 1);
        // Required, or else the client does not bother to send a packet back with the new information
        metadata.put(EntityData.COMMAND_BLOCK_ENABLED, (byte) 1);
    }

    @Override
    public void updateBedrockMetadata(EntityMetadata entityMetadata, GeyserSession session) {
        if (entityMetadata.getId() == 14) {
            metadata.put(EntityData.COMMAND_BLOCK_COMMAND, entityMetadata.getValue());
        }
        if (entityMetadata.getId() == 15) {
            metadata.put(EntityData.COMMAND_BLOCK_LAST_OUTPUT, MessageTranslator.convertMessage((Component) entityMetadata.getValue()));
        }
        super.updateBedrockMetadata(entityMetadata, session);
    }

    /**
     * By default, the command block shown is purple on Bedrock, which does not match Java Edition's orange.
     */
    @Override
    public void updateDefaultBlockMetadata(GeyserSession session) {
        metadata.put(EntityData.DISPLAY_ITEM, session.getBlockTranslator().getBedrockRuntimeCommandBlockId());
        metadata.put(EntityData.DISPLAY_OFFSET, 6);
    }
}
