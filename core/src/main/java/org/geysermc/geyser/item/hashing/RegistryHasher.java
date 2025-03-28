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

import net.kyori.adventure.key.Key;
import org.geysermc.geyser.inventory.item.Potion;
import org.geysermc.geyser.session.cache.registry.JavaRegistries;
import org.geysermc.geyser.session.cache.registry.JavaRegistryKey;
import org.geysermc.geyser.util.MinecraftKey;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ArmorTrim;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ConsumeEffect;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.HolderSet;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.InstrumentComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.JukeboxPlayable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.MobEffectInstance;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ProvidesTrimMaterial;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.SuspiciousStewEffect;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ToolData;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.CustomSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.Sound;

import java.util.Arrays;
import java.util.Map;

public interface RegistryHasher extends MinecraftHasher<Integer> {

    RegistryHasher BLOCK = registry(JavaRegistries.BLOCK);

    RegistryHasher ITEM = registry(JavaRegistries.ITEM);

    RegistryHasher ENTITY_TYPE = enumIdRegistry(EntityType.values());

    RegistryHasher ENCHANTMENT = registry(JavaRegistries.ENCHANTMENT);

    MinecraftHasher<DataComponentType<?>> DATA_COMPONENT_TYPE = KEY.convert(DataComponentType::getKey);

    @SuppressWarnings({"unchecked", "rawtypes"}) // Java generics :(
    MinecraftHasher<DataComponent<?, ?>> DATA_COMPONENT = (component, encoder) -> {
        MinecraftHasher hasher = DataComponentHashers.hasherOrEmpty(component.getType());
        return hasher.hash(component.getValue(), encoder);
    };

    MinecraftHasher<DataComponents> DATA_COMPONENTS = MinecraftHasher.map(RegistryHasher.DATA_COMPONENT_TYPE, DATA_COMPONENT).convert(DataComponents::getDataComponents); // TODO component removals (needs unit value and ! component prefix)

    MinecraftHasher<ItemStack> ITEM_STACK = MinecraftHasher.mapBuilder(builder -> builder
        .accept("id", ITEM, ItemStack::getId)
        .accept("count", INT, ItemStack::getAmount)
        .optionalNullable("components", DATA_COMPONENTS, ItemStack::getDataComponentsPatch));

    MinecraftHasher<Effect> EFFECT = enumRegistry();

    RegistryHasher EFFECT_ID = enumIdRegistry(Effect.values());

    MinecraftHasher<SuspiciousStewEffect> SUSPICIOUS_STEW_EFFECT = MinecraftHasher.mapBuilder(builder -> builder
        .accept("id", EFFECT_ID, SuspiciousStewEffect::getMobEffectId)
        .optional("duration", INT, SuspiciousStewEffect::getDuration, 160));

    MinecraftHasher<MobEffectInstance> MOB_EFFECT_INSTANCE = MinecraftHasher.mapBuilder(builder -> builder
        .accept("id", RegistryHasher.EFFECT, MobEffectInstance::getEffect)
        .optional("amplifier", BYTE, instance -> (byte) instance.getDetails().getAmplifier(), (byte) 0)
        .optional("duration", INT, instance -> instance.getDetails().getDuration(), 0)
        .optional("ambient", BOOL, instance -> instance.getDetails().isAmbient(), false)
        .optional("show_particles", BOOL, instance -> instance.getDetails().isShowParticles(), true)
        .accept("show_icon", BOOL, instance -> instance.getDetails().isShowIcon())); // TODO check this, also hidden effect but is recursive

    RegistryHasher POTION = enumIdRegistry(Potion.values());

    MinecraftHasher<BuiltinSound> BUILTIN_SOUND = KEY.convert(sound -> MinecraftKey.key(sound.getName()));

    MinecraftHasher<CustomSound> CUSTOM_SOUND = MinecraftHasher.mapBuilder(builder -> builder
        .accept("sound_id", KEY, sound -> MinecraftKey.key(sound.getName()))
        .optional("range", FLOAT, CustomSound::getRange, 16.0F));

    MinecraftHasher<Sound> SOUND_EVENT = (sound, encoder) -> {
        if (sound instanceof BuiltinSound builtin) {
            return BUILTIN_SOUND.hash(builtin, encoder);
        }
        return CUSTOM_SOUND.hash((CustomSound) sound, encoder);
    };

    MinecraftHasher<ItemEnchantments> ITEM_ENCHANTMENTS = MinecraftHasher.map(RegistryHasher.ENCHANTMENT, MinecraftHasher.INT).convert(ItemEnchantments::getEnchantments);

    MinecraftHasher<ConsumeEffectType> CONSUME_EFFECT_TYPE = enumRegistry();

    MinecraftHasher<ConsumeEffect> CONSUME_EFFECT = CONSUME_EFFECT_TYPE.dispatch(ConsumeEffectType::fromEffect, type -> type.builder.cast());

    MinecraftHasher<InstrumentComponent.Instrument> DIRECT_INSTRUMENT = MinecraftHasher.mapBuilder(builder -> builder
        .accept("sound_event", SOUND_EVENT, InstrumentComponent.Instrument::soundEvent)
        .accept("use_duration", FLOAT, InstrumentComponent.Instrument::useDuration)
        .accept("range", FLOAT, InstrumentComponent.Instrument::range)
        .accept("description", ComponentHasher.COMPONENT, InstrumentComponent.Instrument::description));

    MinecraftHasher<Holder<InstrumentComponent.Instrument>> INSTRUMENT = holder(JavaRegistries.INSTRUMENT, DIRECT_INSTRUMENT);

    MinecraftHasher<InstrumentComponent> INSTRUMENT_COMPONENT = MinecraftHasher.either(INSTRUMENT, InstrumentComponent::instrumentHolder, KEY, InstrumentComponent::instrumentLocation);

    MinecraftHasher<ToolData.Rule> TOOL_RULE = MinecraftHasher.mapBuilder(builder -> builder
        .accept("blocks", RegistryHasher.BLOCK.holderSet(), ToolData.Rule::getBlocks)
        .optionalNullable("speed", MinecraftHasher.FLOAT, ToolData.Rule::getSpeed)
        .optionalNullable("correct_for_drops", MinecraftHasher.BOOL, ToolData.Rule::getCorrectForDrops));

    MinecraftHasher<Map<Key, String>> TRIM_MATERIAL_ASSET_OVERRIDES = MinecraftHasher.map(KEY, STRING);

    MinecraftHasher<ArmorTrim.TrimMaterial> DIRECT_TRIM_MATERIAL = MinecraftHasher.mapBuilder(builder -> builder
        .accept("asset_name", MinecraftHasher.STRING, ArmorTrim.TrimMaterial::assetBase)
        .optional("override_armor_assets", TRIM_MATERIAL_ASSET_OVERRIDES, ArmorTrim.TrimMaterial::assetOverrides, Map.of())
        .accept("description", ComponentHasher.COMPONENT, ArmorTrim.TrimMaterial::description));

    MinecraftHasher<Holder<ArmorTrim.TrimMaterial>> TRIM_MATERIAL = holder(JavaRegistries.TRIM_MATERIAL, DIRECT_TRIM_MATERIAL);

    MinecraftHasher<ProvidesTrimMaterial> PROVIDES_TRIM_MATERIAL = MinecraftHasher.either(TRIM_MATERIAL, ProvidesTrimMaterial::materialHolder, KEY, ProvidesTrimMaterial::materialLocation);

    MinecraftHasher<ArmorTrim.TrimPattern> DIRECT_TRIM_PATTERN = MinecraftHasher.mapBuilder(builder -> builder
        .accept("asset_id", KEY, ArmorTrim.TrimPattern::assetId)
        .accept("description", ComponentHasher.COMPONENT, ArmorTrim.TrimPattern::description)
        .accept("decal", BOOL, ArmorTrim.TrimPattern::decal));

    MinecraftHasher<Holder<ArmorTrim.TrimPattern>> TRIM_PATTERN = holder(JavaRegistries.TRIM_PATTERN, DIRECT_TRIM_PATTERN);

    MinecraftHasher<ArmorTrim> ARMOR_TRIM = MinecraftHasher.mapBuilder(builder -> builder
        .accept("material", TRIM_MATERIAL, ArmorTrim::material)
        .accept("pattern", TRIM_PATTERN, ArmorTrim::pattern));

    MinecraftHasher<JukeboxPlayable.JukeboxSong> DIRECT_JUKEBOX_SONG = MinecraftHasher.mapBuilder(builder -> builder
        .accept("sound_event", SOUND_EVENT, JukeboxPlayable.JukeboxSong::soundEvent)
        .accept("description", ComponentHasher.COMPONENT, JukeboxPlayable.JukeboxSong::description)
        .accept("length_in_seconds", FLOAT, JukeboxPlayable.JukeboxSong::lengthInSeconds)
        .accept("comparator_output", INT, JukeboxPlayable.JukeboxSong::comparatorOutput));

    MinecraftHasher<Holder<JukeboxPlayable.JukeboxSong>> JUKEBOX_SONG = holder(JavaRegistries.JUKEBOX_SONG, DIRECT_JUKEBOX_SONG);

    MinecraftHasher<JukeboxPlayable> JUKEBOX_PLAYABLE = MinecraftHasher.either(JUKEBOX_SONG, JukeboxPlayable::songHolder, KEY, JukeboxPlayable::songLocation);

    static RegistryHasher registry(JavaRegistryKey<?> registry) {
        MinecraftHasher<Integer> hasher = KEY.sessionConvert(registry::keyFromNetworkId);
        return hasher::hash;
    }

    // We don't need the registry generic type, and this works easier for goat horn instruments and other registries
    static <T> MinecraftHasher<Holder<T>> holder(JavaRegistryKey<?> registry, MinecraftHasher<T> direct) {
        RegistryHasher registryHasher = registry(registry);
        return (value, encoder) -> {
            if (value.isId()) {
                return registryHasher.hash(value.id(), encoder);
            }
            return direct.hash(value.custom(), encoder);
        };
    }

    // TODO note that this only works if the enum constants match
    static <T extends Enum<T>> MinecraftHasher<T> enumRegistry() {
        return KEY.convert(t -> MinecraftKey.key(t.name().toLowerCase()));
    }

    static <T extends Enum<T>> RegistryHasher enumIdRegistry(T[] values) {
        MinecraftHasher<Integer> hasher = KEY.convert(i -> MinecraftKey.key(values[i].name().toLowerCase()));
        return hasher::hash;
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

    enum ConsumeEffectType {
        APPLY_EFFECTS(ConsumeEffect.ApplyEffects.class, builder -> builder
            .acceptList("effects", MOB_EFFECT_INSTANCE, ConsumeEffect.ApplyEffects::effects)
            .optional("probability", FLOAT, ConsumeEffect.ApplyEffects::probability, 1.0F)),
        REMOVE_EFFECTS(ConsumeEffect.RemoveEffects.class, builder -> builder
            .accept("effects", EFFECT_ID.holderSet(), ConsumeEffect.RemoveEffects::effects)),
        CLEAR_ALL_EFFECTS(ConsumeEffect.ClearAllEffects.class),
        TELEPORT_RANDOMLY(ConsumeEffect.TeleportRandomly.class, builder -> builder
            .optional("diameter", FLOAT, ConsumeEffect.TeleportRandomly::diameter, 16.0F)),
        PLAY_SOUND(ConsumeEffect.PlaySound.class, builder -> builder
            .accept("sound", SOUND_EVENT, ConsumeEffect.PlaySound::sound));

        private final Class<? extends ConsumeEffect> clazz;
        private final MapBuilder<? extends ConsumeEffect> builder;

        <T extends ConsumeEffect> ConsumeEffectType(Class<T> clazz) {
            this.clazz = clazz;
            this.builder = MapBuilder.empty();
        }

        <T extends ConsumeEffect> ConsumeEffectType(Class<T> clazz, MapBuilder<T> builder) {
            this.clazz = clazz;
            this.builder = builder;
        }

        static ConsumeEffectType fromEffect(ConsumeEffect effect) {
            Class<? extends ConsumeEffect> clazz = effect.getClass();
            for (ConsumeEffectType type : values()) {
                if (clazz == type.clazz) {
                    return type;
                }
            }
            throw new IllegalStateException("Unimplemented consume effect type for hashing");
        }
    }
}
