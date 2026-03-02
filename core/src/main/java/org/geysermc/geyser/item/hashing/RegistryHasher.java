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

/**
 * {@link RegistryHasher}s are hashers that hash a network integer ID to a namespaced identifier. {@link RegistryHasher}s can be created using static utility methods in this class, and all registry hashers should be kept in here.
 *
 * <p>The {@link DirectType} parameter is only used for registry hashers that are able to encode {@link Holder}s, and must be left as a {@code ?} if this functionality is not in use. This makes it clear the hasher is not
 * supposed to be able to encode holders.</p>
 *
 * <p>To create a hasher that can encode a {@link Holder}, a direct hasher should be created that hashes a {@link DirectType} (in case of a custom holder), and {@link RegistryHasher#registry(JavaRegistryKey, MinecraftHasher)}
 * should be used to create the registry hasher. {@link RegistryHasher#holder()} can then be used to obtain a hasher that encodes a holder of {@link DirectType}.</p>
 *
 * <p>Along with {@link RegistryHasher}s, this class also contains a bunch of hashers for various Minecraft objects. For organisational purposes, these are grouped in various sections with comments.</p>
 *
 * @param <DirectType> the type this hasher hashes. Only used for registry hashers that can hash holders.
 */
@SuppressWarnings("UnstableApiUsage")
public interface RegistryHasher<DirectType> extends MinecraftHasher<Integer> {

    // Java registries

    RegistryHasher<?> BLOCK = registry(JavaRegistries.BLOCK);

    RegistryHasher<?> ITEM = registry(JavaRegistries.ITEM);

    RegistryHasher<?> ENTITY_TYPE = enumIdRegistry(EntityType.values());

    MinecraftHasher<EntityType> ENTITY_TYPE_KEY = enumRegistry();

    MinecraftHasher<BlockEntityType> BLOCK_ENTITY_TYPE_KEY = enumRegistry();

    RegistryHasher<?> ENCHANTMENT = registry(JavaRegistries.ENCHANTMENT);

    RegistryHasher<?> ATTRIBUTE = enumIdRegistry(AttributeType.Builtin.values(), AttributeType::getIdentifier);

    MinecraftHasher<DataComponentType<?>> DATA_COMPONENT_TYPE = KEY.cast(DataComponentType::getKey);

    // Mob effects can both be an enum constant or ID in MCPL.
    MinecraftHasher<Effect> EFFECT = enumRegistry();

    RegistryHasher<?> EFFECT_ID = enumIdRegistry(Effect.values());

    RegistryHasher<?> POTION = enumIdRegistry(Potion.values());

    RegistryHasher<?> VILLAGER_TYPE = enumIdRegistry(VillagerVariant.values());

    // Java data-driven registries

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

    // Entity variants
    // These are all not registries on Java, meaning they serialise as just literal strings, not namespaced IDs

    MinecraftHasher<Integer> FOX_VARIANT = MinecraftHasher.fromIdEnum(FoxVariant.values());

    MinecraftHasher<Integer> SALMON_VARIANT = MinecraftHasher.fromIdEnum(SalmonVariant.values());

    MinecraftHasher<Integer> PARROT_VARIANT = MinecraftHasher.fromIdEnum(ParrotVariant.values());

    MinecraftHasher<Integer> TROPICAL_FISH_PATTERN = MinecraftHasher.<TropicalFishPattern>fromEnum().cast(TropicalFishPattern::fromPackedId);

    MinecraftHasher<Integer> MOOSHROOM_VARIANT = MinecraftHasher.fromIdEnum(MooshroomVariant.values());

    MinecraftHasher<Integer> RABBIT_VARIANT = MinecraftHasher.<RabbitVariant>fromEnum().cast(RabbitVariant::fromId);

    MinecraftHasher<Integer> HORSE_VARIANT = MinecraftHasher.fromIdEnum(HorseVariant.values());

    MinecraftHasher<Integer> LLAMA_VARIANT = MinecraftHasher.fromIdEnum(LlamaVariant.values());

    MinecraftHasher<Integer> AXOLOTL_VARIANT = MinecraftHasher.fromIdEnum(AxolotlVariant.values());

    // Widely used Minecraft types

    MinecraftHasher<DataComponent<?, ?>> DATA_COMPONENT_KEY = MinecraftHasher.either(KEY,
        component -> component.getValue() == null ? null : component.getType().getKey(), KEY_REMOVAL, component -> component.getType().getKey());

    @SuppressWarnings({"unchecked", "rawtypes"}) // Java generics :(
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

    // Encoding of hidden effects is unfortunately not possible
    MapBuilder<MobEffectDetails> MOB_EFFECT_DETAILS = builder -> builder
        .optional("amplifier", BYTE, instance -> (byte) instance.getAmplifier(), (byte) 0)
        .optional("duration", INT, MobEffectDetails::getDuration, 0)
        .optional("ambient", BOOL, MobEffectDetails::isAmbient, false)
        .optional("show_particles", BOOL, MobEffectDetails::isShowParticles, true)
        .accept("show_icon", BOOL, MobEffectDetails::isShowIcon); // Yes, this is not an optional. I checked. Maybe it will be in the future and break everything!

    MinecraftHasher<MobEffectInstance> MOB_EFFECT_INSTANCE = MinecraftHasher.mapBuilder(builder -> builder
        .accept("id", RegistryHasher.EFFECT, MobEffectInstance::getEffect)
        .accept(MOB_EFFECT_DETAILS, MobEffectInstance::getDetails));

    MinecraftHasher<ModifierOperation> ATTRIBUTE_MODIFIER_OPERATION = MinecraftHasher.fromEnum(operation -> switch (operation) {
        case ADD -> "add_value";
        case ADD_MULTIPLIED_BASE -> "add_multiplied_base";
        case ADD_MULTIPLIED_TOTAL -> "add_multiplied_total";
    });

    // Component-specific types

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
        .optionalNullable("nbt", NBT_MAP, AdventureModePredicate.BlockPredicate::getNbt)); // Property and data component matchers are, unfortunately, too complicated to include here

    // Encode as a single element if the list only has one element
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

    /**
     * Creates a hasher that uses the {@link JavaRegistryKey#key(GeyserSession, int)} method to turn a network ID into a {@link Key}, and then encodes this key.
     *
     * @param registry the registry to create a hasher for.
     */
    static RegistryHasher<?> registry(JavaRegistryKey<?> registry) {
        MinecraftHasher<Integer> hasher = KEY.registryCast(registry::key);
        return hasher::hash;
    }

    /**
     * Creates a hasher that encodes network IDs using {@link RegistryHasher#registry(JavaRegistryKey)}, and is also able to encode {@link Holder}s by using the {@code directHasher}.
     *
     * <p>A hasher that encodes {@link Holder}s can be obtained by using {@link RegistryHasher#holder()}</p>
     *
     * @param registry the registry to create a hasher for.
     * @param directHasher the hasher that encodes a custom object.
     * @param <DirectType> the type of custom objects.
     * @see RegistryHasher#holder()
     */
    // We don't use the registry generic type, because various registries don't use the MCPL type as their type
    static <DirectType> RegistryHasher<DirectType> registry(JavaRegistryKey<?> registry, MinecraftHasher<DirectType> directHasher) {
        return new RegistryHasherWithDirectHasher<>(registry(registry), directHasher);
    }

    /**
     * Creates a hasher that encodes a {@link Holder} of {@link DirectType}. If the holder has an ID, the {@link RegistryHasher} is used to encode it. If the holder is custom,
     * a direct hasher specified in {@link RegistryHasher#registry(JavaRegistryKey, MinecraftHasher)} is used to encode it.
     *
     * <p>This method can only be used if this hasher has a direct hasher attached to it. That is only the case if {@link DirectType} is not {@code ?}. If this hasher doesn't have
     * a direct hasher, a {@link IllegalStateException} will be thrown upon use.</p>
     *
     * @throws IllegalStateException when this hasher does not have a direct hasher attached to it.
     */
    default MinecraftHasher<Holder<DirectType>> holder() {
        if (this instanceof RegistryHasher.RegistryHasherWithDirectHasher<DirectType> withDirect) {
            return withDirect.holderHasher;
        }
        throw new IllegalStateException("Tried to create a holder hasher on a registry hasher that does not have a direct hasher specified");
    }

    /**
     * Creates a hasher that hashes a {@link HolderSet} of the registry. {@link HolderSet}s can encode as a hash-prefixed tag, a single namespaced ID, or a list of namespaced IDs.
     *
     * <p>The hasher throws a {@link IllegalStateException} if the holder set does not have a tag nor a list of IDs. This should never happen.</p>
     */
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

    /**
     * Creates a hasher that uses {@link Enum#name()} (lowercased) to create a key in the {@code minecraft} namespace, and then hashes it.
     *
     * <p>Please be aware that you are using literal enum constants as key paths here, meaning that if there is a typo in a constant, or a constant changes name, things
     * may break. Use cautiously.</p>
     *
     * @param <EnumConstant> the enum.
     */
    static <EnumConstant extends Enum<EnumConstant>> MinecraftHasher<EnumConstant> enumRegistry() {
        return KEY.cast(constant -> MinecraftKey.key(constant.name().toLowerCase(Locale.ROOT)));
    }

    /**
     * Uses {@link Enum#name()} (lowercased) to create a function that creates a {@link Key} from a {@link EnumConstant}, and uses this as {@code toKey}
     * function in {@link RegistryHasher#enumIdRegistry(Enum[], Function)}.
     *
     * <p>Please be aware that you are using literal enum constants as key paths here, meaning that if there is a typo in a constant, or a constant changes name, things
     * may break. Use cautiously.</p>
     *
     * @param values the array of {@link EnumConstant}s.
     * @param <EnumConstant> the enum.
     * @see RegistryHasher#enumIdRegistry(Enum[], Function)
     */
    static <EnumConstant extends Enum<EnumConstant>> RegistryHasher<?> enumIdRegistry(EnumConstant[] values) {
        return enumIdRegistry(values, constant -> MinecraftKey.key(constant.name().toLowerCase(Locale.ROOT)));
    }

    /**
     * Creates a hasher that looks up a network ID in the array of {@link EnumConstant}s, and then uses {@code toKey} to turn the constant into a key, which it then hashes.
     *
     * @param values the array of {@link EnumConstant}s.
     * @param toKey the function that turns a {@link EnumConstant} into a {@link Key}.
     * @param <EnumConstant> the enum.
     * @see MinecraftHasher#fromIdEnum(Enum[])
     */
    static <EnumConstant extends Enum<EnumConstant>> RegistryHasher<?> enumIdRegistry(EnumConstant[] values, Function<EnumConstant, Key> toKey) {
        MinecraftHasher<Integer> hasher = KEY.cast(i -> toKey.apply(values[i]));
        return hasher::hash;
    }

    /**
     * Creates a hasher that hashes a {@code Holder<Key>}, also known as an {@code EitherHolder} in Mojmap.
     *
     * <p>Please note that a {@code Holder<Key>} is only a valid representation of an {@code EitherHolder} in MCPL if the stream codec of the {@code EitherHolder} does not support directly encoding unregistered values.</p>
     *
     * @param registry the registry the {@code Holder} is for.
     * @return a hasher that hashes a {@code Holder<Key>} for the given registry.
     */
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
