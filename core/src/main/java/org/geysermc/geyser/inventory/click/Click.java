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

package org.geysermc.geyser.inventory.click;

import org.geysermc.mcprotocollib.protocol.data.game.inventory.*;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Click {
    LEFT(ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK),
    LEFT_BUNDLE(ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK),
    LEFT_BUNDLE_FROM_CURSOR(ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK),
    RIGHT(ContainerActionType.CLICK_ITEM, ClickItemAction.RIGHT_CLICK),
    RIGHT_BUNDLE(ContainerActionType.CLICK_ITEM, ClickItemAction.RIGHT_CLICK),
    LEFT_SHIFT(ContainerActionType.SHIFT_CLICK_ITEM, ShiftClickItemAction.LEFT_CLICK),
    DROP_ONE(ContainerActionType.DROP_ITEM, DropItemAction.DROP_FROM_SELECTED),
    DROP_ALL(ContainerActionType.DROP_ITEM, DropItemAction.DROP_SELECTED_STACK),
    LEFT_OUTSIDE(ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK),
    RIGHT_OUTSIDE(ContainerActionType.CLICK_ITEM, ClickItemAction.RIGHT_CLICK),
    SWAP_TO_HOTBAR_1(ContainerActionType.MOVE_TO_HOTBAR_SLOT, MoveToHotbarAction.SLOT_1),
    SWAP_TO_HOTBAR_2(ContainerActionType.MOVE_TO_HOTBAR_SLOT, MoveToHotbarAction.SLOT_2),
    SWAP_TO_HOTBAR_3(ContainerActionType.MOVE_TO_HOTBAR_SLOT, MoveToHotbarAction.SLOT_3),
    SWAP_TO_HOTBAR_4(ContainerActionType.MOVE_TO_HOTBAR_SLOT, MoveToHotbarAction.SLOT_4),
    SWAP_TO_HOTBAR_5(ContainerActionType.MOVE_TO_HOTBAR_SLOT, MoveToHotbarAction.SLOT_5),
    SWAP_TO_HOTBAR_6(ContainerActionType.MOVE_TO_HOTBAR_SLOT, MoveToHotbarAction.SLOT_6),
    SWAP_TO_HOTBAR_7(ContainerActionType.MOVE_TO_HOTBAR_SLOT, MoveToHotbarAction.SLOT_7),
    SWAP_TO_HOTBAR_8(ContainerActionType.MOVE_TO_HOTBAR_SLOT, MoveToHotbarAction.SLOT_8),
    SWAP_TO_HOTBAR_9(ContainerActionType.MOVE_TO_HOTBAR_SLOT, MoveToHotbarAction.SLOT_9);

    public static final int OUTSIDE_SLOT = -999;

    public final ContainerActionType actionType;
    public final ContainerAction action;
}
