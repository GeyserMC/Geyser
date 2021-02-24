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

package org.geysermc.connector.entity.type;

import lombok.Getter;
import org.geysermc.connector.entity.*;
import org.geysermc.connector.entity.living.*;
import org.geysermc.connector.entity.living.animal.*;
import org.geysermc.connector.entity.living.animal.horse.*;
import org.geysermc.connector.entity.living.animal.tameable.CatEntity;
import org.geysermc.connector.entity.living.animal.tameable.ParrotEntity;
import org.geysermc.connector.entity.living.animal.tameable.WolfEntity;
import org.geysermc.connector.entity.living.merchant.AbstractMerchantEntity;
import org.geysermc.connector.entity.living.merchant.VillagerEntity;
import org.geysermc.connector.entity.living.monster.*;
import org.geysermc.connector.entity.living.monster.raid.AbstractIllagerEntity;
import org.geysermc.connector.entity.living.monster.raid.PillagerEntity;
import org.geysermc.connector.entity.living.monster.raid.RaidParticipantEntity;
import org.geysermc.connector.entity.living.monster.raid.SpellcasterIllagerEntity;
import org.geysermc.connector.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum EntityType {

    CHICKEN(AnimalEntity.class, 10, 0.7f, 0.4f),
    COW(AnimalEntity.class, 11, 1.4f, 0.9f),
    PIG(PigEntity.class, 12, 0.9f),
    SHEEP(SheepEntity.class, 13, 1.3f, 0.9f),
    WOLF(WolfEntity.class, 14, 0.85f, 0.6f),
    VILLAGER(VillagerEntity.class, 15, 1.8f, 0.6f, 0.6f, 1.62f, "minecraft:villager_v2"),
    MOOSHROOM(MooshroomEntity.class, 16, 1.4f, 0.9f),
    SQUID(SquidEntity.class, 17, 0.8f),
    RABBIT(RabbitEntity.class, 18, 0.5f, 0.4f),
    BAT(BatEntity.class, 19, 0.9f, 0.5f),
    IRON_GOLEM(GolemEntity.class, 20, 2.7f, 1.4f),
    SNOW_GOLEM(SnowGolemEntity.class, 21, 1.9f, 0.7f),
    OCELOT(OcelotEntity.class, 22, 0.35f, 0.3f),
    HORSE(HorseEntity.class, 23, 1.6f, 1.3965f),
    DONKEY(ChestedHorseEntity.class, 24, 1.6f, 1.3965f),
    MULE(ChestedHorseEntity.class, 25, 1.6f, 1.3965f),
    SKELETON_HORSE(AbstractHorseEntity.class, 26, 1.6f, 1.3965f),
    ZOMBIE_HORSE(AbstractHorseEntity.class, 27, 1.6f, 1.3965f),
    POLAR_BEAR(PolarBearEntity.class, 28, 1.4f, 1.3f),
    LLAMA(LlamaEntity.class, 29, 1.87f, 0.9f),
    TRADER_LLAMA(TraderLlamaEntity.class, 29, 1.187f, 0.9f, 0f, 0f, "minecraft:llama"),
    PARROT(ParrotEntity.class, 30, 0.9f, 0.5f),
    DOLPHIN(WaterEntity.class, 31, 0.6f, 0.9f),
    ZOMBIE(ZombieEntity.class, 32, 1.8f, 0.6f, 0.6f, 1.62f),
    GIANT(GiantEntity.class, 32, 1.8f, 0.6f, 0.6f, 1.62f, "minecraft:zombie"),
    CREEPER(CreeperEntity.class, 33, 1.7f, 0.6f, 0.6f, 1.62f),
    SKELETON(AbstractSkeletonEntity.class, 34, 1.8f, 0.6f, 0.6f, 1.62f),
    SPIDER(SpiderEntity.class, 35, 0.9f, 1.4f, 1.4f, 1f),
    ZOMBIFIED_PIGLIN(ZombifiedPiglinEntity.class, 36, 1.95f, 0.6f, 0.6f, 1.62f, "minecraft:zombie_pigman"),
    SLIME(SlimeEntity.class, 37, 0.51f),
    ENDERMAN(EndermanEntity.class, 38, 2.9f, 0.6f),
    SILVERFISH(MonsterEntity.class, 39, 0.3f, 0.4f),
    CAVE_SPIDER(MonsterEntity.class, 40, 0.5f, 0.7f),
    GHAST(GhastEntity.class, 41, 4.0f),
    MAGMA_CUBE(MagmaCubeEntity.class, 42, 0.51f),
    BLAZE(BlazeEntity.class, 43, 1.8f, 0.6f),
    ZOMBIE_VILLAGER(ZombieVillagerEntity.class, 44, 1.8f, 0.6f, 0.6f, 1.62f, "minecraft:zombie_villager_v2"),
    WITCH(RaidParticipantEntity.class, 45, 1.8f, 0.6f, 0.6f, 1.62f),
    STRAY(AbstractSkeletonEntity.class, 46, 1.8f, 0.6f, 0.6f, 1.62f),
    HUSK(ZombieEntity.class, 47, 1.8f, 0.6f, 0.6f, 1.62f),
    WITHER_SKELETON(AbstractSkeletonEntity.class, 48, 2.4f, 0.7f),
    GUARDIAN(GuardianEntity.class, 49, 0.85f),
    ELDER_GUARDIAN(ElderGuardianEntity.class, 50, 1.9975f),
    NPC(PlayerEntity.class, 51, 1.8f, 0.6f, 0.6f, 1.62f),
    WITHER(WitherEntity.class, 52, 3.5f, 0.9f),
    ENDER_DRAGON(EnderDragonEntity.class, 53, 0f, 0f),
    SHULKER(ShulkerEntity.class, 54, 1f, 1f),
    ENDERMITE(MonsterEntity.class, 55, 0.3f, 0.4f),
    AGENT(Entity.class, 56, 0f),
    VINDICATOR(AbstractIllagerEntity.class, 57, 1.8f, 0.6f, 0.6f, 1.62f),
    PILLAGER(PillagerEntity.class, 114, 1.8f, 0.6f, 0.6f, 1.62f),
    WANDERING_TRADER(AbstractMerchantEntity.class, 118, 1.8f, 0.6f, 0.6f, 1.62f),
    PHANTOM(FlyingEntity.class, 58, 0.5f, 0.9f, 0.9f, 0.6f),
    RAVAGER(RaidParticipantEntity.class, 59, 1.9f, 1.2f),

    ARMOR_STAND(ArmorStandEntity.class, 61, 1.975f, 0.5f),
    TRIPOD_CAMERA(Entity.class, 62, 0f),
    PLAYER(PlayerEntity.class, 63, 1.8f, 0.6f, 0.6f, 1.62f),
    ITEM(ItemEntity.class, 64, 0.25f, 0.25f, 0.25f, 0.125f),
    PRIMED_TNT(TNTEntity.class, 65, 0.98f, 0.98f, 0.98f, 0f, "minecraft:tnt"),
    FALLING_BLOCK(FallingBlockEntity.class, 66, 0.98f, 0.98f),
    MOVING_BLOCK(Entity.class, 67, 0f),
    THROWN_EXP_BOTTLE(ThrowableEntity.class, 68, 0.25f, 0.25f, 0f, 0f, "minecraft:xp_bottle"),
    EXPERIENCE_ORB(ExpOrbEntity.class, 69, 0f, 0f, 0f, 0f, "minecraft:xp_orb"),
    EYE_OF_ENDER(Entity.class, 70, 0.25f, 0.25f, 0f, 0f, "minecraft:eye_of_ender_signal"),
    END_CRYSTAL(EnderCrystalEntity.class, 71, 2.0f, 2.0f, 2.0f, 0f, "minecraft:ender_crystal"),
    FIREWORK_ROCKET(FireworkEntity.class, 72, 0.25f, 0.25f, 0.25f, 0f, "minecraft:fireworks_rocket"),
    TRIDENT(TridentEntity.class, 73, 0f, 0f, 0f, 0f, "minecraft:thrown_trident"),
    TURTLE(TurtleEntity.class, 74, 0.4f, 1.2f),
    CAT(CatEntity.class, 75, 0.35f, 0.3f),
    SHULKER_BULLET(ThrowableEntity.class, 76, 0.3125f),
    FISHING_BOBBER(FishingHookEntity.class, 77, 0f, 0f, 0f, 0f, "minecraft:fishing_hook"),
    CHALKBOARD(Entity.class, 78, 0f),
    DRAGON_FIREBALL(ItemedFireballEntity.class, 79, 1.0f),
    ARROW(TippedArrowEntity.class, 80, 0.25f, 0.25f),
    SPECTRAL_ARROW(AbstractArrowEntity.class, 80, 0.25f, 0.25f, 0.25f, 0f, "minecraft:arrow"),
    SNOWBALL(ThrowableEntity.class, 81, 0.25f),
    THROWN_EGG(ThrowableEntity.class, 82, 0.25f, 0.25f, 0.25f, 0f, "minecraft:egg"),
    PAINTING(PaintingEntity.class, 83, 0f),
    MINECART(MinecartEntity.class, 84, 0.7f, 0.98f, 0.98f, 0.35f),
    FIREBALL(ItemedFireballEntity.class, 85, 1.0f),
    THROWN_POTION(ThrownPotionEntity.class, 86, 0.25f, 0.25f, 0.25f, 0f, "minecraft:splash_potion"),
    THROWN_ENDERPEARL(ThrowableEntity.class, 87, 0.25f, 0.25f, 0.25f, 0f, "minecraft:ender_pearl"),
    LEASH_KNOT(LeashKnotEntity.class, 88, 0.5f, 0.375f),
    WITHER_SKULL(WitherSkullEntity.class, 89, 0.3125f),
    BOAT(BoatEntity.class, 90, 0.7f, 1.6f, 1.6f, 0.35f),
    WITHER_SKULL_DANGEROUS(WitherSkullEntity.class, 91, 0f),
    LIGHTNING_BOLT(Entity.class, 93, 0f),
    SMALL_FIREBALL(ItemedFireballEntity.class, 94, 0.3125f),
    AREA_EFFECT_CLOUD(AreaEffectCloudEntity.class, 95, 0.5f, 1.0f),
    MINECART_HOPPER(MinecartEntity.class, 96, 0.7f, 0.98f, 0.98f, 0.35f, "minecraft:hopper_minecart"),
    MINECART_TNT(MinecartEntity.class, 97, 0.7f, 0.98f, 0.98f, 0.35f, "minecraft:tnt_minecart"),
    MINECART_CHEST(MinecartEntity.class, 98, 0.7f, 0.98f, 0.98f, 0.35f, "minecraft:chest_minecart"),
    MINECART_FURNACE(FurnaceMinecartEntity.class, 98, 0.7f, 0.98f, 0.98f, 0.35f, "minecraft:minecart"),
    MINECART_SPAWNER(SpawnerMinecartEntity.class, 98, 0.7f, 0.98f, 0.98f, 0.35f, "minecraft:minecart"),
    MINECART_COMMAND_BLOCK(CommandBlockMinecartEntity.class, 100, 0.7f, 0.98f, 0.98f, 0.35f, "minecraft:command_block_minecart"),
    LINGERING_POTION(ThrowableEntity.class, 101, 0f),
    LLAMA_SPIT(ThrowableEntity.class, 102, 0.25f),
    EVOKER_FANGS(Entity.class, 103, 0.8f, 0.5f, 0.5f, 0f, "minecraft:evocation_fang"),
    EVOKER(SpellcasterIllagerEntity.class, 104, 1.95f, 0.6f, 0.6f, 0f, "minecraft:evocation_illager"),
    VEX(VexEntity.class, 105, 0.8f, 0.4f),
    ICE_BOMB(Entity.class, 106, 0f),
    BALLOON(Entity.class, 107, 0f), //TODO
    PUFFERFISH(PufferFishEntity.class, 108, 0.7f, 0.7f),
    SALMON(AbstractFishEntity.class, 109, 0.5f, 0.7f),
    DROWNED(ZombieEntity.class, 110, 1.95f, 0.6f),
    TROPICAL_FISH(TropicalFishEntity.class, 111, 0.6f, 0.6f, 0f, 0f, "minecraft:tropicalfish"),
    COD(AbstractFishEntity.class, 112, 0.25f, 0.5f),
    PANDA(PandaEntity.class, 113, 1.25f, 1.125f, 1.825f),
    FOX(FoxEntity.class, 121, 0.5f, 1.25f),
    BEE(BeeEntity.class, 122, 0.6f, 0.6f),
    STRIDER(StriderEntity.class, 125, 1.7f, 0.9f, 0f, 0f, "minecraft:strider"),
    HOGLIN(HoglinEntity.class, 124, 1.4f, 1.3965f, 1.3965f, 0f, "minecraft:hoglin"),
    ZOGLIN(ZoglinEntity.class, 126, 1.4f, 1.3965f, 1.3965f, 0f, "minecraft:zoglin"),
    PIGLIN(PiglinEntity.class, 123, 1.95f, 0.6f, 0.6f, 0f, "minecraft:piglin"),
    PIGLIN_BRUTE(BasePiglinEntity.class, 127, 1.95f, 0.6f, 0.6f, 0f, "minecraft:piglin_brute"),

    /**
     * Item frames are handled differently since they are a block in Bedrock.
     */
    ITEM_FRAME(ItemFrameEntity.class, 0, 0, 0),

    /**
     * Not an entity in Bedrock, so we replace it with a Pillager
     */
    ILLUSIONER(AbstractIllagerEntity.class, 114, 1.8f, 0.6f, 0.6f, 1.62f, "minecraft:pillager"),

    /**
     * Not an entity in Bedrock, but used for the Ender Dragon's multiple hitboxes
     */
    ENDER_DRAGON_PART(EnderDragonPartEntity.class, 32, 0, 0, 0, 0, "minecraft:armor_stand");

    /**
     * A list of all Java identifiers for use with command suggestions
     */
    public static final String[] ALL_JAVA_IDENTIFIERS;
    private static final EntityType[] VALUES = values();

    static {
        List<String> allJavaIdentifiers = new ArrayList<>();
        for (EntityType type : values()) {
            if (type == AGENT || type == BALLOON || type == CHALKBOARD || type == NPC || type == TRIPOD_CAMERA || type == ENDER_DRAGON_PART) {
                continue;
            }
            allJavaIdentifiers.add("minecraft:" + type.name().toLowerCase());
        }
        ALL_JAVA_IDENTIFIERS = allJavaIdentifiers.toArray(new String[0]);
    }

    private final Class<? extends Entity> entityClass;
    private final int type;
    private final float height;
    private final float width;
    private final float length;
    private final float offset;
    private final String identifier;

    EntityType(Class<? extends Entity> entityClass, int type, float height) {
        //noinspection SuspiciousNameCombination
        this(entityClass, type, height, height);
    }

    EntityType(Class<? extends Entity> entityClass, int type, float height, float width) {
        this(entityClass, type, height, width, width);
    }

    EntityType(Class<? extends Entity> entityClass, int type, float height, float width, float length) {
        this(entityClass, type, height, width, length, 0f);
    }

    EntityType(Class<? extends Entity> entityClass, int type, float height, float width, float length, float offset) {
        this(entityClass, type, height, width, length, offset, null);
    }

    EntityType(Class<? extends Entity> entityClass, int type, float height, float width, float length, float offset, String identifier) {
        this.entityClass = entityClass;
        this.type = type;
        this.height = height;
        this.width = width;
        this.length = length;
        this.offset = offset + 0.00001f;
        this.identifier = identifier == null ? "minecraft:" + name().toLowerCase() : identifier;
    }

    public static EntityType getFromIdentifier(String identifier) {
        for (EntityType type : VALUES) {
            if (type.identifier.equals(identifier)) {
                return type;
            }
        }

        return null;
    }
}
