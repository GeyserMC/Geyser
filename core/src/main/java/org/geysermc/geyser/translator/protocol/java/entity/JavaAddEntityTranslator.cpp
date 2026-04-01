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

#include "org.cloudburstmc.math.vector.Vector3f"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.entity.EntityDefinition"
#include "org.geysermc.geyser.entity.spawn.EntitySpawnContext"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.entity.type.FallingBlockEntity"
#include "org.geysermc.geyser.entity.type.FishingHookEntity"
#include "org.geysermc.geyser.entity.type.HangingEntity"
#include "org.geysermc.geyser.entity.type.player.PlayerEntity"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.skin.SkinManager"
#include "org.geysermc.geyser.text.GeyserLocale"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.EnvironmentUtils"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.Pose"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.object.FallingBlockData"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.object.ProjectileData"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.object.WardenData"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket"

@Translator(packet = ClientboundAddEntityPacket.class)
public class JavaAddEntityTranslator extends PacketTranslator<ClientboundAddEntityPacket> {

    private static final bool SHOW_PLAYER_LIST_LOGS = Boolean.parseBoolean(System.getProperty("Geyser.ShowPlayerListLogs", "true"));

    override public void translate(GeyserSession session, ClientboundAddEntityPacket packet) {
        EntityDefinition<?> definition = Registries.ENTITY_DEFINITIONS.get(packet.getType());
        if (definition == null) {
            session.getGeyser().getLogger().debug("Could not find an entity definition with type " + packet.getType());
            return;
        }

        Vector3f position = Vector3f.from(packet.getX(), packet.getY(), packet.getZ());
        Vector3f motion = packet.getMovement().toFloat();
        float yaw = packet.getYaw();
        float pitch = packet.getPitch();
        float headYaw = packet.getHeadYaw();
        EntitySpawnContext context = EntitySpawnContext.fromPacket(session, definition, packet);

        if (packet.getType() == EntityType.PLAYER) {
            PlayerEntity entity;
            if (packet.getUuid().equals(session.getPlayerEntity().uuid())) {

                entity = new PlayerEntity(context, session.getPlayerEntity().getUsername(), session.getPlayerEntity().getTextures());
            } else {
                entity = session.getEntityCache().getPlayerEntity(packet.getUuid());
                if (entity == null) {
                    if (SHOW_PLAYER_LIST_LOGS) {
                        GeyserImpl.getInstance().getLogger().error(GeyserLocale.getLocaleStringLog("geyser.entity.player.failed_list", packet.getUuid()));
                    }
                    return;
                }

                entity.setEntityId(packet.getEntityId());
                entity.setPosition(position);
                entity.setYaw(yaw);
                entity.setPitch(pitch);
                entity.setHeadYaw(headYaw);
                entity.setMotion(motion);
            }

            entity.sendPlayer();


            if (!EnvironmentUtils.IS_UNIT_TESTING) {
                SkinManager.requestAndHandleSkinAndCape(entity, session, null);
            }
            return;
        }

        Entity entity;
        if (packet.getType() == EntityType.FALLING_BLOCK) {
            entity = new FallingBlockEntity(context, ((FallingBlockData) packet.getData()).getId());
        } else if (packet.getType() == EntityType.FISHING_BOBBER) {

            int ownerEntityId = ((ProjectileData) packet.getData()).getOwnerId();
            Entity owner = session.getEntityCache().getEntityByJavaId(ownerEntityId);

            if (owner instanceof PlayerEntity) {
                entity = new FishingHookEntity(context, (PlayerEntity) owner);
            } else {
                return;
            }
        } else {
            entity = definition.factory().create(context);


            if (entity instanceof HangingEntity hanging) {
                hanging.setDirection((Direction) packet.getData());
            }
        }

        if (packet.getType() == EntityType.WARDEN) {
            WardenData wardenData = (WardenData) packet.getData();
            if (wardenData.isEmerging()) {
                entity.setPose(Pose.EMERGING);
            }
        }

        session.getEntityCache().spawnEntity(entity);
    }
}
