/*
 * Copyright (c) 2026 GeyserMC. http://geysermc.org
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

package org.geysermc.geyser.api.biome.custom;

import org.geysermc.geyser.api.GeyserApi;
import org.jetbrains.annotations.ApiStatus;

/**
 * Ambient ash or spore particles shown in a custom biome, like in the vanilla soul sand
 * valley and warped forest. Bedrock exposes these through its precipitation component,
 * but they are not rain or snow; those follow the Java biome's climate. A biome can have
 * at most one precipitation type.
 *
 * @since 2.11.1
 */
@ApiStatus.NonExtendable
public interface CustomBiomePrecipitation {

    /**
     * The particle type shown in this biome.
     *
     * @return the ambient particle type
     * @since 2.11.1
     */
    Type type();

    /**
     * The particle density, {@code 0.0} or greater. For reference, the vanilla basalt
     * deltas use a white ash density of {@code 2.0}.
     *
     * @return the particle density
     * @since 2.11.1
     */
    float density();

    /**
     * Creates a precipitation instance of the given type and density.
     *
     * @param type the ambient particle type
     * @param density the particle density, {@code 0.0} or greater
     * @return a new precipitation instance
     * @throws NullPointerException when the type is null
     * @throws IllegalArgumentException when the density is negative or not finite
     * @since 2.11.1
     */
    static CustomBiomePrecipitation of(Type type, float density) {
        return GeyserApi.api().provider(CustomBiomePrecipitation.class, type, density);
    }

    /**
     * The available precipitation particle types.
     *
     * @since 2.11.1
     */
    enum Type {
        /**
         * Ash particles, used by the vanilla soul sand valley
         */
        ASH,
        /**
         * White ash particles, used by the vanilla basalt deltas
         */
        WHITE_ASH,
        /**
         * Red spore particles, used by the vanilla crimson forest
         */
        RED_SPORES,
        /**
         * Blue spore particles, used by the vanilla warped forest
         */
        BLUE_SPORES
    }
}
