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
import org.geysermc.geyser.api.util.TriState
import java.util.*

/**
 * This class represents the different ways you can register custom items
 * 
 */
@Deprecated("use the new {@link org.geysermc.geyser.api.item.custom.v2.CustomItemDefinition}.")
interface CustomItemOptions {
    /**
     * Gets if the item should be unbreakable.
     * 
     * @return if the item should be unbreakable
     */
    fun unbreakable(): TriState

    /**
     * Gets the item's custom model data predicate.
     * 
     * @return the item's custom model data
     */
    fun customModelData(): OptionalInt

    /**
     * Gets the item's damage predicate.
     * 
     * @return the item's damage predicate
     */
    fun damagePredicate(): OptionalInt

    /**
     * Gets if this mapping should just translate to the default item.
     * This is used for the damage predicate of damaged 1 damage 0 that is required to allow the default item to exist.
     * 
     * @return true if this mapping should just translate to the default item, false otherwise
     */
    fun defaultItem(): Boolean

    /**
     * Checks if the item has at least one option set
     * 
     * @return true if the item at least one options set
     */
    fun hasCustomItemOptions(): Boolean {
        return this.unbreakable() != TriState.NOT_SET ||
                this.customModelData().isPresent() ||
                this.damagePredicate().isPresent()
    }

    interface Builder {
        fun unbreakable(unbreakable: Boolean): Builder?

        fun customModelData(customModelData: Int): Builder?

        fun damagePredicate(damagePredicate: Int): Builder?

        fun defaultItem(defaultItem: Boolean): Builder?

        fun build(): CustomItemOptions?
    }

    companion object {
        @kotlin.jvm.JvmStatic
        fun builder(): Builder {
            return GeyserApi.Companion.api().provider<Builder, Builder?>(Builder::class.java)
        }
    }
}
