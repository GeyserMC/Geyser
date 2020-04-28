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

package org.geysermc.connector.entity.type;

import lombok.Getter;
import org.geysermc.connector.entity.*;
import org.geysermc.connector.entity.living.*;
import org.geysermc.connector.entity.living.animal.*;
import org.geysermc.connector.entity.living.animal.horse.*;
import org.geysermc.connector.entity.living.animal.tameable.*;
import org.geysermc.connector.entity.living.merchant.*;
import org.geysermc.connector.entity.living.monster.*;
import org.geysermc.connector.entity.living.monster.raid.AbstractIllagerEntity;
import org.geysermc.connector.entity.living.monster.raid.RaidParticipantEntity;
import org.geysermc.connector.entity.living.monster.raid.SpellcasterIllagerEntity;

@Getter
public enum EntityType {

    CHICKEN(AnimalEntity.class, 10, 0.7f, 0.4f),
    COW(AnimalEntity.class, 11, 1.4f, 0.9f),
    PIG(PigEntity.class, 12, 0.9f),
    SHEEP(SheepEntity.class, 13, 1.3f, 0.9f),
    WOLF(WolfEntity.class, 14, 0.85f, 0.6f),
    VILLAGER(VillagerEntity.class, 15, 1.8f, 0.6f, 0.6f, 1.62f),
    MOOSHROOM(AnimalEntity.class, 16, 1.4f, 0.9f),
    SQUID(WaterEntity.class, 17, 0.8f),
    RABBIT(RabbitEntity.class, 18, 0.5f, 0.4f),
    BAT(AmbientEntity.class, 19, 0.9f, 0.5f),
    IRON_GOLEM(GolemEntity.class, 20, 2.7f, 1.4f),
    SNOW_GOLEM(GolemEntity.class, 21, 1.9f, 0.7f),
    OCELOT(OcelotEntity.class, 22, 0.35f, 0.3f),
    HORSE(HorseEntity.class, 23, 1.6f, 1.3965f),
    DONKEY(ChestedHorseEntity.class, 24, 1.6f, 1.3965f),
    MULE(ChestedHorseEntity.class, 25, 1.6f, 1.3965f),
    SKELETON_HORSE(AbstractHorseEntity.class, 26, 1.6f, 1.3965f),
    ZOMBIE_HORSE(AbstractHorseEntity.class, 27, 1.6f, 1.3965f),
    POLAR_BEAR(PolarBearEntity.class, 28, 1.4f, 1.3f),
    LLAMA(LlamaEntity.class, 29, 1.87f, 0.9f),
    TRADER_LLAMA(TraderLlamaEntity.class, 29, 1.187f, 0.9f),
    PARROT(ParrotEntity.class, 30, 0.9f, 0.5f),
    DOLPHIN(WaterEntity.class, 31, 0.6f, 0.9f),
    ZOMBIE(ZombieEntity.class, 32, 1.8f, 0.6f, 0.6f, 1.62f),
    CREEPER(CreeperEntity.class, 33, 1.7f, 0.6f, 0.6f, 1.62f),
    SKELETON(AbstractSkeletonEntity.class, 34, 1.8f, 0.6f, 0.6f, 1.62f),
    SPIDER(SpiderEntity.class, 35, 0.9f, 1.4f, 1.4f, 1f),
    ZOMBIE_PIGMAN(MonsterEntity.class, 36, 1.8f, 0.6f, 0.6f, 1.62f),
    SLIME(InsentientEntity.class, 37, 0.51f),
    ENDERMAN(EndermanEntity.class, 38, 2.9f, 0.6f),
    SILVERFISH(MonsterEntity.class, 39, 0.3f, 0.4f),
    CAVE_SPIDER(MonsterEntity.class, 40, 0.5f, 0.7f),
    GHAST(FlyingEntity.class, 41, 4.0f),
    MAGMA_CUBE(InsentientEntity.class, 42, 0.51f),
    BLAZE(BlazeEntity.class, 43, 1.8f, 0.6f),
    ZOMBIE_VILLAGER(ZombieEntity.class, 44, 1.8f, 0.6f, 0.6f, 1.62f),
    WITCH(RaidParticipantEntity.class, 45, 1.8f, 0.6f, 0.6f, 1.62f),
    STRAY(AbstractSkeletonEntity.class, 46, 1.8f, 0.6f, 0.6f, 1.62f),
    HUSK(ZombieEntity.class, 47, 1.8f, 0.6f, 0.6f, 1.62f),
    WITHER_SKELETON(AbstractSkeletonEntity.class, 48, 2.4f, 0.7f),
    GUARDIAN(GuardianEntity.class, 49, 0.85f),
    ELDER_GUARDIAN(GuardianEntity.class, 50, 1.9975f),
    NPC(PlayerEntity.class, 51, 1.8f, 0.6f, 0.6f, 1.62f),
    WITHER(MonsterEntity.class, 52, 3.5f, 0.9f),
    ENDER_DRAGON(EnderDragonEntity.class, 53, 4f, 13f),
    SHULKER(ShulkerEntity.class, 54, 1f, 1f),
    ENDERMITE(MonsterEntity.class, 55, 0.3f, 0.4f),
    AGENT(Entity.class, 56, 0f),
    VINDICATOR(AbstractIllagerEntity.class, 57, 1.8f, 0.6f, 0.6f, 1.62f),
    PILLAGER(AbstractIllagerEntity.class, 114, 1.8f, 0.6f, 0.6f, 1.62f),
    WANDERING_TRADER(AbstractMerchantEntity.class, 118, 1.8f, 0.6f, 0.6f, 1.62f),
    PHANTOM(FlyingEntity.class, 58, 0.5f, 0.9f, 0.9f, 0.6f),
    RAVAGER(RaidParticipantEntity.class, 59, 1.9f, 1.2f),

    ARMOR_STAND(ArmorStandEntity.class, 61, 0f),
    TRIPOD_CAMERA(Entity.class, 62, 0f),
    PLAYER(PlayerEntity.class, 63, 1.8f, 0.6f, 0.6f, 1.62f),
    ITEM(ItemEntity.class, 64, 0.25f, 0.25f),
    TNT(Entity.class, 65, 0.98f, 0.98f),
    FALLING_BLOCK(FallingBlockEntity.class, 66, 0.98f, 0.98f),
    MOVING_BLOCK(Entity.class, 67, 0f),
    EXPERIENCE_BOTTLE(ThrowableEntity.class, 68, 0.25f, 0.25f),
    EXPERIENCE_ORB(ExpOrbEntity.class, 69, 0f),
    EYE_OF_ENDER(Entity.class, 70, 0f),
    END_CRYSTAL(EnderCrystalEntity.class, 71, 0f),
    FIREWORK_ROCKET(Entity.class, 72, 0f),
    TRIDENT(ArrowEntity.class, 73, 0f),
    TURTLE(AnimalEntity.class, 74, 0.4f, 1.2f),
    CAT(CatEntity.class, 75, 0.35f, 0.3f),
    SHULKER_BULLET(Entity.class, 76, 0f),
    FISHING_BOBBER(FishingHookEntity.class, 77, 0f),
    CHALKBOARD(Entity.class, 78, 0f),
    DRAGON_FIREBALL(ItemedFireballEntity.class, 79, 0f),
    ARROW(ArrowEntity.class, 80, 0.25f, 0.25f),
    SNOWBALL(ThrowableEntity.class, 81, 0f),
    EGG(ThrowableEntity.class, 82, 0f),
    PAINTING(PaintingEntity.class, 83, 0f),
    MINECART(MinecartEntity.class, 84, 0.7f, 0.98f, 0.98f, 0.35f),
    FIREBALL(ItemedFireballEntity.class, 85, 0f),
    POTION(ThrowableEntity.class, 86, 0f),
    ENDER_PEARL(ThrowableEntity.class, 87, 0f),
    LEASH_KNOT(Entity.class, 88, 0f),
    WITHER_SKULL(Entity.class, 89, 0f),
    BOAT(BoatEntity.class, 90, 0.7f, 1.6f, 1.6f, 0.375f),
    WITHER_SKULL_DANGEROUS(Entity.class, 91, 0f),
    LIGHTNING_BOLT(Entity.class, 93, 0f),
    SMALL_FIREBALL(ItemedFireballEntity.class, 94, 0f),
    AREA_EFFECT_CLOUD(Entity.class, 95, 0f),
    HOPPER_MINECART(MinecartEntity.class, 96, 0.7f, 0.98f, 0.98f, 0.35f),
    TNT_MINECART(MinecartEntity.class, 97, 0.7f, 0.98f, 0.98f, 0.35f),
    CHEST_MINECART(MinecartEntity.class, 98, 0.7f, 0.98f, 0.98f, 0.35f),

    COMMAND_BLOCK_MINECART(MinecartEntity.class, 100, 0.7f, 0.98f, 0.98f, 0.35f),
    LINGERING_POTION(ThrowableEntity.class, 101, 0f),
    LLAMA_SPIT(Entity.class, 102, 0f),
    EVOKER_FANGS(Entity.class, 103, 0f),
    EVOKER(SpellcasterIllagerEntity.class, 104, 0f),
    VEX(MonsterEntity.class, 105, 0f),
    ICE_BOMB(Entity.class, 106, 0f),
    BALLOON(Entity.class, 107, 0f), //TODO
    PUFFERFISH(PufferFishEntity.class, 108, 0.7f, 0.7f),
    SALMON(AbstractFishEntity.class, 109, 0.5f, 0.7f),
    DROWNED(ZombieEntity.class, 110, 1.95f, 0.6f),
    TROPICAL_FISH(TropicalFishEntity.class, 111, 0.6f, 0.6f),
    COD(AbstractFishEntity.class, 112, 0.25f, 0.5f),
    PANDA(PandaEntity.class, 113, 1.25f, 1.125f, 1.825f),
    FOX(FoxEntity.class, 121, 0.5f, 1.25f),
    BEE(BeeEntity.class, 122, 0.6f, 0.6f);

    private Class<? extends Entity> entityClass;
    private final int type;
    private final float height;
    private final float width;
    private final float length;
    private final float offset;

    EntityType(Class<? extends Entity> entityClass, int type, float height) {
        this(entityClass, type, height, 0f);
    }

    EntityType(Class<? extends Entity> entityClass, int type, float height, float width) {
        this(entityClass, type, height, width, width);
    }

    EntityType(Class<? extends Entity> entityClass, int type, float height, float width, float length) {
        this(entityClass, type, height, width, length, 0f);
    }

    EntityType(Class<? extends Entity> entityClass, int type, float height, float width, float length, float offset) {
        this.entityClass = entityClass;
        this.type = type;
        this.height = height;
        this.width = width;
        this.length = length;
        this.offset = offset + 0.00001f;
    }
}
