/*
 * Copyright (c) 2024 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.bedrock.camera;

import org.geysermc.geyser.api.bedrock.gui.GuiData;

/**
 * Represent GUI elements on the players HUD display.
 * These can be hidden using {@link GuiData#hideElement(org.geysermc.geyser.api.bedrock.gui.GuiElement...)},
 * and one can reset their visibility using {@link GuiData#resetElement(org.geysermc.geyser.api.bedrock.gui.GuiElement...)}.
 * @deprecated use {@link org.geysermc.geyser.api.bedrock.gui.GuiElement}
 */
public class GuiElement extends org.geysermc.geyser.api.bedrock.gui.GuiElement {
    public static final org.geysermc.geyser.api.bedrock.gui.GuiElement PAPER_DOLL = org.geysermc.geyser.api.bedrock.gui.GuiElement.PAPER_DOLL;
    public static final org.geysermc.geyser.api.bedrock.gui.GuiElement ARMOR = org.geysermc.geyser.api.bedrock.gui.GuiElement.ARMOR;
    public static final org.geysermc.geyser.api.bedrock.gui.GuiElement TOOL_TIPS = org.geysermc.geyser.api.bedrock.gui.GuiElement.TOOL_TIPS;
    public static final org.geysermc.geyser.api.bedrock.gui.GuiElement TOUCH_CONTROLS = org.geysermc.geyser.api.bedrock.gui.GuiElement.TOUCH_CONTROLS;
    public static final org.geysermc.geyser.api.bedrock.gui.GuiElement CROSSHAIR = org.geysermc.geyser.api.bedrock.gui.GuiElement.CROSSHAIR;
    public static final org.geysermc.geyser.api.bedrock.gui.GuiElement HOTBAR = org.geysermc.geyser.api.bedrock.gui.GuiElement.HOTBAR;
    public static final org.geysermc.geyser.api.bedrock.gui.GuiElement HEALTH = org.geysermc.geyser.api.bedrock.gui.GuiElement.HEALTH;
    public static final org.geysermc.geyser.api.bedrock.gui.GuiElement PROGRESS_BAR = org.geysermc.geyser.api.bedrock.gui.GuiElement.PROGRESS_BAR;
    public static final org.geysermc.geyser.api.bedrock.gui.GuiElement FOOD_BAR = org.geysermc.geyser.api.bedrock.gui.GuiElement.FOOD_BAR;
    public static final org.geysermc.geyser.api.bedrock.gui.GuiElement AIR_BUBBLES_BAR = org.geysermc.geyser.api.bedrock.gui.GuiElement.AIR_BUBBLES_BAR;
    public static final org.geysermc.geyser.api.bedrock.gui.GuiElement VEHICLE_HEALTH = org.geysermc.geyser.api.bedrock.gui.GuiElement.VEHICLE_HEALTH;
    public static final org.geysermc.geyser.api.bedrock.gui.GuiElement EFFECTS_BAR = org.geysermc.geyser.api.bedrock.gui.GuiElement.EFFECTS_BAR;
    public static final org.geysermc.geyser.api.bedrock.gui.GuiElement ITEM_TEXT_POPUP = org.geysermc.geyser.api.bedrock.gui.GuiElement.ITEM_TEXT_POPUP;

    public GuiElement(int id) {
        super(id);
    }
}

