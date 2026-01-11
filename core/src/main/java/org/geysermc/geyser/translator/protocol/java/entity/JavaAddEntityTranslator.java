/*
 * Copyright (c) 2019-2025 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.translator.protocol.java.entity;

import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.entity.EntityTypeDefinition;
import org.geysermc.geyser.entity.GeyserEntityType;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.FallingBlockEntity;
import org.geysermc.geyser.entity.type.FishingHookEntity;
import org.geysermc.geyser.entity.type.HangingEntity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkinManager;
import org.geysermc.geyser.text.GeyserLocale;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.EnvironmentUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.FallingBlockData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.ProjectileData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.WardenData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.BuiltinEntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;

@Translator(packet = ClientboundAddEntityPacket.class)
public class JavaAddEntityTranslator extends PacketTranslator<ClientboundAddEntityPacket> {

    private static final boolean SHOW_PLAYER_LIST_LOGS = Boolean.parseBoolean(System.getProperty("Geyser.ShowPlayerListLogs", "true"));

    @Override
    public void translate(GeyserSession session, ClientboundAddEntityPacket packet) {
        GeyserEntityType type = GeyserEntityType.of(packet.getType());
        if (type.isUnregistered()) {
            session.getGeyser().getLogger().warning("Received unregistered entity type " + type + " in add entity packet");
            return;
        }

        EntityTypeDefinition<?> definition = Registries.JAVA_ENTITY_TYPES.get(type);
        if (definition == null) {
            session.getGeyser().getLogger().debug("Could not find an entity definition for add entity packet " + packet);
            return;
        }

        EntitySpawnContext context = EntitySpawnContext.fromPacket(session, definition, packet);
        if (type.is(BuiltinEntityType.PLAYER)) {
            PlayerEntity entity;
            if (packet.getUuid().equals(session.getPlayerEntity().uuid())) {
                // Server is sending a fake version of the current player
                entity = new PlayerEntity(context, session.getPlayerEntity().getUsername(), session.getPlayerEntity().getTexturesProperty());
            } else {
                entity = session.getEntityCache().getPlayerEntity(packet.getUuid());
                if (entity == null) {
                    if (SHOW_PLAYER_LIST_LOGS) {
                        GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.entity.player.failed_list", packet.getUuid()));
                    }
                    return;
                }

                entity.setEntityId(packet.getEntityId());
                entity.position(context.position());
                entity.setYaw(packet.getYaw());
                entity.setPitch(packet.getPitch());
                entity.setHeadYaw(packet.getHeadYaw());
                entity.setMotion(packet.getMovement().toFloat());
            }

            entity.sendPlayer();
            // only load skin if we're not in a test environment.
            // Otherwise, it tries to load various resources
            if (!EnvironmentUtils.IS_UNIT_TESTING) {
                SkinManager.requestAndHandleSkinAndCape(entity, session, null);
            }
            return;
        }

        if (!context.callServerSpawnEvent()) {
            GeyserImpl.getInstance().getLogger().debug(session, "Cancelled entity spawn (%s) at (%s)".formatted(type.identifier(), context.position()));
            return;
        }

        Entity entity;
        if (type.is(BuiltinEntityType.FALLING_BLOCK)) {
            entity = new FallingBlockEntity(context, ((FallingBlockData) packet.getData()).getId());
        } else if (type.is(BuiltinEntityType.FISHING_BOBBER)) {
            // Fishing bobbers need the owner for the line
            int ownerEntityId = ((ProjectileData) packet.getData()).getOwnerId();
            Entity owner = session.getEntityCache().getEntityByJavaId(ownerEntityId);
            // Java clients only spawn fishing hooks with a player as its owner
            if (owner instanceof PlayerEntity) {
                entity = new FishingHookEntity(context, (PlayerEntity) owner);
            } else {
                return;
            }
        } else {
            entity = definition.factory().create(context);

            // This is done over entity metadata in modern versions, but is still sent over network in the spawn packet
            if (entity instanceof HangingEntity hanging) {
                hanging.setDirection((Direction) packet.getData());
            }
        }

        if (type.is(BuiltinEntityType.WARDEN)) {
            WardenData wardenData = (WardenData) packet.getData();
            if (wardenData.isEmerging()) {
                entity.setPose(Pose.EMERGING);
            }
        }

        // Call pre-spawn consumer
        if (context.consumers() != null) {
            context.consumers().forEach(consumer -> consumer.accept(entity));
        }

        session.getEntityCache().spawnEntity(entity);
    }
}
