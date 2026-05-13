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

package org.geysermc.geyser.registry.loader;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomWaypointsEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.api.waypoint.CustomWaypointStyle;
import org.geysermc.geyser.session.cache.waypoint.VanillaWaypoint;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class WaypointStyleLoader implements RegistryLoader<Object, Map<Identifier, CustomWaypointStyle>> {
    private static final Identifier VANILLA_WAYPOINT_STYLE = Identifier.of("default");

    @Override
    public Map<Identifier, CustomWaypointStyle> load(Object ignored) {
        Map<Identifier, CustomWaypointStyle> styles = new Object2ObjectOpenHashMap<>();

        GeyserImpl.getInstance().eventBus().fire(new GeyserDefineCustomWaypointsEvent() {

            @Override
            public Map<Identifier, CustomWaypointStyle> customWaypointStyles() {
                return Collections.unmodifiableMap(styles);
            }

            @Override
            public void register(Identifier identifier, CustomWaypointStyle style) {
                Objects.requireNonNull(identifier, "identifier may not be null");
                Objects.requireNonNull(style, "style may not be null");
                if (styles.containsKey(identifier)) {
                    throw new IllegalArgumentException("Waypoint style with identifier "+ identifier + " was already registered");
                }
                styles.put(identifier, style);
            }
        });

        // Include the vanilla default waypoint style if it was not overridden
        if (!styles.containsKey(VANILLA_WAYPOINT_STYLE)) {
            styles.put(VANILLA_WAYPOINT_STYLE, VanillaWaypoint.VANILLA_DEFAULT);
        }

        return styles;
    }
}
