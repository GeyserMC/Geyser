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

package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeModifier;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.ModifierOperation;
import com.nukkitx.protocol.bedrock.data.AttributeData;
import org.geysermc.connector.entity.attribute.Attribute;
import org.geysermc.connector.entity.attribute.AttributeType;

public class AttributeUtils {

    public static com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute getJavaAttribute(Attribute attribute) {
        if (!attribute.getType().isJavaAttribute())
            return null;

        com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType type = null;
        try {
            type = com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType.valueOf("GENERIC_" + attribute.getType().name());
        } catch (Exception ex) {
            // Catch and loop since attributes can semi-overlap
            for (AttributeType attributeType : AttributeType.values()) {
                if (!attributeType.isJavaAttribute())
                    continue;

                if (!attributeType.getJavaIdentifier().equals(attribute.getType().getJavaIdentifier()))
                    continue;

                try {
                    type = com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType.valueOf("GENERIC_" + attributeType.name());
                } catch (Exception e) {
                    continue;
                }
            }
        }

        if (type == null)
            return null;

        return new com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute(type, attribute.getValue());
    }

    public static AttributeData getBedrockAttribute(Attribute attribute) {
        AttributeType type = attribute.getType();
        if (!type.isBedrockAttribute())
            return null;

        return new AttributeData(type.getBedrockIdentifier(), attribute.getMinimum(), attribute.getMaximum(), attribute.getValue(), attribute.getDefaultValue());
    }

    /**
     * Retrieve the base attribute value with all modifiers applied.
     * https://minecraft.gamepedia.com/Attribute#Modifiers
     * @param attribute The attribute to calculate the total value.
     * @return The finished attribute with all modifiers applied.
     */
    public static double calculateValue(com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute attribute) {
        double base = attribute.getValue();
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (modifier.getOperation() == ModifierOperation.ADD) {
                base += modifier.getAmount();
            }
        }
        double value = base;
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (modifier.getOperation() == ModifierOperation.ADD_MULTIPLIED) {
                value += base * modifier.getAmount();
            }
        }
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (modifier.getOperation() == ModifierOperation.MULTIPLY) {
                value *= 1.0D + modifier.getAmount();
            }
        }
        return value;
    }
}
