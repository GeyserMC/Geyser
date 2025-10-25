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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.entity.property.GeyserEntityProperty;
import org.geysermc.geyser.api.entity.property.type.GeyserFloatEntityProperty;
import org.geysermc.geyser.api.entity.property.type.GeyserStringEnumProperty;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.factory.EntityFactory;
import org.geysermc.geyser.entity.properties.type.BooleanProperty;
import org.geysermc.geyser.entity.properties.type.EnumProperty;
import org.geysermc.geyser.entity.properties.type.FloatProperty;
import org.geysermc.geyser.entity.properties.type.IntProperty;
import org.geysermc.geyser.entity.properties.type.PropertyType;
import org.geysermc.geyser.entity.properties.type.StringEnumProperty;
import org.geysermc.geyser.entity.type.AbstractArrowEntity;
import org.geysermc.geyser.entity.type.AbstractWindChargeEntity;
import org.geysermc.geyser.entity.type.AreaEffectCloudEntity;
import org.geysermc.geyser.entity.type.ArrowEntity;
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.ChestBoatEntity;
import org.geysermc.geyser.entity.type.CommandBlockMinecartEntity;
import org.geysermc.geyser.entity.type.DisplayBaseEntity;
import org.geysermc.geyser.entity.type.EnderCrystalEntity;
import org.geysermc.geyser.entity.type.EnderEyeEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.EvokerFangsEntity;
import org.geysermc.geyser.entity.type.ExpOrbEntity;
import org.geysermc.geyser.entity.type.FallingBlockEntity;
import org.geysermc.geyser.entity.type.FireballEntity;
import org.geysermc.geyser.entity.type.FireworkEntity;
import org.geysermc.geyser.entity.type.FishingHookEntity;
import org.geysermc.geyser.entity.type.FurnaceMinecartEntity;
import org.geysermc.geyser.entity.type.HangingEntity;
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
import org.geysermc.geyser.entity.type.ThrowableEggEntity;
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
import org.geysermc.geyser.entity.type.living.CopperGolemEntity;
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
import org.geysermc.geyser.entity.type.living.animal.FoxEntity;
import org.geysermc.geyser.entity.type.living.animal.FrogEntity;
import org.geysermc.geyser.entity.type.living.animal.GoatEntity;
import org.geysermc.geyser.entity.type.living.animal.HappyGhastEntity;
import org.geysermc.geyser.entity.type.living.animal.HoglinEntity;
import org.geysermc.geyser.entity.type.living.animal.MooshroomEntity;
import org.geysermc.geyser.entity.type.living.animal.OcelotEntity;
import org.geysermc.geyser.entity.type.living.animal.PandaEntity;
import org.geysermc.geyser.entity.type.living.animal.PolarBearEntity;
import org.geysermc.geyser.entity.type.living.animal.PufferFishEntity;
import org.geysermc.geyser.entity.type.living.animal.RabbitEntity;
import org.geysermc.geyser.entity.type.living.animal.SheepEntity;
import org.geysermc.geyser.entity.type.living.animal.SnifferEntity;
import org.geysermc.geyser.entity.type.living.animal.StriderEntity;
import org.geysermc.geyser.entity.type.living.animal.TropicalFishEntity;
import org.geysermc.geyser.entity.type.living.animal.TurtleEntity;
import org.geysermc.geyser.entity.type.living.animal.farm.ChickenEntity;
import org.geysermc.geyser.entity.type.living.animal.farm.CowEntity;
import org.geysermc.geyser.entity.type.living.animal.farm.PigEntity;
import org.geysermc.geyser.entity.type.living.animal.farm.TemperatureVariantAnimal;
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
import org.geysermc.geyser.entity.type.living.monster.CreakingEntity;
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
import org.geysermc.geyser.entity.type.player.AvatarEntity;
import org.geysermc.geyser.entity.type.player.MannequinEntity;
import org.geysermc.geyser.entity.type.player.PlayerEntity;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.translator.text.MessageTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.BuiltinEntityType;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class EntityDefinitions {
    public static final VanillaEntityDefinition<BoatEntity> ACACIA_BOAT;
    public static final VanillaEntityDefinition<ChestBoatEntity> ACACIA_CHEST_BOAT;
    public static final VanillaEntityDefinition<AllayEntity> ALLAY;
    public static final VanillaEntityDefinition<AreaEffectCloudEntity> AREA_EFFECT_CLOUD;
    public static final VanillaEntityDefinition<ArmadilloEntity> ARMADILLO;
    public static final VanillaEntityDefinition<ArmorStandEntity> ARMOR_STAND;
    public static final VanillaEntityDefinition<ArrowEntity> ARROW;
    public static final VanillaEntityDefinition<AxolotlEntity> AXOLOTL;
    public static final VanillaEntityDefinition<BoatEntity> BAMBOO_RAFT;
    public static final VanillaEntityDefinition<ChestBoatEntity> BAMBOO_CHEST_RAFT;
    public static final VanillaEntityDefinition<BatEntity> BAT;
    public static final VanillaEntityDefinition<BeeEntity> BEE;
    public static final VanillaEntityDefinition<BoatEntity> BIRCH_BOAT;
    public static final VanillaEntityDefinition<ChestBoatEntity> BIRCH_CHEST_BOAT;
    public static final VanillaEntityDefinition<BlazeEntity> BLAZE;
    public static final VanillaEntityDefinition<BoggedEntity> BOGGED;
    public static final VanillaEntityDefinition<BreezeEntity> BREEZE;
    public static final VanillaEntityDefinition<AbstractWindChargeEntity> BREEZE_WIND_CHARGE;
    public static final VanillaEntityDefinition<CamelEntity> CAMEL;
    public static final VanillaEntityDefinition<CatEntity> CAT;
    public static final VanillaEntityDefinition<SpiderEntity> CAVE_SPIDER;
    public static final VanillaEntityDefinition<BoatEntity> CHERRY_BOAT;
    public static final VanillaEntityDefinition<ChestBoatEntity> CHERRY_CHEST_BOAT;
    public static final VanillaEntityDefinition<MinecartEntity> CHEST_MINECART;
    public static final VanillaEntityDefinition<ChickenEntity> CHICKEN;
    public static final VanillaEntityDefinition<AbstractFishEntity> COD;
    public static final VanillaEntityDefinition<CopperGolemEntity> COPPER_GOLEM;
    public static final VanillaEntityDefinition<CommandBlockMinecartEntity> COMMAND_BLOCK_MINECART;
    public static final VanillaEntityDefinition<CowEntity> COW;
    public static final VanillaEntityDefinition<CreakingEntity> CREAKING;
    public static final VanillaEntityDefinition<CreeperEntity> CREEPER;
    public static final VanillaEntityDefinition<BoatEntity> DARK_OAK_BOAT;
    public static final VanillaEntityDefinition<ChestBoatEntity> DARK_OAK_CHEST_BOAT;
    public static final VanillaEntityDefinition<DolphinEntity> DOLPHIN;
    public static final VanillaEntityDefinition<ChestedHorseEntity> DONKEY;
    public static final VanillaEntityDefinition<FireballEntity> DRAGON_FIREBALL;
    public static final VanillaEntityDefinition<ZombieEntity> DROWNED;
    public static final VanillaEntityDefinition<ThrowableEggEntity> EGG;
    public static final VanillaEntityDefinition<ElderGuardianEntity> ELDER_GUARDIAN;
    public static final VanillaEntityDefinition<EndermanEntity> ENDERMAN;
    public static final VanillaEntityDefinition<MonsterEntity> ENDERMITE;
    public static final VanillaEntityDefinition<EnderDragonEntity> ENDER_DRAGON;
    public static final VanillaEntityDefinition<ThrowableItemEntity> ENDER_PEARL;
    public static final VanillaEntityDefinition<EnderCrystalEntity> END_CRYSTAL;
    public static final VanillaEntityDefinition<SpellcasterIllagerEntity> EVOKER;
    public static final VanillaEntityDefinition<EvokerFangsEntity> EVOKER_FANGS;
    public static final VanillaEntityDefinition<ThrowableItemEntity> EXPERIENCE_BOTTLE;
    public static final VanillaEntityDefinition<ExpOrbEntity> EXPERIENCE_ORB;
    public static final VanillaEntityDefinition<EnderEyeEntity> EYE_OF_ENDER;
    public static final VanillaEntityDefinition<FallingBlockEntity> FALLING_BLOCK;
    public static final VanillaEntityDefinition<FireballEntity> FIREBALL;
    public static final VanillaEntityDefinition<FireworkEntity> FIREWORK_ROCKET;
    public static final VanillaEntityDefinition<FishingHookEntity> FISHING_BOBBER;
    public static final VanillaEntityDefinition<FoxEntity> FOX;
    public static final VanillaEntityDefinition<FrogEntity> FROG;
    public static final VanillaEntityDefinition<FurnaceMinecartEntity> FURNACE_MINECART; // Not present on Bedrock
    public static final VanillaEntityDefinition<GhastEntity> GHAST;
    public static final VanillaEntityDefinition<GiantEntity> GIANT;
    public static final VanillaEntityDefinition<ItemFrameEntity> GLOW_ITEM_FRAME;
    public static final VanillaEntityDefinition<GlowSquidEntity> GLOW_SQUID;
    public static final VanillaEntityDefinition<GoatEntity> GOAT;
    public static final VanillaEntityDefinition<GuardianEntity> GUARDIAN;
    public static final VanillaEntityDefinition<HappyGhastEntity> HAPPY_GHAST;
    public static final VanillaEntityDefinition<HoglinEntity> HOGLIN;
    public static final VanillaEntityDefinition<MinecartEntity> HOPPER_MINECART;
    public static final VanillaEntityDefinition<HorseEntity> HORSE;
    public static final VanillaEntityDefinition<ZombieEntity> HUSK;
    public static final VanillaEntityDefinition<SpellcasterIllagerEntity> ILLUSIONER; // Not present on Bedrock
    public static final VanillaEntityDefinition<InteractionEntity> INTERACTION;
    public static final VanillaEntityDefinition<IronGolemEntity> IRON_GOLEM;
    public static final VanillaEntityDefinition<ItemEntity> ITEM;
    public static final VanillaEntityDefinition<ItemFrameEntity> ITEM_FRAME;
    public static final VanillaEntityDefinition<BoatEntity> JUNGLE_BOAT;
    public static final VanillaEntityDefinition<ChestBoatEntity> JUNGLE_CHEST_BOAT;
    public static final VanillaEntityDefinition<LeashKnotEntity> LEASH_KNOT;
    public static final VanillaEntityDefinition<LightningEntity> LIGHTNING_BOLT;
    public static final VanillaEntityDefinition<LlamaEntity> LLAMA;
    public static final VanillaEntityDefinition<ThrowableEntity> LLAMA_SPIT;
    public static final VanillaEntityDefinition<MagmaCubeEntity> MAGMA_CUBE;
    public static final VanillaEntityDefinition<BoatEntity> MANGROVE_BOAT;
    public static final VanillaEntityDefinition<ChestBoatEntity> MANGROVE_CHEST_BOAT;
    public static final VanillaEntityDefinition<MannequinEntity> MANNEQUIN;
    public static final VanillaEntityDefinition<MinecartEntity> MINECART;
    public static final VanillaEntityDefinition<MooshroomEntity> MOOSHROOM;
    public static final VanillaEntityDefinition<ChestedHorseEntity> MULE;
    public static final VanillaEntityDefinition<BoatEntity> OAK_BOAT;
    public static final VanillaEntityDefinition<ChestBoatEntity> OAK_CHEST_BOAT;
    public static final VanillaEntityDefinition<OcelotEntity> OCELOT;
    public static final VanillaEntityDefinition<PaintingEntity> PAINTING;
    public static final VanillaEntityDefinition<BoatEntity> PALE_OAK_BOAT;
    public static final VanillaEntityDefinition<ChestBoatEntity> PALE_OAK_CHEST_BOAT;
    public static final VanillaEntityDefinition<PandaEntity> PANDA;
    public static final VanillaEntityDefinition<ParrotEntity> PARROT;
    public static final VanillaEntityDefinition<PhantomEntity> PHANTOM;
    public static final VanillaEntityDefinition<PigEntity> PIG;
    public static final VanillaEntityDefinition<PiglinEntity> PIGLIN;
    public static final VanillaEntityDefinition<BasePiglinEntity> PIGLIN_BRUTE;
    public static final VanillaEntityDefinition<PillagerEntity> PILLAGER;
    public static final VanillaEntityDefinition<PlayerEntity> PLAYER;
    public static final VanillaEntityDefinition<PolarBearEntity> POLAR_BEAR;
    public static final VanillaEntityDefinition<ThrownPotionEntity> SPLASH_POTION;
    public static final VanillaEntityDefinition<ThrownPotionEntity> LINGERING_POTION;
    public static final VanillaEntityDefinition<PufferFishEntity> PUFFERFISH;
    public static final VanillaEntityDefinition<RabbitEntity> RABBIT;
    public static final VanillaEntityDefinition<RavagerEntity> RAVAGER;
    public static final VanillaEntityDefinition<AbstractFishEntity> SALMON;
    public static final VanillaEntityDefinition<SheepEntity> SHEEP;
    public static final VanillaEntityDefinition<ShulkerEntity> SHULKER;
    public static final VanillaEntityDefinition<SnifferEntity> SNIFFER;
    public static final VanillaEntityDefinition<ThrowableEntity> SHULKER_BULLET;
    public static final VanillaEntityDefinition<MonsterEntity> SILVERFISH;
    public static final VanillaEntityDefinition<SkeletonEntity> SKELETON;
    public static final VanillaEntityDefinition<SkeletonHorseEntity> SKELETON_HORSE;
    public static final VanillaEntityDefinition<SlimeEntity> SLIME;
    public static final VanillaEntityDefinition<FireballEntity> SMALL_FIREBALL;
    public static final VanillaEntityDefinition<ThrowableItemEntity> SNOWBALL;
    public static final VanillaEntityDefinition<SnowGolemEntity> SNOW_GOLEM;
    public static final VanillaEntityDefinition<SpawnerMinecartEntity> SPAWNER_MINECART; // Not present on Bedrock
    public static final VanillaEntityDefinition<AbstractArrowEntity> SPECTRAL_ARROW;
    public static final VanillaEntityDefinition<SpiderEntity> SPIDER;
    public static final VanillaEntityDefinition<BoatEntity> SPRUCE_BOAT;
    public static final VanillaEntityDefinition<ChestBoatEntity> SPRUCE_CHEST_BOAT;
    public static final VanillaEntityDefinition<SquidEntity> SQUID;
    public static final VanillaEntityDefinition<AbstractSkeletonEntity> STRAY;
    public static final VanillaEntityDefinition<StriderEntity> STRIDER;
    public static final VanillaEntityDefinition<TadpoleEntity> TADPOLE;
    public static final VanillaEntityDefinition<TextDisplayEntity> TEXT_DISPLAY;
    public static final VanillaEntityDefinition<TNTEntity> TNT;
    public static final VanillaEntityDefinition<MinecartEntity> TNT_MINECART;
    public static final VanillaEntityDefinition<TraderLlamaEntity> TRADER_LLAMA;
    public static final VanillaEntityDefinition<TridentEntity> TRIDENT;
    public static final VanillaEntityDefinition<TropicalFishEntity> TROPICAL_FISH;
    public static final VanillaEntityDefinition<TurtleEntity> TURTLE;
    public static final VanillaEntityDefinition<VexEntity> VEX;
    public static final VanillaEntityDefinition<VillagerEntity> VILLAGER;
    public static final VanillaEntityDefinition<VindicatorEntity> VINDICATOR;
    public static final VanillaEntityDefinition<AbstractMerchantEntity> WANDERING_TRADER;
    public static final VanillaEntityDefinition<WardenEntity> WARDEN;
    public static final VanillaEntityDefinition<AbstractWindChargeEntity> WIND_CHARGE;
    public static final VanillaEntityDefinition<RaidParticipantEntity> WITCH;
    public static final VanillaEntityDefinition<WitherEntity> WITHER;
    public static final VanillaEntityDefinition<AbstractSkeletonEntity> WITHER_SKELETON;
    public static final VanillaEntityDefinition<WitherSkullEntity> WITHER_SKULL;
    public static final VanillaEntityDefinition<WolfEntity> WOLF;
    public static final VanillaEntityDefinition<ZoglinEntity> ZOGLIN;
    public static final VanillaEntityDefinition<ZombieEntity> ZOMBIE;
    public static final VanillaEntityDefinition<ZombieHorseEntity> ZOMBIE_HORSE;
    public static final VanillaEntityDefinition<ZombieVillagerEntity> ZOMBIE_VILLAGER;
    public static final VanillaEntityDefinition<ZombifiedPiglinEntity> ZOMBIFIED_PIGLIN;

    /**
     * Is not sent over the network
     */
    public static final VanillaEntityDefinition<EnderDragonPartEntity> ENDER_DRAGON_PART;
    /**
     * Special Bedrock type
     */
    public static final VanillaEntityDefinition<WitherSkullEntity> WITHER_SKULL_DANGEROUS;

    static {
        EntityDefinition<Entity> entityBase = VanillaEntityDefinition.builder(Entity::new)
                .addTranslator(MetadataTypes.BYTE, Entity::setFlags)
                .addTranslator(MetadataTypes.INT, Entity::setAir) // Air/bubbles
                .addTranslator(MetadataTypes.OPTIONAL_COMPONENT, Entity::setDisplayName)
                .addTranslator(MetadataTypes.BOOLEAN, Entity::setDisplayNameVisible)
                .addTranslator(MetadataTypes.BOOLEAN, Entity::setSilent)
                .addTranslator(MetadataTypes.BOOLEAN, Entity::setGravity)
                .addTranslator(MetadataTypes.POSE, (entity, entityMetadata) -> entity.setPose(entityMetadata.getValue()))
                .addTranslator(MetadataTypes.INT, Entity::setFreezing)
                .build();

        // Extends entity
        {
            AREA_EFFECT_CLOUD = VanillaEntityDefinition.inherited(AreaEffectCloudEntity::new, entityBase)
                    .type(BuiltinEntityType.AREA_EFFECT_CLOUD)
                    .height(0.5f).width(1.0f)
                    .addTranslator(MetadataTypes.FLOAT, AreaEffectCloudEntity::setRadius)
                    .addTranslator(null) // Waiting
                    .addTranslator(MetadataTypes.PARTICLE, AreaEffectCloudEntity::setParticle)
                    .build();
            DRAGON_FIREBALL = VanillaEntityDefinition.inherited(FireballEntity::new, entityBase)
                    .type(BuiltinEntityType.DRAGON_FIREBALL)
                    .heightAndWidth(1.0f)
                    .build();
            END_CRYSTAL = VanillaEntityDefinition.inherited(EnderCrystalEntity::new, entityBase)
                    .type(BuiltinEntityType.END_CRYSTAL)
                    .heightAndWidth(2.0f)
                    .bedrockIdentifier("minecraft:ender_crystal")
                    .addTranslator(MetadataTypes.OPTIONAL_BLOCK_POS, EnderCrystalEntity::setBlockTarget)
                    .addTranslator(MetadataTypes.BOOLEAN,
                            (enderCrystalEntity, entityMetadata) -> enderCrystalEntity.setFlag(EntityFlag.SHOW_BOTTOM, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue())) // There is a base located on the ender crystal
                    .build();
            EXPERIENCE_ORB = VanillaEntityDefinition.inherited(ExpOrbEntity::new, entityBase)
                    .type(BuiltinEntityType.EXPERIENCE_ORB)
                    .addTranslator(null) // int determining xb orb texture
                    .bedrockIdentifier("minecraft:xp_orb")
                    .build();
            EVOKER_FANGS = VanillaEntityDefinition.inherited(EvokerFangsEntity::new, entityBase)
                    .type(BuiltinEntityType.EVOKER_FANGS)
                    .height(0.8f).width(0.5f)
                    .bedrockIdentifier("minecraft:evocation_fang")
                    .build();
            EYE_OF_ENDER = VanillaEntityDefinition.inherited(EnderEyeEntity::new, entityBase)
                    .type(BuiltinEntityType.EYE_OF_ENDER)
                    .heightAndWidth(0.25f)
                    .bedrockIdentifier("minecraft:eye_of_ender_signal")
                    .addTranslator(null)  // Item
                    .build();
            FALLING_BLOCK = VanillaEntityDefinition.<FallingBlockEntity>inherited(null, entityBase)
                    .type(BuiltinEntityType.FALLING_BLOCK)
                    .heightAndWidth(0.98f)
                    .addTranslator(null) // "start block position"
                    .build();
            FIREWORK_ROCKET = VanillaEntityDefinition.inherited(FireworkEntity::new, entityBase)
                    .type(BuiltinEntityType.FIREWORK_ROCKET)
                    .heightAndWidth(0.25f)
                    .bedrockIdentifier("minecraft:fireworks_rocket")
                    .addTranslator(MetadataTypes.ITEM_STACK, FireworkEntity::setFireworkItem)
                    .addTranslator(MetadataTypes.OPTIONAL_UNSIGNED_INT, FireworkEntity::setPlayerGliding)
                    .addTranslator(null) // Shot at angle
                    .build();
            FISHING_BOBBER = VanillaEntityDefinition.<FishingHookEntity>inherited(null, entityBase)
                    .type(BuiltinEntityType.FISHING_BOBBER)
                    .bedrockIdentifier("minecraft:fishing_hook")
                    .addTranslator(MetadataTypes.INT, FishingHookEntity::setHookedEntity)
                    .addTranslator(null) // Biting TODO check
                    .build();
            ITEM = VanillaEntityDefinition.inherited(ItemEntity::new, entityBase)
                    .type(BuiltinEntityType.ITEM)
                    .heightAndWidth(0.25f)
                    .offset(0.125f)
                    .addTranslator(MetadataTypes.ITEM_STACK, ItemEntity::setItem)
                    .build();
            LEASH_KNOT = VanillaEntityDefinition.inherited(LeashKnotEntity::new, entityBase)
                    .type(BuiltinEntityType.LEASH_KNOT)
                    .height(0.5f).width(0.375f)
                    .build();
            LIGHTNING_BOLT = VanillaEntityDefinition.inherited(LightningEntity::new, entityBase)
                    .type(BuiltinEntityType.LIGHTNING_BOLT)
                    .build();
            LLAMA_SPIT = VanillaEntityDefinition.inherited(ThrowableEntity::new, entityBase)
                    .type(BuiltinEntityType.LLAMA_SPIT)
                    .heightAndWidth(0.25f)
                    .build();
            SHULKER_BULLET = VanillaEntityDefinition.inherited(ThrowableEntity::new, entityBase)
                    .type(BuiltinEntityType.SHULKER_BULLET)
                    .heightAndWidth(0.3125f)
                    .build();
            TNT = VanillaEntityDefinition.inherited(TNTEntity::new, entityBase)
                    .type(BuiltinEntityType.TNT)
                    .heightAndWidth(0.98f)
                    .offset(0.49f)
                    .addTranslator(MetadataTypes.INT, TNTEntity::setFuseLength)
                    .build();

            VanillaEntityDefinition<DisplayBaseEntity> displayBase = VanillaEntityDefinition.inherited(DisplayBaseEntity::new, entityBase)
                    .addTranslator(null) // Interpolation delay
                    .addTranslator(null) // Transformation interpolation duration
                    .addTranslator(null) // Position/Rotation interpolation duration
                    .addTranslator(MetadataTypes.VECTOR3, DisplayBaseEntity::setTranslation) // Translation
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
            TEXT_DISPLAY = VanillaEntityDefinition.inherited(TextDisplayEntity::new, displayBase)
                    .type(BuiltinEntityType.TEXT_DISPLAY)
                    .bedrockIdentifier("minecraft:armor_stand")
                    .offset(-0.5f)
                    .addTranslator(MetadataTypes.COMPONENT, TextDisplayEntity::setText)
                    .addTranslator(null) // Line width
                    .addTranslator(null) // Background color
                    .addTranslator(null) // Text opacity
                    .addTranslator(null) // Bit mask
                    .build();

            INTERACTION = VanillaEntityDefinition.inherited(InteractionEntity::new, entityBase)
                    .type(BuiltinEntityType.INTERACTION)
                    .heightAndWidth(1.0f) // default size until server specifies otherwise
                    .bedrockIdentifier("minecraft:armor_stand")
                    .addTranslator(MetadataTypes.FLOAT, InteractionEntity::setWidth)
                    .addTranslator(MetadataTypes.FLOAT, InteractionEntity::setHeight)
                    .addTranslator(MetadataTypes.BOOLEAN, InteractionEntity::setResponse)
                    .build();

            VanillaEntityDefinition<FireballEntity> fireballBase = VanillaEntityDefinition.inherited(FireballEntity::new, entityBase)
                    .addTranslator(null) // Item
                    .build();
            FIREBALL = VanillaEntityDefinition.inherited(FireballEntity::new, fireballBase)
                    .type(BuiltinEntityType.FIREBALL)
                    .heightAndWidth(1.0f)
                    .build();
            SMALL_FIREBALL = VanillaEntityDefinition.inherited(FireballEntity::new, fireballBase)
                    .type(BuiltinEntityType.SMALL_FIREBALL)
                    .heightAndWidth(0.3125f)
                    .build();

            VanillaEntityDefinition<ThrowableItemEntity> throwableItemBase = VanillaEntityDefinition.inherited(ThrowableItemEntity::new, entityBase)
                    .addTranslator(MetadataTypes.ITEM_STACK, ThrowableItemEntity::setItem)
                    .build();
            EGG = VanillaEntityDefinition.inherited(ThrowableEggEntity::new, throwableItemBase)
                    .type(BuiltinEntityType.EGG)
                    .heightAndWidth(0.25f)
                    .property(TemperatureVariantAnimal.TEMPERATE_VARIANT_PROPERTY)
                    .build();
            ENDER_PEARL = VanillaEntityDefinition.inherited(ThrowableItemEntity::new, throwableItemBase)
                    .type(BuiltinEntityType.ENDER_PEARL)
                    .heightAndWidth(0.25f)
                    .build();
            EXPERIENCE_BOTTLE = VanillaEntityDefinition.inherited(ThrowableItemEntity::new, throwableItemBase)
                    .type(BuiltinEntityType.EXPERIENCE_BOTTLE)
                    .heightAndWidth(0.25f)
                    .bedrockIdentifier("minecraft:xp_bottle")
                    .build();
            SPLASH_POTION = VanillaEntityDefinition.inherited(ThrownPotionEntity::new, throwableItemBase)
                    .type(BuiltinEntityType.SPLASH_POTION)
                    .heightAndWidth(0.25f)
                    .bedrockIdentifier("minecraft:splash_potion")
                    .build();
            LINGERING_POTION = VanillaEntityDefinition.inherited(ThrownPotionEntity::new, throwableItemBase)
                .type(BuiltinEntityType.LINGERING_POTION)
                .heightAndWidth(0.25f)
                .bedrockIdentifier("minecraft:splash_potion")
                .build();
            SNOWBALL = VanillaEntityDefinition.inherited(ThrowableItemEntity::new, throwableItemBase)
                    .type(BuiltinEntityType.SNOWBALL)
                    .heightAndWidth(0.25f)
                    .build();

            EntityFactory<AbstractWindChargeEntity> windChargeSupplier = AbstractWindChargeEntity::new;
            BREEZE_WIND_CHARGE = VanillaEntityDefinition.inherited(windChargeSupplier, entityBase)
                    .type(BuiltinEntityType.BREEZE_WIND_CHARGE)
                    .bedrockIdentifier("minecraft:breeze_wind_charge_projectile")
                    .heightAndWidth(0.3125f)
                    .build();
            WIND_CHARGE = VanillaEntityDefinition.inherited(windChargeSupplier, entityBase)
                    .type(BuiltinEntityType.WIND_CHARGE)
                    .bedrockIdentifier("minecraft:wind_charge_projectile")
                    .heightAndWidth(0.3125f)
                    .build();

            VanillaEntityDefinition<AbstractArrowEntity> abstractArrowBase = VanillaEntityDefinition.inherited(AbstractArrowEntity::new, entityBase)
                    .addTranslator(MetadataTypes.BYTE, AbstractArrowEntity::setArrowFlags)
                    .addTranslator(null) // "Piercing level"
                    .addTranslator(null) // If the arrow is in the ground
                    .build();
            ARROW = VanillaEntityDefinition.inherited(ArrowEntity::new, abstractArrowBase)
                    .type(BuiltinEntityType.ARROW)
                    .heightAndWidth(0.25f)
                    .addTranslator(MetadataTypes.INT, ArrowEntity::setPotionEffectColor)
                    .build();
            SPECTRAL_ARROW = VanillaEntityDefinition.inherited(abstractArrowBase.factory(), abstractArrowBase)
                    .type(BuiltinEntityType.SPECTRAL_ARROW)
                    .heightAndWidth(0.25f)
                    .bedrockIdentifier("minecraft:arrow")
                    .build();
            TRIDENT = VanillaEntityDefinition.inherited(TridentEntity::new, abstractArrowBase) // TODO remove class
                    .type(BuiltinEntityType.TRIDENT)
                    .bedrockIdentifier("minecraft:thrown_trident")
                    .addTranslator(null) // Loyalty
                    .addTranslator(MetadataTypes.BOOLEAN, (tridentEntity, entityMetadata) -> tridentEntity.setFlag(EntityFlag.ENCHANTED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();

            VanillaEntityDefinition<HangingEntity> hangingEntityBase = VanillaEntityDefinition.<HangingEntity>inherited(null, entityBase)
                .addTranslator(MetadataTypes.DIRECTION, HangingEntity::setDirectionMetadata)
                .build();

            PAINTING = VanillaEntityDefinition.inherited(PaintingEntity::new, hangingEntityBase)
                .type(BuiltinEntityType.PAINTING)
                .addTranslator(MetadataTypes.PAINTING_VARIANT, PaintingEntity::setPaintingType)
                .build();

            // Item frames are handled differently as they are blocks, not items, in Bedrock
            ITEM_FRAME = VanillaEntityDefinition.inherited(ItemFrameEntity::new, hangingEntityBase)
                    .type(BuiltinEntityType.ITEM_FRAME)
                    .addTranslator(MetadataTypes.ITEM_STACK, ItemFrameEntity::setItemInFrame)
                    .addTranslator(MetadataTypes.INT, ItemFrameEntity::setItemRotation)
                    .build();
            GLOW_ITEM_FRAME = VanillaEntityDefinition.inherited(ITEM_FRAME.factory(), ITEM_FRAME)
                    .type(BuiltinEntityType.GLOW_ITEM_FRAME)
                    .build();

            MINECART = VanillaEntityDefinition.inherited(MinecartEntity::new, entityBase)
                    .type(BuiltinEntityType.MINECART)
                    .height(0.7f).width(0.98f)
                    .offset(0.35f)
                    .addTranslator(MetadataTypes.INT, (minecartEntity, entityMetadata) -> minecartEntity.getDirtyMetadata().put(EntityDataTypes.STRUCTURAL_INTEGRITY, entityMetadata.getValue()))
                    .addTranslator(MetadataTypes.INT, (minecartEntity, entityMetadata) -> minecartEntity.getDirtyMetadata().put(EntityDataTypes.HURT_DIRECTION, entityMetadata.getValue())) // Direction in which the minecart is shaking
                    .addTranslator(MetadataTypes.FLOAT, (minecartEntity, entityMetadata) ->
                            // Power in Java, hurt ticks in Bedrock
                            minecartEntity.getDirtyMetadata().put(EntityDataTypes.HURT_TICKS, Math.min((int) ((FloatEntityMetadata) entityMetadata).getPrimitiveValue(), 15)))
                    .addTranslator(MetadataTypes.OPTIONAL_BLOCK_STATE, MinecartEntity::setCustomBlock)
                    .addTranslator(MetadataTypes.INT, MinecartEntity::setCustomBlockOffset)
                    .build();
            CHEST_MINECART = VanillaEntityDefinition.inherited(MINECART.factory(), MINECART)
                    .type(BuiltinEntityType.CHEST_MINECART)
                    .build();
            COMMAND_BLOCK_MINECART = VanillaEntityDefinition.inherited(CommandBlockMinecartEntity::new, MINECART)
                    .type(BuiltinEntityType.COMMAND_BLOCK_MINECART)
                    .addTranslator(MetadataTypes.STRING, (entity, entityMetadata) -> entity.getDirtyMetadata().put(EntityDataTypes.COMMAND_BLOCK_NAME, entityMetadata.getValue()))
                    .addTranslator(MetadataTypes.COMPONENT, (entity, entityMetadata) -> entity.getDirtyMetadata().put(EntityDataTypes.COMMAND_BLOCK_LAST_OUTPUT, MessageTranslator.convertMessage(entityMetadata.getValue())))
                    .build();
            FURNACE_MINECART = VanillaEntityDefinition.inherited(FurnaceMinecartEntity::new, MINECART)
                    .type(BuiltinEntityType.FURNACE_MINECART)
                    .bedrockIdentifier("minecraft:minecart")
                    .addTranslator(MetadataTypes.BOOLEAN, FurnaceMinecartEntity::setHasFuel)
                    .build();
            HOPPER_MINECART = VanillaEntityDefinition.inherited(MINECART.factory(), MINECART)
                    .type(BuiltinEntityType.HOPPER_MINECART)
                    .build();
            SPAWNER_MINECART = VanillaEntityDefinition.inherited(SpawnerMinecartEntity::new, MINECART)
                    .type(BuiltinEntityType.SPAWNER_MINECART)
                    .bedrockIdentifier("minecraft:minecart")
                    .build();
            TNT_MINECART = VanillaEntityDefinition.inherited(MINECART.factory(), MINECART)
                    .type(BuiltinEntityType.TNT_MINECART)
                    .build();

            WITHER_SKULL = VanillaEntityDefinition.inherited(WitherSkullEntity::new, entityBase)
                    .type(BuiltinEntityType.WITHER_SKULL)
                    .heightAndWidth(0.3125f)
                    .addTranslator(MetadataTypes.BOOLEAN, WitherSkullEntity::setDangerous)
                    .build();
            WITHER_SKULL_DANGEROUS = VanillaEntityDefinition.inherited(WITHER_SKULL.factory(), WITHER_SKULL)
                    .build(false);
        }

        // Boats
        {
            VanillaEntityDefinition<BoatEntity> boatBase = VanillaEntityDefinition.<BoatEntity>inherited(null, entityBase)
                .height(0.6f).width(1.6f)
                .offset(0.35f)
                .addTranslator(MetadataTypes.INT, (boatEntity, entityMetadata) -> boatEntity.getDirtyMetadata().put(EntityDataTypes.HURT_TICKS, entityMetadata.getValue())) // Time since last hit
                .addTranslator(MetadataTypes.INT, (boatEntity, entityMetadata) -> boatEntity.getDirtyMetadata().put(EntityDataTypes.HURT_DIRECTION, entityMetadata.getValue())) // Rocking direction
                .addTranslator(MetadataTypes.FLOAT, (boatEntity, entityMetadata) ->
                    // 'Health' in Bedrock, damage taken in Java - it makes motion in Bedrock
                    boatEntity.getDirtyMetadata().put(EntityDataTypes.STRUCTURAL_INTEGRITY, 40 - ((int) ((FloatEntityMetadata) entityMetadata).getPrimitiveValue())))
                .addTranslator(MetadataTypes.BOOLEAN, BoatEntity::setPaddlingLeft)
                .addTranslator(MetadataTypes.BOOLEAN, BoatEntity::setPaddlingRight)
                .addTranslator(MetadataTypes.INT, (boatEntity, entityMetadata) -> boatEntity.getDirtyMetadata().put(EntityDataTypes.BOAT_BUBBLE_TIME, entityMetadata.getValue())) // May not actually do anything
                .build();

            ACACIA_BOAT = buildBoat(boatBase, BuiltinEntityType.ACACIA_BOAT, BoatEntity.BoatVariant.ACACIA);
            BAMBOO_RAFT = buildBoat(boatBase, BuiltinEntityType.BAMBOO_RAFT, BoatEntity.BoatVariant.BAMBOO);
            BIRCH_BOAT = buildBoat(boatBase, BuiltinEntityType.BIRCH_BOAT, BoatEntity.BoatVariant.BIRCH);
            CHERRY_BOAT = buildBoat(boatBase, BuiltinEntityType.CHERRY_BOAT, BoatEntity.BoatVariant.CHERRY);
            DARK_OAK_BOAT = buildBoat(boatBase, BuiltinEntityType.DARK_OAK_BOAT, BoatEntity.BoatVariant.DARK_OAK);
            JUNGLE_BOAT = buildBoat(boatBase, BuiltinEntityType.JUNGLE_BOAT, BoatEntity.BoatVariant.JUNGLE);
            MANGROVE_BOAT = buildBoat(boatBase, BuiltinEntityType.MANGROVE_BOAT, BoatEntity.BoatVariant.MANGROVE);
            OAK_BOAT = buildBoat(boatBase, BuiltinEntityType.OAK_BOAT, BoatEntity.BoatVariant.OAK);
            SPRUCE_BOAT = buildBoat(boatBase, BuiltinEntityType.SPRUCE_BOAT, BoatEntity.BoatVariant.SPRUCE);
            PALE_OAK_BOAT = buildBoat(boatBase, BuiltinEntityType.PALE_OAK_BOAT, BoatEntity.BoatVariant.PALE_OAK);

            VanillaEntityDefinition<ChestBoatEntity> chestBoatBase = VanillaEntityDefinition.<ChestBoatEntity>inherited(null, boatBase)
                .build();

            ACACIA_CHEST_BOAT = buildChestBoat(chestBoatBase, BuiltinEntityType.ACACIA_CHEST_BOAT, BoatEntity.BoatVariant.ACACIA);
            BAMBOO_CHEST_RAFT = buildChestBoat(chestBoatBase, BuiltinEntityType.BAMBOO_CHEST_RAFT, BoatEntity.BoatVariant.BAMBOO);
            BIRCH_CHEST_BOAT = buildChestBoat(chestBoatBase, BuiltinEntityType.BIRCH_CHEST_BOAT, BoatEntity.BoatVariant.BIRCH);
            CHERRY_CHEST_BOAT = buildChestBoat(chestBoatBase, BuiltinEntityType.CHERRY_CHEST_BOAT, BoatEntity.BoatVariant.CHERRY);
            DARK_OAK_CHEST_BOAT = buildChestBoat(chestBoatBase, BuiltinEntityType.DARK_OAK_CHEST_BOAT, BoatEntity.BoatVariant.DARK_OAK);
            JUNGLE_CHEST_BOAT = buildChestBoat(chestBoatBase, BuiltinEntityType.JUNGLE_CHEST_BOAT, BoatEntity.BoatVariant.JUNGLE);
            MANGROVE_CHEST_BOAT = buildChestBoat(chestBoatBase, BuiltinEntityType.MANGROVE_CHEST_BOAT, BoatEntity.BoatVariant.MANGROVE);
            OAK_CHEST_BOAT = buildChestBoat(chestBoatBase, BuiltinEntityType.OAK_CHEST_BOAT, BoatEntity.BoatVariant.OAK);
            SPRUCE_CHEST_BOAT = buildChestBoat(chestBoatBase, BuiltinEntityType.SPRUCE_CHEST_BOAT, BoatEntity.BoatVariant.SPRUCE);
            PALE_OAK_CHEST_BOAT = buildChestBoat(chestBoatBase, BuiltinEntityType.PALE_OAK_CHEST_BOAT, BoatEntity.BoatVariant.PALE_OAK);
        }

        VanillaEntityDefinition<LivingEntity> livingEntityBase = VanillaEntityDefinition.inherited(LivingEntity::new, entityBase)
                .addTranslator(MetadataTypes.BYTE, LivingEntity::setLivingEntityFlags)
                .addTranslator(MetadataTypes.FLOAT, LivingEntity::setHealth)
                .addTranslator(MetadataTypes.PARTICLES, LivingEntity::setParticles)
                .addTranslator(MetadataTypes.BOOLEAN,
                        (livingEntity, entityMetadata) -> livingEntity.getDirtyMetadata().put(EntityDataTypes.EFFECT_AMBIENCE, (byte) (((BooleanEntityMetadata) entityMetadata).getPrimitiveValue() ? 1 : 0)))
                .addTranslator(null) // Arrow count
                .addTranslator(null) // Stinger count
                .addTranslator(MetadataTypes.OPTIONAL_BLOCK_POS, LivingEntity::setBedPosition)
                .build();

        ARMOR_STAND = VanillaEntityDefinition.inherited(ArmorStandEntity::new, livingEntityBase)
                .type(BuiltinEntityType.ARMOR_STAND)
                .height(1.975f).width(0.5f)
                .addTranslator(MetadataTypes.BYTE, ArmorStandEntity::setArmorStandFlags)
                .addTranslator(MetadataTypes.ROTATIONS, ArmorStandEntity::setHeadRotation)
                .addTranslator(MetadataTypes.ROTATIONS, ArmorStandEntity::setBodyRotation)
                .addTranslator(MetadataTypes.ROTATIONS, ArmorStandEntity::setLeftArmRotation)
                .addTranslator(MetadataTypes.ROTATIONS, ArmorStandEntity::setRightArmRotation)
                .addTranslator(MetadataTypes.ROTATIONS, ArmorStandEntity::setLeftLegRotation)
                .addTranslator(MetadataTypes.ROTATIONS, ArmorStandEntity::setRightLegRotation)
                .build();

        VanillaEntityDefinition<AvatarEntity> avatarEntityBase = VanillaEntityDefinition.<AvatarEntity>inherited(null, livingEntityBase)
            .height(1.8f).width(0.6f)
            .offset(1.62f)
            .addTranslator(null) // Player main hand
            .addTranslator(MetadataTypes.BYTE, AvatarEntity::setSkinVisibility)
            .build();

        MANNEQUIN = VanillaEntityDefinition.inherited(MannequinEntity::new, avatarEntityBase)
            .type(BuiltinEntityType.MANNEQUIN)
            .addTranslator(MetadataTypes.RESOLVABLE_PROFILE, MannequinEntity::setProfile)
            .addTranslator(null) // Immovable
            .addTranslator(MetadataTypes.OPTIONAL_COMPONENT, MannequinEntity::setDescription)
            .build();

        PLAYER = VanillaEntityDefinition.<PlayerEntity>inherited(null, avatarEntityBase)
                .type(BuiltinEntityType.PLAYER)
                .addTranslator(MetadataTypes.FLOAT, PlayerEntity::setAbsorptionHearts)
                .addTranslator(null) // Player score
                .addTranslator(MetadataTypes.OPTIONAL_UNSIGNED_INT, PlayerEntity::setLeftParrot)
                .addTranslator(MetadataTypes.OPTIONAL_UNSIGNED_INT, PlayerEntity::setRightParrot)
                .build();

        VanillaEntityDefinition<MobEntity> mobEntityBase = VanillaEntityDefinition.inherited(MobEntity::new, livingEntityBase)
                .addTranslator(MetadataTypes.BYTE, MobEntity::setMobFlags)
                .build();

        // Extends mob
        {
            ALLAY = VanillaEntityDefinition.inherited(AllayEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.ALLAY)
                    .height(0.6f).width(0.35f)
                    .addTranslator(MetadataTypes.BOOLEAN, AllayEntity::setDancing)
                    .addTranslator(MetadataTypes.BOOLEAN, AllayEntity::setCanDuplicate)
                    .build();
            BAT = VanillaEntityDefinition.inherited(BatEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.BAT)
                    .height(0.9f).width(0.5f)
                    .addTranslator(MetadataTypes.BYTE, BatEntity::setBatFlags)
                    .build();
            BOGGED = VanillaEntityDefinition.inherited(BoggedEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.BOGGED)
                    .height(1.99f).width(0.6f)
                    .addTranslator(MetadataTypes.BOOLEAN, BoggedEntity::setSheared)
                    .build();
            BLAZE = VanillaEntityDefinition.inherited(BlazeEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.BLAZE)
                    .height(1.8f).width(0.6f)
                    .addTranslator(MetadataTypes.BYTE, BlazeEntity::setBlazeFlags)
                    .build();
            BREEZE = VanillaEntityDefinition.inherited(BreezeEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.BREEZE)
                    .height(1.77f).width(0.6f)
                    .build();
            COPPER_GOLEM = VanillaEntityDefinition.inherited(CopperGolemEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.COPPER_GOLEM)
                    .height(0.49f).width(0.98f)
                    .addTranslator(MetadataTypes.WEATHERING_COPPER_STATE, CopperGolemEntity::setWeatheringState)
                    .addTranslator(MetadataTypes.COPPER_GOLEM_STATE, CopperGolemEntity::setGolemState)
                    .property(CopperGolemEntity.CHEST_INTERACTION_PROPERTY)
                    .property(CopperGolemEntity.HAS_FLOWER_PROPERTY)
                    .property(CopperGolemEntity.OXIDATION_LEVEL_STATE_ENUM_PROPERTY)
                    .build();
            CREAKING = VanillaEntityDefinition.inherited(CreakingEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.CREAKING)
                    .height(2.7f).width(0.9f)
                    .addTranslator(MetadataTypes.BOOLEAN, CreakingEntity::setCanMove)
                    .addTranslator(MetadataTypes.BOOLEAN, CreakingEntity::setActive)
                    .addTranslator(MetadataTypes.BOOLEAN, CreakingEntity::setIsTearingDown)
                    .addTranslator(MetadataTypes.OPTIONAL_BLOCK_POS, CreakingEntity::setHomePos)
                    .property(CreakingEntity.STATE_PROPERTY)
                    .property(CreakingEntity.SWAYING_TICKS_PROPERTY)
                    .build();
            CREEPER = VanillaEntityDefinition.inherited(CreeperEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.CREEPER)
                    .height(1.7f).width(0.6f)
                    .offset(1.62f)
                    .addTranslator(MetadataTypes.INT, CreeperEntity::setSwelling)
                    .addTranslator(MetadataTypes.BOOLEAN, (entity, entityMetadata) -> entity.setFlag(EntityFlag.POWERED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .addTranslator(MetadataTypes.BOOLEAN, CreeperEntity::setIgnited)
                    .build();
            ENDERMAN = VanillaEntityDefinition.inherited(EndermanEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.ENDERMAN)
                    .height(2.9f).width(0.6f)
                    .addTranslator(MetadataTypes.OPTIONAL_BLOCK_STATE, EndermanEntity::setCarriedBlock)
                    .addTranslator(MetadataTypes.BOOLEAN, EndermanEntity::setScreaming)
                    .addTranslator(MetadataTypes.BOOLEAN, EndermanEntity::setAngry)
                    .build();
            ENDERMITE = VanillaEntityDefinition.inherited(MonsterEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.ENDERMITE)
                    .height(0.3f).width(0.4f)
                    .build();
            ENDER_DRAGON = VanillaEntityDefinition.inherited(EnderDragonEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.ENDER_DRAGON)
                    .addTranslator(MetadataTypes.INT, EnderDragonEntity::setPhase)
                    .build();
            GHAST = VanillaEntityDefinition.inherited(GhastEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.GHAST)
                    .heightAndWidth(4.0f)
                    .addTranslator(MetadataTypes.BOOLEAN, GhastEntity::setGhastAttacking)
                    .build();
            GIANT = VanillaEntityDefinition.inherited(GiantEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.GIANT)
                    .height(1.8f).width(1.6f)
                    .offset(1.62f)
                    .bedrockIdentifier("minecraft:zombie")
                    .build();
            IRON_GOLEM = VanillaEntityDefinition.inherited(IronGolemEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.IRON_GOLEM)
                    .height(2.7f).width(1.4f)
                    .addTranslator(null) // "is player created", which doesn't seem to do anything clientside
                    .build();
            PHANTOM = VanillaEntityDefinition.inherited(PhantomEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.PHANTOM)
                    .height(0.5f).width(0.9f)
                    .offset(0.6f)
                    .addTranslator(MetadataTypes.INT, PhantomEntity::setPhantomScale)
                    .build();
            SILVERFISH = VanillaEntityDefinition.inherited(MonsterEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.SILVERFISH)
                    .height(0.3f).width(0.4f)
                    .build();
            SHULKER = VanillaEntityDefinition.inherited(ShulkerEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.SHULKER)
                    .heightAndWidth(1f)
                    .addTranslator(MetadataTypes.DIRECTION, ShulkerEntity::setAttachedFace)
                    .addTranslator(MetadataTypes.BYTE, ShulkerEntity::setShulkerHeight)
                    .addTranslator(MetadataTypes.BYTE, ShulkerEntity::setShulkerColor)
                    .build();
            SKELETON = VanillaEntityDefinition.inherited(SkeletonEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.SKELETON)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .addTranslator(MetadataTypes.BOOLEAN, SkeletonEntity::setConvertingToStray)
                    .build();
            SNOW_GOLEM = VanillaEntityDefinition.inherited(SnowGolemEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.SNOW_GOLEM)
                    .height(1.9f).width(0.7f)
                    .addTranslator(MetadataTypes.BYTE, SnowGolemEntity::setSnowGolemFlags)
                    .build();
            SPIDER = VanillaEntityDefinition.inherited(SpiderEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.SPIDER)
                    .height(0.9f).width(1.4f)
                    .offset(1f)
                    .addTranslator(MetadataTypes.BYTE, SpiderEntity::setSpiderFlags)
                    .build();
            CAVE_SPIDER = VanillaEntityDefinition.inherited(SpiderEntity::new, SPIDER)
                    .type(BuiltinEntityType.CAVE_SPIDER)
                    .height(0.5f).width(0.7f)
                    .build();
            STRAY = VanillaEntityDefinition.inherited(AbstractSkeletonEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.STRAY)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .build();
            VEX = VanillaEntityDefinition.inherited(VexEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.VEX)
                    .height(0.8f).width(0.4f)
                    .addTranslator(MetadataTypes.BYTE, VexEntity::setVexFlags)
                    .build();
            WARDEN = VanillaEntityDefinition.inherited(WardenEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.WARDEN)
                    .height(2.9f).width(0.9f)
                    .addTranslator(MetadataTypes.INT, WardenEntity::setAngerLevel)
                    .build();
            WITHER = VanillaEntityDefinition.inherited(WitherEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.WITHER)
                    .height(3.5f).width(0.9f)
                    .addTranslator(MetadataTypes.INT, WitherEntity::setTarget1)
                    .addTranslator(MetadataTypes.INT, WitherEntity::setTarget2)
                    .addTranslator(MetadataTypes.INT, WitherEntity::setTarget3)
                    .addTranslator(MetadataTypes.INT, WitherEntity::setInvulnerableTicks)
                    .build();
            WITHER_SKELETON = VanillaEntityDefinition.inherited(AbstractSkeletonEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.WITHER_SKELETON)
                    .height(2.4f).width(0.7f)
                    .build();
            ZOGLIN = VanillaEntityDefinition.inherited(ZoglinEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.ZOGLIN)
                    .height(1.4f).width(1.3965f)
                    .addTranslator(MetadataTypes.BOOLEAN, ZoglinEntity::setBaby)
                    .build();
            ZOMBIE = VanillaEntityDefinition.inherited(ZombieEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.ZOMBIE)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .addTranslator(MetadataTypes.BOOLEAN, ZombieEntity::setZombieBaby)
                    .addTranslator(null) // "set special type", doesn't do anything
                    .addTranslator(MetadataTypes.BOOLEAN, ZombieEntity::setConvertingToDrowned)
                    .build();
            ZOMBIE_VILLAGER = VanillaEntityDefinition.inherited(ZombieVillagerEntity::new, ZOMBIE)
                    .type(BuiltinEntityType.ZOMBIE_VILLAGER)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .bedrockIdentifier("minecraft:zombie_villager_v2")
                    .addTranslator(MetadataTypes.BOOLEAN, ZombieVillagerEntity::setTransforming)
                    .addTranslator(MetadataTypes.VILLAGER_DATA, ZombieVillagerEntity::setZombieVillagerData)
                    .build();
            ZOMBIFIED_PIGLIN = VanillaEntityDefinition.inherited(ZombifiedPiglinEntity::new, ZOMBIE) //TODO test how zombie entity metadata is handled?
                    .type(BuiltinEntityType.ZOMBIFIED_PIGLIN)
                    .height(1.95f).width(0.6f)
                    .offset(1.62f)
                    .bedrockIdentifier("minecraft:zombie_pigman")
                    .build();

            DROWNED = VanillaEntityDefinition.inherited(ZOMBIE.factory(), ZOMBIE)
                    .type(BuiltinEntityType.DROWNED)
                    .height(1.95f).width(0.6f)
                    .build();
            HUSK = VanillaEntityDefinition.inherited(ZOMBIE.factory(), ZOMBIE)
                    .type(BuiltinEntityType.HUSK)
                    .build();

            GUARDIAN = VanillaEntityDefinition.inherited(GuardianEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.GUARDIAN)
                    .heightAndWidth(0.85f)
                    .addTranslator(null) // Moving //TODO
                    .addTranslator(MetadataTypes.INT, GuardianEntity::setGuardianTarget)
                    .build();
            ELDER_GUARDIAN = VanillaEntityDefinition.inherited(ElderGuardianEntity::new, GUARDIAN)
                    .type(BuiltinEntityType.ELDER_GUARDIAN)
                    .heightAndWidth(1.9975f)
                    .build();

            SLIME = VanillaEntityDefinition.inherited(SlimeEntity::new, mobEntityBase)
                    .type(BuiltinEntityType.SLIME)
                    .heightAndWidth(0.51f)
                    .addTranslator(MetadataTypes.INT, SlimeEntity::setSlimeScale)
                    .build();
            MAGMA_CUBE = VanillaEntityDefinition.inherited(MagmaCubeEntity::new, SLIME)
                    .type(BuiltinEntityType.MAGMA_CUBE)
                    .build();

            VanillaEntityDefinition<AbstractFishEntity> abstractFishEntityBase = VanillaEntityDefinition.inherited(AbstractFishEntity::new, mobEntityBase)
                    .addTranslator(null) // From bucket
                    .build();
            COD = VanillaEntityDefinition.inherited(abstractFishEntityBase.factory(), abstractFishEntityBase)
                    .type(BuiltinEntityType.COD)
                    .height(0.25f).width(0.5f)
                    .build();
            PUFFERFISH = VanillaEntityDefinition.inherited(PufferFishEntity::new, abstractFishEntityBase)
                    .type(BuiltinEntityType.PUFFERFISH)
                    .heightAndWidth(0.7f)
                    .addTranslator(MetadataTypes.INT, PufferFishEntity::setPufferfishSize)
                    .build();
            SALMON = VanillaEntityDefinition.inherited(abstractFishEntityBase.factory(), abstractFishEntityBase)
                    .type(BuiltinEntityType.SALMON)
                    .height(0.5f).width(0.7f)
                    .addTranslator(null) // Scale/variant - TODO
                    .build();
            TADPOLE = VanillaEntityDefinition.inherited(TadpoleEntity::new, abstractFishEntityBase)
                    .type(BuiltinEntityType.TADPOLE)
                    .height(0.3f).width(0.4f)
                    .build();
            TROPICAL_FISH = VanillaEntityDefinition.inherited(TropicalFishEntity::new, abstractFishEntityBase)
                    .type(BuiltinEntityType.TROPICAL_FISH)
                    .heightAndWidth(0.6f)
                    .bedrockIdentifier("minecraft:tropicalfish")
                    .addTranslator(MetadataTypes.INT, TropicalFishEntity::setFishVariant)
                    .build();

            VanillaEntityDefinition<BasePiglinEntity> abstractPiglinEntityBase = VanillaEntityDefinition.inherited(BasePiglinEntity::new, mobEntityBase)
                    .addTranslator(MetadataTypes.BOOLEAN, BasePiglinEntity::setImmuneToZombification)
                    .build();
            PIGLIN = VanillaEntityDefinition.inherited(PiglinEntity::new, abstractPiglinEntityBase)
                    .type(BuiltinEntityType.PIGLIN)
                    .height(1.95f).width(0.6f)
                    .addTranslator(MetadataTypes.BOOLEAN, PiglinEntity::setBaby)
                    .addTranslator(MetadataTypes.BOOLEAN, PiglinEntity::setChargingCrossbow)
                    .addTranslator(MetadataTypes.BOOLEAN, PiglinEntity::setDancing)
                    .build();
            PIGLIN_BRUTE = VanillaEntityDefinition.inherited(abstractPiglinEntityBase.factory(), abstractPiglinEntityBase)
                    .type(BuiltinEntityType.PIGLIN_BRUTE)
                    .height(1.95f).width(0.6f)
                    .build();

            VanillaEntityDefinition<RaidParticipantEntity> raidParticipantEntityBase = VanillaEntityDefinition.inherited(RaidParticipantEntity::new, mobEntityBase)
                    .addTranslator(null) // Celebrating //TODO
                    .build();
            VanillaEntityDefinition<SpellcasterIllagerEntity> spellcasterEntityBase = VanillaEntityDefinition.inherited(SpellcasterIllagerEntity::new, raidParticipantEntityBase)
                    .addTranslator(MetadataTypes.BYTE, SpellcasterIllagerEntity::setSpellType)
                    .build();
            EVOKER = VanillaEntityDefinition.inherited(spellcasterEntityBase.factory(), spellcasterEntityBase)
                    .type(BuiltinEntityType.EVOKER)
                    .height(1.95f).width(0.6f)
                    .bedrockIdentifier("minecraft:evocation_illager")
                    .build();
            ILLUSIONER = VanillaEntityDefinition.inherited(spellcasterEntityBase.factory(), spellcasterEntityBase)
                    .type(BuiltinEntityType.ILLUSIONER)
                    .height(1.95f).width(0.6f)
                    .bedrockIdentifier("minecraft:evocation_illager")
                    .build();
            PILLAGER = VanillaEntityDefinition.inherited(PillagerEntity::new, raidParticipantEntityBase)
                    .type(BuiltinEntityType.PILLAGER)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .addTranslator(MetadataTypes.BOOLEAN, PillagerEntity::setChargingCrossbow)
                    .build();
            RAVAGER = VanillaEntityDefinition.inherited(RavagerEntity::new, raidParticipantEntityBase)
                    .type(BuiltinEntityType.RAVAGER)
                    .height(1.9f).width(1.2f)
                    .build();
            VINDICATOR = VanillaEntityDefinition.inherited(VindicatorEntity::new, raidParticipantEntityBase)
                    .type(BuiltinEntityType.VINDICATOR)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .build();
            WITCH = VanillaEntityDefinition.inherited(raidParticipantEntityBase.factory(), raidParticipantEntityBase)
                    .type(BuiltinEntityType.WITCH)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .addTranslator(null) // Using item
                    .build();
        }

        VanillaEntityDefinition<AgeableEntity> ageableEntityBase = VanillaEntityDefinition.inherited(AgeableEntity::new, mobEntityBase)
                .addTranslator(MetadataTypes.BOOLEAN, AgeableEntity::setBaby)
                .build();

        // Extends ageable
        {
            ARMADILLO = VanillaEntityDefinition.inherited(ArmadilloEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.ARMADILLO)
                    .height(0.65f).width(0.7f)
                    .property(ArmadilloEntity.STATE_PROPERTY)
                    .addTranslator(MetadataTypes.ARMADILLO_STATE, ArmadilloEntity::setArmadilloState)
                    .build();
            AXOLOTL = VanillaEntityDefinition.inherited(AxolotlEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.AXOLOTL)
                    .height(0.42f).width(0.7f)
                    .addTranslator(MetadataTypes.INT, AxolotlEntity::setVariant)
                    .addTranslator(MetadataTypes.BOOLEAN, AxolotlEntity::setPlayingDead)
                    .addTranslator(null) // From bucket
                    .build();
            BEE = VanillaEntityDefinition.inherited(BeeEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.BEE)
                    .heightAndWidth(0.6f)
                    .property(BeeEntity.NECTAR_PROPERTY)
                    .addTranslator(MetadataTypes.BYTE, BeeEntity::setBeeFlags)
                    .addTranslator(MetadataTypes.INT, BeeEntity::setAngerTime)
                    .build();
            CHICKEN = VanillaEntityDefinition.inherited(ChickenEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.CHICKEN)
                    .height(0.7f).width(0.4f)
                    .property(TemperatureVariantAnimal.TEMPERATE_VARIANT_PROPERTY)
                    .addTranslator(MetadataTypes.CHICKEN_VARIANT, ChickenEntity::setVariant)
                    .build();
            COW = VanillaEntityDefinition.inherited(CowEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.COW)
                    .height(1.4f).width(0.9f)
                    .property(TemperatureVariantAnimal.TEMPERATE_VARIANT_PROPERTY)
                    .addTranslator(MetadataTypes.COW_VARIANT, CowEntity::setVariant)
                    .build();
            FOX = VanillaEntityDefinition.inherited(FoxEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.FOX)
                    .height(0.7f).width(0.6f)
                    .addTranslator(MetadataTypes.INT, FoxEntity::setFoxVariant)
                    .addTranslator(MetadataTypes.BYTE, FoxEntity::setFoxFlags)
                    .addTranslator(null) // Trusted player 1
                    .addTranslator(null) // Trusted player 2
                    .build();
            FROG = VanillaEntityDefinition.inherited(FrogEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.FROG)
                    .heightAndWidth(0.5f)
                    .addTranslator(MetadataTypes.FROG_VARIANT, FrogEntity::setVariant)
                    .addTranslator(MetadataTypes.OPTIONAL_UNSIGNED_INT, FrogEntity::setTongueTarget)
                    .build();
            HAPPY_GHAST = VanillaEntityDefinition.inherited(HappyGhastEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.HAPPY_GHAST)
                    .heightAndWidth(4f)
                    .property(HappyGhastEntity.CAN_MOVE_PROPERTY)
                    .addTranslator(null) // Is leash holder
                    .addTranslator(MetadataTypes.BOOLEAN, HappyGhastEntity::setStaysStill)
                    .build();
            HOGLIN = VanillaEntityDefinition.inherited(HoglinEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.HOGLIN)
                    .height(1.4f).width(1.3965f)
                    .addTranslator(MetadataTypes.BOOLEAN, HoglinEntity::setImmuneToZombification)
                    .build();
            GOAT = VanillaEntityDefinition.inherited(GoatEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.GOAT)
                    .height(1.3f).width(0.9f)
                    .addTranslator(MetadataTypes.BOOLEAN, GoatEntity::setScreamer)
                    .addTranslator(MetadataTypes.BOOLEAN, GoatEntity::setHasLeftHorn)
                    .addTranslator(MetadataTypes.BOOLEAN, GoatEntity::setHasRightHorn)
                    .build();
            MOOSHROOM = VanillaEntityDefinition.inherited(MooshroomEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.MOOSHROOM)
                    .height(1.4f).width(0.9f)
                    .addTranslator(MetadataTypes.INT, MooshroomEntity::setMooshroomVariant)
                    .build();
            OCELOT = VanillaEntityDefinition.inherited(OcelotEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.OCELOT)
                    .height(0.7f).width(0.6f)
                    .addTranslator(MetadataTypes.BOOLEAN, (ocelotEntity, entityMetadata) -> ocelotEntity.setFlag(EntityFlag.TRUSTING, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();
            PANDA = VanillaEntityDefinition.inherited(PandaEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.PANDA)
                    .height(1.25f).width(1.125f)
                    .addTranslator(null) // Unhappy counter
                    .addTranslator(null) // Sneeze counter
                    .addTranslator(MetadataTypes.INT, PandaEntity::setEatingCounter)
                    .addTranslator(MetadataTypes.BYTE, PandaEntity::setMainGene)
                    .addTranslator(MetadataTypes.BYTE, PandaEntity::setHiddenGene)
                    .addTranslator(MetadataTypes.BYTE, PandaEntity::setPandaFlags)
                    .build();
            PIG = VanillaEntityDefinition.inherited(PigEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.PIG)
                    .heightAndWidth(0.9f)
                    .property(TemperatureVariantAnimal.TEMPERATE_VARIANT_PROPERTY)
                    .addTranslator(MetadataTypes.INT, PigEntity::setBoost)
                    .addTranslator(MetadataTypes.PIG_VARIANT, PigEntity::setVariant)
                    .build();
            POLAR_BEAR = VanillaEntityDefinition.inherited(PolarBearEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.POLAR_BEAR)
                    .height(1.4f).width(1.3f)
                    .addTranslator(MetadataTypes.BOOLEAN, (entity, entityMetadata) -> entity.setFlag(EntityFlag.STANDING, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();
            RABBIT = VanillaEntityDefinition.inherited(RabbitEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.RABBIT)
                    .height(0.5f).width(0.4f)
                    .addTranslator(MetadataTypes.INT, RabbitEntity::setRabbitVariant)
                    .build();
            SHEEP = VanillaEntityDefinition.inherited(SheepEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.SHEEP)
                    .height(1.3f).width(0.9f)
                    .addTranslator(MetadataTypes.BYTE, SheepEntity::setSheepFlags)
                    .build();
            SNIFFER = VanillaEntityDefinition.inherited(SnifferEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.SNIFFER)
                    .height(1.75f).width(1.9f)
                    .addTranslator(MetadataTypes.SNIFFER_STATE, SnifferEntity::setSnifferState)
                    .addTranslator(null) // Integer, drop seed at tick
                    .build();
            STRIDER = VanillaEntityDefinition.inherited(StriderEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.STRIDER)
                    .height(1.7f).width(0.9f)
                    .addTranslator(MetadataTypes.INT, StriderEntity::setBoost)
                    .addTranslator(MetadataTypes.BOOLEAN, StriderEntity::setCold)
                    .build();
            TURTLE = VanillaEntityDefinition.inherited(TurtleEntity::new, ageableEntityBase)
                    .type(BuiltinEntityType.TURTLE)
                    .height(0.4f).width(1.2f)
                    .addTranslator(null) // Home position
                    .addTranslator(MetadataTypes.BOOLEAN, TurtleEntity::setPregnant)
                    .addTranslator(MetadataTypes.BOOLEAN, TurtleEntity::setLayingEgg)
                    .addTranslator(null) // Travel position
                    .addTranslator(null) // Going home
                    .addTranslator(null) // Travelling
                    .build();

            VanillaEntityDefinition<AbstractMerchantEntity> abstractVillagerEntityBase = VanillaEntityDefinition.inherited(AbstractMerchantEntity::new, ageableEntityBase)
                    .addTranslator(null) // Unhappy ticks
                    .build();
            VILLAGER = VanillaEntityDefinition.inherited(VillagerEntity::new, abstractVillagerEntityBase)
                    .type(BuiltinEntityType.VILLAGER)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .bedrockIdentifier("minecraft:villager_v2")
                    .addTranslator(MetadataTypes.VILLAGER_DATA, VillagerEntity::setVillagerData)
                    .build();
            WANDERING_TRADER = VanillaEntityDefinition.inherited(abstractVillagerEntityBase.factory(), abstractVillagerEntityBase)
                    .type(BuiltinEntityType.WANDERING_TRADER)
                    .height(1.8f).width(0.6f)
                    .offset(1.62f)
                    .build();
        }

        // Water creatures (AgeableWaterCreature)
        {
            DOLPHIN = VanillaEntityDefinition.inherited(DolphinEntity::new, ageableEntityBase)
                .type(BuiltinEntityType.DOLPHIN)
                .height(0.6f).width(0.9f)
                //TODO check
                .addTranslator(null) // treasure position
                .addTranslator(null) // "got fish"
                .addTranslator(null) // "moistness level"
                .build();
            SQUID = VanillaEntityDefinition.inherited(SquidEntity::new, ageableEntityBase)
                .type(BuiltinEntityType.SQUID)
                .heightAndWidth(0.8f)
                .build();
            GLOW_SQUID = VanillaEntityDefinition.inherited(GlowSquidEntity::new, SQUID)
                .type(BuiltinEntityType.GLOW_SQUID)
                .addTranslator(null) // Set dark ticks remaining, possible TODO
                .build();
        }

        // Horses
        {
            VanillaEntityDefinition<AbstractHorseEntity> abstractHorseEntityBase = VanillaEntityDefinition.inherited(AbstractHorseEntity::new, ageableEntityBase)
                    .addTranslator(MetadataTypes.BYTE, AbstractHorseEntity::setHorseFlags)
                    .build();
            CAMEL = VanillaEntityDefinition.inherited(CamelEntity::new, abstractHorseEntityBase)
                    .type(BuiltinEntityType.CAMEL)
                    .height(2.375f).width(1.7f)
                    .addTranslator(MetadataTypes.BOOLEAN, CamelEntity::setDashing)
                    .addTranslator(MetadataTypes.LONG, CamelEntity::setLastPoseTick)
                    .build();
            HORSE = VanillaEntityDefinition.inherited(HorseEntity::new, abstractHorseEntityBase)
                    .type(BuiltinEntityType.HORSE)
                    .height(1.6f).width(1.3965f)
                    .addTranslator(MetadataTypes.INT, HorseEntity::setHorseVariant)
                    .build();
            SKELETON_HORSE = VanillaEntityDefinition.inherited(SkeletonHorseEntity::new, abstractHorseEntityBase)
                    .type(BuiltinEntityType.SKELETON_HORSE)
                    .height(1.6f).width(1.3965f)
                    .build();
            ZOMBIE_HORSE = VanillaEntityDefinition.inherited(ZombieHorseEntity::new, abstractHorseEntityBase)
                    .type(BuiltinEntityType.ZOMBIE_HORSE)
                    .height(1.6f).width(1.3965f)
                    .build();
            VanillaEntityDefinition<ChestedHorseEntity> chestedHorseEntityBase = VanillaEntityDefinition.inherited(ChestedHorseEntity::new, abstractHorseEntityBase)
                    .addTranslator(MetadataTypes.BOOLEAN, (horseEntity, entityMetadata) -> horseEntity.setFlag(EntityFlag.CHESTED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                    .build();
            DONKEY = VanillaEntityDefinition.inherited(chestedHorseEntityBase.factory(), chestedHorseEntityBase)
                    .type(BuiltinEntityType.DONKEY)
                    .height(1.6f).width(1.3965f)
                    .build();
            MULE = VanillaEntityDefinition.inherited(chestedHorseEntityBase.factory(), chestedHorseEntityBase)
                    .type(BuiltinEntityType.MULE)
                    .height(1.6f).width(1.3965f)
                    .build();
            LLAMA = VanillaEntityDefinition.inherited(LlamaEntity::new, chestedHorseEntityBase)
                    .type(BuiltinEntityType.LLAMA)
                    .height(1.87f).width(0.9f)
                    .addTranslator(MetadataTypes.INT, LlamaEntity::setStrength)
                    .addTranslator(MetadataTypes.INT, (entity, entityMetadata) -> entity.getDirtyMetadata().put(EntityDataTypes.VARIANT, entityMetadata.getValue()))
                    .build();
            TRADER_LLAMA = VanillaEntityDefinition.inherited(TraderLlamaEntity::new, LLAMA)
                    .type(BuiltinEntityType.TRADER_LLAMA)
                    .bedrockIdentifier("minecraft:llama")
                    .build();
        }

        VanillaEntityDefinition<TameableEntity> tameableEntityBase = VanillaEntityDefinition.<TameableEntity>inherited(null, ageableEntityBase) // No factory, is abstract
                .addTranslator(MetadataTypes.BYTE, TameableEntity::setTameableFlags)
                .addTranslator(MetadataTypes.OPTIONAL_LIVING_ENTITY_REFERENCE, TameableEntity::setOwner)
                .build();
        CAT = VanillaEntityDefinition.inherited(CatEntity::new, tameableEntityBase)
                .type(BuiltinEntityType.CAT)
                .height(0.35f).width(0.3f)
                .addTranslator(MetadataTypes.CAT_VARIANT, CatEntity::setVariant)
                .addTranslator(MetadataTypes.BOOLEAN, CatEntity::setResting)
                .addTranslator(null) // "resting state one" //TODO
                .addTranslator(MetadataTypes.INT, CatEntity::setCollarColor)
                .build();
        PARROT = VanillaEntityDefinition.inherited(ParrotEntity::new, tameableEntityBase)
                .type(BuiltinEntityType.PARROT)
                .height(0.9f).width(0.5f)
                .addTranslator(MetadataTypes.INT, (parrotEntity, entityMetadata) -> parrotEntity.getDirtyMetadata().put(EntityDataTypes.VARIANT, entityMetadata.getValue())) // Parrot color
                .build();
        WOLF = VanillaEntityDefinition.inherited(WolfEntity::new, tameableEntityBase)
                .type(BuiltinEntityType.WOLF)
                .height(0.85f).width(0.6f)
                .property(WolfEntity.SOUND_VARIANT)
                // "Begging" on wiki.vg, "Interested" in Nukkit - the tilt of the head
                .addTranslator(MetadataTypes.BOOLEAN, (wolfEntity, entityMetadata) -> wolfEntity.setFlag(EntityFlag.INTERESTED, ((BooleanEntityMetadata) entityMetadata).getPrimitiveValue()))
                .addTranslator(MetadataTypes.INT, WolfEntity::setCollarColor)
                .addTranslator(MetadataTypes.INT, WolfEntity::setWolfAngerTime)
                .addTranslator(MetadataTypes.WOLF_VARIANT, WolfEntity::setVariant)
                .addTranslator(null) // sound variant; these aren't clientsided anyways... right??
                .build();

        // As of 1.18 these don't track entity data at all
        ENDER_DRAGON_PART = VanillaEntityDefinition.<EnderDragonPartEntity>builder(null)
                .bedrockIdentifier("minecraft:armor_stand") // Emulated
                .build(false); // Never sent over the network

        Registries.JAVA_ENTITY_IDENTIFIERS.get().put("minecraft:marker", null); // We don't need an entity definition for this as it is never sent over the network
    }

    private static VanillaEntityDefinition<BoatEntity> buildBoat(VanillaEntityDefinition<BoatEntity> base, BuiltinEntityType BuiltinEntityType, BoatEntity.BoatVariant variant) {
        return VanillaEntityDefinition.inherited((session, javaId, bedrockId, uuid, definition, position, motion, yaw, pitch, headYaw) ->
            new BoatEntity(session, javaId, bedrockId, uuid, definition, position, motion, yaw, variant), base)
            .type(BuiltinEntityType)
            .bedrockIdentifier("minecraft:boat")
            .build();
    }

    private static VanillaEntityDefinition<ChestBoatEntity> buildChestBoat(VanillaEntityDefinition<ChestBoatEntity> base, BuiltinEntityType BuiltinEntityType, BoatEntity.BoatVariant variant) {
        return VanillaEntityDefinition.inherited((session, javaId, bedrockId, uuid, definition, position, motion, yaw, pitch, headYaw) ->
                new ChestBoatEntity(session, javaId, bedrockId, uuid, definition, position, motion, yaw, variant), base)
            .type(BuiltinEntityType)
            .bedrockIdentifier("minecraft:chest_boat")
            .build();
    }

    public static void init() {
        // entities would be initialized before this event is called
        GeyserImpl.getInstance().getEventBus().fire(new GeyserDefineEntityPropertiesEvent() {
            @Override
            public GeyserFloatEntityProperty registerFloatProperty(@NonNull Identifier identifier, @NonNull Identifier propertyId, float min, float max, @Nullable Float defaultValue) {
                Objects.requireNonNull(identifier);
                Objects.requireNonNull(propertyId);
                if (propertyId.vanilla()) {
                    throw new IllegalArgumentException("Cannot register custom property in vanilla namespace! " + propertyId);
                }
                FloatProperty property = new FloatProperty(propertyId, max, min, defaultValue);
                registerProperty(identifier, property);
                return property;
            }

            @Override
            public IntProperty registerIntegerProperty(@NonNull Identifier identifier, @NonNull Identifier propertyId, int min, int max, @Nullable Integer defaultValue) {
                Objects.requireNonNull(identifier);
                Objects.requireNonNull(propertyId);
                if (propertyId.vanilla()) {
                    throw new IllegalArgumentException("Cannot register custom property in vanilla namespace! " + propertyId);
                }
                IntProperty property = new IntProperty(propertyId, max, min, defaultValue);
                registerProperty(identifier, property);
                return property;
            }

            @Override
            public BooleanProperty registerBooleanProperty(@NonNull Identifier identifier, @NonNull Identifier propertyId, boolean defaultValue) {
                Objects.requireNonNull(identifier);
                Objects.requireNonNull(propertyId);
                if (propertyId.vanilla()) {
                    throw new IllegalArgumentException("Cannot register custom property in vanilla namespace! " + propertyId);
                }
                BooleanProperty property = new BooleanProperty(propertyId, defaultValue);
                registerProperty(identifier, property);
                return property;
            }

            @Override
            public <E extends Enum<E>> EnumProperty<E> registerEnumProperty(@NonNull Identifier identifier, @NonNull Identifier propertyId, @NonNull Class<E> enumClass, @Nullable E defaultValue) {
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

            @Override
            public GeyserStringEnumProperty registerEnumProperty(@NonNull Identifier identifier, @NonNull Identifier propertyId, @NonNull List<String> values, @Nullable String defaultValue) {
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

            @Override
            public Collection<GeyserEntityProperty<?>> properties(@NonNull Identifier identifier) {
                Objects.requireNonNull(identifier);
                var definition = Registries.JAVA_ENTITY_IDENTIFIERS.get(identifier.toString());
                if (definition == null) {
                    throw new IllegalArgumentException("Unknown entity type: " + identifier);
                }
                return List.copyOf(definition.registeredProperties().getProperties());
            }
        });

        for (var definition : Registries.ENTITY_DEFINITIONS.get().values()) {
            if (!definition.registeredProperties().isEmpty()) { // TODO Null or empty check??
                Registries.BEDROCK_ENTITY_PROPERTIES.get().add(definition.registeredProperties().toNbtMap(definition.identifier()));
            }
        }
    }

    private static <T> void registerProperty(Identifier BuiltinEntityType, PropertyType<T, ?> property) {
        var definition = Registries.JAVA_ENTITY_IDENTIFIERS.get(BuiltinEntityType.toString());
        if (definition == null) {
            throw new IllegalArgumentException("Unknown entity type: " + BuiltinEntityType);
        }

        definition.registeredProperties().add(BuiltinEntityType.toString(), property);
    }

    private EntityDefinitions() {
    }
}
