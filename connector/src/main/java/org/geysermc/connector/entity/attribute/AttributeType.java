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

package org.geysermc.connector.entity.attribute;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AttributeType {

    // Universal Attributes
    FOLLOW_RANGE("minecraft:generic.follow_range", "minecraft:follow_range", 0f, 2048f, 32f),
    KNOCKBACK_RESISTANCE("minecraft:generic.knockback_resistance", "minecraft:knockback_resistance", 0f, 1f, 0f),
    MOVEMENT_SPEED("minecraft:generic.movement_speed", "minecraft:movement", 0f, 1024f, 0.1f),
    FLYING_SPEED("minecraft:generic.flying_speed", "minecraft:movement", 0.0f, 1024.0f, 0.4000000059604645f),
    ATTACK_DAMAGE("minecraft:generic.attack_damage", "minecraft:attack_damage", 0f, 2048f, 1f),
    HORSE_JUMP_STRENGTH("minecraft:horse.jump_strength", "minecraft:horse.jump_strength", 0.0f, 2.0f, 0.7f),

    // Java Attributes
    ARMOR("minecraft:generic.armor", null, 0f, 30f, 0f),
    ARMOR_TOUGHNESS("minecraft:generic.armor_toughness", null, 0F, 20f, 0f),
    ATTACK_KNOCKBACK("minecraft:generic.attack_knockback", null, 1.5f, Float.MAX_VALUE, 0f),
    ATTACK_SPEED("minecraft:generic.attack_speed", null, 0f, 1024f, 4f),
    LUCK("minecraft:generic.luck", null, -1024f, 1024f, 0f),
    MAX_HEALTH("minecraft:generic.max_health", null, 0f, 1024f, 20f),

    // Bedrock Attributes
    ABSORPTION(null, "minecraft:absorption", 0f, Float.MAX_VALUE, 0f),
    EXHAUSTION(null, "minecraft:player.exhaustion", 0f, 5f, 0f),
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

    public Attribute getAttribute(float value) {
        return getAttribute(value, maximum);
    }

    public Attribute getAttribute(float value, float maximum) {
        return new Attribute(this, minimum, maximum, value, defaultValue);
    }

    public boolean isJavaAttribute() {
        return javaIdentifier != null;
    }

    public boolean isBedrockAttribute() {
        return bedrockIdentifier != null;
    }
}
