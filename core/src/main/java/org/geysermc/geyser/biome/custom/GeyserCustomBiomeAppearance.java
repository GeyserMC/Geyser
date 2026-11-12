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

package org.geysermc.geyser.biome.custom;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.api.biome.custom.CustomBiomeAppearance;
import org.geysermc.geyser.api.biome.custom.CustomBiomePrecipitation;

import java.awt.Color;
import java.util.Objects;

@EqualsAndHashCode
@ToString
public final class GeyserCustomBiomeAppearance implements CustomBiomeAppearance {
    private final @Nullable Color skyColor;
    private final @Nullable Color fogColor;
    private final @Nullable Color waterSurfaceColor;
    private final @Nullable Float waterSurfaceOpacity;
    private final @Nullable Color waterFogColor;
    private final @Nullable Float waterFogEndDistance;
    private final @Nullable Color grassColor;
    private final @Nullable Color foliageColor;
    private final @Nullable Color dryFoliageColor;
    private final @Nullable CustomBiomePrecipitation precipitation;

    public GeyserCustomBiomeAppearance(Builder builder) {
        if (builder.isEmpty()) {
            throw new IllegalArgumentException("A biome appearance must set at least one value");
        }
        if (builder.waterFogEndDistance != null
                && (!Float.isFinite(builder.waterFogEndDistance) || builder.waterFogEndDistance < 0.0F)) {
            throw new IllegalArgumentException(
                "Water fog end distance must be finite and non-negative, got " + builder.waterFogEndDistance);
        }
        if (builder.waterSurfaceOpacity != null && (!Float.isFinite(builder.waterSurfaceOpacity)
                || builder.waterSurfaceOpacity < 0.0F || builder.waterSurfaceOpacity > 1.0F)) {
            throw new IllegalArgumentException(
                "Water surface opacity must be between 0.0 and 1.0, got " + builder.waterSurfaceOpacity);
        }
        if (builder.precipitation != null && !(builder.precipitation instanceof GeyserCustomBiomePrecipitation)) {
            throw new IllegalArgumentException("The precipitation was not created with CustomBiomePrecipitation.of()");
        }

        this.skyColor = rgb(builder.skyColor);
        this.fogColor = rgb(builder.fogColor);
        this.waterSurfaceColor = rgb(builder.waterSurfaceColor);
        this.waterSurfaceOpacity = builder.waterSurfaceOpacity;
        this.waterFogColor = rgb(builder.waterFogColor);
        this.waterFogEndDistance = builder.waterFogEndDistance;
        this.grassColor = rgb(builder.grassColor);
        this.foliageColor = rgb(builder.foliageColor);
        this.dryFoliageColor = rgb(builder.dryFoliageColor);
        this.precipitation = builder.precipitation;
    }

    // Appearance colors are RGB only; snapshot them without alpha so it can't affect equality
    private static @Nullable Color rgb(@Nullable Color color) {
        return color == null ? null : new Color(color.getRGB() & 0xFFFFFF);
    }

    /**
     * Reads the appearance of a Java biome from its registry data, using the same
     * {@code effects} and {@code attributes} keys a biome mappings file does. Values that
     * aren't plain, such as attribute modifiers, are skipped.
     *
     * @return the appearance, or null when the biome sets none of the values
     */
    public static @Nullable CustomBiomeAppearance fromJavaBiome(NbtMap data) {
        Builder builder = new Builder();
        NbtMap effects = data.getCompound("effects");
        builder.waterSurfaceColor = color(effects.get("water_color"));
        builder.grassColor = color(effects.get("grass_color"));
        builder.foliageColor = color(effects.get("foliage_color"));
        builder.dryFoliageColor = color(effects.get("dry_foliage_color"));

        NbtMap attributes = data.getCompound("attributes");
        builder.skyColor = color(attributes.get("minecraft:visual/sky_color"));
        builder.fogColor = color(attributes.get("minecraft:visual/fog_color"));
        builder.waterFogColor = color(attributes.get("minecraft:visual/water_fog_color"));
        if (attributes.get("minecraft:visual/water_fog_end_distance") instanceof Number distance
                && Float.isFinite(distance.floatValue()) && distance.floatValue() >= 0.0F) {
            builder.waterFogEndDistance = distance.floatValue();
        }

        return builder.isEmpty() ? null : builder.build();
    }

    // Java sends colors as #rrggbb strings
    private static @Nullable Color color(@Nullable Object value) {
        if (value instanceof String string && string.length() == 7 && string.startsWith("#")) {
            try {
                return new Color(Integer.parseInt(string.substring(1), 16));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    @Override
    public @Nullable Color skyColor() {
        return skyColor;
    }

    @Override
    public @Nullable Color fogColor() {
        return fogColor;
    }

    @Override
    public @Nullable Color waterSurfaceColor() {
        return waterSurfaceColor;
    }

    @Override
    public @Nullable Float waterSurfaceOpacity() {
        return waterSurfaceOpacity;
    }

    @Override
    public @Nullable Color waterFogColor() {
        return waterFogColor;
    }

    @Override
    public @Nullable Float waterFogEndDistance() {
        return waterFogEndDistance;
    }

    @Override
    public @Nullable Color grassColor() {
        return grassColor;
    }

    @Override
    public @Nullable Color foliageColor() {
        return foliageColor;
    }

    @Override
    public @Nullable Color dryFoliageColor() {
        return dryFoliageColor;
    }

    @Override
    public @Nullable CustomBiomePrecipitation precipitation() {
        return precipitation;
    }

    public static class Builder implements CustomBiomeAppearance.Builder {
        private @Nullable Color skyColor;
        private @Nullable Color fogColor;
        private @Nullable Color waterSurfaceColor;
        private @Nullable Float waterSurfaceOpacity;
        private @Nullable Color waterFogColor;
        private @Nullable Float waterFogEndDistance;
        private @Nullable Color grassColor;
        private @Nullable Color foliageColor;
        private @Nullable Color dryFoliageColor;
        private @Nullable CustomBiomePrecipitation precipitation;

        private boolean isEmpty() {
            return skyColor == null && fogColor == null && waterSurfaceColor == null
                && waterSurfaceOpacity == null && waterFogColor == null
                && waterFogEndDistance == null && grassColor == null
                && foliageColor == null && dryFoliageColor == null
                && precipitation == null;
        }

        @Override
        public Builder skyColor(Color skyColor) {
            this.skyColor = Objects.requireNonNull(skyColor, "skyColor may not be null");
            return this;
        }

        @Override
        public Builder fogColor(Color fogColor) {
            this.fogColor = Objects.requireNonNull(fogColor, "fogColor may not be null");
            return this;
        }

        @Override
        public Builder waterSurfaceColor(Color waterSurfaceColor) {
            this.waterSurfaceColor = Objects.requireNonNull(waterSurfaceColor, "waterSurfaceColor may not be null");
            return this;
        }

        @Override
        public Builder waterSurfaceOpacity(float waterSurfaceOpacity) {
            this.waterSurfaceOpacity = waterSurfaceOpacity;
            return this;
        }

        @Override
        public Builder waterFogColor(Color waterFogColor) {
            this.waterFogColor = Objects.requireNonNull(waterFogColor, "waterFogColor may not be null");
            return this;
        }

        @Override
        public Builder waterFogEndDistance(float waterFogEndDistance) {
            this.waterFogEndDistance = waterFogEndDistance;
            return this;
        }

        @Override
        public Builder grassColor(Color grassColor) {
            this.grassColor = Objects.requireNonNull(grassColor, "grassColor may not be null");
            return this;
        }

        @Override
        public Builder foliageColor(Color foliageColor) {
            this.foliageColor = Objects.requireNonNull(foliageColor, "foliageColor may not be null");
            return this;
        }

        @Override
        public Builder dryFoliageColor(Color dryFoliageColor) {
            this.dryFoliageColor = Objects.requireNonNull(dryFoliageColor, "dryFoliageColor may not be null");
            return this;
        }

        @Override
        public Builder precipitation(CustomBiomePrecipitation precipitation) {
            this.precipitation = Objects.requireNonNull(precipitation, "precipitation may not be null");
            return this;
        }

        @Override
        public CustomBiomeAppearance build() {
            return new GeyserCustomBiomeAppearance(this);
        }
    }
}
