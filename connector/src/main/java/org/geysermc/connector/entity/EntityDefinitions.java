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

import com.github.steveice10.mc.protocol.data.game.entity.metadata.MetadataType;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.type.FloatEntityMetadata;
import com.github.steveice10.mc.protocol.data.game.entity.type.EntityType;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import com.nukkitx.protocol.bedrock.data.entity.EntityFlag;
import net.kyori.adventure.text.Component;
import org.geysermc.connector.entity.factory.BaseEntityFactory;
import org.geysermc.connector.entity.factory.EntityFactory;
import org.geysermc.connector.entity.factory.ExperienceOrbEntityFactory;
import org.geysermc.connector.entity.factory.PaintingEntityFactory;
import org.geysermc.connector.entity.living.*;
import org.geysermc.connector.entity.living.animal.*;
import org.geysermc.connector.entity.living.animal.horse.*;
import org.geysermc.connector.entity.living.animal.tameable.CatEntity;
import org.geysermc.connector.entity.living.animal.tameable.ParrotEntity;
import org.geysermc.connector.entity.living.animal.tameable.TameableEntity;
import org.geysermc.connector.entity.living.animal.tameable.WolfEntity;
import org.geysermc.connector.entity.living.merchant.AbstractMerchantEntity;
import org.geysermc.connector.entity.living.merchant.VillagerEntity;
import org.geysermc.connector.entity.living.monster.*;
import org.geysermc.connector.entity.living.monster.raid.PillagerEntity;
import org.geysermc.connector.entity.living.monster.raid.RaidParticipantEntity;
import org.geysermc.connector.entity.living.monster.raid.SpellcasterIllagerEntity;
import org.geysermc.connector.entity.living.monster.raid.VindicatorEntity;
import org.geysermc.connector.entity.player.PlayerEntity;
import org.geysermc.connector.network.translators.chat.MessageTranslator;

public final class EntityDefinitions {
    public static final EntityDefinition<AreaEffectCloudEntity> AREA_EFFECT_CLOUD;
    public static final EntityDefinition<ArmorStandEntity> ARMOR_STAND;
    public static final EntityDefinition<TippedArrowEntity> ARROW;
    public static final EntityDefinition<AxolotlEntity> AXOLOTL;
    public static final EntityDefinition<BatEntity> BAT;
    public static final EntityDefinition<BeeEntity> BEE;
    public static final EntityDefinition<BlazeEntity> BLAZE;
    public static final EntityDefinition<BoatEntity> BOAT;
    public static final EntityDefinition<CatEntity> CAT;
    public static final EntityDefinition<MonsterEntity> CAVE_SPIDER;
    public static final EntityDefinition<ChickenEntity> CHICKEN;
    public static final EntityDefinition<CreeperEntity> CREEPER;
    public static final EntityDefinition<AbstractFishEntity> COD;
    public static final EntityDefinition<AnimalEntity> COW;
    public static final EntityDefinition<ChestedHorseEntity> DONKEY;
    public static final EntityDefinition<WaterEntity> DOLPHIN;
    public static final EntityDefinition<ItemedFireballEntity> DRAGON_FIREBALL;
    public static final EntityDefinition<ZombieEntity> DROWNED;
    public static final EntityDefinition<ElderGuardianEntity> ELDER_GUARDIAN;
    public static final EntityDefinition<EndermanEntity> ENDERMAN;
    public static final EntityDefinition<MonsterEntity> ENDERMITE;
    public static final EntityDefinition<EnderDragonEntity> ENDER_DRAGON;
    public static final EntityDefinition<EnderCrystalEntity> END_CRYSTAL;
    public static final EntityDefinition<SpellcasterIllagerEntity> EVOKER;
    public static final EntityDefinition<Entity> EVOKER_FANGS;
    public static final EntityDefinition<ExpOrbEntity> EXPERIENCE_ORB;
    public static final EntityDefinition<Entity> EYE_OF_ENDER;
    public static final EntityDefinition<FallingBlockEntity> FALLING_BLOCK;
    public static final EntityDefinition<ItemedFireballEntity> FIREBALL;
    public static final EntityDefinition<FishingHookEntity> FISHING_BOBBER;
    public static final EntityDefinition<FireworkEntity> FIREWORK_ROCKET;
    public static final EntityDefinition<FoxEntity> FOX;
    public static final EntityDefinition<GiantEntity> GIANT;
    public static final EntityDefinition<GhastEntity> GHAST;
    public static final EntityDefinition<ItemFrameEntity> GLOW_ITEM_FRAME;
    public static final EntityDefinition<GlowSquidEntity> GLOW_SQUID;
    public static final EntityDefinition<GoatEntity> GOAT;
    public static final EntityDefinition<GuardianEntity> GUARDIAN;
    public static final EntityDefinition<HoglinEntity> HOGLIN;
    public static final EntityDefinition<HorseEntity> HORSE;
    public static final EntityDefinition<ZombieEntity> HUSK;
    public static final EntityDefinition<SpellcasterIllagerEntity> ILLUSIONER; // Not present on Bedrock
    public static final EntityDefinition<IronGolemEntity> IRON_GOLEM;
    public static final EntityDefinition<ItemEntity> ITEM;
    public static final EntityDefinition<ItemFrameEntity> ITEM_FRAME;
    public static final EntityDefinition<LeashKnotEntity> LEASH_KNOT;
    public static final EntityDefinition<LightningEntity> LIGHTNING_BOLT;
    public static final EntityDefinition<LlamaEntity> LLAMA;
    public static final EntityDefinition<ThrowableEntity> LLAMA_SPIT;
    public static final EntityDefinition<MagmaCubeEntity> MAGMA_CUBE;
    public static final EntityDefinition<MinecartEntity> MINECART;
    public static final EntityDefinition<MinecartEntity> MINECART_CHEST;
    public static final EntityDefinition<CommandBlockMinecartEntity> MINECART_COMMAND_BLOCK;
    public static final EntityDefinition<MinecartEntity> MINECART_HOPPER;
    public static final EntityDefinition<FurnaceMinecartEntity> MINECART_FURNACE; // Not present on Bedrock
    public static final EntityDefinition<SpawnerMinecartEntity> MINECART_SPAWNER; // Not present on Bedrock
    public static final EntityDefinition<MinecartEntity> MINECART_TNT;
    public static final EntityDefinition<MooshroomEntity> MOOSHROOM;
    public static final EntityDefinition<ChestedHorseEntity> MULE;
    public static final EntityDefinition<OcelotEntity> OCELOT;
    public static final EntityDefinition<PaintingEntity> PAINTING;
    public static final EntityDefinition<PandaEntity> PANDA;
    public static final EntityDefinition<ParrotEntity> PARROT;
    public static final EntityDefinition<PhantomEntity> PHANTOM;
    public static final EntityDefinition<PigEntity> PIG;
    public static final EntityDefinition<PiglinEntity> PIGLIN;
    public static final EntityDefinition<BasePiglinEntity> PIGLIN_BRUTE;
    public static final EntityDefinition<PillagerEntity> PILLAGER;
    public static final EntityDefinition<PlayerEntity> PLAYER;
    public static final EntityDefinition<PolarBearEntity> POLAR_BEAR;
    public static final EntityDefinition<TNTEntity> PRIMED_TNT;
    public static final EntityDefinition<PufferFishEntity> PUFFERFISH;
    public static final EntityDefinition<RabbitEntity> RABBIT;
    public static final EntityDefinition<RaidParticipantEntity> RAVAGER;
    public static final EntityDefinition<AbstractFishEntity> SALMON;
    public static final EntityDefinition<SheepEntity> SHEEP;
    public static final EntityDefinition<ShulkerEntity> SHULKER;
    public static final EntityDefinition<ThrowableEntity> SHULKER_BULLET;
    public static final EntityDefinition<MonsterEntity> SILVERFISH;
    public static final EntityDefinition<SkeletonEntity> SKELETON;
    public static final EntityDefinition<AbstractHorseEntity> SKELETON_HORSE;
    public static final EntityDefinition<SlimeEntity> SLIME;
    public static final EntityDefinition<ItemedFireballEntity> SMALL_FIREBALL;
    public static final EntityDefinition<ThrowableItemEntity> SNOWBALL;
    public static final EntityDefinition<SnowGolemEntity> SNOW_GOLEM;
    public static final EntityDefinition<AbstractArrowEntity> SPECTRAL_ARROW;
    public static final EntityDefinition<SpiderEntity> SPIDER;
    public static final EntityDefinition<SquidEntity> SQUID;
    public static final EntityDefinition<AbstractSkeletonEntity> STRAY;
    public static final EntityDefinition<StriderEntity> STRIDER;
    public static final EntityDefinition<ThrowableItemEntity> THROWN_EGG;
    public static final EntityDefinition<ThrowableItemEntity> THROWN_ENDERPEARL;
    public static final EntityDefinition<ThrowableItemEntity> THROWN_EXP_BOTTLE;
    public static final EntityDefinition<ThrownPotionEntity> THROWN_POTION;
    public static final EntityDefinition<TropicalFishEntity> TROPICAL_FISH;
    public static final EntityDefinition<TurtleEntity> TURTLE;
    public static final EntityDefinition<TraderLlamaEntity> TRADER_LLAMA;
    public static final EntityDefinition<TridentEntity> TRIDENT;
    public static final EntityDefinition<AbstractMerchantEntity> WANDERING_TRADER;
    public static final EntityDefinition<RaidParticipantEntity> WITCH;
    public static final EntityDefinition<WitherEntity> WITHER;
    public static final EntityDefinition<AbstractSkeletonEntity> WITHER_SKELETON;
    public static final EntityDefinition<WitherSkullEntity> WITHER_SKULL;
    public static final EntityDefinition<WolfEntity> WOLF;
    public static final EntityDefinition<VillagerEntity> VILLAGER;
    public static final EntityDefinition<VindicatorEntity> VINDICATOR;
    public static final EntityDefinition<VexEntity> VEX;
    public static final EntityDefinition<ZoglinEntity> ZOGLIN;
    public static final EntityDefinition<ZombieEntity> ZOMBIE;
    public static final EntityDefinition<AbstractHorseEntity> ZOMBIE_HORSE;
    public static final EntityDefinition<ZombieVillagerEntity> ZOMBIE_VILLAGER;
    public static final EntityDefinition<ZombifiedPiglinEntity> ZOMBIFIED_PIGLIN;

    /**
     * Is not sent over the network
     */
    public static final EntityDefinition<EnderDragonPartEntity> ENDER_DRAGON_PART;
    /**
     * Special Bedrock type
     */
    public static final EntityDefinition<WitherSkullEntity> WITHER_SKULL_DANGEROUS;

    static {
        EntityDefinition<Entity> entityBase = EntityDefinition.builder((BaseEntityFactory<Entity>) Entity::new)
                .addTranslator(MetadataType.BYTE, Entity::setFlags)
                .addTranslator(MetadataType.INT, Entity::setAir) // Air/bubbles
                .addTranslator(MetadataType.OPTIONAL_CHAT, Entity::setDisplayName)
                .addTranslator(MetadataType.BOOLEAN, Entity::setDisplayNameVisible)
                .<Boolean>addTranslator(MetadataType.BOOLEAN, (entity, entityMetadata) -> entity.setFlag(EntityFlag.SILENT, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                .addTranslator(MetadataType.BOOLEAN, Entity::setGravity)
                .addTranslator(MetadataType.POSE, Entity::setPose)
                .addTranslator(MetadataType.INT, Entity::setFreezing)
                .build();

        // Extends entity
        {
            AREA_EFFECT_CLOUD = EntityDefinition.inherited(AreaEffectCloudEntity::new, entityBase)
                    .type(EntityType.AREA_EFFECT_CLOUD)
                    .bedrockId(95)
                    .height(0.5f).width(1.0f)
                    .addTranslator(MetadataType.FLOAT, AreaEffectCloudEntity::setRadius)
                    .addTranslator(MetadataType.INT, (entity, entityMetadata) -> entity.getDirtyMetadata().put(EntityData.EFFECT_COLOR, entityMetadata.getValue()))
                    .addTranslator(null) // Waiting
                    .addTranslator(MetadataType.PARTICLE, AreaEffectCloudEntity::setParticle)
                    .build();
            BOAT = EntityDefinition.inherited(BoatEntity::new, entityBase)
                    .type(EntityType.BOAT)
                    .bedrockId(90)
                    .height(0.6f).width(1.6f)
                    .offset(0.35f)
                    .addTranslator(MetadataType.INT, (boatEntity, entityMetadata) -> boatEntity.getDirtyMetadata().put(EntityData.HURT_TIME, entityMetadata.getValue())) // Time since last hit
                    .addTranslator(MetadataType.INT, (boatEntity, entityMetadata) -> boatEntity.getDirtyMetadata().put(EntityData.HURT_DIRECTION, entityMetadata.getValue())) // Rocking direction
                    .<Float>addTranslator(MetadataType.FLOAT, (boatEntity, entityMetadata) ->
                            // 'Health' in Bedrock, damage taken in Java - it makes motion in Bedrock
                            boatEntity.getDirtyMetadata().put(EntityData.HEALTH, 40 - ((int) ((FloatEntityMetadata) entityMetadata).getPrimitiveValue())))
                    .addTranslator(MetadataType.INT, BoatEntity::setVariant)
                    .addTranslator(MetadataType.BOOLEAN, BoatEntity::setPaddlingLeft)
                    .addTranslator(MetadataType.BOOLEAN, BoatEntity::setPaddlingRight)
                    .addTranslator(MetadataType.INT, (boatEntity, entityMetadata) -> boatEntity.getDirtyMetadata().put(EntityData.BOAT_BUBBLE_TIME, entityMetadata.getValue())) // May not actually do anything
                    .build();
            DRAGON_FIREBALL = EntityDefinition.inherited(ItemedFireballEntity::new, entityBase)
                    .type(EntityType.DRAGON_FIREBALL)
                    .bedrockId(79)
                    .heightAndWidth(1.0f)
                    .build();
            END_CRYSTAL = EntityDefinition.inherited(EnderCrystalEntity::new, entityBase)
                    .type(EntityType.END_CRYSTAL)
                    .bedrockId(71)
                    .heightAndWidth(2.0f)
                    .addTranslator(MetadataType.OPTIONAL_POSITION, EnderCrystalEntity::setBlockTarget)
                    .<Boolean>addTranslator(MetadataType.BOOLEAN,
                            (enderCrystalEntity, entityMetadata) -> enderCrystalEntity.setFlag(EntityFlag.SHOW_BOTTOM, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue())) // There is a base located on the ender crystal
                    .build();
            EXPERIENCE_ORB = EntityDefinition.inherited((ExperienceOrbEntityFactory) ExpOrbEntity::new, entityBase)
                    .type(EntityType.EXPERIENCE_ORB)
                    .bedrockId(69)
                    .identifier("minecraft:xp_orb")
                    .build();
            EVOKER_FANGS = EntityDefinition.inherited(entityBase.factory(), entityBase)
                    .type(EntityType.EVOKER_FANGS)
                    .bedrockId(103)
                    .height(0.8f).width(0.5f)
                    .identifier("minecraft:evocation_fang")
                    .build();
            EYE_OF_ENDER = EntityDefinition.inherited(Entity::new, entityBase)
                    .type(EntityType.EYE_OF_ENDER)
                    .bedrockId(70)
                    .heightAndWidth(0.25f)
                    .identifier("minecraft:eye_of_ender_signal")
                    .build();
            FALLING_BLOCK = EntityDefinition.inherited(new EntityFactory<FallingBlockEntity>() {
                    }, entityBase) // TODO
                    .type(EntityType.FALLING_BLOCK)
                    .bedrockId(66)
                    .heightAndWidth(0.98f)
                    .build();
            FIREBALL = EntityDefinition.inherited(ItemedFireballEntity::new, entityBase)
                    .type(EntityType.FIREBALL)
                    .bedrockId(85)
                    .heightAndWidth(1.0f)
                    .build();
            FIREWORK_ROCKET = EntityDefinition.inherited(FireworkEntity::new, entityBase)
                    .type(EntityType.FIREWORK_ROCKET)
                    .bedrockId(72)
                    .heightAndWidth(0.25f)
                    .identifier("minecraft:fireworks_rocket")
                    .addTranslator(MetadataType.ITEM, FireworkEntity::setFireworkItem)
                    .addTranslator(MetadataType.OPTIONAL_VARINT, FireworkEntity::setPlayerGliding)
                    .build();
            FISHING_BOBBER = EntityDefinition.inherited(new EntityFactory<FishingHookEntity>() {
                    }, entityBase) //TODO
                    .type(EntityType.FISHING_BOBBER)
                    .bedrockId(77)
                    .identifier("minecraft:fishing_book")
                    .addTranslator(MetadataType.INT, FishingHookEntity::setHookedEntity)
                    .build();
            ITEM = EntityDefinition.inherited(ItemEntity::new, entityBase)
                    .type(EntityType.ITEM)
                    .bedrockId(64)
                    .heightAndWidth(0.25f)
                    .offset(0.125f)
                    .addTranslator(MetadataType.ITEM, ItemEntity::setItem)
                    .build();
            LEASH_KNOT = EntityDefinition.inherited(LeashKnotEntity::new, entityBase)
                    .type(EntityType.LEASH_KNOT)
                    .bedrockId(88)
                    .height(0.5f).width(0.375f)
                    .build();
            LIGHTNING_BOLT = EntityDefinition.inherited(LightningEntity::new, entityBase)
                    .type(EntityType.LIGHTNING_BOLT)
                    .bedrockId(93)
                    .build();
            LLAMA_SPIT = EntityDefinition.inherited(ThrowableEntity::new, entityBase)
                    .type(EntityType.LLAMA_SPIT)
                    .bedrockId(102)
                    .heightAndWidth(0.25f)
                    .build();
            PAINTING = EntityDefinition.inherited((PaintingEntityFactory) PaintingEntity::new, entityBase)
                    .type(EntityType.PAINTING)
                    .bedrockId(83)
                    .build();
            PRIMED_TNT = EntityDefinition.inherited(TNTEntity::new, entityBase)
                    .type(EntityType.PRIMED_TNT)
                    .bedrockId(65)
                    .heightAndWidth(0.98f)
                    .identifier("minecraft:tnt")
                    .addTranslator(MetadataType.INT, TNTEntity::setFuseLength)
                    .build();
            SHULKER_BULLET = EntityDefinition.inherited(ThrowableEntity::new, entityBase)
                    .type(EntityType.SHULKER_BULLET)
                    .bedrockId(76)
                    .heightAndWidth(0.3125f)
                    .build();
            SMALL_FIREBALL = EntityDefinition.inherited(ItemedFireballEntity::new, entityBase)
                    .type(EntityType.SMALL_FIREBALL)
                    .bedrockId(94)
                    .heightAndWidth(0.3125f)
                    .build();
            SNOWBALL = EntityDefinition.inherited(ThrowableItemEntity::new, entityBase)
                    .type(EntityType.SNOWBALL)
                    .bedrockId(81)
                    .heightAndWidth(0.25f)
                    .build();
            THROWN_ENDERPEARL = EntityDefinition.inherited(ThrowableItemEntity::new, entityBase)
                    .type(EntityType.THROWN_ENDERPEARL)
                    .bedrockId(87)
                    .heightAndWidth(0.25f)
                    .identifier("minecraft:ender_pearl")
                    .build();
            THROWN_EGG = EntityDefinition.inherited(ThrowableItemEntity::new, entityBase)
                    .type(EntityType.THROWN_EGG)
                    .bedrockId(82)
                    .heightAndWidth(0.25f)
                    .identifier("minecraft:egg")
                    .build();
            THROWN_EXP_BOTTLE = EntityDefinition.inherited(ThrowableItemEntity::new, entityBase)
                    .type(EntityType.THROWN_EXP_BOTTLE)
                    .bedrockId(68)
                    .heightAndWidth(0.25f)
                    .identifier("minecraft:xp_bottle")
                    .build();
            THROWN_POTION = EntityDefinition.inherited(ThrownPotionEntity::new, entityBase)
                    .type(EntityType.THROWN_POTION)
                    .bedrockId(86)
                    .heightAndWidth(0.25f)
                    .identifier("minecraft:splash_potion")
                    .addTranslator(MetadataType.ITEM, ThrownPotionEntity::setPotion)
                    .build();

            EntityDefinition<AbstractArrowEntity> abstractArrowBase = EntityDefinition.inherited(AbstractArrowEntity::new, entityBase)
                    .addTranslator(MetadataType.BYTE, AbstractArrowEntity::setArrowFlags)
                    .addTranslator(null) // "Piercing level"
                    .build();
            ARROW = EntityDefinition.inherited(TippedArrowEntity::new, abstractArrowBase)
                    .type(EntityType.ARROW)
                    .bedrockId(80)
                    .heightAndWidth(0.25f)
                    .addTranslator(MetadataType.INT, TippedArrowEntity::setPotionEffectColor)
                    .build();
            SPECTRAL_ARROW = EntityDefinition.inherited(abstractArrowBase.factory(), abstractArrowBase)
                    .type(EntityType.SPECTRAL_ARROW)
                    .bedrockId(80)
                    .heightAndWidth(0.25f)
                    .identifier("minecraft:arrow")
                    .build();
            TRIDENT = EntityDefinition.inherited(TridentEntity::new, abstractArrowBase) // TODO remove class
                    .type(EntityType.TRIDENT)
                    .bedrockId(73)
                    .identifier("minecraft:thrown_trident")
                    .addTranslator(null) // Loyalty
                    .<Boolean>addTranslator(MetadataType.BOOLEAN, (tridentEntity, entityMetadata) -> tridentEntity.setFlag(EntityFlag.ENCHANTED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();

            // Item frames are handled differently as they are blocks, not items, in Bedrock
            ITEM_FRAME = EntityDefinition.inherited(new EntityFactory<ItemFrameEntity>() {
                    }, entityBase) // TODO
                    .type(EntityType.ITEM_FRAME)
                    .addTranslator(MetadataType.ITEM, ItemFrameEntity::setItemInFrame)
                    .addTranslator(MetadataType.ITEM, ItemFrameEntity::setItemRotation)
                    .build();
            GLOW_ITEM_FRAME = EntityDefinition.inherited(ITEM_FRAME.factory(), ITEM_FRAME)
                    .type(EntityType.GLOW_ITEM_FRAME)
                    .build();

            MINECART = EntityDefinition.inherited(MinecartEntity::new, entityBase)
                    .type(EntityType.MINECART)
                    .bedrockId(84)
                    .height(0.7f).width(0.98f)
                    .offset(0.35f)
                    .addTranslator(MetadataType.INT, (minecartEntity, entityMetadata) -> minecartEntity.getDirtyMetadata().put(EntityData.HEALTH, entityMetadata.getValue()))
                    .addTranslator(MetadataType.INT, (minecartEntity, entityMetadata) -> minecartEntity.getDirtyMetadata().put(EntityData.HURT_DIRECTION, entityMetadata.getValue())) // Direction in which the minecart is shaking
                    .<Float>addTranslator(MetadataType.FLOAT, (minecartEntity, entityMetadata) ->
                            // Power in Java, time in Bedrock
                            minecartEntity.getDirtyMetadata().put(EntityData.HURT_TIME, Math.min((int) ((FloatEntityMetadata) entityMetadata).getPrimitiveValue(), 15)))
                    .addTranslator(MetadataType.BLOCK_STATE, MinecartEntity::setCustomBlock)
                    .addTranslator(MetadataType.INT, MinecartEntity::setCustomBlockOffset)
                    .addTranslator(MetadataType.BOOLEAN, MinecartEntity::setShowCustomBlock)
                    .build();
            MINECART_CHEST = EntityDefinition.inherited(MINECART.factory(), MINECART)
                    .type(EntityType.MINECART_CHEST)
                    .identifier("minecraft:chest_minecart")
                    .build();
            MINECART_COMMAND_BLOCK = EntityDefinition.inherited(CommandBlockMinecartEntity::new, MINECART)
                    .type(EntityType.MINECART_COMMAND_BLOCK)
                    .bedrockId(100)
                    .identifier("minecraft:command_block_minecart")
                    .addTranslator(MetadataType.STRING, (entity, entityMetadata) -> entity.getDirtyMetadata().put(EntityData.COMMAND_BLOCK_COMMAND, entityMetadata.getValue()))
                    .<Component>addTranslator(MetadataType.CHAT, (entity, entityMetadata) -> entity.getDirtyMetadata().put(EntityData.COMMAND_BLOCK_LAST_OUTPUT, MessageTranslator.convertMessage(entityMetadata.getValue())))
                    .build();
            MINECART_FURNACE = EntityDefinition.inherited(FurnaceMinecartEntity::new, MINECART)
                    .type(EntityType.MINECART_FURNACE)
                    .identifier("minecraft:minecart")
                    .addTranslator(MetadataType.BOOLEAN, FurnaceMinecartEntity::setHasFuel)
                    .build();
            MINECART_HOPPER = EntityDefinition.inherited(MINECART.factory(), MINECART)
                    .type(EntityType.MINECART_HOPPER)
                    .identifier("minecraft:hopper_minecart")
                    .build();
            MINECART_SPAWNER = EntityDefinition.inherited(SpawnerMinecartEntity::new, MINECART)
                    .type(EntityType.MINECART_SPAWNER)
                    .identifier("minecraft:minecart")
                    .build();
            MINECART_TNT = EntityDefinition.inherited(MINECART.factory(), MINECART)
                    .type(EntityType.MINECART_TNT)
                    .identifier("minecraft:tnt_minecart")
                    .build();

            WITHER_SKULL = EntityDefinition.inherited(WitherSkullEntity::new, entityBase)
                    .type(EntityType.WITHER_SKULL)
                    .bedrockId(89)
                    .heightAndWidth(0.3125f)
                    .addTranslator(MetadataType.BOOLEAN, WitherSkullEntity::setDangerous)
                    .build();
            WITHER_SKULL_DANGEROUS = EntityDefinition.inherited(WITHER_SKULL.factory(), WITHER_SKULL)
                    .bedrockId(91)
                    .build(false);
        }

        EntityDefinition<LivingEntity> livingEntityBase = EntityDefinition.inherited(LivingEntity::new, entityBase)
                .addTranslator(MetadataType.BYTE, LivingEntity::setLivingEntityFlags)
                .addTranslator(MetadataType.FLOAT, LivingEntity::setHealth)
                .<Float>addTranslator(MetadataType.FLOAT,
                        (livingEntity, entityMetadata) -> livingEntity.getDirtyMetadata().put(EntityData.EFFECT_COLOR, entityMetadata.getValue()))
                .<Boolean>addTranslator(MetadataType.BOOLEAN,
                        (livingEntity, entityMetadata) -> livingEntity.getDirtyMetadata().put(EntityData.EFFECT_AMBIENT, (byte) (((BooleanEntityMetadata) entityMetadata).getPrimitiveValue() ? 1 : 0)))
                .addTranslator(null) // Arrow count
                .addTranslator(null) // Stinger count
                .addTranslator(MetadataType.POSITION, LivingEntity::setBedPosition)
                .build();

        ARMOR_STAND = EntityDefinition.inherited(ArmorStandEntity::new, livingEntityBase)
                .type(EntityType.ARMOR_STAND)
                .bedrockId(61)
                .height(1.975f).width(0.5f)
                .addTranslator(MetadataType.BYTE, ArmorStandEntity::setArmorStandFlags)
                .addTranslator(MetadataType.ROTATION, ArmorStandEntity::setHeadRotation)
                .addTranslator(MetadataType.ROTATION, ArmorStandEntity::setBodyRotation)
                .addTranslator(MetadataType.ROTATION, ArmorStandEntity::setLeftArmRotation)
                .addTranslator(MetadataType.ROTATION, ArmorStandEntity::setRightArmRotation)
                .addTranslator(MetadataType.ROTATION, ArmorStandEntity::setLeftLegRotation)
                .addTranslator(MetadataType.ROTATION, ArmorStandEntity::setRightLegRotation)
                .build();
        PLAYER = EntityDefinition.<PlayerEntity>inherited(null, livingEntityBase)
                .type(EntityType.PLAYER)
                .bedrockId(63)
                .height(1.8f).width(0.6f)
                .offset(1.62f)
                .addTranslator(MetadataType.FLOAT, PlayerEntity::setAbsorptionHearts)
                .addTranslator(null) // Player score
                .addTranslator(MetadataType.BYTE, PlayerEntity::setSkinVisibility)
                .addTranslator(null) // Player main hand
                .addTranslator(MetadataType.NBT_TAG, PlayerEntity::setLeftParrot)
                .addTranslator(MetadataType.NBT_TAG, PlayerEntity::setRightParrot)
                .build();

        EntityDefinition<MobEntity> mobEntityBase = EntityDefinition.inherited(MobEntity::new, livingEntityBase)
                .addTranslator(MetadataType.BYTE, MobEntity::setMobFlags)
                .build();

        // Extends mob
        {
            BAT = EntityDefinition.inherited(BatEntity::new, mobEntityBase)
                    .type(EntityType.BAT)
                    .bedrockId(19)
                    .height(0.9f).width(0.5f)
                    .addTranslator(MetadataType.BYTE, BatEntity::setBatFlags)
                    .build();
            BLAZE = EntityDefinition.inherited(BlazeEntity::new, mobEntityBase)
                    .type(EntityType.BLAZE)
                    .bedrockId(43)
                    .height(1.8f).width(0.6f)
                    .addTranslator(MetadataType.BYTE, BlazeEntity::setBlazeFlags)
                    .build();
            CAVE_SPIDER = EntityDefinition.inherited(MonsterEntity::new, mobEntityBase)
                    .type(EntityType.CAVE_SPIDER)
                    .bedrockId(40)
                    .height(0.5f).width(0.7f)
                    .build();
            CREEPER = EntityDefinition.inherited(CreeperEntity::new, mobEntityBase)
                    .type(EntityType.CREEPER)
                    .bedrockId(33)
                    .height(1.7f).width(0.6f)
                    .offset(1.62f)
                    .addTranslator(MetadataType.INT, CreeperEntity::setSwelling)
                    .<Boolean>addTranslator(MetadataType.BOOLEAN, (entity, entityMetadata) -> entity.setFlag(EntityFlag.POWERED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .addTranslator(MetadataType.BOOLEAN, CreeperEntity::setIgnited)
                    .build();
            DOLPHIN = EntityDefinition.inherited(WaterEntity::new, mobEntityBase)
                    .type(EntityType.DOLPHIN)
                    .bedrockId(31)
                    .height(0.6f).width(0.9f)
                    //TODO check
                    .addTranslator(null) // treasure position
                    .addTranslator(null) // "got fish"
                    .addTranslator(null) // "moistness level"
                    .build();
            ENDERMAN = EntityDefinition.inherited(EndermanEntity::new, mobEntityBase)
                    .type(EntityType.ENDERMAN)
                    .bedrockId(38)
                    .height(2.9f).width(0.6f)
                    .addTranslator(MetadataType.BLOCK_STATE, EndermanEntity::setCarriedBlock)
                    .addTranslator(MetadataType.BOOLEAN, EndermanEntity::setScreaming)
                    .addTranslator(MetadataType.BOOLEAN, EndermanEntity::setAngry)
                    .build();
            ENDERMITE = EntityDefinition.inherited(MonsterEntity::new, mobEntityBase)
                    .type(EntityType.ENDERMITE)
                    .bedrockId(55)
                    .height(0.3f).width(0.4f)
                    .build();
            ENDER_DRAGON = EntityDefinition.inherited(EnderDragonEntity::new, mobEntityBase)
                    .type(EntityType.ENDER_DRAGON)
                    .bedrockId(53)
                    .addTranslator(MetadataType.INT, EnderDragonEntity::setPhase)
                    .build();
            GHAST = EntityDefinition.inherited(GhastEntity::new, mobEntityBase)
                    .type(EntityType.GHAST)
                    .bedrockId(41)
                    .heightAndWidth(4.0f)
                    .addTranslator(MetadataType.BOOLEAN, GhastEntity::setGhastAttacking)
                    .build();
            GIANT = EntityDefinition.inherited(GiantEntity::new, mobEntityBase)
                    .type(EntityType.GIANT)
                    .bedrockId(32)
                    .height(1.8f).width(1.6f)
                    .offset(1.62f)
                    .identifier("minecraft:zombie")
                    .build();
            IRON_GOLEM = EntityDefinition.inherited(IronGolemEntity::new, mobEntityBase)
                    .type(EntityType.IRON_GOLEM)
                    .bedrockId(20)
                    .height(2.7f).width(1.4f)
                    .build();
            PHANTOM = EntityDefinition.inherited(PhantomEntity::new, mobEntityBase)
                    .type(EntityType.PHANTOM)
                    .bedrockId(58)
                    .height(0.5f).width(0.9f)
                    .offset(0.6f)
                    .addTranslator(MetadataType.INT, PhantomEntity::setPhantomScale)
                    .build();
            SILVERFISH = EntityDefinition.inherited(MonsterEntity::new, mobEntityBase)
                    .type(EntityType.SILVERFISH)
                    .bedrockId(39)
                    .height(0.3f).width(0.4f)
                    .build();
            SHULKER = EntityDefinition.inherited(ShulkerEntity::new, mobEntityBase)
                    .type(EntityType.SHULKER)
                    .bedrockId(54)
                    .heightAndWidth(1f)
                    .addTranslator(MetadataType.DIRECTION, ShulkerEntity::setAttachedFace)
                    .addTranslator(MetadataType.BYTE, ShulkerEntity::setShulkerHeight)
                    .addTranslator(MetadataType.BYTE, ShulkerEntity::setShulkerColor)
                    .build();
            SKELETON = EntityDefinition.inherited(SkeletonEntity::new, mobEntityBase)
                    .type(EntityType.SKELETON)
                    .bedrockId(34)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .addTranslator(MetadataType.BOOLEAN, SkeletonEntity::setConvertingToStray)
                    .build();
            SNOW_GOLEM = EntityDefinition.inherited(SnowGolemEntity::new, mobEntityBase)
                    .type(EntityType.SNOW_GOLEM)
                    .bedrockId(21)
                    .height(1.9f).width(0.7f)
                    .addTranslator(MetadataType.BYTE, SnowGolemEntity::setSnowGolemFlags)
                    .build();
            SPIDER = EntityDefinition.inherited(SpiderEntity::new, mobEntityBase)
                    .type(EntityType.SPIDER)
                    .bedrockId(35)
                    .height(0.9f).width(1.4f)
                    .offset(1f)
                    .addTranslator(MetadataType.BYTE, SpiderEntity::setSpiderFlags)
                    .build();
            SQUID = EntityDefinition.inherited(SquidEntity::new, mobEntityBase)
                    .type(EntityType.SQUID)
                    .bedrockId(17)
                    .heightAndWidth(0.8f)
                    .build();
            STRAY = EntityDefinition.inherited(AbstractSkeletonEntity::new, mobEntityBase)
                    .type(EntityType.STRAY)
                    .bedrockId(46)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .build();
            VEX = EntityDefinition.inherited(VexEntity::new, mobEntityBase)
                    .type(EntityType.VEX)
                    .bedrockId(105)
                    .height(0.8f).width(0.4f)
                    .addTranslator(MetadataType.BYTE, VexEntity::setVexFlags)
                    .build();
            WITHER = EntityDefinition.inherited(WitherEntity::new, mobEntityBase)
                    .type(EntityType.WITHER)
                    .bedrockId(52)
                    .height(3.5f).width(0.9f)
                    .addTranslator(MetadataType.INT, WitherEntity::setTarget1)
                    .addTranslator(MetadataType.INT, WitherEntity::setTarget2)
                    .addTranslator(MetadataType.INT, WitherEntity::setTarget3)
                    .addTranslator(MetadataType.INT, WitherEntity::setInvulnerableTicks)
                    .build();
            WITHER_SKELETON = EntityDefinition.inherited(AbstractSkeletonEntity::new, mobEntityBase)
                    .type(EntityType.WITHER_SKELETON)
                    .bedrockId(48)
                    .height(2.4f).width(0.7f)
                    .build();
            ZOGLIN = EntityDefinition.inherited(ZoglinEntity::new, mobEntityBase)
                    .type(EntityType.ZOGLIN)
                    .bedrockId(126)
                    .height(1.4f).width(1.3965f)
                    .addTranslator(MetadataType.BOOLEAN, ZoglinEntity::setBaby)
                    .build();
            ZOMBIE = EntityDefinition.inherited(ZombieEntity::new, mobEntityBase)
                    .type(EntityType.ZOMBIE)
                    .bedrockId(32)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .addTranslator(MetadataType.BOOLEAN, ZombieEntity::setZombieBaby)
                    .addTranslator(null) // "set special type", doesn't do anything
                    .addTranslator(MetadataType.BOOLEAN, ZombieEntity::setConvertingToDrowned)
                    .build();
            ZOMBIE_VILLAGER = EntityDefinition.inherited(ZombieVillagerEntity::new, ZOMBIE)
                    .type(EntityType.ZOMBIE_VILLAGER)
                    .bedrockId(44)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .identifier("minecraft:zombie_villager_v2")
                    .addTranslator(MetadataType.BOOLEAN, ZombieVillagerEntity::setTransforming)
                    .addTranslator(MetadataType.VILLAGER_DATA, ZombieVillagerEntity::setZombieVillagerData)
                    .build();
            ZOMBIFIED_PIGLIN = EntityDefinition.inherited(ZombifiedPiglinEntity::new, ZOMBIE) //TODO test how zombie entity metadata is handled?
                    .type(EntityType.ZOMBIFIED_PIGLIN)
                    .bedrockId(36)
                    .height(1.95f).width(0.6f)
                    .offset(1.62f)
                    .identifier("minecraft:zombie_pigman")
                    .build();

            DROWNED = EntityDefinition.inherited(ZOMBIE.factory(), ZOMBIE)
                    .type(EntityType.DROWNED)
                    .bedrockId(110)
                    .height(1.95f).width(0.6f)
                    .build();
            HUSK = EntityDefinition.inherited(ZOMBIE.factory(), ZOMBIE)
                    .type(EntityType.HUSK)
                    .bedrockId(47)
                    .build();

            GUARDIAN = EntityDefinition.inherited(GuardianEntity::new, mobEntityBase)
                    .type(EntityType.GUARDIAN)
                    .bedrockId(49)
                    .heightAndWidth(0.85f)
                    .addTranslator(null) // Moving //TODO
                    .addTranslator(MetadataType.INT, GuardianEntity::setGuardianTarget)
                    .build();
            ELDER_GUARDIAN = EntityDefinition.inherited(ElderGuardianEntity::new, GUARDIAN)
                    .type(EntityType.ELDER_GUARDIAN)
                    .bedrockId(50)
                    .heightAndWidth(1.9975f)
                    .build();

            SLIME = EntityDefinition.inherited(SlimeEntity::new, mobEntityBase)
                    .type(EntityType.SLIME)
                    .bedrockId(37)
                    .heightAndWidth(0.51f)
                    .addTranslator(MetadataType.INT, SlimeEntity::setScale)
                    .build();
            MAGMA_CUBE = EntityDefinition.inherited(MagmaCubeEntity::new, SLIME)
                    .type(EntityType.MAGMA_CUBE)
                    .bedrockId(42)
                    .build();

            EntityDefinition<AbstractFishEntity> abstractFishEntityBase = EntityDefinition.inherited(AbstractFishEntity::new, mobEntityBase)
                    .addTranslator(null) // From bucket
                    .build();
            COD = EntityDefinition.inherited(abstractFishEntityBase.factory(), abstractFishEntityBase)
                    .type(EntityType.COD)
                    .bedrockId(112)
                    .height(0.25f).width(0.5f)
                    .build();
            PUFFERFISH = EntityDefinition.inherited(PufferFishEntity::new, abstractFishEntityBase)
                    .type(EntityType.PUFFERFISH)
                    .bedrockId(108)
                    .heightAndWidth(0.7f)
                    .addTranslator(MetadataType.INT, PufferFishEntity::setPufferfishSize)
                    .build();
            SALMON = EntityDefinition.inherited(abstractFishEntityBase.factory(), abstractFishEntityBase)
                    .type(EntityType.SALMON)
                    .bedrockId(109)
                    .height(0.5f).width(0.7f)
                    .build();
            TROPICAL_FISH = EntityDefinition.inherited(TropicalFishEntity::new, abstractFishEntityBase)
                    .type(EntityType.TROPICAL_FISH)
                    .bedrockId(111)
                    .heightAndWidth(0.6f)
                    .identifier("minecraft:tropicalfish")
                    .build();

            EntityDefinition<BasePiglinEntity> abstractPiglinEntityBase = EntityDefinition.inherited(BasePiglinEntity::new, mobEntityBase)
                    .addTranslator(MetadataType.BOOLEAN, BasePiglinEntity::setImmuneToZombification)
                    .build();
            PIGLIN = EntityDefinition.inherited(PiglinEntity::new, abstractPiglinEntityBase)
                    .type(EntityType.PIGLIN)
                    .bedrockId(123)
                    .height(1.95f).width(0.6f)
                    .addTranslator(MetadataType.BOOLEAN, PiglinEntity::setBaby)
                    .addTranslator(MetadataType.BOOLEAN, PiglinEntity::setChargingCrossbow)
                    .addTranslator(MetadataType.BOOLEAN, PiglinEntity::setDancing)
                    .build();
            PIGLIN_BRUTE = EntityDefinition.inherited(abstractPiglinEntityBase.factory(), abstractPiglinEntityBase)
                    .type(EntityType.PIGLIN_BRUTE)
                    .bedrockId(127)
                    .height(1.95f).width(0.6f)
                    .build();

            GLOW_SQUID = EntityDefinition.inherited(GlowSquidEntity::new, SQUID)
                    .type(EntityType.GLOW_SQUID)
                    .addTranslator(null) // Set dark ticks remaining, possible TODO
                    .build();

            EntityDefinition<RaidParticipantEntity> raidParticipantEntityBase = EntityDefinition.inherited(RaidParticipantEntity::new, mobEntityBase)
                    .addTranslator(null) // Celebrating //TODO
                    .build();
            EntityDefinition<SpellcasterIllagerEntity> spellcasterEntityBase = EntityDefinition.inherited(SpellcasterIllagerEntity::new, raidParticipantEntityBase)
                    .addTranslator(MetadataType.BYTE, SpellcasterIllagerEntity::setSpellType)
                    .build();
            EVOKER = EntityDefinition.inherited(spellcasterEntityBase.factory(), spellcasterEntityBase)
                    .type(EntityType.EVOKER)
                    .bedrockId(104)
                    .height(1.95f).width(0.6f)
                    .identifier("minecraft:evocation_illager")
                    .build();
            ILLUSIONER = EntityDefinition.inherited(spellcasterEntityBase.factory(), spellcasterEntityBase)
                    .type(EntityType.ILLUSIONER)
                    .bedrockId(104)
                    .height(1.95f).width(0.6f)
                    .identifier("minecraft:evocation_illager")
                    .build();
            PILLAGER = EntityDefinition.inherited(PillagerEntity::new, raidParticipantEntityBase)
                    .type(EntityType.PILLAGER)
                    .bedrockId(114)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .addTranslator(null) // Charging; doesn't have an equivalent on Bedrock //TODO check
                    .build();
            RAVAGER = EntityDefinition.inherited(raidParticipantEntityBase.factory(), raidParticipantEntityBase)
                    .type(EntityType.RAVAGER)
                    .bedrockId(59)
                    .height(1.9f).width(1.2f)
                    .build();
            VINDICATOR = EntityDefinition.inherited(VindicatorEntity::new, raidParticipantEntityBase)
                    .type(EntityType.VINDICATOR)
                    .bedrockId(57)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .build();
            WITCH = EntityDefinition.inherited(raidParticipantEntityBase.factory(), raidParticipantEntityBase)
                    .type(EntityType.WITCH)
                    .bedrockId(45)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .build();
        }

        EntityDefinition<AgeableEntity> ageableEntityBase = EntityDefinition.inherited(AgeableEntity::new, mobEntityBase)
                .addTranslator(MetadataType.BOOLEAN, AgeableEntity::setBaby)
                .build();

        // Extends ageable
        {
            AXOLOTL = EntityDefinition.inherited(AxolotlEntity::new, ageableEntityBase)
                    .type(EntityType.AXOLOTL)
                    .height(0.42f).width(0.7f)
                    .addTranslator(MetadataType.INT, AxolotlEntity::setVariant)
                    .addTranslator(MetadataType.BOOLEAN, AxolotlEntity::setPlayingDead)
                    .build();
            BEE = EntityDefinition.inherited(BeeEntity::new, ageableEntityBase)
                    .type(EntityType.BEE)
                    .bedrockId(122)
                    .heightAndWidth(0.6f)
                    .addTranslator(MetadataType.BYTE, BeeEntity::setBeeFlags)
                    .addTranslator(MetadataType.INT, BeeEntity::setAngerTime)
                    .build();
            CHICKEN = EntityDefinition.inherited(ChickenEntity::new, ageableEntityBase)
                    .type(EntityType.CHICKEN)
                    .bedrockId(10)
                    .height(0.7f).width(0.4f)
                    .build();
            COW = EntityDefinition.inherited(AnimalEntity::new, ageableEntityBase)
                    .type(EntityType.COW)
                    .bedrockId(11)
                    .height(1.4f).width(0.9f)
                    .build();
            FOX = EntityDefinition.inherited(FoxEntity::new, ageableEntityBase)
                    .type(EntityType.FOX)
                    .bedrockId(121)
                    .height(0.5f).width(1.25f)
                    .addTranslator(MetadataType.INT, FoxEntity::setFoxVariant)
                    .addTranslator(MetadataType.BYTE, FoxEntity::setFoxFlags)
                    .build();
            HOGLIN = EntityDefinition.inherited(HoglinEntity::new, ageableEntityBase)
                    .type(EntityType.HOGLIN)
                    .bedrockId(124)
                    .height(1.4f).width(1.3965f)
                    .addTranslator(MetadataType.BOOLEAN, HoglinEntity::setImmuneToZombification)
                    .build();
            GOAT = EntityDefinition.inherited(GoatEntity::new, ageableEntityBase)
                    .type(EntityType.GOAT)
                    .height(1.3f).width(0.9f)
                    .addTranslator(MetadataType.BOOLEAN, GoatEntity::setScreamer)
                    .build();
            MOOSHROOM = EntityDefinition.inherited(MooshroomEntity::new, ageableEntityBase) // TODO remove class
                    .type(EntityType.MOOSHROOM)
                    .bedrockId(16)
                    .height(1.4f).width(0.9f)
                    .<String>addTranslator(MetadataType.STRING, (entity, entityMetadata) -> entity.getDirtyMetadata().put(EntityData.VARIANT, entityMetadata.getValue().equals("brown") ? 1 : 0))
                    .build();
            OCELOT = EntityDefinition.inherited(OcelotEntity::new, ageableEntityBase)
                    .type(EntityType.OCELOT)
                    .bedrockId(22)
                    .height(0.35f).width(0.3f)
                    .<Boolean>addTranslator(MetadataType.BOOLEAN, (ocelotEntity, entityMetadata) -> ocelotEntity.setFlag(EntityFlag.TRUSTING, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();
            PANDA = EntityDefinition.inherited(PandaEntity::new, ageableEntityBase)
                    .type(EntityType.PANDA)
                    .bedrockId(113)
                    .height(1.25f).width(1.125f)
                    .addTranslator(null) // Unhappy counter
                    .addTranslator(null) // Sneeze counter
                    .addTranslator(MetadataType.INT, PandaEntity::setEatingCounter)
                    .addTranslator(MetadataType.BYTE, PandaEntity::setMainGene)
                    .addTranslator(MetadataType.BYTE, PandaEntity::setHiddenGene)
                    .addTranslator(MetadataType.BYTE, PandaEntity::setPandaFlags)
                    .build();
            PIG = EntityDefinition.inherited(PigEntity::new, ageableEntityBase)
                    .type(EntityType.PIG)
                    .bedrockId(12)
                    .heightAndWidth(0.9f)
                    .<Boolean>addTranslator(MetadataType.BOOLEAN, (pigEntity, entityMetadata) -> pigEntity.setFlag(EntityFlag.SADDLED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .addTranslator(null) // Boost time
                    .build();
            POLAR_BEAR = EntityDefinition.inherited(PolarBearEntity::new, ageableEntityBase)
                    .type(EntityType.POLAR_BEAR)
                    .bedrockId(28)
                    .height(1.4f).width(1.3f)
                    .<Boolean>addTranslator(MetadataType.BOOLEAN, (entity, entityMetadata) -> entity.setFlag(EntityFlag.STANDING, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();
            RABBIT = EntityDefinition.inherited(RabbitEntity::new, ageableEntityBase)
                    .type(EntityType.RABBIT)
                    .bedrockId(18)
                    .height(0.5f).width(0.4f)
                    .addTranslator(MetadataType.INT, RabbitEntity::setRabbitVariant)
                    .build();
            SHEEP = EntityDefinition.inherited(SheepEntity::new, ageableEntityBase)
                    .type(EntityType.SHEEP)
                    .bedrockId(13)
                    .heightAndWidth(0.9f)
                    .addTranslator(MetadataType.BYTE, SheepEntity::setSheepFlags)
                    .build();
            STRIDER = EntityDefinition.inherited(StriderEntity::new, ageableEntityBase)
                    .type(EntityType.STRIDER)
                    .bedrockId(125)
                    .height(1.7f).width(0.9f)
                    .addTranslator(null) // Boost time
                    .addTranslator(MetadataType.BOOLEAN, StriderEntity::setCold)
                    .addTranslator(MetadataType.BOOLEAN, StriderEntity::setSaddled)
                    .build();
            TURTLE = EntityDefinition.inherited(TurtleEntity::new, ageableEntityBase)
                    .type(EntityType.TURTLE)
                    .bedrockId(74)
                    .height(0.4f).width(1.2f)
                    .addTranslator(MetadataType.BOOLEAN, TurtleEntity::setPregnant)
                    .addTranslator(MetadataType.BOOLEAN, TurtleEntity::setLayingEgg)
                    .build();

            EntityDefinition<AbstractMerchantEntity> abstractVillagerEntityBase = EntityDefinition.inherited(AbstractMerchantEntity::new, ageableEntityBase)
                    .addTranslator(null) // Unhappy ticks
                    .build();
            VILLAGER = EntityDefinition.inherited(VillagerEntity::new, abstractVillagerEntityBase)
                    .type(EntityType.VILLAGER)
                    .bedrockId(15)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .identifier("minecraft:villager_v2")
                    .addTranslator(MetadataType.VILLAGER_DATA, VillagerEntity::setVillagerData)
                    .build();
            WANDERING_TRADER = EntityDefinition.inherited(abstractVillagerEntityBase.factory(), abstractVillagerEntityBase)
                    .type(EntityType.WANDERING_TRADER)
                    .bedrockId(118)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .build();
        }

        // Horses
        {
            EntityDefinition<AbstractHorseEntity> abstractHorseEntityBase = EntityDefinition.inherited(AbstractHorseEntity::new, ageableEntityBase)
                    .addTranslator(MetadataType.BYTE, AbstractHorseEntity::setHorseFlags)
                    .addTranslator(null) // UUID of owner
                    .build();
            HORSE = EntityDefinition.inherited(HorseEntity::new, abstractHorseEntityBase)
                    .type(EntityType.HORSE)
                    .bedrockId(23)
                    .height(1.6f).width(1.3965f)
                    .addTranslator(MetadataType.BYTE, HorseEntity::setHorseVariant)
                    .build();
            SKELETON_HORSE = EntityDefinition.inherited(abstractHorseEntityBase.factory(), abstractHorseEntityBase)
                    .type(EntityType.SKELETON_HORSE)
                    .bedrockId(26)
                    .height(1.6f).width(1.3965f)
                    .build();
            ZOMBIE_HORSE = EntityDefinition.inherited(abstractHorseEntityBase.factory(), abstractHorseEntityBase)
                    .type(EntityType.ZOMBIE_HORSE)
                    .bedrockId(27)
                    .height(1.6f).width(1.3965f)
                    .build();
            EntityDefinition<ChestedHorseEntity> chestedHorseEntityBase = EntityDefinition.inherited(ChestedHorseEntity::new, abstractHorseEntityBase)
                    .<Boolean>addTranslator(MetadataType.BOOLEAN, (horseEntity, entityMetadata) -> horseEntity.setFlag(EntityFlag.CHESTED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();
            DONKEY = EntityDefinition.inherited(chestedHorseEntityBase.factory(), chestedHorseEntityBase)
                    .type(EntityType.DONKEY)
                    .bedrockId(24)
                    .height(1.6f).width(1.3965f)
                    .build();
            MULE = EntityDefinition.inherited(chestedHorseEntityBase.factory(), chestedHorseEntityBase)
                    .type(EntityType.MULE)
                    .bedrockId(25)
                    .height(1.6f).width(1.3965f)
                    .build();
            LLAMA = EntityDefinition.inherited(LlamaEntity::new, chestedHorseEntityBase)
                    .type(EntityType.LLAMA)
                    .bedrockId(29)
                    .height(1.87f).width(0.9f)
                    .<Integer>addTranslator(MetadataType.INT, (entity, entityMetadata) -> entity.getDirtyMetadata().put(EntityData.STRENGTH, entityMetadata.getValue()))
                    .addTranslator(MetadataType.INT, LlamaEntity::setCarpetedColor)
                    .addTranslator(MetadataType.INT, (entity, entityMetadata) -> entity.getDirtyMetadata().put(EntityData.VARIANT, entityMetadata.getValue()))
                    .build();
            TRADER_LLAMA = EntityDefinition.inherited(TraderLlamaEntity::new, LLAMA)
                    .type(EntityType.TRADER_LLAMA)
                    .identifier("minecraft:llama")
                    .build();
        }

        EntityDefinition<TameableEntity> tameableEntityBase = EntityDefinition.inherited(TameableEntity::new, ageableEntityBase)
                .addTranslator(MetadataType.BYTE, TameableEntity::setTameableFlags)
                .addTranslator(MetadataType.OPTIONAL_UUID, TameableEntity::setOwner)
                .build();
        CAT = EntityDefinition.inherited(CatEntity::new, tameableEntityBase)
                .type(EntityType.CAT)
                .bedrockId(75)
                .height(0.35f).width(0.3f)
                .addTranslator(MetadataType.INT, CatEntity::setCatVariant)
                .addTranslator(MetadataType.BOOLEAN, CatEntity::setResting)
                .addTranslator(null) // "resting state one" //TODO
                .addTranslator(MetadataType.INT, CatEntity::setCollarColor)
                .build();
        PARROT = EntityDefinition.inherited(ParrotEntity::new, tameableEntityBase)
                .type(EntityType.PARROT)
                .bedrockId(30)
                .height(0.9f).width(0.5f)
                .addTranslator(MetadataType.INT, (parrotEntity, entityMetadata) -> parrotEntity.getDirtyMetadata().put(EntityData.VARIANT, entityMetadata.getValue())) // Parrot color
                .build();
        WOLF = EntityDefinition.inherited(WolfEntity::new, tameableEntityBase)
                .type(EntityType.WOLF)
                .bedrockId(14)
                .height(0.85f).width(0.6f)
                // "Begging" on wiki.vg, "Interested" in Nukkit - the tilt of the head
                .<Boolean>addTranslator(MetadataType.BOOLEAN, (wolfEntity, entityMetadata) -> wolfEntity.setFlag(EntityFlag.INTERESTED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                .addTranslator(MetadataType.INT, WolfEntity::setCollarColor)
                .addTranslator(MetadataType.INT, WolfEntity::setWolfAngerTime)
                .build();

        // As of 1.18 these don't track entity data at all
        ENDER_DRAGON_PART = EntityDefinition.<EnderDragonPartEntity>builder(null)
                .bedrockId(32)
                .identifier("minecraft:armor_stand") // Emulated
                .build();
    }

    public static void init() {
        // no-op
    }

    private EntityDefinitions() {
    }
}
