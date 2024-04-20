/*
 * Copyright (c) 2019-2023 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.item;

import com.github.steveice10.mc.protocol.data.game.item.component.DataComponentType;
import com.github.steveice10.mc.protocol.data.game.item.component.DataComponents;
import com.github.steveice10.mc.protocol.data.game.item.component.DyedItemColor;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.IntTag;
import org.geysermc.geyser.translator.item.BedrockItemBuilder;

public interface DyeableLeatherItem {

    static void translateComponentsToBedrock(DataComponents components, BedrockItemBuilder builder) {
        DyedItemColor dyedItemColor = components.get(DataComponentType.DYED_COLOR);
        if (dyedItemColor == null) {
            return;
        }
        builder.putInt("customColor", dyedItemColor.getRgb());
    }

    static void translateNbtToJava(CompoundTag tag) {
        IntTag color = tag.get("customColor");
        if (color == null) {
            return;
        }
        CompoundTag displayTag = tag.get("display");
        if (displayTag == null) {
            displayTag = new CompoundTag("display");
        }
        displayTag.put(color);
        tag.remove("customColor");
    }
}
