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
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Defines a custom Bedrock biome. Geyser registers the definition with the Bedrock client,
 * and uses it when translating chunks that contain the Java biome it was registered for.
 * Java biomes that have neither a definition nor a vanilla equivalent are translated to a
 * vanilla biome fitting the dimension instead.
 *
 * <p>Base climate values are taken from the Java biome the definition is registered for,
 * so rain and snow follow the server's climate. Java behavior that varies with the
 * position inside one biome, such as mountain snow lines, is approximated. Visuals can be
 * set in the optional {@link CustomBiomeAppearance}, which Geyser delivers to clients in
 * a generated resource pack.</p>
 *
 * @since 2.11.1
 */
@ApiStatus.NonExtendable
public interface CustomBiomeDefinition {

    /**
     * The Bedrock identifier of this biome. Namespace and path may only contain lowercase
     * letters, digits, {@code .}, {@code _} and {@code -}; unlike Java identifiers, the
     * path cannot contain {@code /}. The {@code minecraft} namespace and the
     * {@code geyser:auto_} prefix are reserved.
     *
     * @return the Bedrock biome identifier
     * @since 2.11.1
     */
    Identifier bedrockIdentifier();

    /**
     * The Bedrock biome tags of this biome, passed through to the Bedrock definition.
     * Data-driven content, such as spawn rules, matches them with the
     * {@code has_biome_tag} filter. A tag may only contain lowercase letters, digits,
     * {@code .} and {@code _}, with at most one {@code :} separating a namespace, and
     * cannot start with {@code minecraft:}.
     *
     * @return an immutable set of the biome's Bedrock tags
     * @since 2.11.1
     */
    Set<String> tags();

    /**
     * The visual appearance of this biome, or null when Geyser should not generate
     * appearance assets for it. A resource pack supplied by the server owner can still
     * style the biome.
     *
     * @return the biome's appearance
     * @since 2.11.1
     */
    @Nullable CustomBiomeAppearance appearance();

    /**
     * Creates a builder for a custom biome definition.
     *
     * @param bedrockIdentifier the Bedrock identifier of the biome
     * @return a new definition builder
     * @since 2.11.1
     */
    static Builder builder(Identifier bedrockIdentifier) {
        return GeyserApi.api().provider(Builder.class, bedrockIdentifier);
    }

    /**
     * The builder for a custom biome definition.
     * @since 2.11.1
     */
    interface Builder extends GenericBuilder<CustomBiomeDefinition> {

        /**
         * Adds a Bedrock biome tag.
         *
         * @param tag the tag to add
         * @see CustomBiomeDefinition#tags()
         * @return this builder
         * @since 2.11.1
         */
        @This
        Builder tag(String tag);

        /**
         * Sets the biome's visual appearance.
         *
         * @param appearance the biome appearance
         * @see CustomBiomeDefinition#appearance()
         * @return this builder
         * @since 2.11.1
         */
        @This
        Builder appearance(CustomBiomeAppearance appearance);

        /**
         * Convenience method for {@link CustomBiomeDefinition.Builder#appearance(CustomBiomeAppearance)}.
         *
         * @param appearance the builder of the biome appearance
         * @see CustomBiomeDefinition.Builder#appearance(CustomBiomeAppearance)
         * @return this builder
         * @since 2.11.1
         */
        @This
        default Builder appearance(CustomBiomeAppearance.Builder appearance) {
            return appearance(appearance.build());
        }

        /**
         * Creates the custom biome definition.
         *
         * @return the created definition
         * @throws IllegalArgumentException when the identifier or a tag is invalid, or
         * when the appearance was not created through {@link CustomBiomeAppearance#builder()}
         * @since 2.11.1
         */
        @Override
        CustomBiomeDefinition build();
    }
}
