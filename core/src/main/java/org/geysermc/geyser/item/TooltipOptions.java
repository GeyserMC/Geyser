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

package org.geysermc.geyser.item;

import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.TooltipDisplay;

@FunctionalInterface
public interface TooltipOptions {

    TooltipOptions ALL_SHOWN = component -> true;

    TooltipOptions ALL_HIDDEN = component -> false;

    boolean showInTooltip(DataComponentType<?> component);

    static TooltipOptions fromComponents(DataComponents components) {
        TooltipDisplay display = components.get(DataComponentTypes.TOOLTIP_DISPLAY);
        if (display == null) {
            return ALL_SHOWN;
        } else if (display.hideTooltip()) {
            return ALL_HIDDEN;
        } else if (display.hiddenComponents().isEmpty()) {
            return ALL_SHOWN;
        }

        return component -> !display.hiddenComponents().contains(component);
    }

    static boolean hideTooltip(DataComponents components) {
        TooltipDisplay display = components.get(DataComponentTypes.TOOLTIP_DISPLAY);
        return display != null && display.hideTooltip();
    }
}
