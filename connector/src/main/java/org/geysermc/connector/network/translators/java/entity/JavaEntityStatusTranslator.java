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

package org.geysermc.connector.network.translators.java.entity;

import com.github.steveice10.mc.protocol.packet.ingame.server.entity.ServerEntityStatusPacket;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityEventType;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.packet.EntityEventPacket;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEvent2Packet;
import com.nukkitx.protocol.bedrock.packet.SetEntityDataPacket;
import com.nukkitx.protocol.bedrock.packet.SetEntityMotionPacket;
import org.geysermc.connector.entity.Entity;
import org.geysermc.connector.entity.LivingEntity;
import org.geysermc.connector.entity.type.EntityType;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.network.translators.PacketTranslator;
import org.geysermc.connector.network.translators.Translator;
import org.geysermc.connector.network.translators.item.ItemRegistry;

@Translator(packet = ServerEntityStatusPacket.class)
public class JavaEntityStatusTranslator extends PacketTranslator<ServerEntityStatusPacket> {

    @Override
    public void translate(ServerEntityStatusPacket packet, GeyserSession session) {
        Entity entity;
        if (packet.getEntityId() == session.getPlayerEntity().getEntityId()) {
            entity = session.getPlayerEntity();
        } else {
            entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
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
            case LIVING_HURT_THORNS:
                entityEventPacket.setType(EntityEventType.HURT);
                break;
            case LIVING_DEATH:
                entityEventPacket.setType(EntityEventType.DEATH);
                if (entity.getEntityType() == EntityType.THROWN_EGG) {
                    LevelEventPacket particlePacket = new LevelEventPacket();
                    particlePacket.setType(LevelEventType.PARTICLE_ITEM_BREAK);
                    particlePacket.setData(ItemRegistry.EGG.getBedrockId() << 16);
                    particlePacket.setPosition(entity.getPosition());
                    for (int i = 0; i < 6; i++) {
                        session.sendUpstreamPacket(particlePacket);
                    }
                } else if (entity.getEntityType() == EntityType.SNOWBALL) {
                    LevelEventPacket particlePacket = new LevelEventPacket();
                    particlePacket.setType(LevelEventType.PARTICLE_SNOWBALL_POOF);
                    particlePacket.setPosition(entity.getPosition());
                    for (int i = 0; i < 8; i++) {
                        session.sendUpstreamPacket(particlePacket);
                    }
                }
                break;
            case WOLF_SHAKE_WATER:
                entityEventPacket.setType(EntityEventType.SHAKE_WETNESS);
                break;
            case PLAYER_FINISH_USING_ITEM:
                entityEventPacket.setType(EntityEventType.USE_ITEM);
                break;
            case FISHING_HOOK_PULL_PLAYER:
                // Player is pulled from a fishing rod
                // The physics of this are clientside on Java
                long pulledById = entity.getMetadata().getLong(EntityData.TARGET_EID);
                if (session.getPlayerEntity().getGeyserId() == pulledById) {
                    Entity hookOwner = session.getEntityCache().getEntityByGeyserId(entity.getMetadata().getLong(EntityData.OWNER_EID));
                    if (hookOwner != null) {
                        // https://minecraft.gamepedia.com/Fishing_Rod#Hooking_mobs_and_other_entities
                        SetEntityMotionPacket motionPacket = new SetEntityMotionPacket();
                        motionPacket.setRuntimeEntityId(session.getPlayerEntity().getGeyserId());
                        motionPacket.setMotion(hookOwner.getPosition().sub(session.getPlayerEntity().getPosition()).mul(0.1f));
                        session.sendUpstreamPacket(motionPacket);
                    }
                }
                return;
            case TAMEABLE_TAMING_FAILED:
                entityEventPacket.setType(EntityEventType.TAME_FAILED);
                break;
            case TAMEABLE_TAMING_SUCCEEDED:
                entityEventPacket.setType(EntityEventType.TAME_SUCCEEDED);
                break;
            case ZOMBIE_VILLAGER_CURE: // Played when a zombie bites the golden apple
                LevelSoundEvent2Packet soundPacket = new LevelSoundEvent2Packet();
                soundPacket.setSound(SoundEvent.REMEDY);
                soundPacket.setPosition(entity.getPosition());
                soundPacket.setExtraData(-1);
                soundPacket.setIdentifier("");
                soundPacket.setRelativeVolumeDisabled(false);
                session.sendUpstreamPacket(soundPacket);
                return;
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
            case RABBIT_JUMP_OR_MINECART_SPAWNER_DELAY_RESET:
                if (entity.getEntityType() == EntityType.RABBIT) {
                    // This doesn't match vanilla Bedrock behavior but I'm unsure how to make it better
                    // I assume part of the problem is that Bedrock uses a duration and Java just says the rabbit is jumping
                    SetEntityDataPacket dataPacket = new SetEntityDataPacket();
                    dataPacket.getMetadata().put(EntityData.JUMP_DURATION, (byte) 3);
                    dataPacket.setRuntimeEntityId(entity.getGeyserId());
                    session.sendUpstreamPacket(dataPacket);
                    return;
                }
                break;
            case LIVING_EQUIPMENT_BREAK_HEAD:
            case LIVING_EQUIPMENT_BREAK_CHEST:
            case LIVING_EQUIPMENT_BREAK_LEGS:
            case LIVING_EQUIPMENT_BREAK_FEET:
            case LIVING_EQUIPMENT_BREAK_MAIN_HAND:
            case LIVING_EQUIPMENT_BREAK_OFF_HAND:
                LevelSoundEvent2Packet equipmentBreakPacket = new LevelSoundEvent2Packet();
                equipmentBreakPacket.setSound(SoundEvent.BREAK);
                equipmentBreakPacket.setPosition(entity.getPosition());
                equipmentBreakPacket.setExtraData(-1);
                equipmentBreakPacket.setIdentifier("");
                session.sendUpstreamPacket(equipmentBreakPacket);
                return;
            case PLAYER_SWAP_SAME_ITEM: // Not just used for players
                if (entity instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    ItemData newMainHand = livingEntity.getOffHand();
                    livingEntity.setOffHand(livingEntity.getHand());
                    livingEntity.setHand(newMainHand);

                    livingEntity.updateMainHand(session);
                    livingEntity.updateOffHand(session);
                } else {
                    session.getConnector().getLogger().debug("Got status message to swap hands for a non-living entity.");
                }
                return;
        }

        session.sendUpstreamPacket(entityEventPacket);
    }
}
