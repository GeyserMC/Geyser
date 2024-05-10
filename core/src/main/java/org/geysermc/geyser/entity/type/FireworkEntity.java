/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.entity.type;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.OptionalInt;
import java.util.UUID;

public class FireworkEntity extends Entity {

    public FireworkEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    public void setFireworkItem(EntityMetadata<ItemStack, ?> entityMetadata) {
        ItemStack item = entityMetadata.getValue();
        if (item == null) {
            return;
        }
        DataComponents components = item.getDataComponents();
        if (components == null) {
            return;
        }

        // TODO this looked the same, so I'm going to assume it is and (keep below comment if true)
        // Translate using item methods to get firework NBT for Bedrock
        BedrockItemBuilder builder = new BedrockItemBuilder();
        Items.FIREWORK_ROCKET.translateComponentsToBedrock(session, components, builder);
        
        dirtyMetadata.put(EntityDataTypes.DISPLAY_FIREWORK, builder.build());
    }

    public void setPlayerGliding(EntityMetadata<OptionalInt, ?> entityMetadata) {
        OptionalInt optional = entityMetadata.getValue();
        // Checks if the firework has an entity ID (used when a player is gliding)
        // and checks to make sure the player that is gliding is the one getting sent the packet
        // or else every player near the gliding player will boost too.
        if (optional.isPresent() && optional.getAsInt() == session.getPlayerEntity().getEntityId()) {
            PlayerEntity entity = session.getPlayerEntity();
            float yaw = entity.getYaw();
            float pitch = entity.getPitch();
            // Uses math from NukkitX
            entity.setMotion(Vector3f.from(
                    -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * 2,
                    -Math.sin(Math.toRadians(pitch)) * 2,
                    Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)) * 2));
            // Need to update the EntityMotionPacket or else the player won't boost
            SetEntityMotionPacket entityMotionPacket = new SetEntityMotionPacket();
            entityMotionPacket.setRuntimeEntityId(entity.getGeyserId());
            entityMotionPacket.setMotion(entity.getMotion());

            session.sendUpstreamPacket(entityMotionPacket);
        }
    }
}
