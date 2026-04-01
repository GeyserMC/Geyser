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

#include "org.checkerframework.checker.nullness.qual.NonNull"
#include "org.checkerframework.checker.nullness.qual.Nullable"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes"
#include "org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag"
#include "org.geysermc.geyser.GeyserImpl"
#include "org.geysermc.geyser.api.entity.property.GeyserEntityProperty"
#include "org.geysermc.geyser.api.entity.property.type.GeyserFloatEntityProperty"
#include "org.geysermc.geyser.api.entity.property.type.GeyserStringEnumProperty"
#include "org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent"
#include "org.geysermc.geyser.api.util.Identifier"
#include "org.geysermc.geyser.entity.factory.EntityFactory"
#include "org.geysermc.geyser.entity.properties.type.BooleanProperty"
#include "org.geysermc.geyser.entity.properties.type.EnumProperty"
#include "org.geysermc.geyser.entity.properties.type.FloatProperty"
#include "org.geysermc.geyser.entity.properties.type.IntProperty"
#include "org.geysermc.geyser.entity.properties.type.PropertyType"
#include "org.geysermc.geyser.entity.properties.type.StringEnumProperty"
#include "org.geysermc.geyser.entity.type.AbstractArrowEntity"
#include "org.geysermc.geyser.entity.type.AbstractWindChargeEntity"
#include "org.geysermc.geyser.entity.type.AreaEffectCloudEntity"
#include "org.geysermc.geyser.entity.type.ArrowEntity"
#include "org.geysermc.geyser.entity.type.BoatEntity"
#include "org.geysermc.geyser.entity.type.ChestBoatEntity"
#include "org.geysermc.geyser.entity.type.CommandBlockMinecartEntity"
#include "org.geysermc.geyser.entity.type.DisplayBaseEntity"
#include "org.geysermc.geyser.entity.type.EnderCrystalEntity"
#include "org.geysermc.geyser.entity.type.EnderEyeEntity"
#include "org.geysermc.geyser.entity.type.Entity"
#include "org.geysermc.geyser.entity.type.EvokerFangsEntity"
#include "org.geysermc.geyser.entity.type.ExpOrbEntity"
#include "org.geysermc.geyser.entity.type.FallingBlockEntity"
#include "org.geysermc.geyser.entity.type.FireballEntity"
#include "org.geysermc.geyser.entity.type.FireworkEntity"
#include "org.geysermc.geyser.entity.type.FishingHookEntity"
#include "org.geysermc.geyser.entity.type.FurnaceMinecartEntity"
#include "org.geysermc.geyser.entity.type.HangingEntity"
#include "org.geysermc.geyser.entity.type.InteractionEntity"
#include "org.geysermc.geyser.entity.type.ItemEntity"
#include "org.geysermc.geyser.entity.type.ItemFrameEntity"
#include "org.geysermc.geyser.entity.type.LeashKnotEntity"
#include "org.geysermc.geyser.entity.type.LightningEntity"
#include "org.geysermc.geyser.entity.type.LivingEntity"
#include "org.geysermc.geyser.entity.type.MinecartEntity"
#include "org.geysermc.geyser.entity.type.PaintingEntity"
#include "org.geysermc.geyser.entity.type.SpawnerMinecartEntity"
#include "org.geysermc.geyser.entity.type.TNTEntity"
#include "org.geysermc.geyser.entity.type.TextDisplayEntity"
#include "org.geysermc.geyser.entity.type.ThrowableEggEntity"
#include "org.geysermc.geyser.entity.type.ThrowableEntity"
#include "org.geysermc.geyser.entity.type.ThrowableItemEntity"
#include "org.geysermc.geyser.entity.type.ThrownPotionEntity"
#include "org.geysermc.geyser.entity.type.TridentEntity"
#include "org.geysermc.geyser.entity.type.WitherSkullEntity"
#include "org.geysermc.geyser.entity.type.living.AbstractFishEntity"
#include "org.geysermc.geyser.entity.type.living.AgeableEntity"
#include "org.geysermc.geyser.entity.type.living.AllayEntity"
#include "org.geysermc.geyser.entity.type.living.ArmorStandEntity"
#include "org.geysermc.geyser.entity.type.living.BatEntity"
#include "org.geysermc.geyser.entity.type.living.CopperGolemEntity"
#include "org.geysermc.geyser.entity.type.living.DolphinEntity"
#include "org.geysermc.geyser.entity.type.living.GlowSquidEntity"
#include "org.geysermc.geyser.entity.type.living.IronGolemEntity"
#include "org.geysermc.geyser.entity.type.living.MagmaCubeEntity"
#include "org.geysermc.geyser.entity.type.living.MobEntity"
#include "org.geysermc.geyser.entity.type.living.SlimeEntity"
#include "org.geysermc.geyser.entity.type.living.SnowGolemEntity"
#include "org.geysermc.geyser.entity.type.living.SquidEntity"
#include "org.geysermc.geyser.entity.type.living.TadpoleEntity"
#include "org.geysermc.geyser.entity.type.living.animal.ArmadilloEntity"
#include "org.geysermc.geyser.entity.type.living.animal.AxolotlEntity"
#include "org.geysermc.geyser.entity.type.living.animal.BeeEntity"
#include "org.geysermc.geyser.entity.type.living.animal.FoxEntity"
#include "org.geysermc.geyser.entity.type.living.animal.FrogEntity"
#include "org.geysermc.geyser.entity.type.living.animal.GoatEntity"
#include "org.geysermc.geyser.entity.type.living.animal.HappyGhastEntity"
#include "org.geysermc.geyser.entity.type.living.animal.HoglinEntity"
#include "org.geysermc.geyser.entity.type.living.animal.MooshroomEntity"
#include "org.geysermc.geyser.entity.type.living.animal.OcelotEntity"
#include "org.geysermc.geyser.entity.type.living.animal.PandaEntity"
#include "org.geysermc.geyser.entity.type.living.animal.PolarBearEntity"
#include "org.geysermc.geyser.entity.type.living.animal.PufferFishEntity"
#include "org.geysermc.geyser.entity.type.living.animal.RabbitEntity"
#include "org.geysermc.geyser.entity.type.living.animal.SheepEntity"
#include "org.geysermc.geyser.entity.type.living.animal.SnifferEntity"
#include "org.geysermc.geyser.entity.type.living.animal.StriderEntity"
#include "org.geysermc.geyser.entity.type.living.animal.TropicalFishEntity"
#include "org.geysermc.geyser.entity.type.living.animal.TurtleEntity"
#include "org.geysermc.geyser.entity.type.living.animal.farm.ChickenEntity"
#include "org.geysermc.geyser.entity.type.living.animal.farm.CowEntity"
#include "org.geysermc.geyser.entity.type.living.animal.farm.PigEntity"
#include "org.geysermc.geyser.entity.type.living.animal.TemperatureVariantAnimal"
#include "org.geysermc.geyser.entity.type.living.animal.horse.AbstractHorseEntity"
#include "org.geysermc.geyser.entity.type.living.animal.horse.CamelEntity"
#include "org.geysermc.geyser.entity.type.living.animal.horse.CamelHuskEntity"
#include "org.geysermc.geyser.entity.type.living.animal.horse.ChestedHorseEntity"
#include "org.geysermc.geyser.entity.type.living.animal.horse.HorseEntity"
#include "org.geysermc.geyser.entity.type.living.animal.horse.LlamaEntity"
#include "org.geysermc.geyser.entity.type.living.animal.horse.SkeletonHorseEntity"
#include "org.geysermc.geyser.entity.type.living.animal.horse.TraderLlamaEntity"
#include "org.geysermc.geyser.entity.type.living.animal.horse.ZombieHorseEntity"
#include "org.geysermc.geyser.entity.type.living.animal.nautilus.AbstractNautilusEntity"
#include "org.geysermc.geyser.entity.type.living.animal.nautilus.NautilusEntity"
#include "org.geysermc.geyser.entity.type.living.animal.nautilus.ZombieNautilusEntity"
#include "org.geysermc.geyser.entity.type.living.animal.tameable.CatEntity"
#include "org.geysermc.geyser.entity.type.living.animal.tameable.ParrotEntity"
#include "org.geysermc.geyser.entity.type.living.animal.tameable.TameableEntity"
#include "org.geysermc.geyser.entity.type.living.animal.tameable.WolfEntity"
#include "org.geysermc.geyser.entity.type.living.merchant.AbstractMerchantEntity"
#include "org.geysermc.geyser.entity.type.living.merchant.VillagerEntity"
#include "org.geysermc.geyser.entity.type.living.monster.AbstractSkeletonEntity"
#include "org.geysermc.geyser.entity.type.living.monster.BasePiglinEntity"
#include "org.geysermc.geyser.entity.type.living.monster.BlazeEntity"
#include "org.geysermc.geyser.entity.type.living.monster.BoggedEntity"
#include "org.geysermc.geyser.entity.type.living.monster.BreezeEntity"
#include "org.geysermc.geyser.entity.type.living.monster.CreakingEntity"
#include "org.geysermc.geyser.entity.type.living.monster.CreeperEntity"
#include "org.geysermc.geyser.entity.type.living.monster.ElderGuardianEntity"
#include "org.geysermc.geyser.entity.type.living.monster.EnderDragonEntity"
#include "org.geysermc.geyser.entity.type.living.monster.EnderDragonPartEntity"
#include "org.geysermc.geyser.entity.type.living.monster.EndermanEntity"
#include "org.geysermc.geyser.entity.type.living.monster.GhastEntity"
#include "org.geysermc.geyser.entity.type.living.monster.GiantEntity"
#include "org.geysermc.geyser.entity.type.living.monster.GuardianEntity"
#include "org.geysermc.geyser.entity.type.living.monster.MonsterEntity"
#include "org.geysermc.geyser.entity.type.living.monster.PhantomEntity"
#include "org.geysermc.geyser.entity.type.living.monster.PiglinEntity"
#include "org.geysermc.geyser.entity.type.living.monster.ShulkerEntity"
#include "org.geysermc.geyser.entity.type.living.monster.SkeletonEntity"
#include "org.geysermc.geyser.entity.type.living.monster.SpiderEntity"
#include "org.geysermc.geyser.entity.type.living.monster.VexEntity"
#include "org.geysermc.geyser.entity.type.living.monster.WardenEntity"
#include "org.geysermc.geyser.entity.type.living.monster.WitherEntity"
#include "org.geysermc.geyser.entity.type.living.monster.ZoglinEntity"
#include "org.geysermc.geyser.entity.type.living.monster.ZombieEntity"
#include "org.geysermc.geyser.entity.type.living.monster.ZombieVillagerEntity"
#include "org.geysermc.geyser.entity.type.living.monster.ZombifiedPiglinEntity"
#include "org.geysermc.geyser.entity.type.living.monster.raid.PillagerEntity"
#include "org.geysermc.geyser.entity.type.living.monster.raid.RaidParticipantEntity"
#include "org.geysermc.geyser.entity.type.living.monster.raid.RavagerEntity"
#include "org.geysermc.geyser.entity.type.living.monster.raid.SpellcasterIllagerEntity"
#include "org.geysermc.geyser.entity.type.living.monster.raid.VindicatorEntity"
#include "org.geysermc.geyser.entity.type.player.AvatarEntity"
#include "org.geysermc.geyser.entity.type.player.MannequinEntity"
#include "org.geysermc.geyser.entity.type.player.PlayerEntity"
#include "org.geysermc.geyser.registry.Registries"
#include "org.geysermc.geyser.translator.text.MessageTranslator"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata"
#include "org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType"

#include "java.util.Collection"
#include "java.util.List"
#include "java.util.Objects"

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
    public static final EntityDefinition<CamelHuskEntity> CAMEL_HUSK;
    public static final EntityDefinition<CatEntity> CAT;
    public static final EntityDefinition<SpiderEntity> CAVE_SPIDER;
    public static final EntityDefinition<BoatEntity> CHERRY_BOAT;
    public static final EntityDefinition<ChestBoatEntity> CHERRY_CHEST_BOAT;
    public static final EntityDefinition<MinecartEntity> CHEST_MINECART;
    public static final EntityDefinition<ChickenEntity> CHICKEN;
    public static final EntityDefinition<AbstractFishEntity> COD;
    public static final EntityDefinition<CopperGolemEntity> COPPER_GOLEM;
    public static final EntityDefinition<CommandBlockMinecartEntity> COMMAND_BLOCK_MINECART;
    public static final EntityDefinition<CowEntity> COW;
    public static final EntityDefinition<CreakingEntity> CREAKING;
    public static final EntityDefinition<CreeperEntity> CREEPER;
    public static final EntityDefinition<BoatEntity> DARK_OAK_BOAT;
    public static final EntityDefinition<ChestBoatEntity> DARK_OAK_CHEST_BOAT;
    public static final EntityDefinition<DolphinEntity> DOLPHIN;
    public static final EntityDefinition<ChestedHorseEntity> DONKEY;
    public static final EntityDefinition<FireballEntity> DRAGON_FIREBALL;
    public static final EntityDefinition<ZombieEntity> DROWNED;
    public static final EntityDefinition<ThrowableEggEntity> EGG;
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
    public static final EntityDefinition<EnderEyeEntity> EYE_OF_ENDER;
    public static final EntityDefinition<FallingBlockEntity> FALLING_BLOCK;
    public static final EntityDefinition<FireballEntity> FIREBALL;
    public static final EntityDefinition<FireworkEntity> FIREWORK_ROCKET;
    public static final EntityDefinition<FishingHookEntity> FISHING_BOBBER;
    public static final EntityDefinition<FoxEntity> FOX;
    public static final EntityDefinition<FrogEntity> FROG;
    public static final EntityDefinition<FurnaceMinecartEntity> FURNACE_MINECART;
    public static final EntityDefinition<GhastEntity> GHAST;
    public static final EntityDefinition<GiantEntity> GIANT;
    public static final EntityDefinition<ItemFrameEntity> GLOW_ITEM_FRAME;
    public static final EntityDefinition<GlowSquidEntity> GLOW_SQUID;
    public static final EntityDefinition<GoatEntity> GOAT;
    public static final EntityDefinition<GuardianEntity> GUARDIAN;
    public static final EntityDefinition<HappyGhastEntity> HAPPY_GHAST;
    public static final EntityDefinition<HoglinEntity> HOGLIN;
    public static final EntityDefinition<MinecartEntity> HOPPER_MINECART;
    public static final EntityDefinition<HorseEntity> HORSE;
    public static final EntityDefinition<ZombieEntity> HUSK;
    public static final EntityDefinition<SpellcasterIllagerEntity> ILLUSIONER;
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
    public static final EntityDefinition<MannequinEntity> MANNEQUIN;
    public static final EntityDefinition<MinecartEntity> MINECART;
    public static final EntityDefinition<MooshroomEntity> MOOSHROOM;
    public static final EntityDefinition<ChestedHorseEntity> MULE;
    public static final EntityDefinition<NautilusEntity> NAUTILUS;
    public static final EntityDefinition<BoatEntity> OAK_BOAT;
    public static final EntityDefinition<ChestBoatEntity> OAK_CHEST_BOAT;
    public static final EntityDefinition<OcelotEntity> OCELOT;
    public static final EntityDefinition<PaintingEntity> PAINTING;
    public static final EntityDefinition<BoatEntity> PALE_OAK_BOAT;
    public static final EntityDefinition<ChestBoatEntity> PALE_OAK_CHEST_BOAT;
    public static final EntityDefinition<PandaEntity> PANDA;
    public static final EntityDefinition<AbstractSkeletonEntity> PARCHED;
    public static final EntityDefinition<ParrotEntity> PARROT;
    public static final EntityDefinition<PhantomEntity> PHANTOM;
    public static final EntityDefinition<PigEntity> PIG;
    public static final EntityDefinition<PiglinEntity> PIGLIN;
    public static final EntityDefinition<BasePiglinEntity> PIGLIN_BRUTE;
    public static final EntityDefinition<PillagerEntity> PILLAGER;
    public static final EntityDefinition<PlayerEntity> PLAYER;
    public static final EntityDefinition<PolarBearEntity> POLAR_BEAR;
    public static final EntityDefinition<ThrownPotionEntity> SPLASH_POTION;
    public static final EntityDefinition<ThrownPotionEntity> LINGERING_POTION;
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
    public static final EntityDefinition<SpawnerMinecartEntity> SPAWNER_MINECART;
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
    public static final EntityDefinition<ZombieNautilusEntity> ZOMBIE_NAUTILUS;
    public static final EntityDefinition<ZombieVillagerEntity> ZOMBIE_VILLAGER;
    public static final EntityDefinition<ZombifiedPiglinEntity> ZOMBIFIED_PIGLIN;


    public static final EntityDefinition<EnderDragonPartEntity> ENDER_DRAGON_PART;

    public static final EntityDefinition<WitherSkullEntity> WITHER_SKULL_DANGEROUS;

    static {
        EntityDefinition<Entity> entityBase = EntityDefinition.builder(Entity::new)
                .addTranslator(MetadataTypes.BYTE, Entity::setFlags)
                .addTranslator(MetadataTypes.INT, Entity::setAir)
                .addTranslator(MetadataTypes.OPTIONAL_COMPONENT, Entity::setCustomName)
                .addTranslator(MetadataTypes.BOOLEAN, Entity::setCustomNameVisible)
                .addTranslator(MetadataTypes.BOOLEAN, Entity::setSilent)
                .addTranslator(MetadataTypes.BOOLEAN, Entity::setGravity)
                .addTranslator(MetadataTypes.POSE, (entity, entityMetadata) -> entity.setPose(entityMetadata.getValue()))
                .addTranslator(MetadataTypes.INT, Entity::setFreezing)
                .build();


        {
            AREA_EFFECT_CLOUD = EntityDefinition.inherited(AreaEffectCloudEntity::new, entityBase)
                    .type(EntityType.AREA_EFFECT_CLOUD)
                    .height(0.5f).width(1.0f)
                    .addTranslator(MetadataTypes.FLOAT, AreaEffectCloudEntity::setRadius)
                    .addTranslator(null)
                    .addTranslator(MetadataTypes.PARTICLE, AreaEffectCloudEntity::setParticle)
                    .build();
            DRAGON_FIREBALL = EntityDefinition.inherited(FireballEntity::new, entityBase)
                    .type(EntityType.DRAGON_FIREBALL)
                    .heightAndWidth(1.0f)
                    .build();
            END_CRYSTAL = EntityDefinition.inherited(EnderCrystalEntity::new, entityBase)
                    .type(EntityType.END_CRYSTAL)
                    .heightAndWidth(2.0f)
                    .identifier("minecraft:ender_crystal")
                    .addTranslator(MetadataTypes.OPTIONAL_BLOCK_POS, EnderCrystalEntity::setBlockTarget)
                    .addTranslator(MetadataTypes.BOOLEAN,
                            (enderCrystalEntity, entityMetadata) -> enderCrystalEntity.setFlag(EntityFlag.SHOW_BOTTOM, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();
            EXPERIENCE_ORB = EntityDefinition.inherited(ExpOrbEntity::new, entityBase)
                    .type(EntityType.EXPERIENCE_ORB)
                    .addTranslator(null)
                    .identifier("minecraft:xp_orb")
                    .build();
            EVOKER_FANGS = EntityDefinition.inherited(EvokerFangsEntity::new, entityBase)
                    .type(EntityType.EVOKER_FANGS)
                    .height(0.8f).width(0.5f)
                    .identifier("minecraft:evocation_fang")
                    .build();
            EYE_OF_ENDER = EntityDefinition.inherited(EnderEyeEntity::new, entityBase)
                    .type(EntityType.EYE_OF_ENDER)
                    .heightAndWidth(0.25f)
                    .identifier("minecraft:eye_of_ender_signal")
                    .addTranslator(null)
                    .build();
            FALLING_BLOCK = EntityDefinition.<FallingBlockEntity>inherited(null, entityBase)
                    .type(EntityType.FALLING_BLOCK)
                    .heightAndWidth(0.98f)
                    .addTranslator(null)
                    .build();
            FIREWORK_ROCKET = EntityDefinition.inherited(FireworkEntity::new, entityBase)
                    .type(EntityType.FIREWORK_ROCKET)
                    .heightAndWidth(0.25f)
                    .identifier("minecraft:fireworks_rocket")
                    .addTranslator(MetadataTypes.ITEM_STACK, FireworkEntity::setFireworkItem)
                    .addTranslator(MetadataTypes.OPTIONAL_UNSIGNED_INT, FireworkEntity::setPlayerGliding)
                    .addTranslator(null)
                    .build();
            FISHING_BOBBER = EntityDefinition.<FishingHookEntity>inherited(null, entityBase)
                    .type(EntityType.FISHING_BOBBER)
                    .identifier("minecraft:fishing_hook")
                    .addTranslator(MetadataTypes.INT, FishingHookEntity::setHookedEntity)
                    .addTranslator(null)
                    .build();
            ITEM = EntityDefinition.inherited(ItemEntity::new, entityBase)
                    .type(EntityType.ITEM)
                    .heightAndWidth(0.25f)
                    .offset(0.125f)
                    .addTranslator(MetadataTypes.ITEM_STACK, ItemEntity::setItem)
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
            SHULKER_BULLET = EntityDefinition.inherited(ThrowableEntity::new, entityBase)
                    .type(EntityType.SHULKER_BULLET)
                    .heightAndWidth(0.3125f)
                    .build();
            TNT = EntityDefinition.inherited(TNTEntity::new, entityBase)
                    .type(EntityType.TNT)
                    .heightAndWidth(0.98f)
                    .offset(0.49f)
                    .addTranslator(MetadataTypes.INT, TNTEntity::setFuseLength)
                    .build();

            EntityDefinition<DisplayBaseEntity> displayBase = EntityDefinition.inherited(DisplayBaseEntity::new, entityBase)
                    .addTranslator(null)
                    .addTranslator(null)
                    .addTranslator(null)
                    .addTranslator(MetadataTypes.VECTOR3, DisplayBaseEntity::setTranslation)
                    .addTranslator(null)
                    .addTranslator(null)
                    .addTranslator(null)
                    .addTranslator(null)
                    .addTranslator(null)
                    .addTranslator(null)
                    .addTranslator(null)
                    .addTranslator(null)
                    .addTranslator(null)
                    .addTranslator(null)
                    .addTranslator(null)
                    .build();
            TEXT_DISPLAY = EntityDefinition.inherited(TextDisplayEntity::new, displayBase)
                    .type(EntityType.TEXT_DISPLAY)
                    .identifier("minecraft:armor_stand")
                    .addTranslator(MetadataTypes.COMPONENT, TextDisplayEntity::setText)
                    .addTranslator(null)
                    .addTranslator(null)
                    .addTranslator(null)
                    .addTranslator(null) // Bit mask
                    .build();

            INTERACTION = EntityDefinition.inherited(InteractionEntity::new, entityBase)
                    .type(EntityType.INTERACTION)
                    .heightAndWidth(1.0f) // default size until server specifies otherwise
                    .identifier("minecraft:armor_stand")
                    .addTranslator(MetadataTypes.FLOAT, InteractionEntity::setWidth)
                    .addTranslator(MetadataTypes.FLOAT, InteractionEntity::setHeight)
                    .addTranslator(MetadataTypes.BOOLEAN, InteractionEntity::setResponse)
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
                    .addTranslator(MetadataTypes.ITEM_STACK, ThrowableItemEntity::setItem)
                    .build();
            EGG = EntityDefinition.inherited(ThrowableEggEntity::new, throwableItemBase)
                    .type(EntityType.EGG)
                    .heightAndWidth(0.25f)
                    .property(TemperatureVariantAnimal.TEMPERATE_VARIANT_PROPERTY)
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
            SPLASH_POTION = EntityDefinition.inherited(ThrownPotionEntity::new, throwableItemBase)
                    .type(EntityType.SPLASH_POTION)
                    .heightAndWidth(0.25f)
                    .identifier("minecraft:splash_potion")
                    .build();
            LINGERING_POTION = EntityDefinition.inherited(ThrownPotionEntity::new, throwableItemBase)
                .type(EntityType.LINGERING_POTION)
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
                    .addTranslator(MetadataTypes.BYTE, AbstractArrowEntity::setArrowFlags)
                    .addTranslator(null) // "Piercing level"
                    .addTranslator(null) // If the arrow is in the ground
                    .build();
            ARROW = EntityDefinition.inherited(ArrowEntity::new, abstractArrowBase)
                    .type(EntityType.ARROW)
                    .heightAndWidth(0.25f)
                    .addTranslator(MetadataTypes.INT, ArrowEntity::setPotionEffectColor)
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
                    .addTranslator(MetadataTypes.BOOLEAN, (tridentEntity, entityMetadata) -> tridentEntity.setFlag(EntityFlag.ENCHANTED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();

            EntityDefinition<HangingEntity> hangingEntityBase = EntityDefinition.<HangingEntity>inherited(null, entityBase)
                .addTranslator(MetadataTypes.DIRECTION, HangingEntity::setDirectionMetadata)
                .build();

            PAINTING = EntityDefinition.inherited(PaintingEntity::new, hangingEntityBase)
                .type(EntityType.PAINTING)
                .addTranslator(MetadataTypes.PAINTING_VARIANT, PaintingEntity::setPaintingType)
                .build();


            ITEM_FRAME = EntityDefinition.inherited(ItemFrameEntity::new, hangingEntityBase)
                    .type(EntityType.ITEM_FRAME)
                    .addTranslator(MetadataTypes.ITEM_STACK, ItemFrameEntity::setItemInFrame)
                    .addTranslator(MetadataTypes.INT, ItemFrameEntity::setItemRotation)
                    .build();
            GLOW_ITEM_FRAME = EntityDefinition.inherited(ITEM_FRAME.factory(), ITEM_FRAME)
                    .type(EntityType.GLOW_ITEM_FRAME)
                    .build();

            MINECART = EntityDefinition.inherited(MinecartEntity::new, entityBase)
                    .type(EntityType.MINECART)
                    .height(0.7f).width(0.98f)
                    .offset(0.35f)
                    .addTranslator(MetadataTypes.INT, (minecartEntity, entityMetadata) -> minecartEntity.getDirtyMetadata().put(EntityDataTypes.STRUCTURAL_INTEGRITY, entityMetadata.getValue()))
                    .addTranslator(MetadataTypes.INT, (minecartEntity, entityMetadata) -> minecartEntity.getDirtyMetadata().put(EntityDataTypes.HURT_DIRECTION, entityMetadata.getValue())) // Direction in which the minecart is shaking
                    .addTranslator(MetadataTypes.FLOAT, (minecartEntity, entityMetadata) ->

                            minecartEntity.getDirtyMetadata().put(EntityDataTypes.HURT_TICKS, Math.min((int) ((FloatEntityMetadata) entityMetadata).getPrimitiveValue(), 15)))
                    .addTranslator(MetadataTypes.OPTIONAL_BLOCK_STATE, MinecartEntity::setCustomBlock)
                    .addTranslator(MetadataTypes.INT, MinecartEntity::setCustomBlockOffset)
                    .build();
            CHEST_MINECART = EntityDefinition.inherited(MINECART.factory(), MINECART)
                    .type(EntityType.CHEST_MINECART)
                    .build();
            COMMAND_BLOCK_MINECART = EntityDefinition.inherited(CommandBlockMinecartEntity::new, MINECART)
                    .type(EntityType.COMMAND_BLOCK_MINECART)
                    .addTranslator(MetadataTypes.STRING, (entity, entityMetadata) -> entity.getDirtyMetadata().put(EntityDataTypes.COMMAND_BLOCK_NAME, entityMetadata.getValue()))
                    .addTranslator(MetadataTypes.COMPONENT, (entity, entityMetadata) -> entity.getDirtyMetadata().put(EntityDataTypes.COMMAND_BLOCK_LAST_OUTPUT, MessageTranslator.convertMessage(entityMetadata.getValue())))
                    .build();
            FURNACE_MINECART = EntityDefinition.inherited(FurnaceMinecartEntity::new, MINECART)
                    .type(EntityType.FURNACE_MINECART)
                    .identifier("minecraft:minecart")
                    .addTranslator(MetadataTypes.BOOLEAN, FurnaceMinecartEntity::setHasFuel)
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
                    .addTranslator(MetadataTypes.BOOLEAN, WitherSkullEntity::setDangerous)
                    .build();
            WITHER_SKULL_DANGEROUS = EntityDefinition.inherited(WITHER_SKULL.factory(), WITHER_SKULL)
                    .build(false);
        }


        {
            EntityDefinition<BoatEntity> boatBase = EntityDefinition.<BoatEntity>inherited(null, entityBase)
                .height(0.6f).width(1.6f)
                .offset(0.35f)
                .addTranslator(MetadataTypes.INT, (boatEntity, entityMetadata) -> boatEntity.getDirtyMetadata().put(EntityDataTypes.HURT_TICKS, entityMetadata.getValue())) // Time since last hit
                .addTranslator(MetadataTypes.INT, (boatEntity, entityMetadata) -> boatEntity.getDirtyMetadata().put(EntityDataTypes.HURT_DIRECTION, entityMetadata.getValue())) // Rocking direction
                .addTranslator(MetadataTypes.FLOAT, (boatEntity, entityMetadata) ->

                    boatEntity.getDirtyMetadata().put(EntityDataTypes.STRUCTURAL_INTEGRITY, 40 - ((int) ((FloatEntityMetadata) entityMetadata).getPrimitiveValue())))
                .addTranslator(MetadataTypes.BOOLEAN, BoatEntity::setPaddlingLeft)
                .addTranslator(MetadataTypes.BOOLEAN, BoatEntity::setPaddlingRight)
                .addTranslator(MetadataTypes.INT, (boatEntity, entityMetadata) -> boatEntity.getDirtyMetadata().put(EntityDataTypes.BOAT_BUBBLE_TIME, entityMetadata.getValue())) // May not actually do anything
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
            PALE_OAK_BOAT = buildBoat(boatBase, EntityType.PALE_OAK_BOAT, BoatEntity.BoatVariant.PALE_OAK);

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
            PALE_OAK_CHEST_BOAT = buildChestBoat(chestBoatBase, EntityType.PALE_OAK_CHEST_BOAT, BoatEntity.BoatVariant.PALE_OAK);
        }

        EntityDefinition<LivingEntity> livingEntityBase = EntityDefinition.inherited(LivingEntity::new, entityBase)
                .addTranslator(MetadataTypes.BYTE, LivingEntity::setLivingEntityFlags)
                .addTranslator(MetadataTypes.FLOAT, LivingEntity::setHealth)
                .addTranslator(MetadataTypes.PARTICLES, LivingEntity::setParticles)
                .addTranslator(MetadataTypes.BOOLEAN,
                        (livingEntity, entityMetadata) -> livingEntity.getDirtyMetadata().put(EntityDataTypes.EFFECT_AMBIENCE, (byte) (((BooleanEntityMetadata) entityMetadata).getPrimitiveValue() ? 1 : 0)))
                .addTranslator(null) // Arrow count
                .addTranslator(null) // Stinger count
                .addTranslator(MetadataTypes.OPTIONAL_BLOCK_POS, LivingEntity::setBedPosition)
                .build();

        ARMOR_STAND = EntityDefinition.inherited(ArmorStandEntity::new, livingEntityBase)
                .type(EntityType.ARMOR_STAND)
                .height(1.975f).width(0.5f)
                .addTranslator(MetadataTypes.BYTE, ArmorStandEntity::setArmorStandFlags)
                .addTranslator(MetadataTypes.ROTATIONS, ArmorStandEntity::setHeadRotation)
                .addTranslator(MetadataTypes.ROTATIONS, ArmorStandEntity::setBodyRotation)
                .addTranslator(MetadataTypes.ROTATIONS, ArmorStandEntity::setLeftArmRotation)
                .addTranslator(MetadataTypes.ROTATIONS, ArmorStandEntity::setRightArmRotation)
                .addTranslator(MetadataTypes.ROTATIONS, ArmorStandEntity::setLeftLegRotation)
                .addTranslator(MetadataTypes.ROTATIONS, ArmorStandEntity::setRightLegRotation)
                .build();

        EntityDefinition<AvatarEntity> avatarEntityBase = EntityDefinition.<AvatarEntity>inherited(null, livingEntityBase)
            .height(1.8f).width(0.6f)
            .offset(1.62f)
            .addTranslator(null) // Player main hand
            .addTranslator(MetadataTypes.BYTE, AvatarEntity::setSkinVisibility)
            .build();

        MANNEQUIN = EntityDefinition.inherited(MannequinEntity::new, avatarEntityBase)
            .type(EntityType.MANNEQUIN)
            .addTranslator(MetadataTypes.RESOLVABLE_PROFILE, MannequinEntity::setProfile)
            .addTranslator(null) // Immovable
            .addTranslator(MetadataTypes.OPTIONAL_COMPONENT, MannequinEntity::setDescription)
            .build();

        PLAYER = EntityDefinition.<PlayerEntity>inherited(null, avatarEntityBase)
                .type(EntityType.PLAYER)
                .addTranslator(MetadataTypes.FLOAT, PlayerEntity::setAbsorptionHearts)
                .addTranslator(null) // Player score
                .addTranslator(MetadataTypes.OPTIONAL_UNSIGNED_INT, PlayerEntity::setLeftParrot)
                .addTranslator(MetadataTypes.OPTIONAL_UNSIGNED_INT, PlayerEntity::setRightParrot)
                .build();

        EntityDefinition<MobEntity> mobEntityBase = EntityDefinition.inherited(MobEntity::new, livingEntityBase)
                .addTranslator(MetadataTypes.BYTE, MobEntity::setMobFlags)
                .build();


        {
            ALLAY = EntityDefinition.inherited(AllayEntity::new, mobEntityBase)
                    .type(EntityType.ALLAY)
                    .height(0.6f).width(0.35f)
                    .addTranslator(MetadataTypes.BOOLEAN, AllayEntity::setDancing)
                    .addTranslator(MetadataTypes.BOOLEAN, AllayEntity::setCanDuplicate)
                    .build();
            BAT = EntityDefinition.inherited(BatEntity::new, mobEntityBase)
                    .type(EntityType.BAT)
                    .height(0.9f).width(0.5f)
                    .addTranslator(MetadataTypes.BYTE, BatEntity::setBatFlags)
                    .build();
            BOGGED = EntityDefinition.inherited(BoggedEntity::new, mobEntityBase)
                    .type(EntityType.BOGGED)
                    .height(1.99f).width(0.6f)
                    .addTranslator(MetadataTypes.BOOLEAN, BoggedEntity::setSheared)
                    .build();
            BLAZE = EntityDefinition.inherited(BlazeEntity::new, mobEntityBase)
                    .type(EntityType.BLAZE)
                    .height(1.8f).width(0.6f)
                    .addTranslator(MetadataTypes.BYTE, BlazeEntity::setBlazeFlags)
                    .build();
            BREEZE = EntityDefinition.inherited(BreezeEntity::new, mobEntityBase)
                    .type(EntityType.BREEZE)
                    .height(1.77f).width(0.6f)
                    .build();
            COPPER_GOLEM = EntityDefinition.inherited(CopperGolemEntity::new, mobEntityBase)
                    .type(EntityType.COPPER_GOLEM)
                    .height(0.49f).width(0.98f)
                    .addTranslator(MetadataTypes.WEATHERING_COPPER_STATE, CopperGolemEntity::setWeatheringState)
                    .addTranslator(MetadataTypes.COPPER_GOLEM_STATE, CopperGolemEntity::setGolemState)
                    .property(CopperGolemEntity.CHEST_INTERACTION_PROPERTY)
                    .property(CopperGolemEntity.HAS_FLOWER_PROPERTY)
                    .property(CopperGolemEntity.OXIDATION_LEVEL_STATE_ENUM_PROPERTY)
                    .build();
            CREAKING = EntityDefinition.inherited(CreakingEntity::new, mobEntityBase)
                    .type(EntityType.CREAKING)
                    .height(2.7f).width(0.9f)
                    .addTranslator(MetadataTypes.BOOLEAN, CreakingEntity::setCanMove)
                    .addTranslator(MetadataTypes.BOOLEAN, CreakingEntity::setActive)
                    .addTranslator(MetadataTypes.BOOLEAN, CreakingEntity::setIsTearingDown)
                    .addTranslator(MetadataTypes.OPTIONAL_BLOCK_POS, CreakingEntity::setHomePos)
                    .property(CreakingEntity.STATE_PROPERTY)
                    .property(CreakingEntity.SWAYING_TICKS_PROPERTY)
                    .build();
            CREEPER = EntityDefinition.inherited(CreeperEntity::new, mobEntityBase)
                    .type(EntityType.CREEPER)
                    .height(1.7f).width(0.6f)
                    .addTranslator(MetadataTypes.INT, CreeperEntity::setSwelling)
                    .addTranslator(MetadataTypes.BOOLEAN, (entity, entityMetadata) -> entity.setFlag(EntityFlag.POWERED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .addTranslator(MetadataTypes.BOOLEAN, CreeperEntity::setIgnited)
                    .build();
            ENDERMAN = EntityDefinition.inherited(EndermanEntity::new, mobEntityBase)
                    .type(EntityType.ENDERMAN)
                    .height(2.9f).width(0.6f)
                    .addTranslator(MetadataTypes.OPTIONAL_BLOCK_STATE, EndermanEntity::setCarriedBlock)
                    .addTranslator(MetadataTypes.BOOLEAN, EndermanEntity::setScreaming)
                    .addTranslator(MetadataTypes.BOOLEAN, EndermanEntity::setAngry)
                    .build();
            ENDERMITE = EntityDefinition.inherited(MonsterEntity::new, mobEntityBase)
                    .type(EntityType.ENDERMITE)
                    .height(0.3f).width(0.4f)
                    .build();
            ENDER_DRAGON = EntityDefinition.inherited(EnderDragonEntity::new, mobEntityBase)
                    .type(EntityType.ENDER_DRAGON)
                    .addTranslator(MetadataTypes.INT, EnderDragonEntity::setPhase)
                    .build();
            GHAST = EntityDefinition.inherited(GhastEntity::new, mobEntityBase)
                    .type(EntityType.GHAST)
                    .heightAndWidth(4.0f)
                    .addTranslator(MetadataTypes.BOOLEAN, GhastEntity::setGhastAttacking)
                    .build();
            GIANT = EntityDefinition.inherited(GiantEntity::new, mobEntityBase)
                    .type(EntityType.GIANT)
                    .height(1.8f).width(1.6f)
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
                    .addTranslator(MetadataTypes.INT, PhantomEntity::setPhantomScale)
                    .build();
            SILVERFISH = EntityDefinition.inherited(MonsterEntity::new, mobEntityBase)
                    .type(EntityType.SILVERFISH)
                    .height(0.3f).width(0.4f)
                    .build();
            SHULKER = EntityDefinition.inherited(ShulkerEntity::new, mobEntityBase)
                    .type(EntityType.SHULKER)
                    .heightAndWidth(1f)
                    .addTranslator(MetadataTypes.DIRECTION, ShulkerEntity::setAttachedFace)
                    .addTranslator(MetadataTypes.BYTE, ShulkerEntity::setShulkerHeight)
                    .addTranslator(MetadataTypes.BYTE, ShulkerEntity::setShulkerColor)
                    .build();
            SKELETON = EntityDefinition.inherited(SkeletonEntity::new, mobEntityBase)
                    .type(EntityType.SKELETON)
                    .height(1.8f).width(0.6f)
                    .addTranslator(MetadataTypes.BOOLEAN, SkeletonEntity::setConvertingToStray)
                    .build();
            SNOW_GOLEM = EntityDefinition.inherited(SnowGolemEntity::new, mobEntityBase)
                    .type(EntityType.SNOW_GOLEM)
                    .height(1.9f).width(0.7f)
                    .addTranslator(MetadataTypes.BYTE, SnowGolemEntity::setSnowGolemFlags)
                    .build();
            SPIDER = EntityDefinition.inherited(SpiderEntity::new, mobEntityBase)
                    .type(EntityType.SPIDER)
                    .height(0.9f).width(1.4f)
                    .addTranslator(MetadataTypes.BYTE, SpiderEntity::setSpiderFlags)
                    .build();
            CAVE_SPIDER = EntityDefinition.inherited(SpiderEntity::new, SPIDER)
                    .type(EntityType.CAVE_SPIDER)
                    .height(0.5f).width(0.7f)
                    .build();
            STRAY = EntityDefinition.inherited(AbstractSkeletonEntity::new, mobEntityBase)
                    .type(EntityType.STRAY)
                    .height(1.8f).width(0.6f)
                    .build();
            PARCHED = EntityDefinition.inherited(AbstractSkeletonEntity::new, mobEntityBase)
                    .type(EntityType.PARCHED)
                    .height(1.8f).width(0.6f)
                    .build();
            VEX = EntityDefinition.inherited(VexEntity::new, mobEntityBase)
                    .type(EntityType.VEX)
                    .height(0.8f).width(0.4f)
                    .addTranslator(MetadataTypes.BYTE, VexEntity::setVexFlags)
                    .build();
            WARDEN = EntityDefinition.inherited(WardenEntity::new, mobEntityBase)
                    .type(EntityType.WARDEN)
                    .height(2.9f).width(0.9f)
                    .addTranslator(MetadataTypes.INT, WardenEntity::setAngerLevel)
                    .build();
            WITHER = EntityDefinition.inherited(WitherEntity::new, mobEntityBase)
                    .type(EntityType.WITHER)
                    .height(3.5f).width(0.9f)
                    .addTranslator(MetadataTypes.INT, WitherEntity::setTarget1)
                    .addTranslator(MetadataTypes.INT, WitherEntity::setTarget2)
                    .addTranslator(MetadataTypes.INT, WitherEntity::setTarget3)
                    .addTranslator(MetadataTypes.INT, WitherEntity::setInvulnerableTicks)
                    .build();
            WITHER_SKELETON = EntityDefinition.inherited(AbstractSkeletonEntity::new, mobEntityBase)
                    .type(EntityType.WITHER_SKELETON)
                    .height(2.4f).width(0.7f)
                    .build();
            ZOGLIN = EntityDefinition.inherited(ZoglinEntity::new, mobEntityBase)
                    .type(EntityType.ZOGLIN)
                    .height(1.4f).width(1.3965f)
                    .addTranslator(MetadataTypes.BOOLEAN, ZoglinEntity::setBaby)
                    .build();
            ZOMBIE = EntityDefinition.inherited(ZombieEntity::new, mobEntityBase)
                    .type(EntityType.ZOMBIE)
                    .height(1.8f).width(0.6f)
                    .addTranslator(MetadataTypes.BOOLEAN, ZombieEntity::setZombieBaby)
                    .addTranslator(null) // "set special type", doesn't do anything
                    .addTranslator(MetadataTypes.BOOLEAN, ZombieEntity::setConvertingToDrowned)
                    .build();
            ZOMBIE_VILLAGER = EntityDefinition.inherited(ZombieVillagerEntity::new, ZOMBIE)
                    .type(EntityType.ZOMBIE_VILLAGER)
                    .height(1.8f).width(0.6f)
                    .identifier("minecraft:zombie_villager_v2")
                    .addTranslator(MetadataTypes.BOOLEAN, ZombieVillagerEntity::setTransforming)
                    .addTranslator(MetadataTypes.VILLAGER_DATA, ZombieVillagerEntity::setZombieVillagerData)
                    .build();
            ZOMBIFIED_PIGLIN = EntityDefinition.inherited(ZombifiedPiglinEntity::new, ZOMBIE) //TODO test how zombie entity metadata is handled?
                    .type(EntityType.ZOMBIFIED_PIGLIN)
                    .height(1.95f).width(0.6f)
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
                    .addTranslator(MetadataTypes.INT, GuardianEntity::setGuardianTarget)
                    .build();
            ELDER_GUARDIAN = EntityDefinition.inherited(ElderGuardianEntity::new, GUARDIAN)
                    .type(EntityType.ELDER_GUARDIAN)
                    .heightAndWidth(1.9975f)
                    .build();

            SLIME = EntityDefinition.inherited(SlimeEntity::new, mobEntityBase)
                    .type(EntityType.SLIME)
                    .heightAndWidth(0.51f)
                    .addTranslator(MetadataTypes.INT, SlimeEntity::setSlimeScale)
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
                    .addTranslator(MetadataTypes.INT, PufferFishEntity::setPufferfishSize)
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
                    .addTranslator(MetadataTypes.INT, TropicalFishEntity::setFishVariant)
                    .build();

            EntityDefinition<BasePiglinEntity> abstractPiglinEntityBase = EntityDefinition.inherited(BasePiglinEntity::new, mobEntityBase)
                    .addTranslator(MetadataTypes.BOOLEAN, BasePiglinEntity::setImmuneToZombification)
                    .build();
            PIGLIN = EntityDefinition.inherited(PiglinEntity::new, abstractPiglinEntityBase)
                    .type(EntityType.PIGLIN)
                    .height(1.95f).width(0.6f)
                    .addTranslator(MetadataTypes.BOOLEAN, PiglinEntity::setBaby)
                    .addTranslator(MetadataTypes.BOOLEAN, PiglinEntity::setChargingCrossbow)
                    .addTranslator(MetadataTypes.BOOLEAN, PiglinEntity::setDancing)
                    .build();
            PIGLIN_BRUTE = EntityDefinition.inherited(abstractPiglinEntityBase.factory(), abstractPiglinEntityBase)
                    .type(EntityType.PIGLIN_BRUTE)
                    .height(1.95f).width(0.6f)
                    .build();

            EntityDefinition<RaidParticipantEntity> raidParticipantEntityBase = EntityDefinition.inherited(RaidParticipantEntity::new, mobEntityBase)
                    .addTranslator(null) // Celebrating //TODO
                    .build();
            EntityDefinition<SpellcasterIllagerEntity> spellcasterEntityBase = EntityDefinition.inherited(SpellcasterIllagerEntity::new, raidParticipantEntityBase)
                    .addTranslator(MetadataTypes.BYTE, SpellcasterIllagerEntity::setSpellType)
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
                    .addTranslator(MetadataTypes.BOOLEAN, PillagerEntity::setChargingCrossbow)
                    .build();
            RAVAGER = EntityDefinition.inherited(RavagerEntity::new, raidParticipantEntityBase)
                    .type(EntityType.RAVAGER)
                    .height(1.9f).width(1.2f)
                    .build();
            VINDICATOR = EntityDefinition.inherited(VindicatorEntity::new, raidParticipantEntityBase)
                    .type(EntityType.VINDICATOR)
                    .height(1.8f).width(0.6f)
                    .build();
            WITCH = EntityDefinition.inherited(raidParticipantEntityBase.factory(), raidParticipantEntityBase)
                    .type(EntityType.WITCH)
                    .height(1.8f).width(0.6f)
                    .addTranslator(null) // Using item
                    .build();
        }

        EntityDefinition<AgeableEntity> ageableEntityBase = EntityDefinition.inherited(AgeableEntity::new, mobEntityBase)
                .addTranslator(MetadataTypes.BOOLEAN, AgeableEntity::setBaby)
                .build();


        {
            ARMADILLO = EntityDefinition.inherited(ArmadilloEntity::new, ageableEntityBase)
                    .type(EntityType.ARMADILLO)
                    .height(0.65f).width(0.7f)
                    .property(ArmadilloEntity.STATE_PROPERTY)
                    .addTranslator(MetadataTypes.ARMADILLO_STATE, ArmadilloEntity::setArmadilloState)
                    .build();
            AXOLOTL = EntityDefinition.inherited(AxolotlEntity::new, ageableEntityBase)
                    .type(EntityType.AXOLOTL)
                    .height(0.42f).width(0.7f)
                    .addTranslator(MetadataTypes.INT, AxolotlEntity::setVariant)
                    .addTranslator(MetadataTypes.BOOLEAN, AxolotlEntity::setPlayingDead)
                    .addTranslator(null) // From bucket
                    .build();
            BEE = EntityDefinition.inherited(BeeEntity::new, ageableEntityBase)
                    .type(EntityType.BEE)
                    .heightAndWidth(0.6f)
                    .property(BeeEntity.NECTAR_PROPERTY)
                    .addTranslator(MetadataTypes.BYTE, BeeEntity::setBeeFlags)
                    .addTranslator(MetadataTypes.LONG, BeeEntity::setAngerTime)
                    .build();
            CHICKEN = EntityDefinition.inherited(ChickenEntity::new, ageableEntityBase)
                    .type(EntityType.CHICKEN)
                    .height(0.7f).width(0.4f)
                    .property(TemperatureVariantAnimal.TEMPERATE_VARIANT_PROPERTY)
                    .addTranslator(MetadataTypes.CHICKEN_VARIANT, ChickenEntity::setVariant)
                    .build();
            COW = EntityDefinition.inherited(CowEntity::new, ageableEntityBase)
                    .type(EntityType.COW)
                    .height(1.4f).width(0.9f)
                    .property(TemperatureVariantAnimal.TEMPERATE_VARIANT_PROPERTY)
                    .addTranslator(MetadataTypes.COW_VARIANT, CowEntity::setVariant)
                    .build();
            FOX = EntityDefinition.inherited(FoxEntity::new, ageableEntityBase)
                    .type(EntityType.FOX)
                    .height(0.7f).width(0.6f)
                    .addTranslator(MetadataTypes.INT, FoxEntity::setFoxVariant)
                    .addTranslator(MetadataTypes.BYTE, FoxEntity::setFoxFlags)
                    .addTranslator(null) // Trusted player 1
                    .addTranslator(null) // Trusted player 2
                    .build();
            FROG = EntityDefinition.inherited(FrogEntity::new, ageableEntityBase)
                    .type(EntityType.FROG)
                    .heightAndWidth(0.5f)
                    .addTranslator(MetadataTypes.FROG_VARIANT, FrogEntity::setVariant)
                    .addTranslator(MetadataTypes.OPTIONAL_UNSIGNED_INT, FrogEntity::setTongueTarget)
                    .build();
            HAPPY_GHAST = EntityDefinition.inherited(HappyGhastEntity::new, ageableEntityBase)
                    .type(EntityType.HAPPY_GHAST)
                    .heightAndWidth(4f)
                    .property(HappyGhastEntity.CAN_MOVE_PROPERTY)
                    .addTranslator(null) // Is leash holder
                    .addTranslator(MetadataTypes.BOOLEAN, HappyGhastEntity::setStaysStill)
                    .build();
            HOGLIN = EntityDefinition.inherited(HoglinEntity::new, ageableEntityBase)
                    .type(EntityType.HOGLIN)
                    .height(1.4f).width(1.3965f)
                    .addTranslator(MetadataTypes.BOOLEAN, HoglinEntity::setImmuneToZombification)
                    .build();
            GOAT = EntityDefinition.inherited(GoatEntity::new, ageableEntityBase)
                    .type(EntityType.GOAT)
                    .height(1.3f).width(0.9f)
                    .addTranslator(MetadataTypes.BOOLEAN, GoatEntity::setScreamer)
                    .addTranslator(MetadataTypes.BOOLEAN, GoatEntity::setHasLeftHorn)
                    .addTranslator(MetadataTypes.BOOLEAN, GoatEntity::setHasRightHorn)
                    .build();
            MOOSHROOM = EntityDefinition.inherited(MooshroomEntity::new, ageableEntityBase)
                    .type(EntityType.MOOSHROOM)
                    .height(1.4f).width(0.9f)
                    .addTranslator(MetadataTypes.INT, MooshroomEntity::setMooshroomVariant)
                    .build();
            OCELOT = EntityDefinition.inherited(OcelotEntity::new, ageableEntityBase)
                    .type(EntityType.OCELOT)
                    .height(0.7f).width(0.6f)
                    .addTranslator(MetadataTypes.BOOLEAN, (ocelotEntity, entityMetadata) -> ocelotEntity.setFlag(EntityFlag.TRUSTING, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();
            PANDA = EntityDefinition.inherited(PandaEntity::new, ageableEntityBase)
                    .type(EntityType.PANDA)
                    .height(1.25f).width(1.125f)
                    .addTranslator(null) // Unhappy counter
                    .addTranslator(null) // Sneeze counter
                    .addTranslator(MetadataTypes.INT, PandaEntity::setEatingCounter)
                    .addTranslator(MetadataTypes.BYTE, PandaEntity::setMainGene)
                    .addTranslator(MetadataTypes.BYTE, PandaEntity::setHiddenGene)
                    .addTranslator(MetadataTypes.BYTE, PandaEntity::setPandaFlags)
                    .build();
            PIG = EntityDefinition.inherited(PigEntity::new, ageableEntityBase)
                    .type(EntityType.PIG)
                    .heightAndWidth(0.9f)
                    .property(TemperatureVariantAnimal.TEMPERATE_VARIANT_PROPERTY)
                    .addTranslator(MetadataTypes.INT, PigEntity::setBoost)
                    .addTranslator(MetadataTypes.PIG_VARIANT, PigEntity::setVariant)
                    .build();
            POLAR_BEAR = EntityDefinition.inherited(PolarBearEntity::new, ageableEntityBase)
                    .type(EntityType.POLAR_BEAR)
                    .height(1.4f).width(1.3f)
                    .addTranslator(MetadataTypes.BOOLEAN, (entity, entityMetadata) -> entity.setFlag(EntityFlag.STANDING, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();
            RABBIT = EntityDefinition.inherited(RabbitEntity::new, ageableEntityBase)
                    .type(EntityType.RABBIT)
                    .height(0.5f).width(0.4f)
                    .addTranslator(MetadataTypes.INT, RabbitEntity::setRabbitVariant)
                    .build();
            SHEEP = EntityDefinition.inherited(SheepEntity::new, ageableEntityBase)
                    .type(EntityType.SHEEP)
                    .height(1.3f).width(0.9f)
                    .addTranslator(MetadataTypes.BYTE, SheepEntity::setSheepFlags)
                    .build();
            SNIFFER = EntityDefinition.inherited(SnifferEntity::new, ageableEntityBase)
                    .type(EntityType.SNIFFER)
                    .height(1.75f).width(1.9f)
                    .addTranslator(MetadataTypes.SNIFFER_STATE, SnifferEntity::setSnifferState)
                    .addTranslator(null) // Integer, drop seed at tick
                    .build();
            STRIDER = EntityDefinition.inherited(StriderEntity::new, ageableEntityBase)
                    .type(EntityType.STRIDER)
                    .height(1.7f).width(0.9f)
                    .addTranslator(MetadataTypes.INT, StriderEntity::setBoost)
                    .addTranslator(MetadataTypes.BOOLEAN, StriderEntity::setCold)
                    .build();
            TURTLE = EntityDefinition.inherited(TurtleEntity::new, ageableEntityBase)
                    .type(EntityType.TURTLE)
                    .height(0.4f).width(1.2f)
                    .addTranslator(null) // Home position
                    .addTranslator(MetadataTypes.BOOLEAN, TurtleEntity::setPregnant)
                    .addTranslator(MetadataTypes.BOOLEAN, TurtleEntity::setLayingEgg)
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
                    .identifier("minecraft:villager_v2")
                    .addTranslator(MetadataTypes.VILLAGER_DATA, VillagerEntity::setVillagerData)
                    .build();
            WANDERING_TRADER = EntityDefinition.inherited(abstractVillagerEntityBase.factory(), abstractVillagerEntityBase)
                    .type(EntityType.WANDERING_TRADER)
                    .height(1.8f).width(0.6f)
                    .build();
        }


        {
            DOLPHIN = EntityDefinition.inherited(DolphinEntity::new, ageableEntityBase)
                .type(EntityType.DOLPHIN)
                .height(0.6f).width(0.9f)

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


        {
            EntityDefinition<AbstractHorseEntity> abstractHorseEntityBase = EntityDefinition.inherited(AbstractHorseEntity::new, ageableEntityBase)
                    .addTranslator(MetadataTypes.BYTE, AbstractHorseEntity::setHorseFlags)
                    .build();
            CAMEL = EntityDefinition.inherited(CamelEntity::new, abstractHorseEntityBase)
                    .type(EntityType.CAMEL)
                    .height(2.375f).width(1.7f)
                    .addTranslator(MetadataTypes.BOOLEAN, CamelEntity::setDashing)
                    .addTranslator(MetadataTypes.LONG, CamelEntity::setLastPoseTick)
                    .build();
            CAMEL_HUSK = EntityDefinition.inherited(CamelHuskEntity::new, CAMEL)
                    .type(EntityType.CAMEL_HUSK)
                    .build();
            HORSE = EntityDefinition.inherited(HorseEntity::new, abstractHorseEntityBase)
                    .type(EntityType.HORSE)
                    .height(1.6f).width(1.3965f)
                    .addTranslator(MetadataTypes.INT, HorseEntity::setHorseVariant)
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
                    .addTranslator(MetadataTypes.BOOLEAN, (horseEntity, entityMetadata) -> horseEntity.setFlag(EntityFlag.CHESTED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
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
                    .addTranslator(MetadataTypes.INT, LlamaEntity::setStrength)
                    .addTranslator(MetadataTypes.INT, (entity, entityMetadata) -> entity.getDirtyMetadata().put(EntityDataTypes.VARIANT, entityMetadata.getValue()))
                    .build();
            TRADER_LLAMA = EntityDefinition.inherited(TraderLlamaEntity::new, LLAMA)
                    .type(EntityType.TRADER_LLAMA)
                    .identifier("minecraft:llama")
                    .build();
        }

        EntityDefinition<TameableEntity> tameableEntityBase = EntityDefinition.<TameableEntity>inherited(null, ageableEntityBase) // No factory, is abstract
                .addTranslator(MetadataTypes.BYTE, TameableEntity::setTameableFlags)
                .addTranslator(MetadataTypes.OPTIONAL_LIVING_ENTITY_REFERENCE, TameableEntity::setOwner)
                .build();


        {
            EntityDefinition<AbstractNautilusEntity> abstractNautilusBase = EntityDefinition.<AbstractNautilusEntity>inherited(null, tameableEntityBase) // No factory, is abstract
                .width(0.95f).height(0.875f)
                .addTranslator(MetadataTypes.BOOLEAN, AbstractNautilusEntity::setDashing)
                .build();

            NAUTILUS = EntityDefinition.inherited(NautilusEntity::new, abstractNautilusBase)
                .type(EntityType.NAUTILUS)
                .identifier("minecraft:nautilus")
                .build();

            ZOMBIE_NAUTILUS = EntityDefinition.inherited(ZombieNautilusEntity::new, abstractNautilusBase)
                .type(EntityType.ZOMBIE_NAUTILUS)
                .identifier("minecraft:zombie_nautilus")
                .property(ZombieNautilusEntity.VARIANT_ENUM_PROPERTY)
                .addTranslator(MetadataTypes.ZOMBIE_NAUTILUS_VARIANT, ZombieNautilusEntity::setVariant)
                .build();
        }

        CAT = EntityDefinition.inherited(CatEntity::new, tameableEntityBase)
                .type(EntityType.CAT)
                .height(0.35f).width(0.3f)
                .addTranslator(MetadataTypes.CAT_VARIANT, CatEntity::setVariant)
                .addTranslator(MetadataTypes.BOOLEAN, CatEntity::setResting)
                .addTranslator(null) // "resting state one" //TODO
                .addTranslator(MetadataTypes.INT, CatEntity::setCollarColor)
                .build();
        PARROT = EntityDefinition.inherited(ParrotEntity::new, tameableEntityBase)
                .type(EntityType.PARROT)
                .height(0.9f).width(0.5f)
                .addTranslator(MetadataTypes.INT, (parrotEntity, entityMetadata) -> parrotEntity.getDirtyMetadata().put(EntityDataTypes.VARIANT, entityMetadata.getValue())) // Parrot color
                .build();
        WOLF = EntityDefinition.inherited(WolfEntity::new, tameableEntityBase)
                .type(EntityType.WOLF)
                .height(0.85f).width(0.6f)
                .property(WolfEntity.SOUND_VARIANT)

                .addTranslator(MetadataTypes.BOOLEAN, (wolfEntity, entityMetadata) -> wolfEntity.setFlag(EntityFlag.INTERESTED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                .addTranslator(MetadataTypes.INT, WolfEntity::setCollarColor)
                .addTranslator(MetadataTypes.LONG, WolfEntity::setWolfAngerTime)
                .addTranslator(MetadataTypes.WOLF_VARIANT, WolfEntity::setVariant)
                .addTranslator(null) // sound variant; these aren't clientsided anyways... right??
                .build();


        ENDER_DRAGON_PART = EntityDefinition.<EnderDragonPartEntity>builder(null)
                .identifier("minecraft:armor_stand") // Emulated
                .build(false); // Never sent over the network

        Registries.JAVA_ENTITY_IDENTIFIERS.get().put("minecraft:marker", null); // We don't need an entity definition for this as it is never sent over the network
    }

    private static EntityDefinition<BoatEntity> buildBoat(EntityDefinition<BoatEntity> base, EntityType entityType, BoatEntity.BoatVariant variant) {
        return EntityDefinition.inherited((context) -> new BoatEntity(context, variant), base)
            .type(entityType)
            .identifier("minecraft:boat")
            .build();
    }

    private static EntityDefinition<ChestBoatEntity> buildChestBoat(EntityDefinition<ChestBoatEntity> base, EntityType entityType, BoatEntity.BoatVariant variant) {
        return EntityDefinition.inherited((context) -> new ChestBoatEntity(context, variant), base)
            .type(entityType)
            .identifier("minecraft:chest_boat")
            .build();
    }

    public static void init() {

        GeyserImpl.getInstance().getEventBus().fire(new GeyserDefineEntityPropertiesEvent() {
            override public GeyserFloatEntityProperty registerFloatProperty(Identifier identifier, Identifier propertyId, float min, float max, Float defaultValue) {
                Objects.requireNonNull(identifier);
                Objects.requireNonNull(propertyId);
                if (propertyId.vanilla()) {
                    throw new IllegalArgumentException("Cannot register custom property in vanilla namespace! " + propertyId);
                }
                FloatProperty property = new FloatProperty(propertyId, max, min, defaultValue);
                registerProperty(identifier, property);
                return property;
            }

            override public IntProperty registerIntegerProperty(Identifier identifier, Identifier propertyId, int min, int max, @Nullable Integer defaultValue) {
                Objects.requireNonNull(identifier);
                Objects.requireNonNull(propertyId);
                if (propertyId.vanilla()) {
                    throw new IllegalArgumentException("Cannot register custom property in vanilla namespace! " + propertyId);
                }
                IntProperty property = new IntProperty(propertyId, max, min, defaultValue);
                registerProperty(identifier, property);
                return property;
            }

            override public BooleanProperty registerBooleanProperty(@NonNull Identifier identifier, @NonNull Identifier propertyId, bool defaultValue) {
                Objects.requireNonNull(identifier);
                Objects.requireNonNull(propertyId);
                if (propertyId.vanilla()) {
                    throw new IllegalArgumentException("Cannot register custom property in vanilla namespace! " + propertyId);
                }
                BooleanProperty property = new BooleanProperty(propertyId, defaultValue);
                registerProperty(identifier, property);
                return property;
            }

            override public <E extends Enum<E>> EnumProperty<E> registerEnumProperty(@NonNull Identifier identifier, @NonNull Identifier propertyId, @NonNull Class<E> enumClass, @Nullable E defaultValue) {
                Objects.requireNonNull(identifier);
                Objects.requireNonNull(propertyId);
                Objects.requireNonNull(enumClass);
                if (propertyId.vanilla()) {
                    throw new IllegalArgumentException("Cannot register custom property in vanilla namespace! " + propertyId);
                }
                EnumProperty<E> property = new EnumProperty<>(propertyId, enumClass, defaultValue == null ? enumClass.getEnumConstants()[0] : defaultValue);
                registerProperty(identifier, property);
                return property;
            }

            override public GeyserStringEnumProperty registerEnumProperty(@NonNull Identifier identifier, @NonNull Identifier propertyId, @NonNull List<std::string> values, @Nullable std::string defaultValue) {
                Objects.requireNonNull(identifier);
                Objects.requireNonNull(propertyId);
                Objects.requireNonNull(values);
                if (propertyId.vanilla()) {
                    throw new IllegalArgumentException("Cannot register custom property in vanilla namespace! " + propertyId);
                }
                StringEnumProperty property = new StringEnumProperty(propertyId, values, defaultValue);
                registerProperty(identifier, property);
                return property;
            }

            override public Collection<GeyserEntityProperty<?>> properties(@NonNull Identifier identifier) {
                Objects.requireNonNull(identifier);
                var definition = Registries.JAVA_ENTITY_IDENTIFIERS.get(identifier.toString());
                if (definition == null) {
                    throw new IllegalArgumentException("Unknown entity type: " + identifier);
                }
                return List.copyOf(definition.registeredProperties().getProperties());
            }
        });

        for (var definition : Registries.ENTITY_DEFINITIONS.get().values()) {
            if (!definition.registeredProperties().isEmpty()) {
                Registries.BEDROCK_ENTITY_PROPERTIES.get().add(definition.registeredProperties().toNbtMap(definition.identifier()));
            }
        }
    }

    private static <T> void registerProperty(Identifier entityType, PropertyType<T, ?> property) {
        var definition = Registries.JAVA_ENTITY_IDENTIFIERS.get(entityType.toString());
        if (definition == null) {
            throw new IllegalArgumentException("Unknown entity type: " + entityType);
        }

        definition.registeredProperties().add(entityType.toString(), property);
    }

    private EntityDefinitions() {
    }
}
