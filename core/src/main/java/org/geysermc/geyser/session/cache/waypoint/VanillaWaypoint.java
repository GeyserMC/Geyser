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

package org.geysermc.geyser.session.cache.waypoint;

import org.checkerframework.common.returnsreceiver.qual.This;
import org.cloudburstmc.math.vector.Vector2f;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.api.waypoint.CustomWaypointStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record VanillaWaypoint(int nearDistance, int farDistance, List<String> textures) implements CustomWaypointStyle {
    // Default near and far distance: https://mcsrc.dev/1/26.1.2/net/minecraft/client/resources/WaypointStyle#L15-16
    // Default textures from bedrock's resourcepack (26.20)
    public static CustomWaypointStyle VANILLA_DEFAULT = new Builder(128, 332)
        .withTexture("ui/locator_bar_dot_0")
        .withTexture("ui/locator_bar_dot_1")
        .withTexture("ui/locator_bar_dot_2")
        .withTexture("ui/locator_bar_dot_3")
        .build();

    // These are the sizes vanilla BDS uses (26.20)
    private static final Vector2f MEDIUM_WAYPOINT_ICON_SIZE = Vector2f.from(0.6F, 0.6F);
    private static final Vector2f SMALL_WAYPOINT_ICON_SIZE = Vector2f.from(0.2F, 0.2F);

    // inspired by: https://mcsrc.dev/1/26.1.2/net/minecraft/client/resources/WaypointStyle#L45-59
    @Override
    public String getTexturePath(GeyserConnection connection, Identifier style, float distance) {
        if (distance < nearDistance) {
            return textures.getFirst();
        } else if (distance >= farDistance) {
            return textures.getLast();
        } else if (textures.size() == 1) {
            return textures.getFirst();
        } else if (textures.size() == 3) {
            return textures.get(1);
        } else {
            int index = lerpInt((distance - nearDistance) / (farDistance - nearDistance), 1, textures.size() - 1);
            return textures.get(index);
        }
    }

    @Override
    public Vector2f getTextureSize(GeyserConnection connection, Identifier style, float distance) {
        if (distance < nearDistance) {
            return Vector2f.ONE;
        } else if (distance >= farDistance) {
            return SMALL_WAYPOINT_ICON_SIZE;
        }
        int index = lerpInt((distance - nearDistance) / (farDistance - nearDistance), 1, textures.size() - 1);
        // TODO check me
        return index <= (textures.size() - 2) / 2 ? Vector2f.ONE : MEDIUM_WAYPOINT_ICON_SIZE;
    }

    // inspired by: https://mcsrc.dev/1/26.1.2/net/minecraft/util/Mth#L523-525
    private static int lerpInt(final float alpha1, final int p0, final int p1) {
        return p0 + (int) Math.floor(alpha1 * (p1 - p0));
    }

    public static class Builder implements CustomWaypointStyle.VanillaBuilder {
        private final int nearDistance;
        private final int farDistance;
        private final List<String> textures = new ArrayList<>();

        public Builder(int nearDistance, int farDistance) {
            if (nearDistance < 0.0F) {
                throw new IllegalArgumentException("nearDistance may not be below 0.0");
            } else if (nearDistance >= farDistance) {
                throw new IllegalArgumentException("nearDistance may not be equal or above farDistance");
            }
            this.nearDistance = nearDistance;
            this.farDistance = farDistance;
        }

        @Override
        public CustomWaypointStyle.@This VanillaBuilder withTexture(String texture) {
            Objects.requireNonNull(texture, "texture may not be null");
            textures.add(texture);
            return this;
        }

        @Override
        public CustomWaypointStyle build() {
            if (textures.isEmpty()) {
                throw new IllegalArgumentException("Waypoint style must have at least one texture");
            }
            return new VanillaWaypoint(nearDistance, farDistance, List.copyOf(textures));
        }
    }
}

