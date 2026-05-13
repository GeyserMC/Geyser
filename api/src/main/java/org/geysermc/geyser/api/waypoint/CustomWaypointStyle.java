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
import org.geysermc.geyser.api.util.GenericBuilder;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

public interface CustomWaypointStyle {

    String getTexturePath(GeyserConnection connection, Identifier style, float distance);

    Vector2f getTextureSize(GeyserConnection connection, Identifier style, float distance);

    @ApiStatus.NonExtendable
    interface VanillaBuilder extends GenericBuilder<CustomWaypointStyle> {

        @This
        default VanillaBuilder withTexture(Identifier texture) {
            Objects.requireNonNull(texture, "texture may not be null");
            return withTexture("ui/" + texture.namespace() + "/locator_bar_dot/" + texture.path());
        }

        @This
        VanillaBuilder withTexture(String texture);

        @Override
        CustomWaypointStyle build();
    }

    static CustomWaypointStyle vanillaLike(@NonNegative int nearDistance, @Positive int farDistance) {
        return GeyserApi.api().provider(VanillaBuilder.class, nearDistance, farDistance);
    }
}
