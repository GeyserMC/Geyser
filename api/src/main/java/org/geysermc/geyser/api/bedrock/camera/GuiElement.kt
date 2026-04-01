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
package org.geysermc.geyser.api.bedrock.camera

/**
 * Represent GUI elements on the players HUD display.
 * These can be hidden using [CameraData.hideElement],
 * and one can reset their visibility using [CameraData.resetElement].
 */
class GuiElement private constructor(private val id: Int) {
    /**
     * Internal use only; don't depend on these values being consistent.
     */
    fun id(): Int {
        return this.id
    }

    companion object {
        @kotlin.jvm.JvmField
        val PAPER_DOLL: GuiElement = GuiElement(0)
        @kotlin.jvm.JvmField
        val ARMOR: GuiElement = GuiElement(1)
        @kotlin.jvm.JvmField
        val TOOL_TIPS: GuiElement = GuiElement(2)
        val TOUCH_CONTROLS: GuiElement = GuiElement(3)
        val CROSSHAIR: GuiElement = GuiElement(4)
        val HOTBAR: GuiElement = GuiElement(5)
        @kotlin.jvm.JvmField
        val HEALTH: GuiElement = GuiElement(6)
        @kotlin.jvm.JvmField
        val PROGRESS_BAR: GuiElement = GuiElement(7)
        @kotlin.jvm.JvmField
        val FOOD_BAR: GuiElement = GuiElement(8)
        @kotlin.jvm.JvmField
        val AIR_BUBBLES_BAR: GuiElement = GuiElement(9)
        @kotlin.jvm.JvmField
        val VEHICLE_HEALTH: GuiElement = GuiElement(10)
        val EFFECTS_BAR: GuiElement = GuiElement(11)
        val ITEM_TEXT_POPUP: GuiElement = GuiElement(12)
    }
}
