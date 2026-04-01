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
import org.geysermc.geyser.api.pack.ResourcePackManifest
import org.geysermc.geyser.api.pack.option.SubpackOption.Companion.subpack

/**
 * Can be used to specify which subpack from a resource pack a player should load.
 * Available subpacks can be seen in a resource pack manifest [ResourcePackManifest.subpacks].
 * @since 2.6.2
 */
interface SubpackOption : ResourcePackOption<String?> {
    companion object {
        /**
         * Creates a subpack option based on a [ResourcePackManifest.Subpack].
         * 
         * @param subpack the chosen subpack
         * @return a subpack option specifying that subpack
         * @since 2.6.2
         */
        fun subpack(subpack: ResourcePackManifest.Subpack): SubpackOption {
            return named(subpack.name())
        }

        /**
         * Creates a subpack option based on a subpack name.
         * 
         * @param subpackName the name of the subpack
         * @return a subpack option specifying a subpack with that name
         * @since 2.6.2
         */
        fun named(subpackName: String): SubpackOption {
            return GeyserApi.Companion.api()
                .provider<SubpackOption, SubpackOption?>(SubpackOption::class.java, subpackName)
        }

        /**
         * Creates a subpack option with no subpack specified.
         * 
         * @return a subpack option specifying no subpack
         * @since 2.6.2
         */
        fun empty(): SubpackOption {
            return GeyserApi.Companion.api().provider<SubpackOption, SubpackOption?>(SubpackOption::class.java, "")
        }
    }
}
