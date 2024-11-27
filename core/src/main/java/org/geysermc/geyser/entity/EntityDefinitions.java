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

package org.geysermc.geyser.entity;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.factory.EntityFactory;
import org.geysermc.geyser.entity.properties.GeyserEntityProperties;
import org.geysermc.geyser.entity.type.AbstractArrowEntity;
import org.geysermc.geyser.entity.type.AbstractWindChargeEntity;
import org.geysermc.geyser.entity.type.AreaEffectCloudEntity;
import org.geysermc.geyser.entity.type.ArrowEntity;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.ChestBoatEntity;
import org.geysermc.geyser.entity.type.CommandBlockMinecartEntity;
import org.geysermc.geyser.entity.type.DisplayBaseEntity;
import org.geysermc.geyser.entity.type.EnderCrystalEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.EvokerFangsEntity;
import org.geysermc.geyser.entity.type.ExpOrbEntity;
import org.geysermc.geyser.entity.type.FallingBlockEntity;
import org.geysermc.geyser.entity.type.FireballEntity;
import org.geysermc.geyser.entity.type.FireworkEntity;
import org.geysermc.geyser.entity.type.FishingHookEntity;
import org.geysermc.geyser.entity.type.FurnaceMinecartEntity;
import org.geysermc.geyser.entity.type.InteractionEntity;
import org.geysermc.geyser.entity.type.ItemEntity;
import org.geysermc.geyser.entity.type.ItemFrameEntity;
import org.geysermc.geyser.entity.type.LeashKnotEntity;
import org.geysermc.geyser.entity.type.LightningEntity;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.entity.type.MinecartEntity;
import org.geysermc.geyser.entity.type.PaintingEntity;
import org.geysermc.geyser.entity.type.SpawnerMinecartEntity;
import org.geysermc.geyser.entity.type.TNTEntity;
import org.geysermc.geyser.entity.type.TextDisplayEntity;
import org.geysermc.geyser.entity.type.ThrowableEntity;
import org.geysermc.geyser.entity.type.ThrowableItemEntity;
import org.geysermc.geyser.entity.type.ThrownPotionEntity;
import org.geysermc.geyser.entity.type.TridentEntity;
import org.geysermc.geyser.entity.type.WitherSkullEntity;
import org.geysermc.geyser.entity.type.living.AbstractFishEntity;
import org.geysermc.geyser.entity.type.living.AgeableEntity;
import org.geysermc.geyser.entity.type.living.AllayEntity;
import org.geysermc.geyser.entity.type.living.ArmorStandEntity;
import org.geysermc.geyser.entity.type.living.BatEntity;
import org.geysermc.geyser.entity.type.living.DolphinEntity;
import org.geysermc.geyser.entity.type.living.GlowSquidEntity;
import org.geysermc.geyser.entity.type.living.IronGolemEntity;
import org.geysermc.geyser.entity.type.living.MagmaCubeEntity;
import org.geysermc.geyser.entity.type.living.MobEntity;
import org.geysermc.geyser.entity.type.living.SlimeEntity;
import org.geysermc.geyser.entity.type.living.SnowGolemEntity;
import org.geysermc.geyser.entity.type.living.SquidEntity;
import org.geysermc.geyser.entity.type.living.TadpoleEntity;
import org.geysermc.geyser.entity.type.living.animal.ArmadilloEntity;
import org.geysermc.geyser.entity.type.living.animal.AxolotlEntity;
import org.geysermc.geyser.entity.type.living.animal.BeeEntity;
import org.geysermc.geyser.entity.type.living.animal.ChickenEntity;
import org.geysermc.geyser.entity.type.living.animal.CowEntity;
import org.geysermc.geyser.entity.type.living.animal.FoxEntity;
import org.geysermc.geyser.entity.type.living.animal.FrogEntity;
import org.geysermc.geyser.entity.type.living.animal.GoatEntity;
import org.geysermc.geyser.entity.type.living.animal.HoglinEntity;
import org.geysermc.geyser.entity.type.living.animal.MooshroomEntity;
import org.geysermc.geyser.entity.type.living.animal.OcelotEntity;
import org.geysermc.geyser.entity.type.living.animal.PandaEntity;
import org.geysermc.geyser.entity.type.living.animal.PigEntity;
import org.geysermc.geyser.entity.type.living.animal.PolarBearEntity;
import org.geysermc.geyser.entity.type.living.animal.PufferFishEntity;
import org.geysermc.geyser.entity.type.living.animal.RabbitEntity;
import org.geysermc.geyser.entity.type.living.animal.SheepEntity;
import org.geysermc.geyser.entity.type.living.animal.SnifferEntity;
import org.geysermc.geyser.entity.type.living.animal.StriderEntity;
import org.geysermc.geyser.entity.type.living.animal.TropicalFishEntity;
import org.geysermc.geyser.entity.type.living.animal.TurtleEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.AbstractHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.CamelEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.ChestedHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.HorseEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.LlamaEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.SkeletonHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.TraderLlamaEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.ZombieHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.tameable.CatEntity;
import org.geysermc.geyser.entity.type.living.animal.tameable.ParrotEntity;
import org.geysermc.geyser.entity.type.living.animal.tameable.TameableEntity;
import org.geysermc.geyser.entity.type.living.animal.tameable.WolfEntity;
import org.geysermc.geyser.entity.type.living.merchant.AbstractMerchantEntity;
import org.geysermc.geyser.entity.type.living.merchant.VillagerEntity;
import org.geysermc.geyser.entity.type.living.monster.AbstractSkeletonEntity;
import org.geysermc.geyser.entity.type.living.monster.BasePiglinEntity;
import org.geysermc.geyser.entity.type.living.monster.BlazeEntity;
import org.geysermc.geyser.entity.type.living.monster.BoggedEntity;
import org.geysermc.geyser.entity.type.living.monster.BreezeEntity;
import org.geysermc.geyser.entity.type.living.monster.CreeperEntity;
import org.geysermc.geyser.entity.type.living.monster.ElderGuardianEntity;
import org.geysermc.geyser.entity.type.living.monster.EnderDragonEntity;
import org.geysermc.geyser.entity.type.living.monster.EnderDragonPartEntity;
import org.geysermc.geyser.entity.type.living.monster.EndermanEntity;
import org.geysermc.geyser.entity.type.living.monster.GhastEntity;
import org.geysermc.geyser.entity.type.living.monster.GiantEntity;
import org.geysermc.geyser.entity.type.living.monster.GuardianEntity;
import org.geysermc.geyser.entity.type.living.monster.MonsterEntity;
import org.geysermc.geyser.entity.type.living.monster.PhantomEntity;
import org.geysermc.geyser.entity.type.living.monster.PiglinEntity;
import org.geysermc.geyser.entity.type.living.monster.ShulkerEntity;
import org.geysermc.geyser.entity.type.living.monster.SkeletonEntity;
import org.geysermc.geyser.entity.type.living.monster.SpiderEntity;
import org.geysermc.geyser.entity.type.living.monster.VexEntity;
import org.geysermc.geyser.entity.type.living.monster.WardenEntity;
import org.geysermc.geyser.entity.type.living.monster.WitherEntity;
import org.geysermc.geyser.entity.type.living.monster.ZoglinEntity;
import org.geysermc.geyser.entity.type.living.monster.ZombieEntity;
import org.geysermc.geyser.entity.type.living.monster.ZombieVillagerEntity;
import org.geysermc.geyser.entity.type.living.monster.ZombifiedPiglinEntity;
import org.geysermc.geyser.entity.type.living.monster.raid.PillagerEntity;
import org.geysermc.geyser.entity.type.living.monster.raid.RaidParticipantEntity;
import org.geysermc.geyser.entity.type.living.monster.raid.RavagerEntity;
import org.geysermc.geyser.entity.type.living.monster.raid.SpellcasterIllagerEntity;
import org.geysermc.geyser.entity.type.living.monster.raid.VindicatorEntity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

public final class EntityDefinitions {
    public static final EntityDefinition<BoatEntity> ACACIA_BOAT;
    public static final EntityDefinition<ChestBoatEntity> ACACIA_CHEST_BOAT;
    public static final EntityDefinition<AllayEntity> ALLAY;
    public static final EntityDefinition<AreaEffectCloudEntity> AREA_EFFECT_CLOUD;
    public static final EntityDefinition<ArmadilloEntity> ARMADILLO;
    public static final EntityDefinition<ArmorStandEntity> ARMOR_STAND;
    public static final EntityDefinition<ArrowEntity> ARROW;
    public static final EntityDefinition<AxolotlEntity> AXOLOTL;
    public static final EntityDefinition<BoatEntity> BAMBOO_RAFT;
    public static final EntityDefinition<ChestBoatEntity> BAMBOO_CHEST_RAFT;
    public static final EntityDefinition<BatEntity> BAT;
    public static final EntityDefinition<BeeEntity> BEE;
    public static final EntityDefinition<BoatEntity> BIRCH_BOAT;
    public static final EntityDefinition<ChestBoatEntity> BIRCH_CHEST_BOAT;
    public static final EntityDefinition<BlazeEntity> BLAZE;
    public static final EntityDefinition<BoggedEntity> BOGGED;
    public static final EntityDefinition<BreezeEntity> BREEZE;
    public static final EntityDefinition<AbstractWindChargeEntity> BREEZE_WIND_CHARGE;
    public static final EntityDefinition<CamelEntity> CAMEL;
    public static final EntityDefinition<CatEntity> CAT;
    public static final EntityDefinition<SpiderEntity> CAVE_SPIDER;
    public static final EntityDefinition<BoatEntity> CHERRY_BOAT;
    public static final EntityDefinition<ChestBoatEntity> CHERRY_CHEST_BOAT;
    public static final EntityDefinition<MinecartEntity> CHEST_MINECART;
    public static final EntityDefinition<ChickenEntity> CHICKEN;
    public static final EntityDefinition<AbstractFishEntity> COD;
    public static final EntityDefinition<CommandBlockMinecartEntity> COMMAND_BLOCK_MINECART;
    public static final EntityDefinition<CowEntity> COW;
    public static final EntityDefinition<CreeperEntity> CREEPER;
    public static final EntityDefinition<BoatEntity> DARK_OAK_BOAT;
    public static final EntityDefinition<ChestBoatEntity> DARK_OAK_CHEST_BOAT;
    public static final EntityDefinition<DolphinEntity> DOLPHIN;
    public static final EntityDefinition<ChestedHorseEntity> DONKEY;
    public static final EntityDefinition<FireballEntity> DRAGON_FIREBALL;
    public static final EntityDefinition<ZombieEntity> DROWNED;
    public static final EntityDefinition<ThrowableItemEntity> EGG;
    public static final EntityDefinition<ElderGuardianEntity> ELDER_GUARDIAN;
    public static final EntityDefinition<EndermanEntity> ENDERMAN;
    public static final EntityDefinition<MonsterEntity> ENDERMITE;
    public static final EntityDefinition<EnderDragonEntity> ENDER_DRAGON;
    public static final EntityDefinition<ThrowableItemEntity> ENDER_PEARL;
    public static final EntityDefinition<EnderCrystalEntity> END_CRYSTAL;
    public static final EntityDefinition<SpellcasterIllagerEntity> EVOKER;
    public static final EntityDefinition<EvokerFangsEntity> EVOKER_FANGS;
    public static final EntityDefinition<ThrowableItemEntity> EXPERIENCE_BOTTLE;
    public static final EntityDefinition<ExpOrbEntity> EXPERIENCE_ORB;
    public static final EntityDefinition<Entity> EYE_OF_ENDER;
    public static final EntityDefinition<FallingBlockEntity> FALLING_BLOCK;
    public static final EntityDefinition<FireballEntity> FIREBALL;
    public static final EntityDefinition<FireworkEntity> FIREWORK_ROCKET;
    public static final EntityDefinition<FishingHookEntity> FISHING_BOBBER;
    public static final EntityDefinition<FoxEntity> FOX;
    public static final EntityDefinition<FrogEntity> FROG;
    public static final EntityDefinition<FurnaceMinecartEntity> FURNACE_MINECART; // Not present on Bedrock
    public static final EntityDefinition<GhastEntity> GHAST;
    public static final EntityDefinition<GiantEntity> GIANT;
    public static final EntityDefinition<ItemFrameEntity> GLOW_ITEM_FRAME;
    public static final EntityDefinition<GlowSquidEntity> GLOW_SQUID;
    public static final EntityDefinition<GoatEntity> GOAT;
    public static final EntityDefinition<GuardianEntity> GUARDIAN;
    public static final EntityDefinition<HoglinEntity> HOGLIN;
    public static final EntityDefinition<MinecartEntity> HOPPER_MINECART;
    public static final EntityDefinition<HorseEntity> HORSE;
    public static final EntityDefinition<ZombieEntity> HUSK;
    public static final EntityDefinition<SpellcasterIllagerEntity> ILLUSIONER; // Not present on Bedrock
    public static final EntityDefinition<InteractionEntity> INTERACTION;
    public static final EntityDefinition<IronGolemEntity> IRON_GOLEM;
    public static final EntityDefinition<ItemEntity> ITEM;
    public static final EntityDefinition<ItemFrameEntity> ITEM_FRAME;
    public static final EntityDefinition<BoatEntity> JUNGLE_BOAT;
    public static final EntityDefinition<ChestBoatEntity> JUNGLE_CHEST_BOAT;
    public static final EntityDefinition<LeashKnotEntity> LEASH_KNOT;
    public static final EntityDefinition<LightningEntity> LIGHTNING_BOLT;
    public static final EntityDefinition<LlamaEntity> LLAMA;
    public static final EntityDefinition<ThrowableEntity> LLAMA_SPIT;
    public static final EntityDefinition<MagmaCubeEntity> MAGMA_CUBE;
    public static final EntityDefinition<BoatEntity> MANGROVE_BOAT;
    public static final EntityDefinition<ChestBoatEntity> MANGROVE_CHEST_BOAT;
    public static final EntityDefinition<MinecartEntity> MINECART;
    public static final EntityDefinition<MooshroomEntity> MOOSHROOM;
    public static final EntityDefinition<ChestedHorseEntity> MULE;
    public static final EntityDefinition<BoatEntity> OAK_BOAT;
    public static final EntityDefinition<ChestBoatEntity> OAK_CHEST_BOAT;
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
    public static final EntityDefinition<ThrownPotionEntity> POTION;
    public static final EntityDefinition<PufferFishEntity> PUFFERFISH;
    public static final EntityDefinition<RabbitEntity> RABBIT;
    public static final EntityDefinition<RavagerEntity> RAVAGER;
    public static final EntityDefinition<AbstractFishEntity> SALMON;
    public static final EntityDefinition<SheepEntity> SHEEP;
    public static final EntityDefinition<ShulkerEntity> SHULKER;
    public static final EntityDefinition<SnifferEntity> SNIFFER;
    public static final EntityDefinition<ThrowableEntity> SHULKER_BULLET;
    public static final EntityDefinition<MonsterEntity> SILVERFISH;
    public static final EntityDefinition<SkeletonEntity> SKELETON;
    public static final EntityDefinition<SkeletonHorseEntity> SKELETON_HORSE;
    public static final EntityDefinition<SlimeEntity> SLIME;
    public static final EntityDefinition<FireballEntity> SMALL_FIREBALL;
    public static final EntityDefinition<ThrowableItemEntity> SNOWBALL;
    public static final EntityDefinition<SnowGolemEntity> SNOW_GOLEM;
    public static final EntityDefinition<SpawnerMinecartEntity> SPAWNER_MINECART; // Not present on Bedrock
    public static final EntityDefinition<AbstractArrowEntity> SPECTRAL_ARROW;
    public static final EntityDefinition<SpiderEntity> SPIDER;
    public static final EntityDefinition<BoatEntity> SPRUCE_BOAT;
    public static final EntityDefinition<ChestBoatEntity> SPRUCE_CHEST_BOAT;
    public static final EntityDefinition<SquidEntity> SQUID;
    public static final EntityDefinition<AbstractSkeletonEntity> STRAY;
    public static final EntityDefinition<StriderEntity> STRIDER;
    public static final EntityDefinition<TadpoleEntity> TADPOLE;
    public static final EntityDefinition<TextDisplayEntity> TEXT_DISPLAY;
    public static final EntityDefinition<TNTEntity> TNT;
    public static final EntityDefinition<MinecartEntity> TNT_MINECART;
    public static final EntityDefinition<TraderLlamaEntity> TRADER_LLAMA;
    public static final EntityDefinition<TridentEntity> TRIDENT;
    public static final EntityDefinition<TropicalFishEntity> TROPICAL_FISH;
    public static final EntityDefinition<TurtleEntity> TURTLE;
    public static final EntityDefinition<VexEntity> VEX;
    public static final EntityDefinition<VillagerEntity> VILLAGER;
    public static final EntityDefinition<VindicatorEntity> VINDICATOR;
    public static final EntityDefinition<AbstractMerchantEntity> WANDERING_TRADER;
    public static final EntityDefinition<WardenEntity> WARDEN;
    public static final EntityDefinition<AbstractWindChargeEntity> WIND_CHARGE;
    public static final EntityDefinition<RaidParticipantEntity> WITCH;
    public static final EntityDefinition<WitherEntity> WITHER;
    public static final EntityDefinition<AbstractSkeletonEntity> WITHER_SKELETON;
    public static final EntityDefinition<WitherSkullEntity> WITHER_SKULL;
    public static final EntityDefinition<WolfEntity> WOLF;
    public static final EntityDefinition<ZoglinEntity> ZOGLIN;
    public static final EntityDefinition<ZombieEntity> ZOMBIE;
    public static final EntityDefinition<ZombieHorseEntity> ZOMBIE_HORSE;
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
        EntityDefinition<Entity> entityBase = EntityDefinition.builder(Entity::new)
                .addTranslator(MetadataType.BYTE, Entity::setFlags)
                .addTranslator(MetadataType.INT, Entity::setAir) // Air/bubbles
                .addTranslator(MetadataType.OPTIONAL_CHAT, Entity::setDisplayName)
                .addTranslator(MetadataType.BOOLEAN, Entity::setDisplayNameVisible)
                .addTranslator(MetadataType.BOOLEAN, Entity::setSilent)
                .addTranslator(MetadataType.BOOLEAN, Entity::setGravity)
                .addTranslator(MetadataType.POSE, (entity, entityMetadata) -> entity.setPose(entityMetadata.getValue()))
                .addTranslator(MetadataType.INT, Entity::setFreezing)
                .build();

        // Extends entity
        {
            AREA_EFFECT_CLOUD = EntityDefinition.inherited(AreaEffectCloudEntity::new, entityBase)
                    .type(EntityType.AREA_EFFECT_CLOUD)
                    .height(0.5f).width(1.0f)
                    .addTranslator(MetadataType.FLOAT, AreaEffectCloudEntity::setRadius)
                    .addTranslator(null) // Waiting
                    .addTranslator(MetadataType.PARTICLE, AreaEffectCloudEntity::setParticle)
                    .build();
            DRAGON_FIREBALL = EntityDefinition.inherited(FireballEntity::new, entityBase)
                    .type(EntityType.DRAGON_FIREBALL)
                    .heightAndWidth(1.0f)
                    .build();
            END_CRYSTAL = EntityDefinition.inherited(EnderCrystalEntity::new, entityBase)
                    .type(EntityType.END_CRYSTAL)
                    .heightAndWidth(2.0f)
                    .identifier("minecraft:ender_crystal")
                    .addTranslator(MetadataType.OPTIONAL_POSITION, EnderCrystalEntity::setBlockTarget)
                    .addTranslator(MetadataType.BOOLEAN,
                            (enderCrystalEntity, entityMetadata) -> enderCrystalEntity.setFlag(EntityFlag.SHOW_BOTTOM, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue())) // There is a base located on the ender crystal
                    .build();
            EXPERIENCE_ORB = EntityDefinition.inherited(ExpOrbEntity::new, entityBase)
                    .type(EntityType.EXPERIENCE_ORB)
                    .identifier("minecraft:xp_orb")
                    .build();
            EVOKER_FANGS = EntityDefinition.inherited(EvokerFangsEntity::new, entityBase)
                    .type(EntityType.EVOKER_FANGS)
                    .height(0.8f).width(0.5f)
                    .identifier("minecraft:evocation_fang")
                    .build();
            EYE_OF_ENDER = EntityDefinition.inherited(Entity::new, entityBase)
                    .type(EntityType.EYE_OF_ENDER)
                    .heightAndWidth(0.25f)
                    .identifier("minecraft:eye_of_ender_signal")
                    .addTranslator(null)  // Item
                    .build();
            FALLING_BLOCK = EntityDefinition.<FallingBlockEntity>inherited(null, entityBase)
                    .type(EntityType.FALLING_BLOCK)
                    .heightAndWidth(0.98f)
                    .addTranslator(null) // "start block position"
                    .build();
            FIREWORK_ROCKET = EntityDefinition.inherited(FireworkEntity::new, entityBase)
                    .type(EntityType.FIREWORK_ROCKET)
                    .heightAndWidth(0.25f)
                    .identifier("minecraft:fireworks_rocket")
                    .addTranslator(MetadataType.ITEM, FireworkEntity::setFireworkItem)
                    .addTranslator(MetadataType.OPTIONAL_VARINT, FireworkEntity::setPlayerGliding)
                    .addTranslator(null) // Shot at angle
                    .build();
            FISHING_BOBBER = EntityDefinition.<FishingHookEntity>inherited(null, entityBase)
                    .type(EntityType.FISHING_BOBBER)
                    .identifier("minecraft:fishing_hook")
                    .addTranslator(MetadataType.INT, FishingHookEntity::setHookedEntity)
                    .addTranslator(null) // Biting TODO check
                    .build();
            ITEM = EntityDefinition.inherited(ItemEntity::new, entityBase)
                    .type(EntityType.ITEM)
                    .heightAndWidth(0.25f)
                    .offset(0.125f)
                    .addTranslator(MetadataType.ITEM, ItemEntity::setItem)
                    .build();
            LEASH_KNOT = EntityDefinition.inherited(LeashKnotEntity::new, entityBase)
                    .type(EntityType.LEASH_KNOT)
                    .height(0.5f).width(0.375f)
                    .build();
            LIGHTNING_BOLT = EntityDefinition.inherited(LightningEntity::new, entityBase)
                    .type(EntityType.LIGHTNING_BOLT)
                    .build();
            LLAMA_SPIT = EntityDefinition.inherited(ThrowableEntity::new, entityBase)
                    .type(EntityType.LLAMA_SPIT)
                    .heightAndWidth(0.25f)
                    .build();
            PAINTING = EntityDefinition.<PaintingEntity>inherited(null, entityBase)
                    .type(EntityType.PAINTING)
                    .addTranslator(MetadataType.PAINTING_VARIANT, PaintingEntity::setPaintingType)
                    .build();
            SHULKER_BULLET = EntityDefinition.inherited(ThrowableEntity::new, entityBase)
                    .type(EntityType.SHULKER_BULLET)
                    .heightAndWidth(0.3125f)
                    .build();
            TNT = EntityDefinition.inherited(TNTEntity::new, entityBase)
                    .type(EntityType.TNT)
                    .heightAndWidth(0.98f)
                    .offset(0.49f)
                    .addTranslator(MetadataType.INT, TNTEntity::setFuseLength)
                    .build();

            EntityDefinition<DisplayBaseEntity> displayBase = EntityDefinition.inherited(DisplayBaseEntity::new, entityBase)
                    .addTranslator(null) // Interpolation delay
                    .addTranslator(null) // Transformation interpolation duration
                    .addTranslator(null) // Position/Rotation interpolation duration
                    .addTranslator(MetadataType.VECTOR3, DisplayBaseEntity::setTranslation) // Translation
                    .addTranslator(null) // Scale
                    .addTranslator(null) // Left rotation
                    .addTranslator(null) // Right rotation
                    .addTranslator(null) // Billboard render constraints
                    .addTranslator(null) // Brightness override
                    .addTranslator(null) // View range
                    .addTranslator(null) // Shadow radius
                    .addTranslator(null) // Shadow strength
                    .addTranslator(null) // Width
                    .addTranslator(null) // Height
                    .addTranslator(null) // Glow color override
                    .build();
            TEXT_DISPLAY = EntityDefinition.inherited(TextDisplayEntity::new, displayBase)
                    .type(EntityType.TEXT_DISPLAY)
                    .identifier("minecraft:armor_stand")
                    .offset(-0.5f)
                    .addTranslator(MetadataType.CHAT, TextDisplayEntity::setText)
                    .addTranslator(null) // Line width
                    .addTranslator(null) // Background color
                    .addTranslator(null) // Text opacity
                    .addTranslator(null) // Bit mask
                    .build();

            INTERACTION = EntityDefinition.inherited(InteractionEntity::new, entityBase)
                    .type(EntityType.INTERACTION)
                    .heightAndWidth(1.0f) // default size until server specifies otherwise
                    .identifier("minecraft:armor_stand")
                    .addTranslator(MetadataType.FLOAT, InteractionEntity::setWidth)
                    .addTranslator(MetadataType.FLOAT, InteractionEntity::setHeight)
                    .addTranslator(MetadataType.BOOLEAN, InteractionEntity::setResponse)
                    .build();

            EntityDefinition<FireballEntity> fireballBase = EntityDefinition.inherited(FireballEntity::new, entityBase)
                    .addTranslator(null) // Item
                    .build();
            FIREBALL = EntityDefinition.inherited(FireballEntity::new, fireballBase)
                    .type(EntityType.FIREBALL)
                    .heightAndWidth(1.0f)
                    .build();
            SMALL_FIREBALL = EntityDefinition.inherited(FireballEntity::new, fireballBase)
                    .type(EntityType.SMALL_FIREBALL)
                    .heightAndWidth(0.3125f)
                    .build();

            EntityDefinition<ThrowableItemEntity> throwableItemBase = EntityDefinition.inherited(ThrowableItemEntity::new, entityBase)
                    .addTranslator(MetadataType.ITEM, ThrowableItemEntity::setItem)
                    .build();
            EGG = EntityDefinition.inherited(ThrowableItemEntity::new, throwableItemBase)
                    .type(EntityType.EGG)
                    .heightAndWidth(0.25f)
                    .build();
            ENDER_PEARL = EntityDefinition.inherited(ThrowableItemEntity::new, throwableItemBase)
                    .type(EntityType.ENDER_PEARL)
                    .heightAndWidth(0.25f)
                    .build();
            EXPERIENCE_BOTTLE = EntityDefinition.inherited(ThrowableItemEntity::new, throwableItemBase)
                    .type(EntityType.EXPERIENCE_BOTTLE)
                    .heightAndWidth(0.25f)
                    .identifier("minecraft:xp_bottle")
                    .build();
            POTION = EntityDefinition.inherited(ThrownPotionEntity::new, throwableItemBase)
                    .type(EntityType.POTION)
                    .heightAndWidth(0.25f)
                    .identifier("minecraft:splash_potion")
                    .build();
            SNOWBALL = EntityDefinition.inherited(ThrowableItemEntity::new, throwableItemBase)
                    .type(EntityType.SNOWBALL)
                    .heightAndWidth(0.25f)
                    .build();

            EntityFactory<AbstractWindChargeEntity> windChargeSupplier = AbstractWindChargeEntity::new;
            BREEZE_WIND_CHARGE = EntityDefinition.inherited(windChargeSupplier, entityBase)
                    .type(EntityType.BREEZE_WIND_CHARGE)
                    .identifier("minecraft:breeze_wind_charge_projectile")
                    .heightAndWidth(0.3125f)
                    .build();
            WIND_CHARGE = EntityDefinition.inherited(windChargeSupplier, entityBase)
                    .type(EntityType.WIND_CHARGE)
                    .identifier("minecraft:wind_charge_projectile")
                    .heightAndWidth(0.3125f)
                    .build();

            EntityDefinition<AbstractArrowEntity> abstractArrowBase = EntityDefinition.inherited(AbstractArrowEntity::new, entityBase)
                    .addTranslator(MetadataType.BYTE, AbstractArrowEntity::setArrowFlags)
                    .addTranslator(null) // "Piercing level"
                    .addTranslator(null) // If the arrow is in the ground
                    .build();
            ARROW = EntityDefinition.inherited(ArrowEntity::new, abstractArrowBase)
                    .type(EntityType.ARROW)
                    .heightAndWidth(0.25f)
                    .addTranslator(MetadataType.INT, ArrowEntity::setPotionEffectColor)
                    .build();
            SPECTRAL_ARROW = EntityDefinition.inherited(abstractArrowBase.factory(), abstractArrowBase)
                    .type(EntityType.SPECTRAL_ARROW)
                    .heightAndWidth(0.25f)
                    .identifier("minecraft:arrow")
                    .build();
            TRIDENT = EntityDefinition.inherited(TridentEntity::new, abstractArrowBase) // TODO remove class
                    .type(EntityType.TRIDENT)
                    .identifier("minecraft:thrown_trident")
                    .addTranslator(null) // Loyalty
                    .addTranslator(MetadataType.BOOLEAN, (tridentEntity, entityMetadata) -> tridentEntity.setFlag(EntityFlag.ENCHANTED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();

            // Item frames are handled differently as they are blocks, not items, in Bedrock
            ITEM_FRAME = EntityDefinition.<ItemFrameEntity>inherited(null, entityBase)
                    .type(EntityType.ITEM_FRAME)
                    .addTranslator(MetadataType.ITEM, ItemFrameEntity::setItemInFrame)
                    .addTranslator(MetadataType.INT, ItemFrameEntity::setItemRotation)
                    .build();
            GLOW_ITEM_FRAME = EntityDefinition.inherited(ITEM_FRAME.factory(), ITEM_FRAME)
                    .type(EntityType.GLOW_ITEM_FRAME)
                    .build();

            MINECART = EntityDefinition.inherited(MinecartEntity::new, entityBase)
                    .type(EntityType.MINECART)
                    .height(0.7f).width(0.98f)
                    .offset(0.35f)
                    .addTranslator(MetadataType.INT, (minecartEntity, entityMetadata) -> minecartEntity.getDirtyMetadata().put(EntityDataTypes.STRUCTURAL_INTEGRITY, entityMetadata.getValue()))
                    .addTranslator(MetadataType.INT, (minecartEntity, entityMetadata) -> minecartEntity.getDirtyMetadata().put(EntityDataTypes.HURT_DIRECTION, entityMetadata.getValue())) // Direction in which the minecart is shaking
                    .addTranslator(MetadataType.FLOAT, (minecartEntity, entityMetadata) ->
                            // Power in Java, hurt ticks in Bedrock
                            minecartEntity.getDirtyMetadata().put(EntityDataTypes.HURT_TICKS, Math.min((int) ((FloatEntityMetadata) entityMetadata).getPrimitiveValue(), 15)))
                    .addTranslator(MetadataType.INT, MinecartEntity::setCustomBlock)
                    .addTranslator(MetadataType.INT, MinecartEntity::setCustomBlockOffset)
                    .addTranslator(MetadataType.BOOLEAN, MinecartEntity::setShowCustomBlock)
                    .build();
            CHEST_MINECART = EntityDefinition.inherited(MINECART.factory(), MINECART)
                    .type(EntityType.CHEST_MINECART)
                    .build();
            COMMAND_BLOCK_MINECART = EntityDefinition.inherited(CommandBlockMinecartEntity::new, MINECART)
                    .type(EntityType.COMMAND_BLOCK_MINECART)
                    .addTranslator(MetadataType.STRING, (entity, entityMetadata) -> entity.getDirtyMetadata().put(EntityDataTypes.COMMAND_BLOCK_NAME, entityMetadata.getValue()))
                    .addTranslator(MetadataType.CHAT, (entity, entityMetadata) -> entity.getDirtyMetadata().put(EntityDataTypes.COMMAND_BLOCK_LAST_OUTPUT, MessageTranslator.convertMessage(entityMetadata.getValue())))
                    .build();
            FURNACE_MINECART = EntityDefinition.inherited(FurnaceMinecartEntity::new, MINECART)
                    .type(EntityType.FURNACE_MINECART)
                    .identifier("minecraft:minecart")
                    .addTranslator(MetadataType.BOOLEAN, FurnaceMinecartEntity::setHasFuel)
                    .build();
            HOPPER_MINECART = EntityDefinition.inherited(MINECART.factory(), MINECART)
                    .type(EntityType.HOPPER_MINECART)
                    .build();
            SPAWNER_MINECART = EntityDefinition.inherited(SpawnerMinecartEntity::new, MINECART)
                    .type(EntityType.SPAWNER_MINECART)
                    .identifier("minecraft:minecart")
                    .build();
            TNT_MINECART = EntityDefinition.inherited(MINECART.factory(), MINECART)
                    .type(EntityType.TNT_MINECART)
                    .build();

            WITHER_SKULL = EntityDefinition.inherited(WitherSkullEntity::new, entityBase)
                    .type(EntityType.WITHER_SKULL)
                    .heightAndWidth(0.3125f)
                    .addTranslator(MetadataType.BOOLEAN, WitherSkullEntity::setDangerous)
                    .build();
            WITHER_SKULL_DANGEROUS = EntityDefinition.inherited(WITHER_SKULL.factory(), WITHER_SKULL)
                    .build(false);
        }

        // Boats
        {
            EntityDefinition<BoatEntity> boatBase = EntityDefinition.<BoatEntity>inherited(null, entityBase)
                .height(0.6f).width(1.6f)
                .offset(0.35f)
                .addTranslator(MetadataType.INT, (boatEntity, entityMetadata) -> boatEntity.getDirtyMetadata().put(EntityDataTypes.HURT_TICKS, entityMetadata.getValue())) // Time since last hit
                .addTranslator(MetadataType.INT, (boatEntity, entityMetadata) -> boatEntity.getDirtyMetadata().put(EntityDataTypes.HURT_DIRECTION, entityMetadata.getValue())) // Rocking direction
                .addTranslator(MetadataType.FLOAT, (boatEntity, entityMetadata) ->
                    // 'Health' in Bedrock, damage taken in Java - it makes motion in Bedrock
                    boatEntity.getDirtyMetadata().put(EntityDataTypes.STRUCTURAL_INTEGRITY, 40 - ((int) ((FloatEntityMetadata) entityMetadata).getPrimitiveValue())))
                .addTranslator(MetadataType.BOOLEAN, BoatEntity::setPaddlingLeft)
                .addTranslator(MetadataType.BOOLEAN, BoatEntity::setPaddlingRight)
                .addTranslator(MetadataType.INT, (boatEntity, entityMetadata) -> boatEntity.getDirtyMetadata().put(EntityDataTypes.BOAT_BUBBLE_TIME, entityMetadata.getValue())) // May not actually do anything
                .build();

            ACACIA_BOAT = buildBoat(boatBase, EntityType.ACACIA_BOAT, BoatEntity.BoatVariant.ACACIA);
            BAMBOO_RAFT = buildBoat(boatBase, EntityType.BAMBOO_RAFT, BoatEntity.BoatVariant.BAMBOO);
            BIRCH_BOAT = buildBoat(boatBase, EntityType.BIRCH_BOAT, BoatEntity.BoatVariant.BIRCH);
            CHERRY_BOAT = buildBoat(boatBase, EntityType.CHERRY_BOAT, BoatEntity.BoatVariant.CHERRY);
            DARK_OAK_BOAT = buildBoat(boatBase, EntityType.DARK_OAK_BOAT, BoatEntity.BoatVariant.DARK_OAK);
            JUNGLE_BOAT = buildBoat(boatBase, EntityType.JUNGLE_BOAT, BoatEntity.BoatVariant.JUNGLE);
            MANGROVE_BOAT = buildBoat(boatBase, EntityType.MANGROVE_BOAT, BoatEntity.BoatVariant.MANGROVE);
            OAK_BOAT = buildBoat(boatBase, EntityType.OAK_BOAT, BoatEntity.BoatVariant.OAK);
            SPRUCE_BOAT = buildBoat(boatBase, EntityType.SPRUCE_BOAT, BoatEntity.BoatVariant.SPRUCE);

            EntityDefinition<ChestBoatEntity> chestBoatBase = EntityDefinition.<ChestBoatEntity>inherited(null, boatBase)
                .build();

            ACACIA_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityType.ACACIA_CHEST_BOAT, BoatEntity.BoatVariant.ACACIA);
            BAMBOO_CHEST_RAFT = buildChestBoat(chestBoatBase, EntityType.BAMBOO_CHEST_RAFT, BoatEntity.BoatVariant.BAMBOO);
            BIRCH_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityType.BIRCH_CHEST_BOAT, BoatEntity.BoatVariant.BIRCH);
            CHERRY_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityType.CHERRY_CHEST_BOAT, BoatEntity.BoatVariant.CHERRY);
            DARK_OAK_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityType.DARK_OAK_CHEST_BOAT, BoatEntity.BoatVariant.DARK_OAK);
            JUNGLE_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityType.JUNGLE_CHEST_BOAT, BoatEntity.BoatVariant.JUNGLE);
            MANGROVE_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityType.MANGROVE_CHEST_BOAT, BoatEntity.BoatVariant.MANGROVE);
            OAK_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityType.OAK_CHEST_BOAT, BoatEntity.BoatVariant.OAK);
            SPRUCE_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityType.SPRUCE_CHEST_BOAT, BoatEntity.BoatVariant.SPRUCE);
        }

        EntityDefinition<LivingEntity> livingEntityBase = EntityDefinition.inherited(LivingEntity::new, entityBase)
                .addTranslator(MetadataType.BYTE, LivingEntity::setLivingEntityFlags)
                .addTranslator(MetadataType.FLOAT, LivingEntity::setHealth)
                .addTranslator(MetadataType.PARTICLES, LivingEntity::setParticles)
                .addTranslator(MetadataType.BOOLEAN,
                        (livingEntity, entityMetadata) -> livingEntity.getDirtyMetadata().put(EntityDataTypes.EFFECT_AMBIENCE, (byte) (((BooleanEntityMetadata) entityMetadata).getPrimitiveValue() ? 1 : 0)))
                .addTranslator(null) // Arrow count
                .addTranslator(null) // Stinger count
                .addTranslator(MetadataType.OPTIONAL_POSITION, LivingEntity::setBedPosition)
                .build();

        ARMOR_STAND = EntityDefinition.inherited(ArmorStandEntity::new, livingEntityBase)
                .type(EntityType.ARMOR_STAND)
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
            ALLAY = EntityDefinition.inherited(AllayEntity::new, mobEntityBase)
                    .type(EntityType.ALLAY)
                    .height(0.6f).width(0.35f)
                    .addTranslator(MetadataType.BOOLEAN, AllayEntity::setDancing)
                    .addTranslator(MetadataType.BOOLEAN, AllayEntity::setCanDuplicate)
                    .build();
            BAT = EntityDefinition.inherited(BatEntity::new, mobEntityBase)
                    .type(EntityType.BAT)
                    .height(0.9f).width(0.5f)
                    .addTranslator(MetadataType.BYTE, BatEntity::setBatFlags)
                    .build();
            BOGGED = EntityDefinition.inherited(BoggedEntity::new, mobEntityBase)
                    .type(EntityType.BOGGED)
                    .height(1.99f).width(0.6f)
                    .addTranslator(MetadataType.BOOLEAN, BoggedEntity::setSheared)
                    .build();
            BLAZE = EntityDefinition.inherited(BlazeEntity::new, mobEntityBase)
                    .type(EntityType.BLAZE)
                    .height(1.8f).width(0.6f)
                    .addTranslator(MetadataType.BYTE, BlazeEntity::setBlazeFlags)
                    .build();
            BREEZE = EntityDefinition.inherited(BreezeEntity::new, mobEntityBase)
                    .type(EntityType.BREEZE)
                    .height(1.77f).width(0.6f)
                    .build();
            CREEPER = EntityDefinition.inherited(CreeperEntity::new, mobEntityBase)
                    .type(EntityType.CREEPER)
                    .height(1.7f).width(0.6f)
                    .offset(1.62f)
                    .addTranslator(MetadataType.INT, CreeperEntity::setSwelling)
                    .addTranslator(MetadataType.BOOLEAN, (entity, entityMetadata) -> entity.setFlag(EntityFlag.POWERED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .addTranslator(MetadataType.BOOLEAN, CreeperEntity::setIgnited)
                    .build();
            ENDERMAN = EntityDefinition.inherited(EndermanEntity::new, mobEntityBase)
                    .type(EntityType.ENDERMAN)
                    .height(2.9f).width(0.6f)
                    .addTranslator(MetadataType.OPTIONAL_BLOCK_STATE, EndermanEntity::setCarriedBlock)
                    .addTranslator(MetadataType.BOOLEAN, EndermanEntity::setScreaming)
                    .addTranslator(MetadataType.BOOLEAN, EndermanEntity::setAngry)
                    .build();
            ENDERMITE = EntityDefinition.inherited(MonsterEntity::new, mobEntityBase)
                    .type(EntityType.ENDERMITE)
                    .height(0.3f).width(0.4f)
                    .build();
            ENDER_DRAGON = EntityDefinition.inherited(EnderDragonEntity::new, mobEntityBase)
                    .type(EntityType.ENDER_DRAGON)
                    .addTranslator(MetadataType.INT, EnderDragonEntity::setPhase)
                    .build();
            GHAST = EntityDefinition.inherited(GhastEntity::new, mobEntityBase)
                    .type(EntityType.GHAST)
                    .heightAndWidth(4.0f)
                    .addTranslator(MetadataType.BOOLEAN, GhastEntity::setGhastAttacking)
                    .build();
            GIANT = EntityDefinition.inherited(GiantEntity::new, mobEntityBase)
                    .type(EntityType.GIANT)
                    .height(1.8f).width(1.6f)
                    .offset(1.62f)
                    .identifier("minecraft:zombie")
                    .build();
            IRON_GOLEM = EntityDefinition.inherited(IronGolemEntity::new, mobEntityBase)
                    .type(EntityType.IRON_GOLEM)
                    .height(2.7f).width(1.4f)
                    .addTranslator(null) // "is player created", which doesn't seem to do anything clientside
                    .build();
            PHANTOM = EntityDefinition.inherited(PhantomEntity::new, mobEntityBase)
                    .type(EntityType.PHANTOM)
                    .height(0.5f).width(0.9f)
                    .offset(0.6f)
                    .addTranslator(MetadataType.INT, PhantomEntity::setPhantomScale)
                    .build();
            SILVERFISH = EntityDefinition.inherited(MonsterEntity::new, mobEntityBase)
                    .type(EntityType.SILVERFISH)
                    .height(0.3f).width(0.4f)
                    .build();
            SHULKER = EntityDefinition.inherited(ShulkerEntity::new, mobEntityBase)
                    .type(EntityType.SHULKER)
                    .heightAndWidth(1f)
                    .addTranslator(MetadataType.DIRECTION, ShulkerEntity::setAttachedFace)
                    .addTranslator(MetadataType.BYTE, ShulkerEntity::setShulkerHeight)
                    .addTranslator(MetadataType.BYTE, ShulkerEntity::setShulkerColor)
                    .build();
            SKELETON = EntityDefinition.inherited(SkeletonEntity::new, mobEntityBase)
                    .type(EntityType.SKELETON)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .addTranslator(MetadataType.BOOLEAN, SkeletonEntity::setConvertingToStray)
                    .build();
            SNOW_GOLEM = EntityDefinition.inherited(SnowGolemEntity::new, mobEntityBase)
                    .type(EntityType.SNOW_GOLEM)
                    .height(1.9f).width(0.7f)
                    .addTranslator(MetadataType.BYTE, SnowGolemEntity::setSnowGolemFlags)
                    .build();
            SPIDER = EntityDefinition.inherited(SpiderEntity::new, mobEntityBase)
                    .type(EntityType.SPIDER)
                    .height(0.9f).width(1.4f)
                    .offset(1f)
                    .addTranslator(MetadataType.BYTE, SpiderEntity::setSpiderFlags)
                    .build();
            CAVE_SPIDER = EntityDefinition.inherited(SpiderEntity::new, SPIDER)
                    .type(EntityType.CAVE_SPIDER)
                    .height(0.5f).width(0.7f)
                    .build();
            STRAY = EntityDefinition.inherited(AbstractSkeletonEntity::new, mobEntityBase)
                    .type(EntityType.STRAY)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .build();
            VEX = EntityDefinition.inherited(VexEntity::new, mobEntityBase)
                    .type(EntityType.VEX)
                    .height(0.8f).width(0.4f)
                    .addTranslator(MetadataType.BYTE, VexEntity::setVexFlags)
                    .build();
            WARDEN = EntityDefinition.inherited(WardenEntity::new, mobEntityBase)
                    .type(EntityType.WARDEN)
                    .height(2.9f).width(0.9f)
                    .addTranslator(MetadataType.INT, WardenEntity::setAngerLevel)
                    .build();
            WITHER = EntityDefinition.inherited(WitherEntity::new, mobEntityBase)
                    .type(EntityType.WITHER)
                    .height(3.5f).width(0.9f)
                    .addTranslator(MetadataType.INT, WitherEntity::setTarget1)
                    .addTranslator(MetadataType.INT, WitherEntity::setTarget2)
                    .addTranslator(MetadataType.INT, WitherEntity::setTarget3)
                    .addTranslator(MetadataType.INT, WitherEntity::setInvulnerableTicks)
                    .build();
            WITHER_SKELETON = EntityDefinition.inherited(AbstractSkeletonEntity::new, mobEntityBase)
                    .type(EntityType.WITHER_SKELETON)
                    .height(2.4f).width(0.7f)
                    .build();
            ZOGLIN = EntityDefinition.inherited(ZoglinEntity::new, mobEntityBase)
                    .type(EntityType.ZOGLIN)
                    .height(1.4f).width(1.3965f)
                    .addTranslator(MetadataType.BOOLEAN, ZoglinEntity::setBaby)
                    .build();
            ZOMBIE = EntityDefinition.inherited(ZombieEntity::new, mobEntityBase)
                    .type(EntityType.ZOMBIE)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .addTranslator(MetadataType.BOOLEAN, ZombieEntity::setZombieBaby)
                    .addTranslator(null) // "set special type", doesn't do anything
                    .addTranslator(MetadataType.BOOLEAN, ZombieEntity::setConvertingToDrowned)
                    .build();
            ZOMBIE_VILLAGER = EntityDefinition.inherited(ZombieVillagerEntity::new, ZOMBIE)
                    .type(EntityType.ZOMBIE_VILLAGER)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .identifier("minecraft:zombie_villager_v2")
                    .addTranslator(MetadataType.BOOLEAN, ZombieVillagerEntity::setTransforming)
                    .addTranslator(MetadataType.VILLAGER_DATA, ZombieVillagerEntity::setZombieVillagerData)
                    .build();
            ZOMBIFIED_PIGLIN = EntityDefinition.inherited(ZombifiedPiglinEntity::new, ZOMBIE) //TODO test how zombie entity metadata is handled?
                    .type(EntityType.ZOMBIFIED_PIGLIN)
                    .height(1.95f).width(0.6f)
                    .offset(1.62f)
                    .identifier("minecraft:zombie_pigman")
                    .build();

            DROWNED = EntityDefinition.inherited(ZOMBIE.factory(), ZOMBIE)
                    .type(EntityType.DROWNED)
                    .height(1.95f).width(0.6f)
                    .build();
            HUSK = EntityDefinition.inherited(ZOMBIE.factory(), ZOMBIE)
                    .type(EntityType.HUSK)
                    .build();

            GUARDIAN = EntityDefinition.inherited(GuardianEntity::new, mobEntityBase)
                    .type(EntityType.GUARDIAN)
                    .heightAndWidth(0.85f)
                    .addTranslator(null) // Moving //TODO
                    .addTranslator(MetadataType.INT, GuardianEntity::setGuardianTarget)
                    .build();
            ELDER_GUARDIAN = EntityDefinition.inherited(ElderGuardianEntity::new, GUARDIAN)
                    .type(EntityType.ELDER_GUARDIAN)
                    .heightAndWidth(1.9975f)
                    .build();

            SLIME = EntityDefinition.inherited(SlimeEntity::new, mobEntityBase)
                    .type(EntityType.SLIME)
                    .heightAndWidth(0.51f)
                    .addTranslator(MetadataType.INT, SlimeEntity::setSlimeScale)
                    .build();
            MAGMA_CUBE = EntityDefinition.inherited(MagmaCubeEntity::new, SLIME)
                    .type(EntityType.MAGMA_CUBE)
                    .build();

            EntityDefinition<AbstractFishEntity> abstractFishEntityBase = EntityDefinition.inherited(AbstractFishEntity::new, mobEntityBase)
                    .addTranslator(null) // From bucket
                    .build();
            COD = EntityDefinition.inherited(abstractFishEntityBase.factory(), abstractFishEntityBase)
                    .type(EntityType.COD)
                    .height(0.25f).width(0.5f)
                    .build();
            PUFFERFISH = EntityDefinition.inherited(PufferFishEntity::new, abstractFishEntityBase)
                    .type(EntityType.PUFFERFISH)
                    .heightAndWidth(0.7f)
                    .addTranslator(MetadataType.INT, PufferFishEntity::setPufferfishSize)
                    .build();
            SALMON = EntityDefinition.inherited(abstractFishEntityBase.factory(), abstractFishEntityBase)
                    .type(EntityType.SALMON)
                    .height(0.5f).width(0.7f)
                    .addTranslator(null) // Scale/variant - TODO
                    .build();
            TADPOLE = EntityDefinition.inherited(TadpoleEntity::new, abstractFishEntityBase)
                    .type(EntityType.TADPOLE)
                    .height(0.3f).width(0.4f)
                    .build();
            TROPICAL_FISH = EntityDefinition.inherited(TropicalFishEntity::new, abstractFishEntityBase)
                    .type(EntityType.TROPICAL_FISH)
                    .heightAndWidth(0.6f)
                    .identifier("minecraft:tropicalfish")
                    .addTranslator(MetadataType.INT, TropicalFishEntity::setFishVariant)
                    .build();

            EntityDefinition<BasePiglinEntity> abstractPiglinEntityBase = EntityDefinition.inherited(BasePiglinEntity::new, mobEntityBase)
                    .addTranslator(MetadataType.BOOLEAN, BasePiglinEntity::setImmuneToZombification)
                    .build();
            PIGLIN = EntityDefinition.inherited(PiglinEntity::new, abstractPiglinEntityBase)
                    .type(EntityType.PIGLIN)
                    .height(1.95f).width(0.6f)
                    .addTranslator(MetadataType.BOOLEAN, PiglinEntity::setBaby)
                    .addTranslator(MetadataType.BOOLEAN, PiglinEntity::setChargingCrossbow)
                    .addTranslator(MetadataType.BOOLEAN, PiglinEntity::setDancing)
                    .build();
            PIGLIN_BRUTE = EntityDefinition.inherited(abstractPiglinEntityBase.factory(), abstractPiglinEntityBase)
                    .type(EntityType.PIGLIN_BRUTE)
                    .height(1.95f).width(0.6f)
                    .build();

            EntityDefinition<RaidParticipantEntity> raidParticipantEntityBase = EntityDefinition.inherited(RaidParticipantEntity::new, mobEntityBase)
                    .addTranslator(null) // Celebrating //TODO
                    .build();
            EntityDefinition<SpellcasterIllagerEntity> spellcasterEntityBase = EntityDefinition.inherited(SpellcasterIllagerEntity::new, raidParticipantEntityBase)
                    .addTranslator(MetadataType.BYTE, SpellcasterIllagerEntity::setSpellType)
                    .build();
            EVOKER = EntityDefinition.inherited(spellcasterEntityBase.factory(), spellcasterEntityBase)
                    .type(EntityType.EVOKER)
                    .height(1.95f).width(0.6f)
                    .identifier("minecraft:evocation_illager")
                    .build();
            ILLUSIONER = EntityDefinition.inherited(spellcasterEntityBase.factory(), spellcasterEntityBase)
                    .type(EntityType.ILLUSIONER)
                    .height(1.95f).width(0.6f)
                    .identifier("minecraft:evocation_illager")
                    .build();
            PILLAGER = EntityDefinition.inherited(PillagerEntity::new, raidParticipantEntityBase)
                    .type(EntityType.PILLAGER)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .addTranslator(MetadataType.BOOLEAN, PillagerEntity::setChargingCrossbow)
                    .build();
            RAVAGER = EntityDefinition.inherited(RavagerEntity::new, raidParticipantEntityBase)
                    .type(EntityType.RAVAGER)
                    .height(1.9f).width(1.2f)
                    .build();
            VINDICATOR = EntityDefinition.inherited(VindicatorEntity::new, raidParticipantEntityBase)
                    .type(EntityType.VINDICATOR)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .build();
            WITCH = EntityDefinition.inherited(raidParticipantEntityBase.factory(), raidParticipantEntityBase)
                    .type(EntityType.WITCH)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .addTranslator(null) // Using item
                    .build();
        }

        EntityDefinition<AgeableEntity> ageableEntityBase = EntityDefinition.inherited(AgeableEntity::new, mobEntityBase)
                .addTranslator(MetadataType.BOOLEAN, AgeableEntity::setBaby)
                .build();

        // Extends ageable
        {
            ARMADILLO = EntityDefinition.inherited(ArmadilloEntity::new, ageableEntityBase)
                    .type(EntityType.ARMADILLO)
                    .height(0.65f).width(0.7f)
                    .properties(new GeyserEntityProperties.Builder()
                        .addEnum(
                            "minecraft:armadillo_state",
                            "unrolled",
                            "rolled_up",
                            "rolled_up_peeking",
                            "rolled_up_relaxing",
                            "rolled_up_unrolling")
                        .build())
                    .addTranslator(MetadataType.ARMADILLO_STATE, ArmadilloEntity::setArmadilloState)
                    .build();
            AXOLOTL = EntityDefinition.inherited(AxolotlEntity::new, ageableEntityBase)
                    .type(EntityType.AXOLOTL)
                    .height(0.42f).width(0.7f)
                    .addTranslator(MetadataType.INT, AxolotlEntity::setVariant)
                    .addTranslator(MetadataType.BOOLEAN, AxolotlEntity::setPlayingDead)
                    .addTranslator(null) // From bucket
                    .build();
            BEE = EntityDefinition.inherited(BeeEntity::new, ageableEntityBase)
                    .type(EntityType.BEE)
                    .heightAndWidth(0.6f)
                    .properties(new GeyserEntityProperties.Builder()
                        .addBoolean("minecraft:has_nectar")
                        .build())
                    .addTranslator(MetadataType.BYTE, BeeEntity::setBeeFlags)
                    .addTranslator(MetadataType.INT, BeeEntity::setAngerTime)
                    .build();
            CHICKEN = EntityDefinition.inherited(ChickenEntity::new, ageableEntityBase)
                    .type(EntityType.CHICKEN)
                    .height(0.7f).width(0.4f)
                    .build();
            COW = EntityDefinition.inherited(CowEntity::new, ageableEntityBase)
                    .type(EntityType.COW)
                    .height(1.4f).width(0.9f)
                    .build();
            FOX = EntityDefinition.inherited(FoxEntity::new, ageableEntityBase)
                    .type(EntityType.FOX)
                    .height(0.7f).width(0.6f)
                    .addTranslator(MetadataType.INT, FoxEntity::setFoxVariant)
                    .addTranslator(MetadataType.BYTE, FoxEntity::setFoxFlags)
                    .addTranslator(null) // Trusted player 1
                    .addTranslator(null) // Trusted player 2
                    .build();
            FROG = EntityDefinition.inherited(FrogEntity::new, ageableEntityBase)
                    .type(EntityType.FROG)
                    .heightAndWidth(0.5f)
                    .addTranslator(MetadataType.FROG_VARIANT, FrogEntity::setFrogVariant)
                    .addTranslator(MetadataType.OPTIONAL_VARINT, FrogEntity::setTongueTarget)
                    .build();
            HOGLIN = EntityDefinition.inherited(HoglinEntity::new, ageableEntityBase)
                    .type(EntityType.HOGLIN)
                    .height(1.4f).width(1.3965f)
                    .addTranslator(MetadataType.BOOLEAN, HoglinEntity::setImmuneToZombification)
                    .build();
            GOAT = EntityDefinition.inherited(GoatEntity::new, ageableEntityBase)
                    .type(EntityType.GOAT)
                    .height(1.3f).width(0.9f)
                    .addTranslator(MetadataType.BOOLEAN, GoatEntity::setScreamer)
                    .addTranslator(MetadataType.BOOLEAN, GoatEntity::setHasLeftHorn)
                    .addTranslator(MetadataType.BOOLEAN, GoatEntity::setHasRightHorn)
                    .build();
            MOOSHROOM = EntityDefinition.inherited(MooshroomEntity::new, ageableEntityBase)
                    .type(EntityType.MOOSHROOM)
                    .height(1.4f).width(0.9f)
                    .addTranslator(MetadataType.STRING, MooshroomEntity::setVariant)
                    .build();
            OCELOT = EntityDefinition.inherited(OcelotEntity::new, ageableEntityBase)
                    .type(EntityType.OCELOT)
                    .height(0.7f).width(0.6f)
                    .addTranslator(MetadataType.BOOLEAN, (ocelotEntity, entityMetadata) -> ocelotEntity.setFlag(EntityFlag.TRUSTING, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();
            PANDA = EntityDefinition.inherited(PandaEntity::new, ageableEntityBase)
                    .type(EntityType.PANDA)
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
                    .heightAndWidth(0.9f)
                    .addTranslator(MetadataType.BOOLEAN, (pigEntity, entityMetadata) -> pigEntity.setFlag(EntityFlag.SADDLED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .addTranslator(MetadataType.INT, PigEntity::setBoost)
                    .build();
            POLAR_BEAR = EntityDefinition.inherited(PolarBearEntity::new, ageableEntityBase)
                    .type(EntityType.POLAR_BEAR)
                    .height(1.4f).width(1.3f)
                    .addTranslator(MetadataType.BOOLEAN, (entity, entityMetadata) -> entity.setFlag(EntityFlag.STANDING, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();
            RABBIT = EntityDefinition.inherited(RabbitEntity::new, ageableEntityBase)
                    .type(EntityType.RABBIT)
                    .height(0.5f).width(0.4f)
                    .addTranslator(MetadataType.INT, RabbitEntity::setRabbitVariant)
                    .build();
            SHEEP = EntityDefinition.inherited(SheepEntity::new, ageableEntityBase)
                    .type(EntityType.SHEEP)
                    .height(1.3f).width(0.9f)
                    .addTranslator(MetadataType.BYTE, SheepEntity::setSheepFlags)
                    .build();
            SNIFFER = EntityDefinition.inherited(SnifferEntity::new, ageableEntityBase)
                    .type(EntityType.SNIFFER)
                    .height(1.75f).width(1.9f)
                    .addTranslator(MetadataType.SNIFFER_STATE, SnifferEntity::setSnifferState)
                    .addTranslator(null) // Integer, drop seed at tick
                    .build();
            STRIDER = EntityDefinition.inherited(StriderEntity::new, ageableEntityBase)
                    .type(EntityType.STRIDER)
                    .height(1.7f).width(0.9f)
                    .addTranslator(MetadataType.INT, StriderEntity::setBoost)
                    .addTranslator(MetadataType.BOOLEAN, StriderEntity::setCold)
                    .addTranslator(MetadataType.BOOLEAN, StriderEntity::setSaddled)
                    .build();
            TURTLE = EntityDefinition.inherited(TurtleEntity::new, ageableEntityBase)
                    .type(EntityType.TURTLE)
                    .height(0.4f).width(1.2f)
                    .addTranslator(null) // Home position
                    .addTranslator(MetadataType.BOOLEAN, TurtleEntity::setPregnant)
                    .addTranslator(MetadataType.BOOLEAN, TurtleEntity::setLayingEgg)
                    .addTranslator(null) // Travel position
                    .addTranslator(null) // Going home
                    .addTranslator(null) // Travelling
                    .build();

            EntityDefinition<AbstractMerchantEntity> abstractVillagerEntityBase = EntityDefinition.inherited(AbstractMerchantEntity::new, ageableEntityBase)
                    .addTranslator(null) // Unhappy ticks
                    .build();
            VILLAGER = EntityDefinition.inherited(VillagerEntity::new, abstractVillagerEntityBase)
                    .type(EntityType.VILLAGER)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .identifier("minecraft:villager_v2")
                    .addTranslator(MetadataType.VILLAGER_DATA, VillagerEntity::setVillagerData)
                    .build();
            WANDERING_TRADER = EntityDefinition.inherited(abstractVillagerEntityBase.factory(), abstractVillagerEntityBase)
                    .type(EntityType.WANDERING_TRADER)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .build();
        }

        // Water creatures (AgeableWaterCreature)
        {
            DOLPHIN = EntityDefinition.inherited(DolphinEntity::new, ageableEntityBase)
                .type(EntityType.DOLPHIN)
                .height(0.6f).width(0.9f)
                //TODO check
                .addTranslator(null) // treasure position
                .addTranslator(null) // "got fish"
                .addTranslator(null) // "moistness level"
                .build();
            SQUID = EntityDefinition.inherited(SquidEntity::new, ageableEntityBase)
                .type(EntityType.SQUID)
                .heightAndWidth(0.8f)
                .build();
            GLOW_SQUID = EntityDefinition.inherited(GlowSquidEntity::new, SQUID)
                .type(EntityType.GLOW_SQUID)
                .addTranslator(null) // Set dark ticks remaining, possible TODO
                .build();
        }

        // Horses
        {
            EntityDefinition<AbstractHorseEntity> abstractHorseEntityBase = EntityDefinition.inherited(AbstractHorseEntity::new, ageableEntityBase)
                    .addTranslator(MetadataType.BYTE, AbstractHorseEntity::setHorseFlags)
                    .build();
            CAMEL = EntityDefinition.inherited(CamelEntity::new, abstractHorseEntityBase)
                    .type(EntityType.CAMEL)
                    .height(2.375f).width(1.7f)
                    .addTranslator(MetadataType.BOOLEAN, CamelEntity::setDashing)
                    .addTranslator(MetadataType.LONG, CamelEntity::setLastPoseTick)
                    .build();
            HORSE = EntityDefinition.inherited(HorseEntity::new, abstractHorseEntityBase)
                    .type(EntityType.HORSE)
                    .height(1.6f).width(1.3965f)
                    .addTranslator(MetadataType.INT, HorseEntity::setHorseVariant)
                    .build();
            SKELETON_HORSE = EntityDefinition.inherited(SkeletonHorseEntity::new, abstractHorseEntityBase)
                    .type(EntityType.SKELETON_HORSE)
                    .height(1.6f).width(1.3965f)
                    .build();
            ZOMBIE_HORSE = EntityDefinition.inherited(ZombieHorseEntity::new, abstractHorseEntityBase)
                    .type(EntityType.ZOMBIE_HORSE)
                    .height(1.6f).width(1.3965f)
                    .build();
            EntityDefinition<ChestedHorseEntity> chestedHorseEntityBase = EntityDefinition.inherited(ChestedHorseEntity::new, abstractHorseEntityBase)
                    .addTranslator(MetadataType.BOOLEAN, (horseEntity, entityMetadata) -> horseEntity.setFlag(EntityFlag.CHESTED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();
            DONKEY = EntityDefinition.inherited(chestedHorseEntityBase.factory(), chestedHorseEntityBase)
                    .type(EntityType.DONKEY)
                    .height(1.6f).width(1.3965f)
                    .build();
            MULE = EntityDefinition.inherited(chestedHorseEntityBase.factory(), chestedHorseEntityBase)
                    .type(EntityType.MULE)
                    .height(1.6f).width(1.3965f)
                    .build();
            LLAMA = EntityDefinition.inherited(LlamaEntity::new, chestedHorseEntityBase)
                    .type(EntityType.LLAMA)
                    .height(1.87f).width(0.9f)
                    .addTranslator(MetadataType.INT, LlamaEntity::setStrength)
                    .addTranslator(MetadataType.INT, (entity, entityMetadata) -> entity.getDirtyMetadata().put(EntityDataTypes.VARIANT, entityMetadata.getValue()))
                    .build();
            TRADER_LLAMA = EntityDefinition.inherited(TraderLlamaEntity::new, LLAMA)
                    .type(EntityType.TRADER_LLAMA)
                    .identifier("minecraft:llama")
                    .build();
        }

        EntityDefinition<TameableEntity> tameableEntityBase = EntityDefinition.<TameableEntity>inherited(null, ageableEntityBase) // No factory, is abstract
                .addTranslator(MetadataType.BYTE, TameableEntity::setTameableFlags)
                .addTranslator(MetadataType.OPTIONAL_UUID, TameableEntity::setOwner)
                .build();
        CAT = EntityDefinition.inherited(CatEntity::new, tameableEntityBase)
                .type(EntityType.CAT)
                .height(0.35f).width(0.3f)
                .addTranslator(MetadataType.CAT_VARIANT, CatEntity::setCatVariant)
                .addTranslator(MetadataType.BOOLEAN, CatEntity::setResting)
                .addTranslator(null) // "resting state one" //TODO
                .addTranslator(MetadataType.INT, CatEntity::setCollarColor)
                .build();
        PARROT = EntityDefinition.inherited(ParrotEntity::new, tameableEntityBase)
                .type(EntityType.PARROT)
                .height(0.9f).width(0.5f)
                .addTranslator(MetadataType.INT, (parrotEntity, entityMetadata) -> parrotEntity.getDirtyMetadata().put(EntityDataTypes.VARIANT, entityMetadata.getValue())) // Parrot color
                .build();
        WOLF = EntityDefinition.inherited(WolfEntity::new, tameableEntityBase)
                .type(EntityType.WOLF)
                .height(0.85f).width(0.6f)
                // "Begging" on wiki.vg, "Interested" in Nukkit - the tilt of the head
                .addTranslator(MetadataType.BOOLEAN, (wolfEntity, entityMetadata) -> wolfEntity.setFlag(EntityFlag.INTERESTED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                .addTranslator(MetadataType.INT, WolfEntity::setCollarColor)
                .addTranslator(MetadataType.INT, WolfEntity::setWolfAngerTime)
                .addTranslator(MetadataType.WOLF_VARIANT, WolfEntity::setWolfVariant)
                .build();

        // As of 1.18 these don't track entity data at all
        ENDER_DRAGON_PART = EntityDefinition.<EnderDragonPartEntity>builder(null)
                .identifier("minecraft:armor_stand") // Emulated
                .build(false); // Never sent over the network

        Registries.JAVA_ENTITY_IDENTIFIERS.get().put("minecraft:marker", null); // We don't need an entity definition for this as it is never sent over the network
    }

    private static EntityDefinition<BoatEntity> buildBoat(EntityDefinition<BoatEntity> base, EntityType entityType, BoatEntity.BoatVariant variant) {
        return EntityDefinition.inherited((session, javaId, bedrockId, uuid, definition, position, motion, yaw, pitch, headYaw) ->
            new BoatEntity(session, javaId, bedrockId, uuid, definition, position, motion, yaw, variant), base)
            .type(entityType)
            .identifier("minecraft:boat")
            .build();
    }

    private static EntityDefinition<ChestBoatEntity> buildChestBoat(EntityDefinition<ChestBoatEntity> base, EntityType entityType, BoatEntity.BoatVariant variant) {
        return EntityDefinition.inherited((session, javaId, bedrockId, uuid, definition, position, motion, yaw, pitch, headYaw) ->
                new ChestBoatEntity(session, javaId, bedrockId, uuid, definition, position, motion, yaw, variant), base)
            .type(entityType)
            .identifier("minecraft:chest_boat")
            .build();
    }

    public static void init() {
        // no-op
    }

    private EntityDefinitions() {
    }
}
