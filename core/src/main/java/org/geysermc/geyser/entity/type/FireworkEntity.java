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
import org.cloudburstmc.protocol.bedrock.data.MovementEffectType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.packet.MovementEffectPacket;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.item.TooltipOptions;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.OptionalInt;
import java.util.UUID;

public class FireworkEntity extends Entity {

    private boolean attachedToSession;

    public FireworkEntity(GeyserSession session, int entityId, long geyserId, UUID uuid, EntityDefinition<?> definition, Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    public void setFireworkItem(EntityMetadata<ItemStack, ?> entityMetadata) {
        ItemStack item = entityMetadata.getValue();
        if (item == null) {
            return;
        }
        DataComponents components = item.getDataComponentsPatch();
        if (components == null) {
            return;
        }

        // TODO this looked the same, so I'm going to assume it is and (keep below comment if true)
        // Translate using item methods to get firework NBT for Bedrock
        BedrockItemBuilder builder = new BedrockItemBuilder();
        TooltipOptions tooltip = TooltipOptions.fromComponents(components);
        Items.FIREWORK_ROCKET.translateComponentsToBedrock(session, components, tooltip, builder);
        
        dirtyMetadata.put(EntityDataTypes.DISPLAY_FIREWORK, builder.build());
    }

    public void setPlayerGliding(EntityMetadata<OptionalInt, ?> entityMetadata) {
        session.getAttachedFireworkRockets().remove(this.geyserId);

        OptionalInt optional = entityMetadata.getValue();
        if (optional.isPresent() && optional.getAsInt() == session.getPlayerEntity().getEntityId()) {
            // If we don't send this, the bedrock client will always stop boosting after 20 ticks
            // However this is not the case for Java as the player will stop boosting after entity despawn.
            // So we let player boost "infinitely" and then only stop them when the entity despawn.
            // Also doing this allow player to boost simply by having a fireworks rocket attached to them
            // and not necessary have to use a rocket (as some plugin do this to boost player)
            sendElytraBoost(Integer.MAX_VALUE);
            this.attachedToSession = true;

            // We need to keep track of the fireworks rockets.
            session.getAttachedFireworkRockets().add(this.getGeyserId());
        } else {
            // Also ensure player stop boosting in cases like metadata changes.
            if (this.attachedToSession && session.getAttachedFireworkRockets().isEmpty()) {
                sendElytraBoost(0);
                this.attachedToSession = false;
            }
        }
    }

    @Override
    public void despawnEntity() {
        session.getAttachedFireworkRockets().remove(this.geyserId);
        // We have to ensure that these fireworks is attached to entity and this is the only one that is attached to the player.
        // Else player will stop boosting even if the fireworks is not attached to them or there is a fireworks that is boosting them
        // and not just this one.
        if (this.attachedToSession && session.getAttachedFireworkRockets().isEmpty()) {
            // Since we send an effect packet for player to boost "infinitely", we have to stop them when the entity despawn.
            sendElytraBoost(0);
            this.attachedToSession = false;
        }

        super.despawnEntity();
    }

    private void sendElytraBoost(int duration) {
        MovementEffectPacket movementEffect = new MovementEffectPacket();
        movementEffect.setDuration(duration);
        movementEffect.setEffectType(MovementEffectType.GLIDE_BOOST);
        movementEffect.setEntityRuntimeId(session.getPlayerEntity().getGeyserId());
        session.sendUpstreamPacket(movementEffect);
    }
}
