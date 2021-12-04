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

package org.geysermc.geyser.inventory.click;

import com.github.steveice10.mc.protocol.data.game.inventory.ClickItemAction;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerActionType;
import com.github.steveice10.mc.protocol.data.game.inventory.*;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Click {
    LEFT(ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK),
    RIGHT(ContainerActionType.CLICK_ITEM, ClickItemAction.RIGHT_CLICK),
    LEFT_SHIFT(ContainerActionType.SHIFT_CLICK_ITEM, ShiftClickItemAction.LEFT_CLICK),
    DROP_ONE(ContainerActionType.DROP_ITEM, DropItemAction.DROP_FROM_SELECTED),
    DROP_ALL(ContainerActionType.DROP_ITEM, DropItemAction.DROP_SELECTED_STACK),
    LEFT_OUTSIDE(ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK),
    RIGHT_OUTSIDE(ContainerActionType.CLICK_ITEM, ClickItemAction.RIGHT_CLICK);

    public static final int OUTSIDE_SLOT = -999;

    public final ContainerActionType actionType;
    public final ContainerAction action;
}
