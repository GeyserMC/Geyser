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

package org.geysermc.connector.utils;

import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeModifier;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.ModifierOperation;

public class AttributeUtils {
    /**
     * Retrieve the base attribute value with all modifiers applied.
     * https://minecraft.gamepedia.com/Attribute#Modifiers
     * @param attribute The attribute to calculate the total value.
     * @return The finished attribute with all modifiers applied.
     */
    public static double calculateValue(Attribute attribute) {
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
