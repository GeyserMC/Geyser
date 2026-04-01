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
package org.geysermc.geyser.api.item.custom

import org.geysermc.geyser.api.GeyserApi
import java.util.*

/**
 * This is used to store data for a custom item.
 * 
 */
@Deprecated("use the new {@link org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition}")
interface CustomItemData {
    /**
     * Gets the item's name.
     * 
     * @return the item's name
     */
    fun name(): String

    /**
     * Gets the custom item options of the item.
     * 
     * @return the custom item options of the item.
     */
    fun customItemOptions(): CustomItemOptions?

    /**
     * Gets the item's display name. By default, this is the item's name.
     * 
     * @return the item's display name
     */
    fun displayName(): String

    /**
     * Gets the item's icon. By default, this is the item's name.
     * 
     * @return the item's icon
     */
    fun icon(): String

    /**
     * Gets if the item is allowed to be put into the offhand.
     * 
     * @return true if the item is allowed to be used in the offhand, false otherwise
     */
    fun allowOffhand(): Boolean

    /**
     * Gets if the item should be displayed as handheld, like a tool.
     * 
     * @return true if the item should be displayed as handheld, false otherwise
     */
    fun displayHandheld(): Boolean

    /**
     * Gets the item's creative category, or tab id.
     * 
     * @return the item's creative category
     */
    fun creativeCategory(): OptionalInt

    /**
     * Gets the item's creative group.
     * 
     * @return the item's creative group
     */
    fun creativeGroup(): String?

    /**
     * Gets the item's texture size. This is to resize the item if the texture is not 16x16.
     * 
     * @return the item's texture size
     */
    @Deprecated(
        """setting the texture size is deprecated; use attachables instead
      """
    )
    fun textureSize(): Int

    /**
     * Gets the item's render offsets. If it is null, the item will be rendered normally, with no offsets.
     * 
     * @return the item's render offsets
     */
    @Deprecated(
        """render offsets have been deprecated; attachables should be used instead
      """
    )
    fun renderOffsets(): CustomRenderOffsets?

    /**
     * Gets the item's set of tags that can be used in Molang.
     * Equivalent to "tag:some_tag"
     * 
     * @return the item's tags, if they exist
     */
    fun tags(): MutableSet<String?>

    interface Builder {
        /**
         * Will also set the display name and icon to the provided parameter, if it is currently not set.
         */
        fun name(name: String): Builder?

        fun customItemOptions(customItemOptions: CustomItemOptions): Builder?

        fun displayName(displayName: String): Builder?

        fun icon(icon: String): Builder?

        fun allowOffhand(allowOffhand: Boolean): Builder?

        fun displayHandheld(displayHandheld: Boolean): Builder?

        fun creativeCategory(creativeCategory: Int): Builder?

        fun creativeGroup(creativeGroup: String?): Builder?

        @Deprecated("")
        fun textureSize(textureSize: Int): Builder?

        @Deprecated("")
        fun renderOffsets(renderOffsets: CustomRenderOffsets?): Builder?

        fun tags(tags: MutableSet<String?>?): Builder?

        fun build(): CustomItemData?
    }

    companion object {
        @kotlin.jvm.JvmStatic
        fun builder(): Builder {
            return GeyserApi.Companion.api().provider<Builder, Builder?>(Builder::class.java)
        }
    }
}
