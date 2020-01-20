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

package org.geysermc.connector.entity.attribute;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AttributeType {

    // Universal Attributes
    FOLLOW_RANGE("generic.followRange", "minecraft:follow_range", 0f, 2048f, 32f),
    KNOCKBACK_RESISTANCE("generic.knockbackResistance", "minecraft:knockback_resistance", 0f, 1f, 0f),
    MOVEMENT_SPEED("generic.movementSpeed", "minecraft:movement", 0f, 1024f, 0.1f),
    FLYING_SPEED("generic.flyingSpeed", "minecraft:movement", 0.0f, 1024.0f, 0.4000000059604645f),
    ATTACK_DAMAGE("generic.attackDamage", "minecraft:attack_damage", 0f, 2048f, 1f),

    // Java Attributes
    ARMOR("generic.armor", null, 0f, 30f, 0f),
    ARMOR_TOUGHNESS("generic.armorToughness", null, 0F, 20f, 0f),
    ATTACK_KNOCKBACK("generic.attackKnockback", null, 1.5f, Float.MAX_VALUE, 0f),
    ATTACK_SPEED("generic.attackSpeed", null, 0f, 1024f, 4f),
    LUCK("generic.luck", null, -1024f, 1024f, 0f),
    MAX_HEALTH("generic.maxHealth", null, 0f, 1024f, 20f),

    // Bedrock Attributes
    ABSORPTION(null, "minecraft:absorption", 0f, Float.MAX_VALUE, 0f),
    EXHAUSTION(null, "minecraft:player.exhaustion", 0f, 5f, 0f),
    EXPERIENCE(null, "minecraft:player.experience", 0f, 1f, 0f),
    EXPERIENCE_LEVEL(null, "minecraft:player.level", 0f, 24791.00f, 0f),
    HEALTH(null, "minecraft:health", 0f, 1024f, 20f),
    HUNGER(null, "minecraft:player.hunger", 0f, 20f, 20f),
    SATURATION(null, "minecraft:player.saturation", 0f, 20f, 20f);

    private String javaIdentifier;
    private String bedrockIdentifier;

    private float minimum;
    private float maximum;
    private float defaultValue;

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
