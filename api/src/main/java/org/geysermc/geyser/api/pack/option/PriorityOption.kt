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
package org.geysermc.geyser.api.pack.option

import org.geysermc.geyser.api.GeyserApi
import org.geysermc.geyser.api.pack.option.PriorityOption.Companion.priority

/**
 * Allows specifying a pack priority that decides the order on how packs are sent to the client.
 * If two resource packs modify the same texture - for example if one removes the pumpkin overlay and
 * the other is just making it translucent, one of the packs will override the other.
 * Specifically, the pack with the higher priority will override the pack changes of the lower priority.
 * @since 2.6.2
 */
interface PriorityOption : ResourcePackOption<Int?> {
    companion object {
        /**
         * Constructs a priority option based on a value between 0 and 10.
         * The higher the number, the higher will this pack appear in the resource pack stack.
         * 
         * @param priority an integer that is above 0, but smaller than 10
         * @return the priority option
         * @since 2.6.2
         */
        fun priority(priority: Int): PriorityOption {
            require(!(priority < -100 || priority > 100)) { "Priority must be between -100 and 100 inclusive!" }
            return GeyserApi.Companion.api()
                .provider<PriorityOption, PriorityOption?>(PriorityOption::class.java, priority)
        }

        val HIGHEST: PriorityOption = priority(100)
        val HIGH: PriorityOption = priority(50)
        @kotlin.jvm.JvmField
        val NORMAL: PriorityOption = priority(0)
        @kotlin.jvm.JvmField
        val LOW: PriorityOption = priority(-50)
        val LOWEST: PriorityOption = priority(-100)
    }
}
