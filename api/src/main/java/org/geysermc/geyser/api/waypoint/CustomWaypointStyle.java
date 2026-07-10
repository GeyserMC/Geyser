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

package org.geysermc.geyser.api.waypoint;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.cloudburstmc.math.vector.Vector2f;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.event.connection.SessionDefineCustomWaypointsEvent;
import org.geysermc.geyser.api.util.GenericBuilder;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Custom waypoint styles apply to icons/players (known as waypoints) on the locator bar on the client.
 * Java Edition allows defining custom waypoint styles for an {@link Identifier} in a resourcepack,
 * and Bedrock Edition allows specifying custom textures for waypoints through packets.
 *
 * <p>This interface may be implemented by developers if they wish to display custom textures on the locator
 * bar on Bedrock Edition. Implementations must always implement both {@link CustomWaypointStyle#texturePath(Identifier, float)} and
 * {@link CustomWaypointStyle#textureSize(Identifier, float)}, and should be registered for one or more waypoint style identifiers
 * in the {@link SessionDefineCustomWaypointsEvent}. For implementation details, see the Javadocs at the respective methods.</p>
 *
 * <p>If you wish to define a custom waypoint style that acts like custom waypoint styles on Java Edition, use {@link CustomWaypointStyle#vanillaLike(int, int)}.</p>
 *
 * @see VanillaBuilder
 * @see SessionDefineCustomWaypointsEvent
 * @since 2.11.0
 */
public interface CustomWaypointStyle {

    /**
     * Returns the path of the texture to display on the locator bar for a waypoint with this {@code style} identifier, at the specified {@code distance}.
     * This method will be called whenever the position of a waypoint changes.
     *
     * <p>Please note that the {@code distance} parameter may not represent an accurate distance between the waypoint and the player:</p>
     *
     * <ul>
     *     <li>For {@code coordinate} waypoints, the accuracy will be within 1 block.</li>
     *     <li>For {@code chunk} waypoints, the accuracy will be within 8 blocks.</li>
     *     <li>For {@code azimuth} waypoints, the distance is fixed at {@link Integer#MAX_VALUE}.</li>
     * </ul>
     *
     * @implSpec
     * Implementations should not prefix the path with {@code "textures/"}, as this is done by Geyser. Implementations should never
     * return null, and upon doing so, Geyser will log a warning and use the {@code "textures/ui/locator_bar_dot_0"} texture as fallback.
     *
     * @param style the {@link Identifier} of the waypoint's custom style on Java Edition
     * @param distance the distance between the {@link GeyserConnection} and the waypoint
     * @return the path of the texture to display on the locator bar
     * @see CustomWaypointStyle#textureSize(Identifier, float)
     * @since 2.11.0
     */
    String texturePath(Identifier style, float distance);

    /**
     * Returns the size of the texture to display on the locator bar for a waypoint with this {@code style} identifier, at the specified {@code distance}.
     * This method will be called whenever the position of a waypoint changes.
     *
     <p>Please note that the {@code distance} parameter may not represent an accurate distance between the waypoint and the player:</p>
     *
     * <ul>
     *     <li>For {@code coordinate} waypoints, the accuracy will be within 1 block.</li>
     *     <li>For {@code chunk} waypoints, the accuracy will be within 8 blocks.</li>
     *     <li>For {@code azimuth} waypoints, the distance is fixed at {@link Integer#MAX_VALUE}.</li>
     * </ul>
     *
     * @implSpec
     * Implementations should return a reasonable texture size. None of the returned vector's components may ever be below 0 - when this happens,
     * Geyser will log a warning and use [0;0] as fallback.
     *
     * @param style the {@link Identifier} of the waypoint's custom style on Java Edition
     * @param distance the distance between the {@link GeyserConnection} and the waypoint
     * @return the size of the texture to display on the locator bar
     * @see CustomWaypointStyle#texturePath(Identifier, float)
     * @since 2.11.0
     */
    Vector2f textureSize(Identifier style, float distance);

    /**
     * This builder can be used to create an implementation of {@link CustomWaypointStyle} that acts like the vanilla Java Edition
     * custom waypoint styles.
     *
     * <p>When creating an instance of this builder, a {@code nearDistance} and {@code farDistance} have to be specified,
     * which act the same as their Java Edition counterparts. Then, 1 or multiple textures must be added. The builder uses these textures as follows:</p>
     *
     * <ul>
     *     <li>The first texture is shown for any distance below the {@code nearDistance}.</li>
     *     <li>The last texture is shown for any distance at or above the {@code farDistance}.</li>
     *     <li>Any textures between the first and last are spaced out equally between {@code nearDistance} and {@code farDistance}, and will be used as such.</li>
     * </ul>
     *
     * <p>This behaviour matches the Java Edition custom waypoint styles, which also allow specifying a list of textures. For convenience,
     * a shorthand {@link VanillaBuilder#withTexture(Identifier)} method exists, which takes a texture {@link Identifier} and turns it into a texture string.</p>
     *
     * @see VanillaBuilder#withTexture(Identifier)
     * @see VanillaBuilder#withTexture(String)
     * @see CustomWaypointStyle#vanillaLike(int, int)
     * @since 2.11.0
     */
    @ApiStatus.NonExtendable
    interface VanillaBuilder extends GenericBuilder<CustomWaypointStyle> {

        /**
         * Adds a new texture to this {@link CustomWaypointStyle} in the form of a texture {@link Identifier}.
         *
         * <p>This {@link Identifier} is turned into a texture string using the following format: {@code "ui/<namespace>/locator_bar_dot/<path>"}.</p>
         *
         * @param texture the texture to add
         * @return this builder
         * @since 2.11.0
         */
        @This
        default VanillaBuilder withTexture(Identifier texture) {
            Objects.requireNonNull(texture, "texture may not be null");
            return withTexture("ui/" + texture.namespace() + "/locator_bar_dot/" + texture.path());
        }

        /**
         * Adds a new texture to this {@link CustomWaypointStyle}.
         *
         * @param texture the texture to add
         * @return this builder
         * @since 2.11.0
         */
        @This
        VanillaBuilder withTexture(String texture);

        /**
         * Creates an implementation of {@link CustomWaypointStyle}, using the specified {@code nearDistance} and {@code farDistance}, and the textures added to this builder.
         *
         * @return the created {@link CustomWaypointStyle}
         * @throws IllegalArgumentException when no textures were added to this builder
         * @since 2.11.0
         */
        @Override
        CustomWaypointStyle build();
    }

    /**
     * Creates a new {@link VanillaBuilder} with the given {@code nearDistance} and {@code farDistance} values.
     *
     * <p>In Java Edition, the default value for {@code nearDistance} is {@code 128}, and for {@code farDistance} is {@code 332}.</p>
     *
     * <p>Please see {@link VanillaBuilder} for a throughout description of {@link VanillaBuilder}s and the {@code nearDistance} and {@code farDistance} values.</p>
     *
     * @param nearDistance the nearDistance to be used
     * @param farDistance the farDistance to be used
     * @return the created {@link VanillaBuilder}
     * @throws IllegalArgumentException when the {@code nearDistance} is below 0, or at or above the {@code farDistance}
     * @see VanillaBuilder
     * @since 2.11.0
     */
    static CustomWaypointStyle.VanillaBuilder vanillaLike(@NonNegative int nearDistance, @Positive int farDistance) {
        return GeyserApi.api().provider(VanillaBuilder.class, nearDistance, farDistance);
    }

    /**
     * Creates a new {@link VanillaBuilder} with the default values of {@code 128} for {@code nearDistance} and {@code 332} for {@code far}.
     *
     * <p>Please see {@link VanillaBuilder} for a throughout description of {@link VanillaBuilder}s and the {@code nearDistance} and {@code farDistance} values.</p>
     *
     * @return the created {@link VanillaBuilder}
     * @throws IllegalArgumentException when the {@code nearDistance} is below 0, or at or above the {@code farDistance}
     * @see VanillaBuilder
     * @see CustomWaypointStyle#vanillaLike(int, int)
     * @since 2.11.0
     */
    static CustomWaypointStyle.VanillaBuilder vanillaLike() {
        return vanillaLike(128, 332);
    }
}
