/*
 * Copyright (c) 2019 GeyserMC. http://geysermc.org
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
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;

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
            case LIVING_HURT:
            case LIVING_HURT_SWEET_BERRY_BUSH:
                entityEventPacket.setEvent(EntityEventPacket.Event.HURT_ANIMATION);
                break;
            case LIVING_DEATH:
                entityEventPacket.setEvent(EntityEventPacket.Event.DEATH_ANIMATION);
                break;
            case WOLF_SHAKE_WATER:
                entityEventPacket.setEvent(EntityEventPacket.Event.SHAKE_WET);
                break;
            case PLAYER_FINISH_USING_ITEM:
                entityEventPacket.setEvent(EntityEventPacket.Event.USE_ITEM);
                break;
            case FISHING_HOOK_PULL_PLAYER:
                entityEventPacket.setEvent(EntityEventPacket.Event.FISH_HOOK_LURED);
                break;
            case TAMEABLE_TAMING_FAILED:
                entityEventPacket.setEvent(EntityEventPacket.Event.TAME_FAIL);
                break;
            case TAMEABLE_TAMING_SUCCEEDED:
                entityEventPacket.setEvent(EntityEventPacket.Event.TAME_SUCCESS);
            case ZOMBIE_VILLAGER_CURE:
                entityEventPacket.setEvent(EntityEventPacket.Event.ZOMBIE_VILLAGER_CURE);
                break;
            case ANIMAL_EMIT_HEARTS:
                entityEventPacket.setEvent(EntityEventPacket.Event.LOVE_PARTICLES);
                break;
            case FIREWORK_EXPLODE:
                entityEventPacket.setEvent(EntityEventPacket.Event.FIREWORK_PARTICLES);
                break;
            case WITCH_EMIT_PARTICLES:
                entityEventPacket.setEvent(EntityEventPacket.Event.WITCH_SPELL_PARTICLES);
                break;
            case TOTEM_OF_UNDYING_MAKE_SOUND:
                entityEventPacket.setEvent(EntityEventPacket.Event.CONSUME_TOTEM);
                break;
            case SHEEP_GRAZE_OR_TNT_CART_EXPLODE:
                entityEventPacket.setEvent(EntityEventPacket.Event.MINECART_TNT_PRIME_FUSE);
                break;
            case IRON_GOLEM_HOLD_POPPY:
                entityEventPacket.setEvent(EntityEventPacket.Event.IRON_GOLEM_OFFER_FLOWER);
                break;
            case IRON_GOLEM_EMPTY_HAND:
                entityEventPacket.setEvent(EntityEventPacket.Event.IRON_GOLEM_WITHDRAW_FLOWER);
                break;
        }

        session.getUpstream().sendPacket(entityEventPacket);
    }
}
