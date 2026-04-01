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
import net.kyori.adventure.key.Key;
import org.geysermc.geyser.inventory.item.Potion;
import org.geysermc.geyser.item.hashing.data.ConsumeEffectType;
import org.geysermc.geyser.item.hashing.data.FireworkExplosionShape;
import org.geysermc.geyser.item.hashing.data.ItemContainerSlot;
import org.geysermc.geyser.item.hashing.data.entity.AxolotlVariant;
import org.geysermc.geyser.item.hashing.data.entity.FoxVariant;
import org.geysermc.geyser.item.hashing.data.entity.HorseVariant;
import org.geysermc.geyser.item.hashing.data.entity.LlamaVariant;
import org.geysermc.geyser.item.hashing.data.entity.MooshroomVariant;
import org.geysermc.geyser.item.hashing.data.entity.ParrotVariant;
import org.geysermc.geyser.item.hashing.data.entity.RabbitVariant;
import org.geysermc.geyser.item.hashing.data.entity.SalmonVariant;
import org.geysermc.geyser.item.hashing.data.entity.TropicalFishPattern;
import org.geysermc.geyser.item.hashing.data.entity.VillagerVariant;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.ModifierOperation;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.AdventureModePredicate;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ArmorTrim;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.BannerPatternLayer;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.BeehiveOccupant;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.BlocksAttacks;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ConsumeEffect;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Fireworks;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.InstrumentComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemAttributeModifiers;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.JukeboxPlayable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.KineticWeapon;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectDetails;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectInstance;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ProvidesTrimMaterial;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.SuspiciousStewEffect;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.SwingAnimation;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Unit;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityType;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.CustomSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.Sound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;


@SuppressWarnings("UnstableApiUsage")
public interface RegistryHasher<DirectType> extends MinecraftHasher<Integer> {

    

    RegistryHasher<?> BLOCK = registry(JavaRegistries.BLOCK);

    RegistryHasher<?> ITEM = registry(JavaRegistries.ITEM);

    RegistryHasher<?> ENTITY_TYPE = enumIdRegistry(EntityType.values());

    MinecraftHasher<EntityType> ENTITY_TYPE_KEY = enumRegistry();

    MinecraftHasher<BlockEntityType> BLOCK_ENTITY_TYPE_KEY = enumRegistry();

    RegistryHasher<?> ENCHANTMENT = registry(JavaRegistries.ENCHANTMENT);

    RegistryHasher<?> ATTRIBUTE = enumIdRegistry(AttributeType.Builtin.values(), AttributeType::getIdentifier);

    MinecraftHasher<DataComponentType<?>> DATA_COMPONENT_TYPE = KEY.cast(DataComponentType::getKey);

    
    MinecraftHasher<Effect> EFFECT = enumRegistry();

    RegistryHasher<?> EFFECT_ID = enumIdRegistry(Effect.values());

    RegistryHasher<?> POTION = enumIdRegistry(Potion.values());

    RegistryHasher<?> VILLAGER_TYPE = enumIdRegistry(VillagerVariant.values());

    

    MinecraftHasher<BuiltinSound> BUILTIN_SOUND = KEY.cast(sound -> MinecraftKey.key(sound.getName()));

    MinecraftHasher<CustomSound> CUSTOM_SOUND = MinecraftHasher.mapBuilder(builder -> builder
        .accept("sound_id", KEY, sound -> MinecraftKey.key(sound.getName()))
        .optional("range", FLOAT, CustomSound::getRange, 16.0F));

    MinecraftHasher<Sound> SOUND_EVENT = (sound, encoder) -> {
        if (sound instanceof BuiltinSound builtin) {
            return BUILTIN_SOUND.hash(builtin, encoder);
        }
        return CUSTOM_SOUND.hash((CustomSound) sound, encoder);
    };

    RegistryHasher<?> DAMAGE_TYPE = registry(JavaRegistries.DAMAGE_TYPE);

    MinecraftHasher<InstrumentComponent.Instrument> DIRECT_INSTRUMENT = MinecraftHasher.mapBuilder(builder -> builder
        .accept("sound_event", SOUND_EVENT, InstrumentComponent.Instrument::soundEvent)
        .accept("use_duration", FLOAT, InstrumentComponent.Instrument::useDuration)
        .accept("range", FLOAT, InstrumentComponent.Instrument::range)
        .accept("description", ComponentHasher.COMPONENT, InstrumentComponent.Instrument::description));

    RegistryHasher<InstrumentComponent.Instrument> INSTRUMENT = registry(JavaRegistries.INSTRUMENT, DIRECT_INSTRUMENT);

    MinecraftHasher<ArmorTrim.TrimMaterial> DIRECT_TRIM_MATERIAL = MinecraftHasher.mapBuilder(builder -> builder
        .accept("asset_name", MinecraftHasher.STRING, ArmorTrim.TrimMaterial::assetBase)
        .optional("override_armor_assets", MinecraftHasher.map(KEY, STRING), ArmorTrim.TrimMaterial::assetOverrides, Map.of())
        .accept("description", ComponentHasher.COMPONENT, ArmorTrim.TrimMaterial::description));

    RegistryHasher<ArmorTrim.TrimMaterial> TRIM_MATERIAL = registry(JavaRegistries.TRIM_MATERIAL, DIRECT_TRIM_MATERIAL);

    MinecraftHasher<ArmorTrim.TrimPattern> DIRECT_TRIM_PATTERN = MinecraftHasher.mapBuilder(builder -> builder
        .accept("asset_id", KEY, ArmorTrim.TrimPattern::assetId)
        .accept("description", ComponentHasher.COMPONENT, ArmorTrim.TrimPattern::description)
        .accept("decal", BOOL, ArmorTrim.TrimPattern::decal));

    RegistryHasher<ArmorTrim.TrimPattern> TRIM_PATTERN = registry(JavaRegistries.TRIM_PATTERN, DIRECT_TRIM_PATTERN);

    MinecraftHasher<JukeboxPlayable.JukeboxSong> DIRECT_JUKEBOX_SONG = MinecraftHasher.mapBuilder(builder -> builder
        .accept("sound_event", SOUND_EVENT, JukeboxPlayable.JukeboxSong::soundEvent)
        .accept("description", ComponentHasher.COMPONENT, JukeboxPlayable.JukeboxSong::description)
        .accept("length_in_seconds", FLOAT, JukeboxPlayable.JukeboxSong::lengthInSeconds)
        .accept("comparator_output", INT, JukeboxPlayable.JukeboxSong::comparatorOutput));

    RegistryHasher<JukeboxPlayable.JukeboxSong> JUKEBOX_SONG = registry(JavaRegistries.JUKEBOX_SONG, DIRECT_JUKEBOX_SONG);

    MinecraftHasher<BannerPatternLayer.BannerPattern> DIRECT_BANNER_PATTERN = MinecraftHasher.mapBuilder(builder -> builder
        .accept("asset_id", KEY, BannerPatternLayer.BannerPattern::getAssetId)
        .accept("translation_key", STRING, BannerPatternLayer.BannerPattern::getTranslationKey));

    RegistryHasher<BannerPatternLayer.BannerPattern> BANNER_PATTERN = registry(JavaRegistries.BANNER_PATTERN, DIRECT_BANNER_PATTERN);

    RegistryHasher<?> WOLF_VARIANT = registry(JavaRegistries.WOLF_VARIANT);

    RegistryHasher<?> WOLF_SOUND_VARIANT = registry(JavaRegistries.WOLF_SOUND_VARIANT);

    RegistryHasher<?> PIG_VARIANT = registry(JavaRegistries.PIG_VARIANT);

    RegistryHasher<?> COW_VARIANT = registry(JavaRegistries.COW_VARIANT);

    RegistryHasher<?> FROG_VARIANT = registry(JavaRegistries.FROG_VARIANT);

    RegistryHasher<?> PAINTING_VARIANT = registry(JavaRegistries.PAINTING_VARIANT);

    RegistryHasher<?> CAT_VARIANT = registry(JavaRegistries.CAT_VARIANT);

    
    

    MinecraftHasher<Integer> FOX_VARIANT = MinecraftHasher.fromIdEnum(FoxVariant.values());

    MinecraftHasher<Integer> SALMON_VARIANT = MinecraftHasher.fromIdEnum(SalmonVariant.values());

    MinecraftHasher<Integer> PARROT_VARIANT = MinecraftHasher.fromIdEnum(ParrotVariant.values());

    MinecraftHasher<Integer> TROPICAL_FISH_PATTERN = MinecraftHasher.<TropicalFishPattern>fromEnum().cast(TropicalFishPattern::fromPackedId);

    MinecraftHasher<Integer> MOOSHROOM_VARIANT = MinecraftHasher.fromIdEnum(MooshroomVariant.values());

    MinecraftHasher<Integer> RABBIT_VARIANT = MinecraftHasher.<RabbitVariant>fromEnum().cast(RabbitVariant::fromId);

    MinecraftHasher<Integer> HORSE_VARIANT = MinecraftHasher.fromIdEnum(HorseVariant.values());

    MinecraftHasher<Integer> LLAMA_VARIANT = MinecraftHasher.fromIdEnum(LlamaVariant.values());

    MinecraftHasher<Integer> AXOLOTL_VARIANT = MinecraftHasher.fromIdEnum(AxolotlVariant.values());

    

    MinecraftHasher<DataComponent<?, ?>> DATA_COMPONENT_KEY = MinecraftHasher.either(KEY,
        component -> component.getValue() == null ? null : component.getType().getKey(), KEY_REMOVAL, component -> component.getType().getKey());

    @SuppressWarnings({"unchecked", "rawtypes"}) 
    MinecraftHasher<DataComponent<?, ?>> DATA_COMPONENT_VALUE = (component, encoder) -> {
        if (component.getValue() == null) {
            return UNIT.hash(Unit.INSTANCE, encoder);
        }
        MinecraftHasher hasher = DataComponentHashers.hasher(component.getType());
        return hasher.hash(component.getValue(), encoder);
    };

    MinecraftHasher<DataComponents> DATA_COMPONENTS = MinecraftHasher.mapSet(DATA_COMPONENT_KEY, DATA_COMPONENT_VALUE).cast(components -> components.getDataComponents().values());

    MinecraftHasher<ItemStack> ITEM_STACK = MinecraftHasher.mapBuilder(builder -> builder
        .accept("id", ITEM, ItemStack::getId)
        .accept("count", INT, ItemStack::getAmount)
        .optionalNullable("components", DATA_COMPONENTS, ItemStack::getDataComponentsPatch));

    
    MapBuilder<MobEffectDetails> MOB_EFFECT_DETAILS = builder -> builder
        .optional("amplifier", BYTE, instance -> (byte) instance.getAmplifier(), (byte) 0)
        .optional("duration", INT, MobEffectDetails::getDuration, 0)
        .optional("ambient", BOOL, MobEffectDetails::isAmbient, false)
        .optional("show_particles", BOOL, MobEffectDetails::isShowParticles, true)
        .accept("show_icon", BOOL, MobEffectDetails::isShowIcon); 

    MinecraftHasher<MobEffectInstance> MOB_EFFECT_INSTANCE = MinecraftHasher.mapBuilder(builder -> builder
        .accept("id", RegistryHasher.EFFECT, MobEffectInstance::getEffect)
        .accept(MOB_EFFECT_DETAILS, MobEffectInstance::getDetails));

    MinecraftHasher<ModifierOperation> ATTRIBUTE_MODIFIER_OPERATION = MinecraftHasher.fromEnum(operation -> switch (operation) {
        case ADD -> "add_value";
        case ADD_MULTIPLIED_BASE -> "add_multiplied_base";
        case ADD_MULTIPLIED_TOTAL -> "add_multiplied_total";
    });

    

    MinecraftHasher<ItemEnchantments> ITEM_ENCHANTMENTS = MinecraftHasher.map(RegistryHasher.ENCHANTMENT, MinecraftHasher.INT).cast(ItemEnchantments::getEnchantments);

    MinecraftHasher<ItemContainerSlot> CONTAINER_SLOT = MinecraftHasher.mapBuilder(builder -> builder
        .accept("slot", INT, ItemContainerSlot::index)
        .accept("item", ITEM_STACK, ItemContainerSlot::item));

    MinecraftHasher<List<ItemStack>> ITEM_CONTAINER_CONTENTS = CONTAINER_SLOT.list().cast(stacks -> {
        List<ItemContainerSlot> slots = new ArrayList<>();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack != null) {
                slots.add(new ItemContainerSlot(i, stacks.get(i)));
            }
        }
        return slots;
    });

    MinecraftHasher<AdventureModePredicate.BlockPredicate> BLOCK_PREDICATE = MinecraftHasher.mapBuilder(builder -> builder
        .optionalNullable("blocks", BLOCK.holderSet(), AdventureModePredicate.BlockPredicate::getBlocks)
        .optionalNullable("nbt", NBT_MAP, AdventureModePredicate.BlockPredicate::getNbt)); 

    
    MinecraftHasher<AdventureModePredicate> ADVENTURE_MODE_PREDICATE = MinecraftHasher.either(BLOCK_PREDICATE,
        predicate -> predicate.getPredicates().size() == 1 ? predicate.getPredicates().get(0) : null, BLOCK_PREDICATE.list(), AdventureModePredicate::getPredicates);

    MinecraftHasher<ItemAttributeModifiers.DisplayType> ATTRIBUTE_MODIFIER_DISPLAY_TYPE = MinecraftHasher.fromEnum();

    MinecraftHasher<ItemAttributeModifiers.Display> ATTRIBUTE_MODIFIER_DISPLAY = ATTRIBUTE_MODIFIER_DISPLAY_TYPE.dispatch(ItemAttributeModifiers.Display::getType,
        displayType -> switch (displayType) {
            case DEFAULT, HIDDEN -> MapBuilder.unit();
            case OVERRIDE -> builder -> builder
                .accept("value", ComponentHasher.COMPONENT, ItemAttributeModifiers.Display::getComponent);
        });

    MinecraftHasher<ItemAttributeModifiers.Entry> ATTRIBUTE_MODIFIER_ENTRY = MinecraftHasher.mapBuilder(builder -> builder
        .accept("type", RegistryHasher.ATTRIBUTE, ItemAttributeModifiers.Entry::getAttribute)
        .accept("id", KEY, entry -> entry.getModifier().getId())
        .accept("amount", DOUBLE, entry -> entry.getModifier().getAmount())
        .accept("operation", ATTRIBUTE_MODIFIER_OPERATION, entry -> entry.getModifier().getOperation())
        .optional("slot", EQUIPMENT_SLOT_GROUP, ItemAttributeModifiers.Entry::getSlot, ItemAttributeModifiers.EquipmentSlotGroup.ANY)
        .optionalPredicate("display", ATTRIBUTE_MODIFIER_DISPLAY, ItemAttributeModifiers.Entry::getDisplay, display -> display.getType() != ItemAttributeModifiers.DisplayType.DEFAULT));

    MinecraftHasher<Consumable.ItemUseAnimation> ITEM_USE_ANIMATION = MinecraftHasher.fromEnum();

    MinecraftHasher<ConsumeEffectType> CONSUME_EFFECT_TYPE = enumRegistry();

    MinecraftHasher<ConsumeEffect> CONSUME_EFFECT = CONSUME_EFFECT_TYPE.dispatch(ConsumeEffectType::fromEffect, ConsumeEffectType::mapBuilder);

    MinecraftHasher<SuspiciousStewEffect> SUSPICIOUS_STEW_EFFECT = MinecraftHasher.mapBuilder(builder -> builder
        .accept("id", EFFECT_ID, SuspiciousStewEffect::getMobEffectId)
        .optional("duration", INT, SuspiciousStewEffect::getDuration, 160));

    MinecraftHasher<InstrumentComponent> INSTRUMENT_COMPONENT = MinecraftHasher.either(INSTRUMENT.holder(), InstrumentComponent::instrumentHolder, KEY, InstrumentComponent::instrumentLocation);

    MinecraftHasher<ToolData.Rule> TOOL_RULE = MinecraftHasher.mapBuilder(builder -> builder
        .accept("blocks", RegistryHasher.BLOCK.holderSet(), ToolData.Rule::getBlocks)
        .optionalNullable("speed", MinecraftHasher.FLOAT, ToolData.Rule::getSpeed)
        .optionalNullable("correct_for_drops", MinecraftHasher.BOOL, ToolData.Rule::getCorrectForDrops));

    MinecraftHasher<BlocksAttacks.DamageReduction> BLOCKS_ATTACKS_DAMAGE_REDUCTION = MinecraftHasher.mapBuilder(builder -> builder
        .optional("horizontal_blocking_angle", FLOAT, BlocksAttacks.DamageReduction::horizontalBlockingAngle, 90.0F)
        .optionalNullable("type", DAMAGE_TYPE.holderSet(), BlocksAttacks.DamageReduction::type)
        .accept("base", FLOAT, BlocksAttacks.DamageReduction::base)
        .accept("factor", FLOAT, BlocksAttacks.DamageReduction::factor));

    MinecraftHasher<BlocksAttacks.ItemDamageFunction> BLOCKS_ATTACKS_ITEM_DAMAGE_FUNCTION = MinecraftHasher.mapBuilder(builder -> builder
        .accept("threshold", FLOAT, BlocksAttacks.ItemDamageFunction::threshold)
        .accept("base", FLOAT, BlocksAttacks.ItemDamageFunction::base)
        .accept("factor", FLOAT, BlocksAttacks.ItemDamageFunction::factor));

    MinecraftHasher<KineticWeapon.Condition> KINETIC_WEAPON_CONDITION = MinecraftHasher.mapBuilder(builder -> builder
        .accept("max_duration_ticks", MinecraftHasher.INT, KineticWeapon.Condition::maxDurationTicks)
        .optional("min_speed", MinecraftHasher.FLOAT, KineticWeapon.Condition::minSpeed, 0.0F)
        .optional("min_relative_speed", MinecraftHasher.FLOAT, KineticWeapon.Condition::minRelativeSpeed, 0.0F));

    MinecraftHasher<SwingAnimation.Type> SWING_ANIMATION_TYPE = MinecraftHasher.fromEnum();

    MinecraftHasher<ProvidesTrimMaterial> PROVIDES_TRIM_MATERIAL = MinecraftHasher.either(TRIM_MATERIAL.holder(), ProvidesTrimMaterial::materialHolder, KEY, ProvidesTrimMaterial::materialLocation);

    MinecraftHasher<ArmorTrim> ARMOR_TRIM = MinecraftHasher.mapBuilder(builder -> builder
        .accept("material", TRIM_MATERIAL.holder(), ArmorTrim::material)
        .accept("pattern", TRIM_PATTERN.holder(), ArmorTrim::pattern));

    MinecraftHasher<JukeboxPlayable> JUKEBOX_PLAYABLE = MinecraftHasher.either(JUKEBOX_SONG.holder(), JukeboxPlayable::songHolder, KEY, JukeboxPlayable::songLocation);

    MinecraftHasher<BannerPatternLayer> BANNER_PATTERN_LAYER = MinecraftHasher.mapBuilder(builder -> builder
        .accept("pattern", BANNER_PATTERN.holder(), BannerPatternLayer::getPattern)
        .accept("color", DYE_COLOR, BannerPatternLayer::getColorId));

    MinecraftHasher<Integer> FIREWORK_EXPLOSION_SHAPE = MinecraftHasher.fromIdEnum(FireworkExplosionShape.values());

    MinecraftHasher<Fireworks.FireworkExplosion> FIREWORK_EXPLOSION = MinecraftHasher.mapBuilder(builder -> builder
        .accept("shape", FIREWORK_EXPLOSION_SHAPE, Fireworks.FireworkExplosion::getShapeId)
        .optionalList("colors", INT, explosion -> IntStream.of(explosion.getColors()).boxed().toList())
        .optionalList("fade_colors", INT, explosion -> IntStream.of(explosion.getFadeColors()).boxed().toList())
        .optional("has_trail", BOOL, Fireworks.FireworkExplosion::isHasTrail, false)
        .optional("has_twinkle", BOOL, Fireworks.FireworkExplosion::isHasTwinkle, false));

    MinecraftHasher<BeehiveOccupant> BEEHIVE_OCCUPANT = MinecraftHasher.mapBuilder(builder -> builder
        .accept("id", RegistryHasher.ENTITY_TYPE_KEY, beehiveOccupant -> beehiveOccupant.getEntityData().type())
        .accept(beehiveOccupant -> beehiveOccupant.getEntityData().tag(), MapBuilder.inlineNbtMap())
        .accept("ticks_in_hive", INT, BeehiveOccupant::getTicksInHive)
        .accept("min_ticks_in_hive", INT, BeehiveOccupant::getMinTicksInHive));

    
    static RegistryHasher<?> registry(JavaRegistryKey<?> registry) {
        MinecraftHasher<Integer> hasher = KEY.registryCast(registry::key);
        return hasher::hash;
    }

    
    // We don't use the registry generic type, because various registries don't use the MCPL type as their type
    static <DirectType> RegistryHasher<DirectType> registry(JavaRegistryKey<?> registry, MinecraftHasher<DirectType> directHasher) {
        return new RegistryHasherWithDirectHasher<>(registry(registry), directHasher);
    }

    
    default MinecraftHasher<Holder<DirectType>> holder() {
        if (this instanceof RegistryHasher.RegistryHasherWithDirectHasher<DirectType> withDirect) {
            return withDirect.holderHasher;
        }
        throw new IllegalStateException("Tried to create a holder hasher on a registry hasher that does not have a direct hasher specified");
    }

    
    default MinecraftHasher<HolderSet> holderSet() {
        return (holder, encoder) -> {
            if (holder.getLocation() != null) {
                return TAG.hash(holder.getLocation(), encoder);
            } else if (holder.getHolders() != null) {
                if (holder.getHolders().length == 1) {
                    return hash(holder.getHolders()[0], encoder);
                }
                return list().hash(Arrays.stream(holder.getHolders()).boxed().toList(), encoder);
            }
            throw new IllegalStateException("HolderSet must have either tag location or holders");
        };
    }

    
    static <EnumConstant extends Enum<EnumConstant>> MinecraftHasher<EnumConstant> enumRegistry() {
        return KEY.cast(constant -> MinecraftKey.key(constant.name().toLowerCase(Locale.ROOT)));
    }

    
    static <EnumConstant extends Enum<EnumConstant>> RegistryHasher<?> enumIdRegistry(EnumConstant[] values) {
        return enumIdRegistry(values, constant -> MinecraftKey.key(constant.name().toLowerCase(Locale.ROOT)));
    }

    
    static <EnumConstant extends Enum<EnumConstant>> RegistryHasher<?> enumIdRegistry(EnumConstant[] values, Function<EnumConstant, Key> toKey) {
        MinecraftHasher<Integer> hasher = KEY.cast(i -> toKey.apply(values[i]));
        return hasher::hash;
    }

    
    static MinecraftHasher<Holder<Key>> eitherHolderHasher(JavaRegistryKey<?> registry) {
        return MinecraftHasher.KEY.registryCast((registries, holder) -> holder.getOrCompute(id -> registry.key(registries, id)));
    }

    class RegistryHasherWithDirectHasher<DirectType> implements RegistryHasher<DirectType> {
        private final MinecraftHasher<Integer> id;
        private final MinecraftHasher<Holder<DirectType>> holderHasher;

        public RegistryHasherWithDirectHasher(MinecraftHasher<Integer> id, MinecraftHasher<DirectType> direct) {
            this.id = id;
            this.holderHasher = (value, encoder) -> {
                if (value.isId()) {
                    return hash(value.id(), encoder);
                }
                return direct.hash(value.custom(), encoder);
            };
        }

        @Override
        public HashCode hash(Integer value, MinecraftHashEncoder encoder) {
            return id.hash(value, encoder);
        }
    }
}
