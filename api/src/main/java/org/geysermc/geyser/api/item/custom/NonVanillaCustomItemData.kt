/*
 * Copyright (c) 2019-2024 GeyserMC. http://geysermc.org
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
package org.geysermc.geyser.api.item.custom

import org.checkerframework.checker.index.qual.NonNegative
import org.geysermc.geyser.api.GeyserApi

/**
 * Represents a completely custom item that is not based on an existing vanilla Minecraft item.
 * 
 */
@Deprecated("use the new {@link org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition}")
interface NonVanillaCustomItemData : CustomItemData {
    /**
     * Gets the java identifier for this item.
     * 
     * @return The java identifier for this item.
     */
    fun identifier(): String

    /**
     * Gets the java item id of the item.
     * 
     * @return the java item id of the item
     */
    fun javaId(): @NonNegative Int

    /**
     * Gets the stack size of the item.
     * 
     * @return the stack size of the item
     */
    fun stackSize(): @NonNegative Int

    /**
     * Gets the max damage of the item.
     * 
     * @return the max damage of the item
     */
    fun maxDamage(): Int

    /**
     * Gets the attack damage of the item.
     * This is purely visual, and only applied to tools
     * 
     * @return the attack damage of the item
     */
    fun attackDamage(): Int

    /**
     * Gets the tool type of the item.
     * 
     * @return the tool type of the item
     */
    fun toolType(): String?

    @Deprecated("no longer used")
    fun toolTier(): String?

    /**
     * Gets the armor type of the item.
     * 
     * @return the armor type of the item
     */
    fun armorType(): String?

    /**
     * Gets the armor protection value of the item.
     * 
     * @return the armor protection value of the item
     */
    fun protectionValue(): Int

    /**
     * Gets the item's translation string.
     * 
     * @return the item's translation string
     */
    fun translationString(): String?

    @Deprecated("No longer used.")
    fun repairMaterials(): MutableSet<String?>?

    /**
     * Gets if the item is a hat. This is used to determine if the item should be rendered on the player's head, and
     * normally allow the player to equip it. This is not meant for armor.
     * 
     * @return if the item is a hat
     */
    val isHat: Boolean

    /**
     * Gets if the item is a foil. This is used to determine if the item should be rendered with an enchantment glint effect.
     * 
     * @return if the item is a foil
     */
    val isFoil: Boolean

    /**
     * Gets if the item is edible.
     * 
     * @return if the item is edible
     */
    val isEdible: Boolean

    /**
     * Gets if the food item can always be eaten.
     * 
     * @return if the item is allowed to be eaten all the time
     */
    fun canAlwaysEat(): Boolean

    /**
     * Gets if the item is chargable, like a bow.
     * 
     * @return if the item should act like a chargable item
     */
    val isChargeable: Boolean

    @get:Deprecated(
        """Use {@link #displayHandheld()} instead.
      Gets if the item is a tool. This is used to set the render type of the item, if the item is handheld.
     
      """
    )
    val isTool: Boolean
        /**
         * @return if the item is a tool
         */
        get() = displayHandheld()

    /**
     * Gets the block the item places.
     * 
     * @return the block the item places
     */
    fun block(): String?

    interface Builder : CustomItemData.Builder {
        override fun name(name: String): Builder?

        fun identifier(identifier: String): Builder?

        fun javaId(javaId: @NonNegative Int): Builder?

        fun stackSize(stackSize: @NonNegative Int): Builder?

        fun maxDamage(maxDamage: Int): Builder?

        fun attackDamage(attackDamage: Int): Builder?

        fun toolType(toolType: String?): Builder?

        fun toolTier(toolTier: String?): Builder?

        fun armorType(armorType: String?): Builder?

        fun protectionValue(protectionValue: Int): Builder?

        fun translationString(translationString: String?): Builder?

        fun repairMaterials(repairMaterials: MutableSet<String?>?): Builder?

        fun hat(isHat: Boolean): Builder?

        fun foil(isFoil: Boolean): Builder?

        fun edible(isEdible: Boolean): Builder?

        fun canAlwaysEat(canAlwaysEat: Boolean): Builder?

        fun chargeable(isChargeable: Boolean): Builder?

        fun block(block: String?): Builder?

        @Deprecated("Use {@link #displayHandheld(boolean)} instead.")
        fun tool(isTool: Boolean): Builder? {
            return displayHandheld(isTool)
        }

        override fun creativeCategory(creativeCategory: Int): Builder?

        override fun creativeGroup(creativeGroup: String?): Builder?

        override fun customItemOptions(customItemOptions: CustomItemOptions): Builder?

        override fun displayName(displayName: String): Builder?

        override fun icon(icon: String): Builder?

        override fun allowOffhand(allowOffhand: Boolean): Builder?

        override fun displayHandheld(displayHandheld: Boolean): Builder?

        @Deprecated("")
        override fun textureSize(textureSize: Int): Builder?

        @Deprecated("")
        override fun renderOffsets(renderOffsets: CustomRenderOffsets?): Builder?

        override fun tags(tags: MutableSet<String?>?): Builder?

        override fun build(): NonVanillaCustomItemData?
    }

    companion object {
        fun builder(): Builder {
            return GeyserApi.Companion.api().provider<Builder, Builder?>(Builder::class.java)
        }
    }
}
