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

/**
 * Represent GUI elements on the players HUD display.
 * These can be hidden using {@link CameraData#hideElement(GuiElement...)},
 * and one can reset their visibility using {@link CameraData#resetElement(GuiElement...)}.
 */
public class GuiElement {
    public static final GuiElement PAPER_DOLL = new GuiElement(0);
    public static final GuiElement ARMOR = new GuiElement(1);
    public static final GuiElement TOOL_TIPS = new GuiElement(2);
    public static final GuiElement TOUCH_CONTROLS = new GuiElement(3);
    public static final GuiElement CROSSHAIR = new GuiElement(4);
    public static final GuiElement HOTBAR = new GuiElement(5);
    public static final GuiElement HEALTH = new GuiElement(6);
    public static final GuiElement PROGRESS_BAR = new GuiElement(7);
    public static final GuiElement FOOD_BAR = new GuiElement(8);
    public static final GuiElement AIR_BUBBLES_BAR = new GuiElement(9);
    public static final GuiElement VEHICLE_HEALTH = new GuiElement(10);
    public static final GuiElement EFFECTS_BAR = new GuiElement(11);
    public static final GuiElement ITEM_TEXT_POPUP = new GuiElement(12);

    private GuiElement(int id) {
        this.id = id;
    }

    private final int id;

    /**
     * Internal use only; don't depend on these values being consistent.
     */
    public int id() {
        return this.id;
    }
}
