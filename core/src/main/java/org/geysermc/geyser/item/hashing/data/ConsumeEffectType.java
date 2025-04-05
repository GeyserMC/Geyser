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

package org.geysermc.geyser.item.hashing.data;

import lombok.Getter;
import org.geysermc.geyser.item.hashing.MapBuilder;
import org.geysermc.geyser.item.hashing.MinecraftHasher;
import org.geysermc.geyser.item.hashing.RegistryHasher;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ConsumeEffect;

public enum ConsumeEffectType {
    APPLY_EFFECTS(ConsumeEffect.ApplyEffects.class, builder -> builder
        .acceptList("effects", RegistryHasher.MOB_EFFECT_INSTANCE, ConsumeEffect.ApplyEffects::effects)
        .optional("probability", MinecraftHasher.FLOAT, ConsumeEffect.ApplyEffects::probability, 1.0F)),
    REMOVE_EFFECTS(ConsumeEffect.RemoveEffects.class, builder -> builder
        .accept("effects", RegistryHasher.EFFECT_ID.holderSet(), ConsumeEffect.RemoveEffects::effects)),
    CLEAR_ALL_EFFECTS(ConsumeEffect.ClearAllEffects.class),
    TELEPORT_RANDOMLY(ConsumeEffect.TeleportRandomly.class, builder -> builder
        .optional("diameter", MinecraftHasher.FLOAT, ConsumeEffect.TeleportRandomly::diameter, 16.0F)),
    PLAY_SOUND(ConsumeEffect.PlaySound.class, builder -> builder
        .accept("sound", RegistryHasher.SOUND_EVENT, ConsumeEffect.PlaySound::sound));

    private final Class<? extends ConsumeEffect> clazz;
    @Getter
    private final MapBuilder<? extends ConsumeEffect> builder;

    <T extends ConsumeEffect> ConsumeEffectType(Class<T> clazz) {
        this.clazz = clazz;
        this.builder = MapBuilder.empty();
    }

    <T extends ConsumeEffect> ConsumeEffectType(Class<T> clazz, MapBuilder<T> builder) {
        this.clazz = clazz;
        this.builder = builder;
    }

    public static ConsumeEffectType fromEffect(ConsumeEffect effect) {
        Class<? extends ConsumeEffect> clazz = effect.getClass();
        for (ConsumeEffectType type : values()) {
            if (clazz == type.clazz) {
                return type;
            }
        }
        throw new IllegalStateException("Unimplemented consume effect type for hashing");
    }
}
