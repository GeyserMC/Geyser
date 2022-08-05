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

package org.geysermc.geyser.translator.protocol.java.entity.spawn;

import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.github.steveice10.mc.protocol.data.game.entity.object.FallingBlockData;
import com.github.steveice10.mc.protocol.data.game.entity.object.ProjectileData;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import com.nukkitx.math.vector.Vector3f;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.entity.EntityDefinition;
import org.geysermc.geyser.api.event.downstream.entity.ServerSpawnEntityEvent;
import org.geysermc.geyser.entity.GeyserEntityDefinition;
import org.geysermc.geyser.entity.type.*;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;

@Translator(packet = ClientboundAddEntityPacket.class)
public class JavaAddEntityTranslator extends PacketTranslator<ClientboundAddEntityPacket> {

    @Override
    public void translate(GeyserSession session, ClientboundAddEntityPacket packet) {
        Vector3f position = Vector3f.from(packet.getX(), packet.getY(), packet.getZ());
        Vector3f motion = Vector3f.from(packet.getMotionX(), packet.getMotionY(), packet.getMotionZ());
        float yaw = packet.getYaw();
        float pitch = packet.getPitch();
        float headYaw = packet.getHeadYaw();

        GeyserEntityDefinition<?> definition = Registries.ENTITY_DEFINITIONS.get(packet.getType());
        if (definition == null) {
            session.getGeyser().getLogger().warning(GeyserLocale.getLocaleStringLog("geyser.entity.type_null", packet.getType()));
            return;
        }

        ServerSpawnEntityEvent event = new ServerSpawnEntityEvent(session, packet.getEntityId(), packet.getUuid(), definition);
        GeyserImpl.getInstance().eventBus().fire(event);

        GeyserEntityDefinition<?> eventDefinition = (GeyserEntityDefinition<?>) event.entityDefinition();

        // If the definition is changed, we need to update our current
        // definition to reflect that.
        if (!eventDefinition.equals(definition)) {
            definition = definition.toBuilder()
                    .identifier(eventDefinition.entityIdentifier())
                    .width(eventDefinition.width())
                    .height(eventDefinition.height())
                    .offset(eventDefinition.offset())
                    .build();
        }

        System.out.println("def used " + definition);

        Entity entity;
        if (packet.getType() == EntityType.FALLING_BLOCK) {
            entity = new FallingBlockEntity(session, packet.getEntityId(), session.getEntityCache().getNextEntityId().incrementAndGet(), packet.getUuid(),
                    position, motion, yaw, pitch, headYaw, ((FallingBlockData) packet.getData()).getId());
        } else if (packet.getType() == EntityType.ITEM_FRAME || packet.getType() == EntityType.GLOW_ITEM_FRAME) {
            // Item frames need the hanging direction
            entity = new ItemFrameEntity(session, packet.getEntityId(), session.getEntityCache().getNextEntityId().incrementAndGet(), packet.getUuid(),
                    definition, position, motion, yaw, pitch, headYaw, (Direction) packet.getData());
        } else if (packet.getType() == EntityType.PAINTING) {
            entity = new PaintingEntity(session, packet.getEntityId(), session.getEntityCache().getNextEntityId().incrementAndGet(), packet.getUuid(),
                    definition, position, motion, yaw, pitch, headYaw, (Direction) packet.getData());
        } else if (packet.getType() == EntityType.FISHING_BOBBER) {
            // Fishing bobbers need the owner for the line
            int ownerEntityId = ((ProjectileData) packet.getData()).getOwnerId();
            Entity owner = session.getEntityCache().getEntityByJavaId(ownerEntityId);
            // Java clients only spawn fishing hooks with a player as its owner
            if (owner instanceof PlayerEntity) {
                entity = new FishingHookEntity(session, packet.getEntityId(), session.getEntityCache().getNextEntityId().incrementAndGet(), packet.getUuid(),
                        position, motion, yaw, pitch, headYaw, (PlayerEntity) owner);
            } else {
                return;
            }
        } else {
            entity = definition.factory().create(session, packet.getEntityId(), session.getEntityCache().getNextEntityId().incrementAndGet(),
                    packet.getUuid(), definition, position, motion, yaw, pitch, headYaw);
        }
        session.getEntityCache().spawnEntity(entity);
    }
}
