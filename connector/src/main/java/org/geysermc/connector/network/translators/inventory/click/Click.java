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

package org.geysermc.connector.network.translators.inventory.click;

import com.github.steveice10.mc.protocol.data.game.window.*;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Click {
    LEFT(WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK),
    RIGHT(WindowAction.CLICK_ITEM, ClickItemParam.RIGHT_CLICK),
    LEFT_SHIFT(WindowAction.SHIFT_CLICK_ITEM, ShiftClickItemParam.LEFT_CLICK),
    DROP_ONE(WindowAction.DROP_ITEM, DropItemParam.DROP_FROM_SELECTED),
    DROP_ALL(WindowAction.DROP_ITEM, DropItemParam.DROP_SELECTED_STACK),
    LEFT_OUTSIDE(WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK),
    RIGHT_OUTSIDE(WindowAction.CLICK_ITEM, ClickItemParam.RIGHT_CLICK);

    public static final int OUTSIDE_SLOT = -999;

    public final WindowAction windowAction;
    public final WindowActionParam actionParam;
}
