/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
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

package org.geysermc.connector.network.translators.java.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityStatusPacket;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;

@Translator(packet = ServerEntityStatusPacket.class)
public class JavaEntityStatusTranslator extends PacketTranslator<ServerEntityStatusPacket> {

    @Override
    public void translate(ServerEntityStatusPacket packet, GeyserSession session) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            entity = session.getPlayerEntity();
        }
        if (entity == null)
            return;

        EntityEventPacket entityEventPacket = new EntityEventPacket();
        entityEventPacket.setRuntimeEntityId(entity.getGeyserId());
        switch (packet.getStatus()) {
            case PLAYER_ENABLE_REDUCED_DEBUG:
                session.setReducedDebugInfo(true);
                return;
            case PLAYER_DISABLE_REDUCED_DEBUG:
                session.setReducedDebugInfo(false);
                return;
            case PLAYER_OP_PERMISSION_LEVEL_0:
                session.setOpPermissionLevel(0);
                session.sendAdventureSettings();
                return;
            case PLAYER_OP_PERMISSION_LEVEL_1:
                session.setOpPermissionLevel(1);
                session.sendAdventureSettings();
                return;
            case PLAYER_OP_PERMISSION_LEVEL_2:
                session.setOpPermissionLevel(2);
                session.sendAdventureSettings();
                return;
            case PLAYER_OP_PERMISSION_LEVEL_3:
                session.setOpPermissionLevel(3);
                session.sendAdventureSettings();
                return;
            case PLAYER_OP_PERMISSION_LEVEL_4:
                session.setOpPermissionLevel(4);
                session.sendAdventureSettings();
                return;

            // EntityEventType.HURT sends extra data depending on the type of damage. However this appears to have no visual changes
            case LIVING_BURN:
            case LIVING_DROWN:
            case LIVING_HURT:
            case LIVING_HURT_SWEET_BERRY_BUSH:
                entityEventPacket.setType(EntityEventType.HURT);
                break;
            case LIVING_DEATH:
                entityEventPacket.setType(EntityEventType.DEATH);
                break;
            case WOLF_SHAKE_WATER:
                entityEventPacket.setType(EntityEventType.SHAKE_WETNESS);
                break;
            case PLAYER_FINISH_USING_ITEM:
                entityEventPacket.setType(EntityEventType.USE_ITEM);
                break;
            case FISHING_HOOK_PULL_PLAYER:
                entityEventPacket.setType(EntityEventType.FISH_HOOK_TEASE); //TODO: CHECK
                break;
            case TAMEABLE_TAMING_FAILED:
                entityEventPacket.setType(EntityEventType.TAME_FAILED);
                break;
            case TAMEABLE_TAMING_SUCCEEDED:
                entityEventPacket.setType(EntityEventType.TAME_SUCCEEDED);
                break;
            case ZOMBIE_VILLAGER_CURE:
                entityEventPacket.setType(EntityEventType.ZOMBIE_VILLAGER_CURE);
                break;
            case ANIMAL_EMIT_HEARTS:
                entityEventPacket.setType(EntityEventType.LOVE_PARTICLES);
                break;
            case FIREWORK_EXPLODE:
                entityEventPacket.setType(EntityEventType.FIREWORK_EXPLODE);
                break;
            case WITCH_EMIT_PARTICLES:
                entityEventPacket.setType(EntityEventType.WITCH_HAT_MAGIC); //TODO: CHECK
                break;
            case TOTEM_OF_UNDYING_MAKE_SOUND:
                entityEventPacket.setType(EntityEventType.CONSUME_TOTEM);
                break;
            case SHEEP_GRAZE_OR_TNT_CART_EXPLODE:
                if (entity.getEntityType() == EntityType.SHEEP) {
                    entityEventPacket.setType(EntityEventType.EAT_GRASS);
                } else {
                    entityEventPacket.setType(EntityEventType.PRIME_TNT_MINECART);
                }
                break;
            case IRON_GOLEM_HOLD_POPPY:
                entityEventPacket.setType(EntityEventType.GOLEM_FLOWER_OFFER);
                break;
            case IRON_GOLEM_EMPTY_HAND:
                entityEventPacket.setType(EntityEventType.GOLEM_FLOWER_WITHDRAW);
                break;
        }

        session.sendUpstreamPacket(entityEventPacket);
    }
}
