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

import org.checkerframework.common.returnsreceiver.qual.This;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.util.GenericBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.awt.Color;

/**
 * The visual appearance of a custom biome, delivered to Bedrock clients through a
 * generated resource pack. All values are optional, and at least one value must be set.
 *
 * <p>The alpha component of colors is ignored. When only one of the two water fog values
 * is set, the other is completed from the vanilla Bedrock default fog ({@code #44AFF5},
 * fully opaque at 60 blocks).</p>
 *
 * @since 2.11.1
 */
@ApiStatus.NonExtendable
public interface CustomBiomeAppearance {

    /**
     * The sky color, or null to use the client's default. The Vibrant Visuals renderer
     * uses it as the daytime zenith tint; the horizon and the vanilla night colors remain.
     *
     * @return the sky color
     * @since 2.11.1
     */
    @Nullable Color skyColor();

    /**
     * The color of the distance fog in air, or null to use the client's default. Rain
     * shows it darkened, matching Java's full-rain fog. The Vibrant Visuals renderer
     * does not apply this value as of Bedrock 1.26.44.
     *
     * @return the fog color
     * @since 2.11.1
     */
    @Nullable Color fogColor();

    /**
     * The color of the water surface, or null to use the client's default. The Vibrant
     * Visuals renderer mixes it into the water rather than applying it directly, so the
     * exact rendered color can differ.
     *
     * @return the water surface color
     * @since 2.11.1
     */
    @Nullable Color waterSurfaceColor();

    /**
     * The opacity of the water surface, between {@code 0.0} and {@code 1.0} inclusive, or
     * null to use the client's default. The Vibrant Visuals renderer does not apply
     * this value as of Bedrock 1.26.44.
     *
     * @return the water surface opacity
     * @since 2.11.1
     */
    @Nullable Float waterSurfaceOpacity();

    /**
     * The color of the fog seen underwater, or null when not set. The Vibrant Visuals
     * renderer does not apply it to the underwater haze as of Bedrock 1.26.44, though
     * nearby submerged surfaces still pick up the tint.
     *
     * @return the underwater fog color
     * @since 2.11.1
     */
    @Nullable Color waterFogColor();

    /**
     * The distance, in blocks, at which the underwater fog is fully opaque, or null when
     * not set.
     *
     * @return the underwater fog end distance
     * @since 2.11.1
     */
    @Nullable Float waterFogEndDistance();

    /**
     * The grass tint, or null to let the client derive one from the biome's climate.
     *
     * @return the grass color
     * @since 2.11.1
     */
    @Nullable Color grassColor();

    /**
     * The foliage tint, or null to let the client derive one from the biome's climate.
     *
     * @return the foliage color
     * @since 2.11.1
     */
    @Nullable Color foliageColor();

    /**
     * The dry foliage tint, or null to use the client's default.
     *
     * @return the dry foliage color
     * @since 2.11.1
     */
    @Nullable Color dryFoliageColor();

    /**
     * The ambient ash or spore particles shown in this biome, or null for none.
     *
     * @return the biome's ambient ash or spore particles
     * @since 2.11.1
     */
    @Nullable CustomBiomePrecipitation precipitation();

    /**
     * Creates a builder for a custom biome appearance.
     *
     * @return a new appearance builder
     * @since 2.11.1
     */
    static Builder builder() {
        return GeyserApi.api().provider(Builder.class);
    }

    /**
     * The builder for a custom biome appearance.
     * @since 2.11.1
     */
    interface Builder extends GenericBuilder<CustomBiomeAppearance> {

        /**
         * Sets the sky color.
         *
         * @param skyColor the sky color
         * @see CustomBiomeAppearance#skyColor()
         * @return this builder
         * @since 2.11.1
         */
        @This
        Builder skyColor(Color skyColor);

        /**
         * Sets the fog color.
         *
         * @param fogColor the fog color
         * @see CustomBiomeAppearance#fogColor()
         * @return this builder
         * @since 2.11.1
         */
        @This
        Builder fogColor(Color fogColor);

        /**
         * Sets the color of the water surface.
         *
         * @param waterSurfaceColor the water surface color
         * @see CustomBiomeAppearance#waterSurfaceColor()
         * @return this builder
         * @since 2.11.1
         */
        @This
        Builder waterSurfaceColor(Color waterSurfaceColor);

        /**
         * Sets the opacity of the water surface, between {@code 0.0} and {@code 1.0}
         * inclusive.
         *
         * @param waterSurfaceOpacity the water surface opacity
         * @see CustomBiomeAppearance#waterSurfaceOpacity()
         * @return this builder
         * @since 2.11.1
         */
        @This
        Builder waterSurfaceOpacity(float waterSurfaceOpacity);

        /**
         * Sets the color of the fog seen underwater.
         *
         * @param waterFogColor the underwater fog color
         * @see CustomBiomeAppearance#waterFogColor()
         * @return this builder
         * @since 2.11.1
         */
        @This
        Builder waterFogColor(Color waterFogColor);

        /**
         * Sets the distance, in blocks, at which the underwater fog is fully opaque.
         *
         * @param waterFogEndDistance the underwater fog end distance
         * @see CustomBiomeAppearance#waterFogEndDistance()
         * @return this builder
         * @since 2.11.1
         */
        @This
        Builder waterFogEndDistance(float waterFogEndDistance);

        /**
         * Sets the grass tint.
         *
         * @param grassColor the grass color
         * @see CustomBiomeAppearance#grassColor()
         * @return this builder
         * @since 2.11.1
         */
        @This
        Builder grassColor(Color grassColor);

        /**
         * Sets the foliage tint.
         *
         * @param foliageColor the foliage color
         * @see CustomBiomeAppearance#foliageColor()
         * @return this builder
         * @since 2.11.1
         */
        @This
        Builder foliageColor(Color foliageColor);

        /**
         * Sets the dry foliage tint.
         *
         * @param dryFoliageColor the dry foliage color
         * @see CustomBiomeAppearance#dryFoliageColor()
         * @return this builder
         * @since 2.11.1
         */
        @This
        Builder dryFoliageColor(Color dryFoliageColor);

        /**
         * Sets the ambient ash or spore particles shown in this biome.
         *
         * @param precipitation the ambient ash or spore particles
         * @see CustomBiomeAppearance#precipitation()
         * @return this builder
         * @since 2.11.1
         */
        @This
        Builder precipitation(CustomBiomePrecipitation precipitation);

        /**
         * Creates the custom biome appearance.
         *
         * @return the created appearance
         * @throws IllegalArgumentException when no value was set, when a numeric value is
         * out of range, or when the precipitation was not created through {@link CustomBiomePrecipitation#of}
         * @since 2.11.1
         */
        @Override
        CustomBiomeAppearance build();
    }
}
