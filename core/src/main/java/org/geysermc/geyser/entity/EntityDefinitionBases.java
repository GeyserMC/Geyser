/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
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
import org.geysermc.geyser.entity.type.BoatEntity;
import org.geysermc.geyser.entity.type.ChestBoatEntity;
import org.geysermc.geyser.entity.type.DisplayBaseEntity;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.FireballEntity;
import org.geysermc.geyser.entity.type.HangingEntity;
import org.geysermc.geyser.entity.type.LivingEntity;
import org.geysermc.geyser.entity.type.ThrowableItemEntity;
import org.geysermc.geyser.entity.type.living.AbstractFishEntity;
import org.geysermc.geyser.entity.type.living.AgeableEntity;
import org.geysermc.geyser.entity.type.living.MobEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.AbstractHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.tameable.TameableEntity;
import org.geysermc.geyser.entity.type.living.monster.BasePiglinEntity;
import org.geysermc.geyser.entity.type.living.monster.raid.RaidParticipantEntity;
import org.geysermc.geyser.entity.type.living.monster.raid.SpellcasterIllagerEntity;
import org.geysermc.geyser.entity.type.player.AvatarEntity;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.FloatEntityMetadata;

public final class EntityDefinitionBases {
    public static final EntityDefinitionBase<Entity> ENTITY;
    public static final EntityDefinitionBase<DisplayBaseEntity> DISPLAY;
    public static final EntityDefinitionBase<FireballEntity> FIREBALL;
    public static final EntityDefinitionBase<ThrowableItemEntity> THROWABLE;
    public static final EntityDefinitionBase<HangingEntity> HANGING;
    public static final EntityDefinitionBase<BoatEntity> BOAT;
    public static final EntityDefinitionBase<ChestBoatEntity> CHEST_BOAT;
    public static final EntityDefinitionBase<LivingEntity> LIVING_ENTITY;
    public static final EntityDefinitionBase<AvatarEntity> AVATAR;
    public static final EntityDefinitionBase<MobEntity> MOB;
    public static final EntityDefinitionBase<AbstractFishEntity> FISH;
    public static final EntityDefinitionBase<BasePiglinEntity> PIGLIN;
    public static final EntityDefinitionBase<RaidParticipantEntity> RAID_PARTICIPANT;
    public static final EntityDefinitionBase<SpellcasterIllagerEntity> SPELLCASTER;
    public static final EntityDefinitionBase<AgeableEntity> AGEABLE;
    public static final EntityDefinitionBase<AbstractHorseEntity> HORSE;
    public static final EntityDefinitionBase<TameableEntity> TAMABLE;

    static {
        ENTITY = EntityDefinition.baseBuilder(Entity.class)
            .addTranslator(MetadataTypes.BYTE, Entity::setFlags)
            .addTranslator(MetadataTypes.INT, Entity::setAir) // Air/bubbles
            .addTranslator(MetadataTypes.OPTIONAL_COMPONENT, Entity::setDisplayName)
            .addTranslator(MetadataTypes.BOOLEAN, Entity::setDisplayNameVisible)
            .addTranslator(MetadataTypes.BOOLEAN, Entity::setSilent)
            .addTranslator(MetadataTypes.BOOLEAN, Entity::setGravity)
            .addTranslator(MetadataTypes.POSE, (entity, entityMetadata) -> entity.setPose(entityMetadata.getValue()))
            .addTranslator(MetadataTypes.INT, Entity::setFreezing)
            .build();
        DISPLAY = EntityDefinitionBase.baseInherited(DisplayBaseEntity.class, ENTITY)
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
        FIREBALL = EntityDefinitionBase.baseInherited(FireballEntity.class, ENTITY)
            .addTranslator(null) // Item
            .build();
        THROWABLE = EntityDefinitionBase.baseInherited(ThrowableItemEntity.class, ENTITY)
            .addTranslator(MetadataTypes.ITEM_STACK, ThrowableItemEntity::setItem)
            .build();
        HANGING = EntityDefinitionBase.baseInherited(HangingEntity.class, ENTITY)
            .addTranslator(MetadataTypes.DIRECTION, HangingEntity::setDirectionMetadata)
            .build();
        BOAT = EntityDefinitionBase.baseInherited(BoatEntity.class, ENTITY)
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
        CHEST_BOAT = EntityDefinitionBase.baseInherited(ChestBoatEntity.class, BOAT)
            .build();
        LIVING_ENTITY = EntityDefinitionBase.baseInherited(LivingEntity.class, ENTITY)
            .addTranslator(MetadataTypes.BYTE, LivingEntity::setLivingEntityFlags)
            .addTranslator(MetadataTypes.FLOAT, LivingEntity::setHealth)
            .addTranslator(MetadataTypes.PARTICLES, LivingEntity::setParticles)
            .addTranslator(MetadataTypes.BOOLEAN,
                (livingEntity, entityMetadata) -> livingEntity.getDirtyMetadata().put(EntityDataTypes.EFFECT_AMBIENCE, (byte) (((BooleanEntityMetadata) entityMetadata).getPrimitiveValue() ? 1 : 0)))
            .addTranslator(null) // Arrow count
            .addTranslator(null) // Stinger count
            .addTranslator(MetadataTypes.OPTIONAL_BLOCK_POS, LivingEntity::setBedPosition)
            .build();
        AVATAR = EntityDefinitionBase.baseInherited(AvatarEntity.class, LIVING_ENTITY)
            .height(1.8f).width(0.6f)
            .offset(1.62f)
            .addTranslator(null) // Player main hand
            .addTranslator(MetadataTypes.BYTE, AvatarEntity::setSkinVisibility)
            .build();
        MOB = EntityDefinitionBase.baseInherited(MobEntity.class, LIVING_ENTITY)
            .addTranslator(MetadataTypes.BYTE, MobEntity::setMobFlags)
            .build();
        FISH = EntityDefinitionBase.baseInherited(AbstractFishEntity.class, MOB)
            .addTranslator(null) // From bucket
            .build();
        PIGLIN = EntityDefinitionBase.baseInherited(BasePiglinEntity.class, MOB)
            .addTranslator(MetadataTypes.BOOLEAN, BasePiglinEntity::setImmuneToZombification)
            .build();
        RAID_PARTICIPANT = EntityDefinitionBase.baseInherited(RaidParticipantEntity.class, MOB)
            .addTranslator(null) // Celebrating //TODO
            .build();
        SPELLCASTER = EntityDefinitionBase.baseInherited(SpellcasterIllagerEntity.class, RAID_PARTICIPANT)
            .addTranslator(MetadataTypes.BYTE, SpellcasterIllagerEntity::setSpellType)
            .build();
        AGEABLE = EntityDefinitionBase.baseInherited(AgeableEntity.class, MOB)
            .addTranslator(MetadataTypes.BOOLEAN, AgeableEntity::setBaby)
            .build();
        HORSE = EntityDefinitionBase.baseInherited(AbstractHorseEntity.class, AGEABLE)
            .addTranslator(MetadataTypes.BYTE, AbstractHorseEntity::setHorseFlags)
            .build();
        TAMABLE = EntityDefinitionBase.baseInherited(TameableEntity.class, AGEABLE)
            .addTranslator(MetadataTypes.BYTE, TameableEntity::setTameableFlags)
            .addTranslator(MetadataTypes.OPTIONAL_LIVING_ENTITY_REFERENCE, TameableEntity::setOwner)
            .build();
    }

    private EntityDefinitionBases() {
    }
}
