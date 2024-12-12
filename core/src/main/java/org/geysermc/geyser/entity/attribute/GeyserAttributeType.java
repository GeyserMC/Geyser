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

package org.geysermc.geyser.entity.attribute;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GeyserAttributeType {

    // Universal Attributes
    FOLLOW_RANGE("minecraft:generic.follow_range", "minecraft:follow_range", 0f, 2048f, 32f),
    KNOCKBACK_RESISTANCE("minecraft:generic.knockback_resistance", "minecraft:knockback_resistance", 0f, 1f, 0f),
    MOVEMENT_SPEED("minecraft:generic.movement_speed", "minecraft:movement", 0f, 1024f, 0.1f),
    FLYING_SPEED("minecraft:generic.flying_speed", "minecraft:movement", 0.0f, 1024.0f, 0.4000000059604645f),
    ATTACK_DAMAGE("minecraft:generic.attack_damage", "minecraft:attack_damage", 0f, 2048f, 1f),
    HORSE_JUMP_STRENGTH("minecraft:horse.jump_strength", "minecraft:horse.jump_strength", 0.0f, 2.0f, 0.7f),
    LUCK("minecraft:generic.luck", "minecraft:luck", -1024f, 1024f, 0f),

    // Java Attributes
    ARMOR("minecraft:generic.armor", null, 0f, 30f, 0f),
    ARMOR_TOUGHNESS("minecraft:generic.armor_toughness", null, 0F, 20f, 0f),
    ATTACK_KNOCKBACK("minecraft:generic.attack_knockback", null, 1.5f, Float.MAX_VALUE, 0f),
    ATTACK_SPEED("minecraft:generic.attack_speed", null, 0f, 1024f, 4f),
    MAX_HEALTH("minecraft:generic.max_health", null, 0f, 1024f, 20f),
    SCALE("minecraft:generic.scale", null, 0.0625f, 16f, 1f),
    BLOCK_INTERACTION_RANGE("minecraft:player.block_interaction_range", null, 0.0f, 64f, 4.5f),
    MINING_EFFICIENCY("minecraft:mining_efficiency", null, 0f, 1024f, 0f),
    BLOCK_BREAK_SPEED("minecraft:block_break_speed", null, 0f, 1024f, 1f),
    SUBMERGED_MINING_SPEED("minecraft:submerged_mining_speed", null, 0f, 20f, 0.2f),

    // Bedrock Attributes
    ABSORPTION(null, "minecraft:absorption", 0f, 1024f, 0f),
    EXHAUSTION(null, "minecraft:player.exhaustion", 0f, 20f, 0f),
    EXPERIENCE(null, "minecraft:player.experience", 0f, 1f, 0f),
    EXPERIENCE_LEVEL(null, "minecraft:player.level", 0f, 24791.00f, 0f),
    HEALTH(null, "minecraft:health", 0f, 1024f, 20f),
    HUNGER(null, "minecraft:player.hunger", 0f, 20f, 20f),
    SATURATION(null, "minecraft:player.saturation", 0f, 20f, 20f);

    private final String javaIdentifier;
    private final String bedrockIdentifier;

    private final float minimum;
    private final float maximum;
    private final float defaultValue;

    public AttributeData getAttribute() {
        return getAttribute(defaultValue);
    }

    public AttributeData getAttribute(float value) {
        return getAttribute(value, maximum);
    }

    public @Nullable AttributeData getAttribute(float value, float maximum) {
        if (bedrockIdentifier == null) {
            return null;
        }
        // Minimum, maximum, and default values are hardcoded on Java Edition, whereas controlled by the server on Bedrock
        return new AttributeData(bedrockIdentifier, minimum, maximum, value, defaultValue);
    }
}
