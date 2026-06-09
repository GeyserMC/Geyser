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

package org.geysermc.geyser.item.hashing;

import com.google.common.hash.HashCode;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistryProvider;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.AttackRange;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.BlockStateProperties;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.BlocksAttacks;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.CustomModelData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Equippable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Fireworks;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.FoodProperties;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.IntComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemAttributeModifiers;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.KineticWeapon;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.LodestoneTracker;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.PiercingWeapon;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.PotionContents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.SwingAnimation;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.TooltipDisplay;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.TypedEntityData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Unit;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.UseCooldown;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.UseEffects;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Weapon;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.WritableBookContent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.WrittenBookContent;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public class DataComponentHashers {
    @VisibleForTesting
    public static final Set<DataComponentType<?>> NOT_HASHED = Set.of(DataComponentTypes.CREATIVE_SLOT_LOCK, DataComponentTypes.MAP_POST_PROCESSING, DataComponentTypes.ADDITIONAL_TRADE_COST);
    private static final Map<DataComponentType<?>, MinecraftHasher<?>> hashers = new HashMap<>();

    static {
        register(DataComponentTypes.CUSTOM_DATA, MinecraftHasher.NBT_MAP);
        registerInt(DataComponentTypes.MAX_STACK_SIZE);
        registerInt(DataComponentTypes.MAX_DAMAGE);
        registerInt(DataComponentTypes.DAMAGE);
        registerUnit(DataComponentTypes.UNBREAKABLE);
        registerMap(DataComponentTypes.USE_EFFECTS, builder -> builder
            .optional("can_sprint", MinecraftHasher.BOOL, UseEffects::canSprint, false)
            .optional("interact_vibrations", MinecraftHasher.BOOL, UseEffects::interactVibrations, true)
            .optional("speed_multiplier", MinecraftHasher.FLOAT, UseEffects::speedMultiplier, 0.2F));

        register(DataComponentTypes.CUSTOM_NAME, ComponentHasher.COMPONENT);
        register(DataComponentTypes.MINIMUM_ATTACK_CHARGE, MinecraftHasher.FLOAT);
        register(DataComponentTypes.DAMAGE_TYPE, RegistryHasher.DAMAGE_TYPE);
        register(DataComponentTypes.ITEM_NAME, ComponentHasher.COMPONENT);
        register(DataComponentTypes.ITEM_MODEL, MinecraftHasher.KEY);
        register(DataComponentTypes.LORE, ComponentHasher.COMPONENT.list());
        register(DataComponentTypes.RARITY, MinecraftHasher.RARITY);
        register(DataComponentTypes.ENCHANTMENTS, RegistryHasher.ITEM_ENCHANTMENTS);

        register(DataComponentTypes.CAN_PLACE_ON, RegistryHasher.ADVENTURE_MODE_PREDICATE);
        register(DataComponentTypes.CAN_BREAK, RegistryHasher.ADVENTURE_MODE_PREDICATE);
        register(DataComponentTypes.ATTRIBUTE_MODIFIERS, RegistryHasher.ATTRIBUTE_MODIFIER_ENTRY.list().cast(ItemAttributeModifiers::getModifiers));

        registerMap(DataComponentTypes.CUSTOM_MODEL_DATA, builder -> builder
            .optionalList("floats", MinecraftHasher.FLOAT, CustomModelData::floats)
            .optionalList("flags", MinecraftHasher.BOOL, CustomModelData::flags)
            .optionalList("strings", MinecraftHasher.STRING, CustomModelData::strings)
            .optionalList("colors", MinecraftHasher.INT, CustomModelData::colors));

        registerMap(DataComponentTypes.TOOLTIP_DISPLAY, builder -> builder
            .optional("hide_tooltip", MinecraftHasher.BOOL, TooltipDisplay::hideTooltip, false)
            .optionalList("hidden_components", RegistryHasher.DATA_COMPONENT_TYPE, TooltipDisplay::hiddenComponents));

        registerInt(DataComponentTypes.REPAIR_COST);

        register(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, MinecraftHasher.BOOL);
        registerUnit(DataComponentTypes.INTANGIBLE_PROJECTILE);

        registerMap(DataComponentTypes.FOOD, builder -> builder
            .accept("nutrition", MinecraftHasher.INT, FoodProperties::getNutrition)
            .accept("saturation", MinecraftHasher.FLOAT, FoodProperties::getSaturationModifier)
            .optional("can_always_eat", MinecraftHasher.BOOL, FoodProperties::isCanAlwaysEat, false));
        registerMap(DataComponentTypes.CONSUMABLE, builder -> builder
            .optional("consume_seconds", MinecraftHasher.FLOAT, Consumable::consumeSeconds, 1.6F)
            .optional("animation", RegistryHasher.ITEM_USE_ANIMATION, Consumable::animation, Consumable.ItemUseAnimation.EAT)
            .optional("sound", RegistryHasher.SOUND_EVENT, Consumable::sound, BuiltinSound.ENTITY_GENERIC_EAT)
            .optional("has_consume_particles", MinecraftHasher.BOOL, Consumable::hasConsumeParticles, true)
            .optionalList("on_consume_effects", RegistryHasher.CONSUME_EFFECT, Consumable::onConsumeEffects));

        register(DataComponentTypes.USE_REMAINDER, RegistryHasher.ITEM_STACK);

        registerMap(DataComponentTypes.USE_COOLDOWN, builder -> builder
            .accept("seconds", MinecraftHasher.FLOAT, UseCooldown::seconds)
            .optionalNullable("cooldown_group", MinecraftHasher.KEY, UseCooldown::cooldownGroup));
        registerMap(DataComponentTypes.DAMAGE_RESISTANT, builder -> builder
            .accept("types", RegistryHasher.DAMAGE_TYPE.holderSet(), Function.identity()));
        registerMap(DataComponentTypes.TOOL, builder -> builder
            .acceptList("rules", RegistryHasher.TOOL_RULE, ToolData::getRules)
            .optional("default_mining_speed", MinecraftHasher.FLOAT, ToolData::getDefaultMiningSpeed, 1.0F)
            .optional("damage_per_block", MinecraftHasher.INT, ToolData::getDamagePerBlock, 1)
            .optional("can_destroy_blocks_in_creative", MinecraftHasher.BOOL, ToolData::isCanDestroyBlocksInCreative, true));
        registerMap(DataComponentTypes.WEAPON, builder -> builder
            .optional("item_damage_per_attack", MinecraftHasher.INT, Weapon::itemDamagePerAttack, 1)
            .optional("disable_blocking_for_seconds", MinecraftHasher.FLOAT, Weapon::disableBlockingForSeconds, 0.0F));
        registerMap(DataComponentTypes.PIERCING_WEAPON, builder -> builder
            .optional("deals_knockback", MinecraftHasher.BOOL, PiercingWeapon::dealsKnockback, true)
            .optional("dismounts", MinecraftHasher.BOOL, PiercingWeapon::dismounts, false)
            .optionalNullable("sound", RegistryHasher.SOUND_EVENT, PiercingWeapon::sound)
            .optionalNullable("hit_sound", RegistryHasher.SOUND_EVENT, PiercingWeapon::hitSound));
        registerMap(DataComponentTypes.ATTACK_RANGE, builder -> builder
            .optional("min_reach", MinecraftHasher.FLOAT, AttackRange::minReach, 0.0F)
            .optional("max_reach", MinecraftHasher.FLOAT, AttackRange::maxReach, 3.0F)
            .optional("min_creative_reach", MinecraftHasher.FLOAT, AttackRange::minCreativeReach, 0.0F)
            .optional("max_creative_reach", MinecraftHasher.FLOAT, AttackRange::maxCreativeReach, 5.0F)
            .optional("hitbox_margin", MinecraftHasher.FLOAT, AttackRange::hitboxMargin, 0.3F)
            .optional("mob_factor", MinecraftHasher.FLOAT, AttackRange::mobFactor, 1.0F));
        registerMap(DataComponentTypes.SWING_ANIMATION, builder -> builder
            .optional("type", RegistryHasher.SWING_ANIMATION_TYPE, SwingAnimation::type, SwingAnimation.Type.WHACK)
            .optional("duration", MinecraftHasher.INT, SwingAnimation::duration, 6));
        registerMap(DataComponentTypes.ENCHANTABLE, builder -> builder
            .accept("value", MinecraftHasher.INT, Function.identity()));
        registerMap(DataComponentTypes.EQUIPPABLE, builder -> builder
            .accept("slot", MinecraftHasher.EQUIPMENT_SLOT, Equippable::slot)
            .optional("equip_sound", RegistryHasher.SOUND_EVENT, Equippable::equipSound, BuiltinSound.ITEM_ARMOR_EQUIP_GENERIC)
            .optionalNullable("asset_id", MinecraftHasher.KEY, Equippable::model)
            .optionalNullable("camera_overlay", MinecraftHasher.KEY, Equippable::cameraOverlay)
            .optionalNullable("allowed_entities", RegistryHasher.ENTITY_TYPE.holderSet(), Equippable::allowedEntities)
            .optional("dispensable", MinecraftHasher.BOOL, Equippable::dispensable, true)
            .optional("swappable", MinecraftHasher.BOOL, Equippable::swappable, true)
            .optional("damage_on_hurt", MinecraftHasher.BOOL, Equippable::damageOnHurt, true)
            .optional("equip_on_interact", MinecraftHasher.BOOL, Equippable::equipOnInteract, false)
            .optional("can_be_sheared", MinecraftHasher.BOOL, Equippable::canBeSheared, false)
            .optional("shearing_sound", RegistryHasher.SOUND_EVENT, Equippable::shearingSound, BuiltinSound.ITEM_SHEARS_SNIP));
        registerMap(DataComponentTypes.REPAIRABLE, builder -> builder
            .accept("items", RegistryHasher.ITEM.holderSet(), Function.identity()));

        registerUnit(DataComponentTypes.GLIDER);
        register(DataComponentTypes.TOOLTIP_STYLE, MinecraftHasher.KEY);

        registerMap(DataComponentTypes.DEATH_PROTECTION, builder -> builder
            .optionalList("death_effects", RegistryHasher.CONSUME_EFFECT, Function.identity()));
        registerMap(DataComponentTypes.BLOCKS_ATTACKS, builder -> builder
            .optional("block_delay_seconds", MinecraftHasher.FLOAT, BlocksAttacks::blockDelaySeconds, 0.0F)
            .optional("disable_cooldown_scale", MinecraftHasher.FLOAT, BlocksAttacks::disableCooldownScale, 1.0F)
            .optional("damage_reductions", RegistryHasher.BLOCKS_ATTACKS_DAMAGE_REDUCTION.list(), BlocksAttacks::damageReductions, List.of(new BlocksAttacks.DamageReduction(90.0F, null, 0.0F, 1.0F)))
            .optional("item_damage", RegistryHasher.BLOCKS_ATTACKS_ITEM_DAMAGE_FUNCTION, BlocksAttacks::itemDamage, new BlocksAttacks.ItemDamageFunction(1.0F, 0.0F, 1.0F))
            .optionalNullable("bypassed_by", RegistryHasher.DAMAGE_TYPE.holderSet(), BlocksAttacks::bypassedBy)
            .optionalNullable("block_sound", RegistryHasher.SOUND_EVENT, BlocksAttacks::blockSound)
            .optionalNullable("disabled_sound", RegistryHasher.SOUND_EVENT, BlocksAttacks::disableSound));
        registerMap(DataComponentTypes.KINETIC_WEAPON, builder -> builder
            .optional("contact_cooldown_ticks", MinecraftHasher.INT, KineticWeapon::contactCooldownTicks, 10)
            .optional("delay_ticks", MinecraftHasher.INT, KineticWeapon::delayTicks, 0)
            .optionalNullable("dismount_conditions", RegistryHasher.KINETIC_WEAPON_CONDITION, KineticWeapon::dismountConditions)
            .optionalNullable("knockback_conditions", RegistryHasher.KINETIC_WEAPON_CONDITION, KineticWeapon::knockbackConditions)
            .optionalNullable("damage_conditions", RegistryHasher.KINETIC_WEAPON_CONDITION, KineticWeapon::damageConditions)
            .optional("forward_movement", MinecraftHasher.FLOAT, KineticWeapon::forwardMovement, 0.0F)
            .optional("damage_multiplier", MinecraftHasher.FLOAT, KineticWeapon::damageMultiplier, 1.0F)
            .optionalNullable("sound", RegistryHasher.SOUND_EVENT, KineticWeapon::sound)
            .optionalNullable("hit_sound", RegistryHasher.SOUND_EVENT, KineticWeapon::hitSound));
        register(DataComponentTypes.STORED_ENCHANTMENTS, RegistryHasher.ITEM_ENCHANTMENTS);

        register(DataComponentTypes.DYE, MinecraftHasher.DYE_COLOR);

        registerInt(DataComponentTypes.DYED_COLOR);
        registerInt(DataComponentTypes.MAP_COLOR);
        registerInt(DataComponentTypes.MAP_ID);
        register(DataComponentTypes.MAP_DECORATIONS, MinecraftHasher.NBT_MAP);

        register(DataComponentTypes.CHARGED_PROJECTILES, RegistryHasher.ITEM_STACK.list());
        register(DataComponentTypes.BUNDLE_CONTENTS, RegistryHasher.ITEM_STACK.list());

        registerMap(DataComponentTypes.POTION_CONTENTS, builder -> builder
            .optional("potion", RegistryHasher.POTION, PotionContents::getPotionId, -1)
            .optional("custom_color", MinecraftHasher.INT, PotionContents::getCustomColor, -1)
            .optionalList("custom_effects", RegistryHasher.MOB_EFFECT_INSTANCE, PotionContents::getCustomEffects)
            .optionalNullable("custom_name", MinecraftHasher.STRING, PotionContents::getCustomName));

        register(DataComponentTypes.POTION_DURATION_SCALE, MinecraftHasher.FLOAT);
        register(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, RegistryHasher.SUSPICIOUS_STEW_EFFECT.list());

        registerMap(DataComponentTypes.WRITABLE_BOOK_CONTENT, builder -> builder
            .optionalList("pages", MinecraftHasher.STRING.filterable(), WritableBookContent::getPages));
        registerMap(DataComponentTypes.WRITTEN_BOOK_CONTENT, builder -> builder
            .accept("title", MinecraftHasher.STRING.filterable(), WrittenBookContent::getTitle)
            .accept("author", MinecraftHasher.STRING, WrittenBookContent::getAuthor)
            .optional("generation", MinecraftHasher.INT, WrittenBookContent::getGeneration, 0)
            .optionalList("pages", ComponentHasher.COMPONENT.filterable(), WrittenBookContent::getPages)
            .optional("resolved", MinecraftHasher.BOOL, WrittenBookContent::isResolved, false));

        register(DataComponentTypes.TRIM, RegistryHasher.ARMOR_TRIM);
        register(DataComponentTypes.DEBUG_STICK_STATE, MinecraftHasher.NBT_MAP);
        registerMap(DataComponentTypes.ENTITY_DATA, builder -> builder
            .accept("id", RegistryHasher.ENTITY_TYPE_KEY, TypedEntityData::type)
            .accept(TypedEntityData::tag, MapBuilder.inlineNbtMap()));
        register(DataComponentTypes.BUCKET_ENTITY_DATA, MinecraftHasher.NBT_MAP);
        registerMap(DataComponentTypes.BLOCK_ENTITY_DATA, builder -> builder
            .accept("id", RegistryHasher.BLOCK_ENTITY_TYPE_KEY, TypedEntityData::type)
            .accept(TypedEntityData::tag, MapBuilder.inlineNbtMap()));

        register(DataComponentTypes.INSTRUMENT, RegistryHasher.INSTRUMENT.holder());
        register(DataComponentTypes.PROVIDES_TRIM_MATERIAL, RegistryHasher.TRIM_MATERIAL.holder());

        registerInt(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER);

        register(DataComponentTypes.JUKEBOX_PLAYABLE, RegistryHasher.JUKEBOX_SONG.holder());
        register(DataComponentTypes.PROVIDES_BANNER_PATTERNS, RegistryHasher.BANNER_PATTERN.holderSet());
        register(DataComponentTypes.RECIPES, MinecraftHasher.NBT_LIST);

        registerMap(DataComponentTypes.LODESTONE_TRACKER, builder -> builder
            .optionalNullable("target", MinecraftHasher.GLOBAL_POS, LodestoneTracker::getPos)
            .optional("tracked", MinecraftHasher.BOOL, LodestoneTracker::isTracked, true));

        register(DataComponentTypes.FIREWORK_EXPLOSION, RegistryHasher.FIREWORK_EXPLOSION);
        registerMap(DataComponentTypes.FIREWORKS, builder -> builder
            .optional("flight_duration", MinecraftHasher.BYTE, fireworks -> (byte) fireworks.getFlightDuration(), (byte) 0)
            .optionalList("explosions", RegistryHasher.FIREWORK_EXPLOSION, Fireworks::getExplosions));

        register(DataComponentTypes.PROFILE, MinecraftHasher.RESOLVABLE_PROFILE);
        register(DataComponentTypes.NOTE_BLOCK_SOUND, MinecraftHasher.KEY);
        register(DataComponentTypes.BANNER_PATTERNS, RegistryHasher.BANNER_PATTERN_LAYER.list());
        register(DataComponentTypes.BASE_COLOR, MinecraftHasher.DYE_COLOR);
        register(DataComponentTypes.POT_DECORATIONS, RegistryHasher.ITEM.list());
        register(DataComponentTypes.CONTAINER, RegistryHasher.ITEM_CONTAINER_CONTENTS);
        register(DataComponentTypes.BLOCK_STATE, MinecraftHasher.map(MinecraftHasher.STRING, MinecraftHasher.STRING).cast(BlockStateProperties::getProperties));
        register(DataComponentTypes.BEES, RegistryHasher.BEEHIVE_OCCUPANT.list());

        register(DataComponentTypes.LOCK, MinecraftHasher.NBT_MAP);
        register(DataComponentTypes.CONTAINER_LOOT, MinecraftHasher.NBT_MAP);
        register(DataComponentTypes.BREAK_SOUND, RegistryHasher.SOUND_EVENT);

        register(DataComponentTypes.VILLAGER_VARIANT, RegistryHasher.VILLAGER_TYPE);
        register(DataComponentTypes.WOLF_VARIANT, RegistryHasher.WOLF_VARIANT);
        register(DataComponentTypes.WOLF_SOUND_VARIANT, RegistryHasher.WOLF_SOUND_VARIANT);
        register(DataComponentTypes.WOLF_COLLAR, MinecraftHasher.DYE_COLOR);
        register(DataComponentTypes.FOX_VARIANT, RegistryHasher.FOX_VARIANT);
        register(DataComponentTypes.SALMON_SIZE, RegistryHasher.SALMON_VARIANT);
        register(DataComponentTypes.PARROT_VARIANT, RegistryHasher.PARROT_VARIANT);
        register(DataComponentTypes.TROPICAL_FISH_PATTERN, RegistryHasher.TROPICAL_FISH_PATTERN);
        register(DataComponentTypes.TROPICAL_FISH_BASE_COLOR, MinecraftHasher.DYE_COLOR);
        register(DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR, MinecraftHasher.DYE_COLOR);
        register(DataComponentTypes.MOOSHROOM_VARIANT, RegistryHasher.MOOSHROOM_VARIANT);
        register(DataComponentTypes.RABBIT_VARIANT, RegistryHasher.RABBIT_VARIANT);
        register(DataComponentTypes.PIG_VARIANT, RegistryHasher.PIG_VARIANT);
        register(DataComponentTypes.PIG_SOUND_VARIANT, RegistryHasher.PIG_SOUND_VARIANT);
        register(DataComponentTypes.COW_VARIANT, RegistryHasher.COW_VARIANT);
        register(DataComponentTypes.COW_SOUND_VARIANT, RegistryHasher.COW_SOUND_VARIANT);
        register(DataComponentTypes.CHICKEN_VARIANT, RegistryHasher.CHICKEN_VARIANT);
        register(DataComponentTypes.CHICKEN_SOUND_VARIANT, RegistryHasher.CHICKEN_SOUND_VARIANT);
        register(DataComponentTypes.ZOMBIE_NAUTILUS_VARIANT, RegistryHasher.ZOMBIE_NAUTILUS_VARIANT);
        register(DataComponentTypes.FROG_VARIANT, RegistryHasher.FROG_VARIANT);
        register(DataComponentTypes.HORSE_VARIANT, RegistryHasher.HORSE_VARIANT);
        register(DataComponentTypes.PAINTING_VARIANT, RegistryHasher.PAINTING_VARIANT.cast(Holder::id)); // This can and will throw when a direct holder was received, which is still possible due to a bug in 1.21.6.
        register(DataComponentTypes.LLAMA_VARIANT, RegistryHasher.LLAMA_VARIANT);
        register(DataComponentTypes.AXOLOTL_VARIANT, RegistryHasher.AXOLOTL_VARIANT);
        register(DataComponentTypes.CAT_VARIANT, RegistryHasher.CAT_VARIANT);
        register(DataComponentTypes.CAT_SOUND_VARIANT, RegistryHasher.CAT_SOUND_VARIANT);
        register(DataComponentTypes.CAT_COLLAR, MinecraftHasher.DYE_COLOR);
        register(DataComponentTypes.SHEEP_COLOR, MinecraftHasher.DYE_COLOR);
        register(DataComponentTypes.SHULKER_COLOR, MinecraftHasher.DYE_COLOR);
    }

    private static void registerUnit(DataComponentType<Unit> component) {
        register(component, MinecraftHasher.UNIT);
    }

    private static void registerInt(IntComponentType component) {
        register(component, MinecraftHasher.INT);
    }

    private static <T> void registerMap(DataComponentType<T> component, MapBuilder<T> builder) {
        register(component, MinecraftHasher.mapBuilder(builder));
    }

    private static <T> void register(DataComponentType<T> component, MinecraftHasher<T> hasher) {
        if (hashers.containsKey(component)) {
            throw new IllegalArgumentException("Tried to register a hasher for a component twice");
        }
        hashers.put(component, hasher);
    }

    public static <T> MinecraftHasher<T> hasher(DataComponentType<T> component) {
        MinecraftHasher<T> hasher = (MinecraftHasher<T>) hashers.get(component);
        if (hasher == null) {
            throw new IllegalStateException("Unregistered hasher for component " + component + "!");
        }
        return hasher;
    }

    public static <T> HashCode hash(JavaRegistryProvider registries, DataComponentType<T> component, T value) {
        try {
            return hasher(component).hash(value, new MinecraftHashEncoder(registries));
        } catch (Exception exception) {
            GeyserImpl.getInstance().getLogger().error("Failed to hash item data component " + component.getKey() + " with value " + value + "!");
            GeyserImpl.getInstance().getLogger().error("This is a Geyser bug, please report this!");
            throw exception;
        }
    }

    public static <V, T extends DataComponentType<V>> HashCode hash(JavaRegistryProvider registries, DataComponent<V, T> component) {
        return hash(registries, component.getType(), component.getValue());
    }

    public static HashedStack hashStack(GeyserSession session, ItemStack stack) {
        if (stack == null) {
            return null;
        }

        DataComponents patch = stack.getDataComponentsPatch();
        if (patch == null) {
            return new HashedStack(stack.getId(), stack.getAmount(), Map.of(), Set.of());
        }
        Map<DataComponentType<?>, DataComponent<?, ?>> components = patch.getDataComponents();
        Map<DataComponentType<?>, Integer> hashedAdditions = new HashMap<>();
        Set<DataComponentType<?>> removals = new HashSet<>();
        for (Map.Entry<DataComponentType<?>, DataComponent<?, ?>> component : components.entrySet()) {
            if (NOT_HASHED.contains(component.getKey())) {
                GeyserImpl.getInstance().getLogger().debug("Not hashing component " + component.getKey() + " on stack " + stack);
            } else if (component.getValue().getValue() == null) {
                removals.add(component.getKey());
            } else {
                hashedAdditions.put(component.getKey(), hash(session.getRegistryCache(), component.getValue()).asInt());
            }
        }
        return new HashedStack(stack.getId(), stack.getAmount(), hashedAdditions, removals);
    }
}
