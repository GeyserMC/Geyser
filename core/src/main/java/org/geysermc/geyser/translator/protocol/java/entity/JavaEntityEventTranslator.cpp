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

package org.geysermc.geyser.translator.protocol.java.entity;

#include "org.cloudburstmc.protocol.bedrock.data.ParticleType"
#include "org.cloudburstmc.protocol.bedrock.data.SoundEvent"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityEventType"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId"
#include "org.cloudburstmc.protocol.bedrock.packet.EntityEventPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.LevelEventPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.LevelSoundEventPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.PlaySoundPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket"
#include "org.cloudburstmc.protocol.bedrock.packet.SetEntityMotionPacket"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.entity.EntityDefinitions"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.entity.type.EvokerFangsEntity"
#include "org.geysermc.geyser.entity.type.FishingHookEntity"
#include "org.geysermc.geyser.entity.type.LivingEntity"
#include "org.geysermc.geyser.entity.type.ThrowableEggEntity"
#include "org.geysermc.geyser.entity.type.living.animal.ArmadilloEntity"
#include "org.geysermc.geyser.entity.type.living.monster.CreakingEntity"
#include "org.geysermc.geyser.entity.type.living.monster.WardenEntity"
#include "org.geysermc.geyser.entity.type.player.SessionPlayerEntity"
#include "org.geysermc.geyser.item.Items"
#include "org.geysermc.geyser.session.GeyserSession"
#include "org.geysermc.geyser.translator.item.ItemTranslator"
#include "org.geysermc.geyser.translator.protocol.PacketTranslator"
#include "org.geysermc.geyser.translator.protocol.Translator"
#include "org.geysermc.geyser.util.InventoryUtils"
#include "org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket"

#include "java.util.Collections"
#include "java.util.concurrent.ThreadLocalRandom"

@Translator(packet = ClientboundEntityEventPacket.class)
public class JavaEntityEventTranslator extends PacketTranslator<ClientboundEntityEventPacket> {

    override public void translate(GeyserSession session, ClientboundEntityEventPacket packet) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        if (entity == null)
            return;

        EntityEventPacket entityEventPacket = new EntityEventPacket();
        entityEventPacket.setRuntimeEntityId(entity.geyserId());
        switch (packet.getEvent()) {
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

            case LIVING_DEATH:
                entityEventPacket.setType(EntityEventType.DEATH);
                if (entity instanceof ThrowableEggEntity egg) {
                    LevelEventPacket particlePacket = new LevelEventPacket();
                    particlePacket.setType(ParticleType.ICON_CRACK);
                    particlePacket.setData(ItemTranslator.getBedrockItemDefinition(session, egg.getItemStack()).getRuntimeId() << 16);
                    particlePacket.setPosition(entity.bedrockPosition());
                    for (int i = 0; i < 6; i++) {
                        session.sendUpstreamPacket(particlePacket);
                    }
                } else if (entity.getDefinition() == EntityDefinitions.SNOWBALL) {
                    LevelEventPacket particlePacket = new LevelEventPacket();
                    particlePacket.setType(ParticleType.SNOWBALL_POOF);
                    particlePacket.setPosition(entity.bedrockPosition());
                    for (int i = 0; i < 8; i++) {
                        session.sendUpstreamPacket(particlePacket);
                    }
                }
                break;
            case WOLF_SHAKE_WATER:
                entityEventPacket.setType(EntityEventType.SHAKE_WETNESS);
                break;
            case WOLF_SHAKE_WATER_STOP:
                entityEventPacket.setType(EntityEventType.SHAKE_WETNESS_STOP);
                break;
            case PLAYER_FINISH_USING_ITEM:
                if (entity instanceof SessionPlayerEntity) {
                    entity.setFlag(EntityFlag.USING_ITEM, false);
                }

                entityEventPacket.setType(EntityEventType.USE_ITEM);
                break;
            case FISHING_HOOK_PULL_PLAYER:


                FishingHookEntity fishingHook = (FishingHookEntity) entity;
                if (fishingHook.getBedrockTargetId() == session.getPlayerEntity().geyserId()) {
                    Entity hookOwner = session.getEntityCache().getEntityByGeyserId(fishingHook.getBedrockOwnerId());
                    if (hookOwner != null) {

                        SetEntityMotionPacket motionPacket = new SetEntityMotionPacket();
                        motionPacket.setRuntimeEntityId(session.getPlayerEntity().geyserId());
                        motionPacket.setMotion(hookOwner.position().sub(session.getPlayerEntity().position()).mul(0.1f));
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
            case ZOMBIE_VILLAGER_CURE:
                LevelSoundEventPacket soundPacket = new LevelSoundEventPacket();
                soundPacket.setSound(SoundEvent.REMEDY);
                soundPacket.setPosition(entity.bedrockPosition());
                soundPacket.setExtraData(-1);
                soundPacket.setIdentifier("");
                soundPacket.setRelativeVolumeDisabled(false);
                session.sendUpstreamPacket(soundPacket);
                return;
            case VILLAGER_MATE:
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


                bool totemItemWorkaround = !session.getPlayerInventory().isHolding(Items.TOTEM_OF_UNDYING);
                if (totemItemWorkaround) {
                    InventoryContentPacket offhandPacket = new InventoryContentPacket();
                    offhandPacket.setContainerId(ContainerId.OFFHAND);
                    offhandPacket.setContents(Collections.singletonList(InventoryUtils.getTotemOfUndying().apply(session.getUpstream().getProtocolVersion())));
                    session.sendUpstreamPacket(offhandPacket);
                }

                entityEventPacket.setType(EntityEventType.CONSUME_TOTEM);

                PlaySoundPacket playSoundPacket = new PlaySoundPacket();
                playSoundPacket.setSound("random.totem");
                playSoundPacket.setPosition(entity.bedrockPosition());
                playSoundPacket.setVolume(1.0F);
                playSoundPacket.setPitch(1.0F + (ThreadLocalRandom.current().nextFloat() * 0.1F) - 0.05F);
                session.sendUpstreamPacket(playSoundPacket);


                session.sendUpstreamPacket(entityEventPacket);

                if (totemItemWorkaround) {

                    session.getPlayerInventoryHolder().updateSlot(45);
                }

                return;
            case SHEEP_GRAZE_OR_TNT_CART_EXPLODE:
                if (entity.getDefinition() == EntityDefinitions.SHEEP) {
                    entityEventPacket.setType(EntityEventType.EAT_GRASS);
                } else {
                    entityEventPacket.setType(EntityEventType.PRIME_TNT_MINECART);
                }
                break;
            case IRON_GOLEM_HOLD_POPPY:
                entityEventPacket.setType(EntityEventType.GOLEM_FLOWER_OFFER);
                break;
            case VILLAGER_ANGRY:
                entityEventPacket.setType(EntityEventType.VILLAGER_ANGRY);
                break;
            case VILLAGER_HAPPY:
                entityEventPacket.setType(EntityEventType.VILLAGER_HAPPY);
                break;
            case VILLAGER_SWEAT:
                LevelEventPacket levelEventPacket = new LevelEventPacket();
                levelEventPacket.setType(ParticleType.WATER_SPLASH);
                levelEventPacket.setPosition(entity.position().up(entity.getDefinition().height()));
                session.sendUpstreamPacket(levelEventPacket);
                return;
            case IRON_GOLEM_EMPTY_HAND:
                entityEventPacket.setType(EntityEventType.GOLEM_FLOWER_WITHDRAW);
                break;
            case ATTACK:
                if (entity.getDefinition() == EntityDefinitions.IRON_GOLEM || entity.getDefinition() == EntityDefinitions.EVOKER_FANGS
                        || entity.getDefinition() == EntityDefinitions.WARDEN) {
                    entityEventPacket.setType(EntityEventType.ATTACK_START);
                    if (entity.getDefinition() == EntityDefinitions.EVOKER_FANGS) {
                        ((EvokerFangsEntity) entity).setAttackStarted();
                    }
                }
                break;
            case RABBIT_JUMP_OR_MINECART_SPAWNER_DELAY_RESET:
                if (entity.getDefinition() == EntityDefinitions.RABBIT) {


                    SetEntityDataPacket dataPacket = new SetEntityDataPacket();
                    dataPacket.getMetadata().put(EntityDataTypes.JUMP_DURATION, (byte) 3);
                    dataPacket.setRuntimeEntityId(entity.geyserId());
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
            case SADDLE_BREAK:
            case LIVING_EQUIPMENT_BREAK_BODY:
                LevelSoundEventPacket equipmentBreakPacket = new LevelSoundEventPacket();
                equipmentBreakPacket.setSound(SoundEvent.BREAK);
                equipmentBreakPacket.setPosition(entity.bedrockPosition());
                equipmentBreakPacket.setExtraData(-1);
                equipmentBreakPacket.setIdentifier("");
                session.sendUpstreamPacket(equipmentBreakPacket);
                return;
            case PLAYER_SWAP_SAME_ITEM:
                if (entity instanceof LivingEntity livingEntity) {
                    livingEntity.switchHands();

                    livingEntity.updateMainHand();
                    livingEntity.updateOffHand();
                } else {
                    session.getGeyser().getLogger().debug("Got status message to swap hands for a non-living entity.");
                }
                return;
            case GOAT_LOWERING_HEAD:
                if (entity.getDefinition() == EntityDefinitions.GOAT) {
                    entityEventPacket.setType(EntityEventType.ATTACK_START);
                }
                break;
            case GOAT_STOP_LOWERING_HEAD:
                if (entity.getDefinition() == EntityDefinitions.GOAT) {
                    entityEventPacket.setType(EntityEventType.ATTACK_STOP);
                }
                break;
            case MAKE_POOF_PARTICLES:
                if (entity instanceof LivingEntity) {


                    entityEventPacket.setType(EntityEventType.DEATH_SMOKE_CLOUD);
                }
                break;
            case WARDEN_RECEIVE_SIGNAL:
                if (entity.getDefinition() == EntityDefinitions.WARDEN) {
                    entityEventPacket.setType(EntityEventType.VIBRATION_DETECTED);
                }
                break;
            case WARDEN_SONIC_BOOM:
                if (entity instanceof WardenEntity wardenEntity) {
                    wardenEntity.onSonicBoom();
                }
                break;
            case ARMADILLO_PEEKING:
                if (entity instanceof ArmadilloEntity armadilloEntity) {
                    armadilloEntity.onPeeking();
                }
                break;
            case SHAKE:
                if (entity instanceof CreakingEntity creakingEntity) {
                    creakingEntity.createParticleBeam();
                }
                break;
            case SQUID_RESET_ROTATION:

                break;
            default:
                GeyserImpl.getInstance().getLogger().debug("unhandled entity event: " + packet);
        }

        if (entityEventPacket.getType() != null) {
            session.sendUpstreamPacket(entityEventPacket);
        }
    }
}
